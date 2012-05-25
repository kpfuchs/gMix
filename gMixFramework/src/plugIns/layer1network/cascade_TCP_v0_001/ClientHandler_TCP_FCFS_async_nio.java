/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2012  Karl-Peter Fuchs
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package plugIns.layer1network.cascade_TCP_v0_001;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import framework.core.controller.SubImplementation;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.userDatabase.DatabaseEventListener;
import framework.core.userDatabase.User;
import framework.core.userDatabase.UserAttachment;
import framework.core.util.Util;


public class ClientHandler_TCP_FCFS_async_nio extends SubImplementation implements DatabaseEventListener {
	//TODO: add timeout for inactive users
	
	private int port; 
	private InetAddress bindAddress;
	private int backlog; 
	private int soTimeout;
	private int maxConnections;
	private Integer numberOfActiveConnections = 0;
	private Selector selector = null;
	private ServerSocketChannel serverSocketChannel;
	//private int queueBlockSize;
	
	private NIOLoop nioLoop;
	private ReplyThread replyThread;
	private Object sync = new Object();
	
	private int maxRequestLength;
	
	private int maxMessages;
	private int messageCounterReplies = 0;
	private volatile boolean replyThreadSleeping = false;
	
	// if data sent by a client is available, but the internal message queue is 
	// full, the data won't be read until space becomes available in the queue.
	// in that case, the SelectionKey indicating that data is available will be 
	// queued in this list and removed from the selector. the reason for 
	// removing them is to prevent unnecessary iteration of the nio-loop, when 
	// only readRequests are available, but the queue is full
	private LinkedList<SelectionKey> delayedReadRequestEvents =  new LinkedList<SelectionKey>();
	private LinkedList<Request> delayedRequests =  new LinkedList<Request>();
	private LinkedList<UserChannelData> usersWithRepliesReady = new LinkedList<UserChannelData>();
	
	
	@Override
	public void constructor() {
		this.bindAddress = settings.getPropertyAsInetAddress("GLOBAL_MIX_BIND_ADDRESS");
		this.port = settings.getPropertyAsInt("GLOBAL_MIX_BIND_PORT");
		this.backlog = settings.getPropertyAsInt("BACKLOG");
		this.soTimeout = settings.getPropertyAsInt("SO_TIMEOUT");
		this.maxConnections = settings.getPropertyAsInt("MAX_CONNECTIONS");
		this.maxRequestLength = settings.getPropertyAsInt("MAX_REQUEST_LENGTH");
		this.maxMessages = settings.getPropertyAsInt("MAX_MESSAGES_IN_IOH");
		//this.queueBlockSize = settings.getPropertyAsInt("QUEUE_BLOCK_SIZE");
		
		anonNode.getUserDatabase().registerEventListener(this);
		//InfoServiceServer.mixAddresses[mix.getIdentifier()] = bindAddress;
		//InfoServiceServer.mixPorts[mix.getIdentifier()] = port;
		this.nioLoop = new NIOLoop();
		if (anonNode.IS_DUPLEX)
			this.replyThread = new ReplyThread();
	}
	

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}
	

	@Override
	public void begin() {
		this.nioLoop.start();
		if (anonNode.IS_DUPLEX)
			this.replyThread.start();
	}
	

	private class NIOLoop extends Thread {
		
		@Override
		public void run() {
			
			openServerSocket();
			
			SelectionKey key = null;
			
			while (true) { // handle read, write and accept events
				
				try {
						
					registerWriteRequests();
					
					int remainingCapacity = anonNode.getRequestInputQueue().remainingCapacity();
					boolean requestsDelayed = delayedRequests.size() > 0;
					boolean requestQueueFull = remainingCapacity == 0;
						
					// try to hand over as many delayed requests to the recodingScheme as possible
					if (requestsDelayed &! requestQueueFull) {
						Iterator<Request> requests = delayedRequests.iterator();
						while (requests.hasNext()) {
							Request request = requests.next();
							requests.remove();
							anonNode.putInRequestInputQueue(request);
							remainingCapacity--;
							if (remainingCapacity == 0)
								break;
						}
					}
					
					System.out.println("###!!## c");
					boolean readRequestEventsDelayed = delayedReadRequestEvents.size() > 0;
					requestQueueFull = remainingCapacity == 0;
						
					// handle delayed read request events (read requests are delayed without reading the 
					// available data when the request queue is full. this prevents the clients from 
					// sending even more data (if the data isen't read from the buffer, clients can't 
					// send more data due to the tc-protocol characteristics))
					if (readRequestEventsDelayed &! requestQueueFull) {
							
						Iterator<SelectionKey> selectedKeys = delayedReadRequestEvents.iterator();
							
						while (selectedKeys.hasNext()) {
								
							key = selectedKeys.next();
							selectedKeys.remove();
								
							if (!key.isValid())
								continue;
								
							assert key.isReadable();
							assert key.attachment() != null;
							assert key.attachment() instanceof UserChannelData;
								
							UserChannelData userdata = (UserChannelData)key.attachment();
							
							if (!userdata.valid)
								continue;
							
							Vector<Request> requests = tryToReadRequests(userdata);
							int count = requests.size();
							for (int i=0; i<count; i++) 
								if (i<remainingCapacity) {
									anonNode.putInRequestInputQueue(requests.remove(0));
									remainingCapacity--;
								} else {
									Request r = requests.remove(0);
									delayedRequests.add(r);
									UserChannelData userData = r.getOwner().getAttachment(getThis(), UserChannelData.class);
									if (!userData.valid)
										continue;
						        	SelectionKey selectionkey = userData.socketChannel.keyFor(selector);
						        	selectionkey.interestOps(SelectionKey.OP_READ);
								}
								
							if (remainingCapacity == 0)
								break;
								
						}
								
					}
					
					// wait for event(s)
					selector.select();
					
					// handle new events
					Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
					boolean wakeUpReplyThread = false;
						
					while (selectedKeys.hasNext()) {
						key = selectedKeys.next();
						selectedKeys.remove();
							
						if (!key.isValid())
							continue;
							
						if (key.isAcceptable()) {
								
							handleAcceptRequest();
								
						} else if (key.isReadable()) {
							if (remainingCapacity == 0) { // don't read new data when queue is full. otherwise clients will send even more data
								delayedReadRequestEvents.add(key);
							} else { // read as much data ass possible; queue rest
								UserChannelData userdata = (UserChannelData)key.attachment();
								if (!userdata.valid)
									continue;
								Vector<Request> requests = tryToReadRequests(userdata);
								int count = requests.size();
								for (int i=0; i<count; i++) {
									if (i<remainingCapacity) {
										anonNode.putInRequestInputQueue(requests.remove(0));
										remainingCapacity--;
									} else {
										delayedRequests.add(requests.remove(0));
									}
								}
							}
							
						} else if (key.isWritable()) {
							synchronized (sync) {
								UserChannelData userdata = (UserChannelData)key.attachment();
								if (!userdata.valid)
									continue;
								int repliesWritten = tryToSendReplies(userdata);
								messageCounterReplies -= repliesWritten;
								if (repliesWritten > 0)
									wakeUpReplyThread = true;
							}
								
						}
						
					} // end while (handle new events)
					
					if (wakeUpReplyThread) {
						synchronized (sync) {
							if (replyThreadSleeping)
								sync.notifyAll();
						}
					}
					
				} catch (IOException e) {
					e.printStackTrace();
					if (key != null && key.attachment() instanceof UserChannelData) {
						removeUser((UserChannelData)key.attachment());
						key.cancel();
					}
					synchronized (sync) {
						if (replyThreadSleeping)
							sync.notifyAll();
					}
					continue;		
				}
		
			} // end while

		} // end run
	
	} // end NIOLoop


	private class ReplyThread extends Thread {
		
		@Override
		public void run() {
			
			while (true) { // wait for data from messageProcessor, store it in UserChannelData, add reference on UserChannelData in usersWithRepliesReady and wake up selector (which will send the data later)
				
				Reply[] reply = anonNode.getFromReplyOutputQueue(); // blocking method
				for (int i=0; i<reply.length; i++) {
					// block if message limit reached
					synchronized (sync) {
						while (messageCounterReplies >= maxMessages) {
							replyThreadSleeping = true;
							try {
								sync.wait();
							} catch (InterruptedException e) {
								e.printStackTrace();
								continue;
							}
							replyThreadSleeping = false;
						}
						UserChannelData userData = reply[i].getOwner().getAttachment(getThis(), UserChannelData.class);
						if (!userData.valid)
							continue;
						messageCounterReplies++;
						userData.dataToSend.add(ByteBuffer.wrap(Util.concatArrays(Util.intToByteArray(reply[i].getByteMessage().length), reply[i].getByteMessage())));
						usersWithRepliesReady.add(userData);
					}

					// wake up selector so it can send the data
					selector.wakeup();
				} 
			}
			
		}
		
	} // end ReplyThread

	
	private class UserChannelData extends UserAttachment {
		
		ByteBuffer receivedData;
		Vector<ByteBuffer> dataToSend = new Vector<ByteBuffer>();
		SocketChannel socketChannel;
		boolean requestLengthHeaderRead;
		boolean valid = true;
		
		
		public UserChannelData(User owner, Object callingInstance) {
			super(owner, callingInstance);
		}
		
		
		public void clear() {
			synchronized (sync) {
				messageCounterReplies -=  dataToSend.size();
			}
			receivedData.clear();
			dataToSend.clear();
			try {socketChannel.close();} catch (IOException e) {}
			socketChannel = null;
			valid = false;
		}
		
	} // end UserChannelData
	
	
	private void removeUser(UserChannelData userData) {
		userData.clear();
		synchronized (sync) {
			while(usersWithRepliesReady.remove(userData));
		}
		numberOfActiveConnections--;
		userDatabase.removeUser(userData.getOwner(), this);
		selector.wakeup();
	}
	
	
	private void openServerSocket() {
		
		try {
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.configureBlocking(false);
			ServerSocket serverSocket = serverSocketChannel.socket();
			InetSocketAddress endpoint = new InetSocketAddress(bindAddress, port);
			serverSocket.bind(endpoint, backlog);
			serverSocket.setSoTimeout(soTimeout);
			
			// generate selector
			selector = Selector.open(); 
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
			System.out.println(anonNode +" listening on " +bindAddress +":" +port);
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not open ServerSocket"); 
		}

	}
	
	
	/**
	 * Registers all write requests from <code>writeRequests</code> with 
	 * <code>selector</code>.
	 * 
	 * @see #writeRequests
	 * @see #selector
	 */
	private void registerWriteRequests() {
		
		synchronized(sync) {
		    
			Iterator<UserChannelData> users = usersWithRepliesReady.iterator();
	        	
	        while (users.hasNext()) { 
	        	UserChannelData userData = users.next();
	        	if (!userData.valid)
					continue;
	        	SelectionKey selectionkey = userData.socketChannel.keyFor(selector);
	        	selectionkey.interestOps(SelectionKey.OP_WRITE);
	        }
		        	
	        // delete old writeRequests
	        usersWithRepliesReady.clear();
	        
		}
		
	}
	
	
	/**
	 * Handles an accept request. Accepts connections until the maximum number 
	 * of connections is reached (see <code>numberOfActiveConnections</code>, 
	 * <code>maxConnections</code>). Generates <code>User</code> objects and 
	 * adds them to the <code>UserDatabase</code> (if connection accepted).
	 * 
	 * @throws IOException If an I/O error occurres.
	 */
	private void handleAcceptRequest() throws IOException {
		
		if (numberOfActiveConnections < maxConnections) {
				
			SocketChannel clientSocketChannel = serverSocketChannel.accept();
			clientSocketChannel.configureBlocking(false);
			numberOfActiveConnections++;
			User user = userDatabase.generateUser();
			userDatabase.addUser(user, this);
			UserChannelData userData = new UserChannelData(user, this);
			userData.socketChannel = clientSocketChannel;
			clientSocketChannel.register(selector, SelectionKey.OP_READ, userData);
			
		} else {
			System.out.println(anonNode +" connection refused. too many connections ("+numberOfActiveConnections+")"); 
		}
		
	}


	private Vector<Request> tryToReadRequests(UserChannelData userData) throws IOException {
		Vector<Request> requests = new Vector<Request>();
		Request next = tryToReadRequest(userData);
		while (next != null) {
			requests.add(next);
			next = tryToReadRequest(userData);
		}
		return requests;

	}
	

	private Request tryToReadRequest(UserChannelData userData) throws IOException {
		
		if (userData.receivedData == null)
			userData.receivedData = ByteBuffer.allocate(4);
		
		// try to read header (containing the length of the message)
		if (!userData.requestLengthHeaderRead) {
			
			if (userData.receivedData.position() == 0)
				userData.receivedData.limit(4);
			
			if (userData.socketChannel.read(userData.receivedData) == -1) // read data
				throw new IOException("warning: lost connection to user " +userData.getOwner());
			
			if (userData.receivedData.hasRemaining()) {
				return null;
			} else {
				userData.receivedData.flip();
				byte[] lengthHeader = new byte[4];
				userData.receivedData.get(lengthHeader);
				int messageLength = Util.byteArrayToInt(lengthHeader);
				if (messageLength > maxRequestLength)
					throw new IOException("warning: user " +userData.getOwner() +" sent a too large message");
				userData.receivedData.clear();
				userData.receivedData = ByteBuffer.allocate(messageLength);
				userData.requestLengthHeaderRead = true;
			}
			
		} 
		
		// try to read the message itself
		assert userData.requestLengthHeaderRead == true;
		
		if (userData.socketChannel.read(userData.receivedData) == -1) // read data
			throw new IOException("warning: lost connection to user " +userData.getOwner());
		
		if (userData.receivedData.hasRemaining()) {
			return null;
		} else {
			userData.receivedData.flip();
			byte[] message = userData.receivedData.array();
			userData.receivedData.clear();
			userData.receivedData = null;
			userData.requestLengthHeaderRead = false;
			return MixMessage.getInstanceRequest(message, userData.getOwner());
		}
		
	}
	
	
	private int tryToSendReplies(UserChannelData userData) throws IOException {
		int repliesSent = 0;
		while(tryToSendReply(userData))
			repliesSent++;
		return repliesSent;
	}
	
	
	private boolean tryToSendReply(UserChannelData userData) throws IOException {
		
		ByteBuffer messageToSend = userData.dataToSend.get(0);
		int written = userData.socketChannel.write(messageToSend);
		assert written != 0;
		if (messageToSend.hasRemaining()) {
			return false;
		} else {
			userData.dataToSend.remove(0);
			return true;
		}

	}
	
	
	@Override
	public void userAdded(User user) {
		// no need to do anything
	}


	@Override
	public void userRemoved(User user) {
		UserChannelData userData = user.getAttachment(getThis(), UserChannelData.class);
		userData.clear();
		synchronized (sync) {
			while(usersWithRepliesReady.remove(userData));
		}
		numberOfActiveConnections--;
		selector.wakeup();
	}

	
	private ClientHandler_TCP_FCFS_async_nio getThis() {
		return this;
	}
	
}
