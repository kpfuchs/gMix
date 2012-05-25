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
import java.util.concurrent.LinkedBlockingQueue;

import framework.core.controller.SubImplementation;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.userDatabase.DatabaseEventListener;
import framework.core.userDatabase.User;
import framework.core.userDatabase.UserAttachment;
import framework.core.util.Util;


public class ClientHandler_TCP_FCFS_async_nio2 extends SubImplementation implements DatabaseEventListener {
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
	private RequestSender requestSender;
	private ReplyFetcher replyFetcher;
	private ReplySender replySender;
	
	private LinkedBlockingQueue<SelectionKey> dataAvailableToReadEvents;
	private LinkedBlockingQueue<SelectionKey> dataAvailableToWriteEvents;
	private LinkedList<UserChannelData> usersWithRepliesReady;
	private Object sync = new Object();
	
	private int maxRequestLength;
	private int maxMessages;
	private volatile int messageCounterReplies = 0;
	private volatile boolean replyFetcherSleeping = false;
	
	
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
		
		this.userDatabase.registerEventListener(this);
		
		this.dataAvailableToReadEvents = new LinkedBlockingQueue<SelectionKey>();
		this.dataAvailableToWriteEvents = new LinkedBlockingQueue<SelectionKey>();
		this.usersWithRepliesReady = new LinkedList<UserChannelData>();
		
		this.nioLoop = new NIOLoop();
		this.requestSender = new RequestSender();
		if (anonNode.IS_DUPLEX) {
			this.replyFetcher = new ReplyFetcher();
			this.replySender = new ReplySender();
		}
	}
	

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}
	

	@Override
	public void begin() {
		this.nioLoop.start();
		this.requestSender.start();
		if (anonNode.IS_DUPLEX) {
			this.replyFetcher.start();
			this.replySender.start();
		}
	}
	
	
	private class NIOLoop extends Thread {
		
		@Override
		public void run() {
			
			openServerSocket();
			
			SelectionKey key = null;
			
			while (true) { // handle read, write and accept events
				/*System.out.println("NIOLoop-Status: ");
				System.out.println("   usersWithRepliesReady.size(): " +usersWithRepliesReady.size()); 
				System.out.println("   dataAvailableToReadEvents.size(): " +dataAvailableToReadEvents.size()); 
				System.out.println("   dataAvailableToWriteEvents.size(): " +dataAvailableToWriteEvents.size()); 
				System.out.println("   numberOfActiveConnections: " +numberOfActiveConnections); 
				System.out.println("   messageCounterReplies: " +messageCounterReplies);
				*/
				try {
					// notify selector about replies ready to be sent (to clients)
					synchronized (sync) {
					    Iterator<UserChannelData> users = usersWithRepliesReady.iterator();
				        while (users.hasNext()) { 
				        	UserChannelData userData = users.next();
				        	if (!userData.valid)
								continue;
				        	SelectionKey selectionkey = userData.socketChannel.keyFor(selector);
				        	selectionkey.interestOps(SelectionKey.OP_WRITE);
				        }
					    // delete old writeRequests
				        if (usersWithRepliesReady.size() > 0) {
				        	usersWithRepliesReady.clear();
						}
					}
					
					// wait for event(s)
					selector.select();
					
					// handle events
					Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
					while (selectedKeys.hasNext()) {
						key = selectedKeys.next();
						selectedKeys.remove();
						if (!key.isValid())
							continue;
						if (key.isAcceptable()) {
							handleAcceptRequest();
						} else if (key.isReadable()) {
							dataAvailableToReadEvents.put(key);
						} else if (key.isWritable()) {
							dataAvailableToWriteEvents.put(key);
						}
					}
					
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				} catch (IOException e) {
					e.printStackTrace();
					if (key != null && key.attachment() instanceof UserChannelData) {
						removeUser((UserChannelData)key.attachment());
						key.cancel();
					}
					if (replyFetcher != null)
						replyFetcher.wakeup();
					continue;		
				}
				
			}

		}
	
	} // end NIOLoop


	private class ReplyFetcher extends Thread {
		
		@Override
		public void run() {
			
			while (true) { // wait for data from recoding scheme, store it in UserChannelData, add reference on UserChannelData in usersWithRepliesReady and wake up selector (which will send the data later)
				
				Reply[] reply = anonNode.getFromReplyOutputQueue(); // blocks until a reply is available
				for (int i=0; i<reply.length; i++) {
					// block if message limit reached
					synchronized (sync) {
						while (messageCounterReplies >= maxMessages) {
							replyFetcherSleeping = true;
							try {
								sync.wait();
							} catch (InterruptedException e) {
								e.printStackTrace();
								continue;
							}
							replyFetcherSleeping = false;
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
		
		
		public void wakeup() {
			synchronized (sync) {
				if (replyFetcherSleeping)
					sync.notifyAll();
			}
		}
		
	} // end ReplyFetcher

	
	private class ReplySender extends Thread {
		
		@Override
		public void run() {
			
			SelectionKey key = null;
			
			while (true) {
				try {
					// wait for data from useres
					key = dataAvailableToWriteEvents.take(); // might block
					synchronized (sync) {
						UserChannelData userdata = (UserChannelData)key.attachment();
						if (!userdata.valid)
							continue;
						int repliesWritten = tryToSendReplies(userdata);
						messageCounterReplies -= repliesWritten;
						if (repliesWritten > 0)
							replyFetcher.wakeup();
						
						if (userdata.dataToSend.size() == 0)
							key.interestOps(SelectionKey.OP_READ);
					}
					
					
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				} catch (IOException e) {
					e.printStackTrace();
					if (key != null && key.attachment() instanceof UserChannelData) {
						removeUser((UserChannelData)key.attachment());
						key.cancel();
					}
					replyFetcher.wakeup();
					continue;
				}
			}
			
		}
		
	}
	
	
	private class RequestSender extends Thread {
		
		@Override
		public void run() {
			
			SelectionKey key = null;
			
			while (true) {
				
				try {
					// wait for data from useres
					key = dataAvailableToReadEvents.take(); // might block
					
					// convert received data to Request-objects (if enough data available)
					Vector<Request> requests;
					UserChannelData userdata = (UserChannelData)key.attachment();
					if (!userdata.valid)
						continue;
					requests = tryToReadRequests(userdata);
					// make data available to recoding scheme via inputOutputHandlerInternal
					anonNode.putInRequestInputQueue(requests.toArray(new Request[0])); // might block
					
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				} catch (IOException e) {
					e.printStackTrace();
					if (key != null && key.attachment() instanceof UserChannelData) {
						removeUser((UserChannelData)key.attachment());
						key.cancel();
					}
					if (replyFetcher != null)
						replyFetcher.wakeup();
					continue;
				}
			}
		}
		
	} // end RequestSender
	
	
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
		if (userData.dataToSend.size() == 0)
			return false;
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

	
	private ClientHandler_TCP_FCFS_async_nio2 getThis() {
		return this;
	}
	
}
