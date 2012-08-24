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
package plugIns.layer1network.sourceRouting_TCP_v0_001;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import framework.core.controller.SubImplementation;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.routing.RoutingMode;
import framework.core.userDatabase.User;
import framework.core.userDatabase.UserAttachment;
import framework.core.util.Util;


public class ClientHandler_TCP_RR_sync extends SubImplementation implements ClientReplyReceiver {
	//TODO: add timeout for inactive users
	
	private int port; 
	private InetAddress bindAddress;
	private int backlog;
	private ServerSocket serverSocket;
	private int maxRequestLength;
	//private int maxReplyLength; // TODO
	private Vector<ChannelData> channels;
	private Vector<ChannelData> newConnections;
	private int expectedConnections;
	private int requestBufferSize;
	private int replyBufferSize;
	private Object attachmentKey = new Object();
	//private int queueBlockSize;
	
	// for connection based mixes:
	private ConnectionBasedAcceptorThread acceptorThread;
	private ConnectionBasedRequestThread requestThread;
	private ConnectionBasedReplyThread replyThread;
	
	// for message-based (none-connection-based) mixes:
	private MessageReceiverThread receiverThread;
	private MessageSenderThread senderThread;
	
	private ClientReplyProvider clientReplyProvider;
	
	
	@Override
	public void constructor() {
		if (anonNode.ROUTING_MODE == RoutingMode.CASCADE)
			throw new RuntimeException("this is a free route plug-in; ROUTING_MODE is set to CASCADE -> will exit now"); 
		this.bindAddress = settings.getPropertyAsInetAddress("GLOBAL_MIX_BIND_ADDRESS");
		this.port = settings.getPropertyAsInt("GLOBAL_MIX_BIND_PORT");
		this.backlog = settings.getPropertyAsInt("BACKLOG");
		this.maxRequestLength = settings.getPropertyAsInt("MAX_REQUEST_LENGTH");
		//this.maxReplyLength = settings.getPropertyAsInt("MAX_REPLY_LENGTH");
		this.requestBufferSize = settings.getPropertyAsInt("REQUEST_BUFFER_SIZE");
		this.replyBufferSize = settings.getPropertyAsInt("REPLY_BUFFER_SIZE");
		this.expectedConnections = anonNode.EXPECTED_NUMBER_OF_USERS;
		//this.queueBlockSize = settings.getPropertyAsInt("QUEUE_BLOCK_SIZE");
		this.channels = new Vector<ChannelData>(expectedConnections);
		this.newConnections = new Vector<ChannelData>(100);
	}
	

	@Override
	public void initialize() {
		
	}
	

	@Override
	public void begin() {
		try {
			this.serverSocket = new ServerSocket(port, backlog, bindAddress);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not open ServerSocket"); 
		}
		System.out.println(anonNode +" listening on " +bindAddress +":" +port);
		
		if (anonNode.IS_CONNECTION_BASED) {
			this.acceptorThread = new ConnectionBasedAcceptorThread();
			this.requestThread = new ConnectionBasedRequestThread();
			if (anonNode.IS_DUPLEX)
				this.replyThread = new ConnectionBasedReplyThread();
			this.acceptorThread.start();
			this.requestThread.start();
			if (anonNode.IS_DUPLEX)
				this.replyThread.start();
		} else { // not connection-based
			this.receiverThread = new MessageReceiverThread();
			if (anonNode.IS_DUPLEX)
				this.senderThread = new MessageSenderThread();
			this.receiverThread.start();
			if (anonNode.IS_DUPLEX)
				this.senderThread.start();
		}
	}

	
	@Override
	public void setClientReplyProvider(ClientReplyProvider provider) {
		this.clientReplyProvider = provider;
	} 
	
	
	private class MessageReceiverThread extends Thread {
		
		@Override
		public void run() {
			while (true) {
				try {
					Socket client = serverSocket.accept();
					User user = userDatabase.generateUser();
					ChannelData channelData = new ChannelData(user);
					userDatabase.addUser(channelData.user);
					InputStream in = client.getInputStream();
					if (anonNode.IS_DUPLEX) {
						channelData.replyAddress = client.getInetAddress();
						channelData.replyPort = Util.forceReadShort(in);
					}
					int len = Util.forceReadInt(in);
					if (len > maxRequestLength) {
						System.err.println("warning: user " +user +" sent a too large message");
						client.close();
						continue;
					}
					byte[] msg = Util.forceRead(in, len);
					client.close();
					Request r = MixMessage.getInstanceRequest(msg, user);
					user.prevHopAddress = MixMessage.CLIENT;
					if (anonNode.DISPLAY_ROUTE_INFO)
						System.out.println("" +anonNode +" received message on layer1"); 
					anonNode.putInRequestInputQueue(r);
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
			}
		}
		
	}
	
	
	private class MessageSenderThread extends Thread {
		
		@Override
		public void run() {
			while (true) {
				Reply[] replies = anonNode.getFromReplyOutputQueue();
				for (Reply reply: replies) {
					try {
						assert reply != null;
						assert reply.getOwner() != null;
						//System.out.println("sende auf layer 0 fuer " +reply.getOwner().toString() +": " +Util.md5(reply.getByteMessage())); 
						ChannelData channel = reply.getOwner().getAttachment(attachmentKey, ChannelData.class);
						assert channel != null;
						Socket client = new Socket(channel.replyAddress, channel.replyPort);
						if (anonNode.DISPLAY_ROUTE_INFO)
							System.out.println("" +anonNode +" sending reply on layer 1 to client (" +channel.replyAddress +":" +channel.replyPort +")"); 
						client.getOutputStream().write(Util.intToByteArray(reply.getByteMessage().length));
						client.getOutputStream().write(reply.getByteMessage());
						client.getOutputStream().flush();
						client.close();
					} catch (IOException e) {
						System.err.println("warning: connection to " +reply.getOwner() +" lost");
						e.printStackTrace();
						continue;
					}
				}
			}
		}
		
	}
	
	
	private class ConnectionBasedAcceptorThread extends Thread {
		
		@Override
		public void run() {
			int counter = 0;
			while (true) {
				try {
					Socket client = serverSocket.accept();
					if (++counter%100 == 0)
						System.out.println(counter +" connections"); 
					User user = userDatabase.generateUser();
					ChannelData channelData = new ChannelData(user);
					userDatabase.addUser(channelData.user);
					channelData.socket = client;
					channelData.inputStream = new BufferedInputStream(client.getInputStream(), requestBufferSize);
					if (anonNode.IS_DUPLEX)
						channelData.outputStream = new BufferedOutputStream(client.getOutputStream(), replyBufferSize);
					synchronized (acceptorThread) {
						newConnections.add(channelData);
					}
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
			}
		}
		
	}
	
	
	private synchronized void dropChannel(ChannelData ch) {
		channels.remove(ch);
		try {
			ch.inputStream.close();
			ch.outputStream.close();
			ch.socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private class ConnectionBasedRequestThread extends Thread {
		
		@Override
		public void run() {
			int maxReadsPerChannelInARow = settings.getPropertyAsInt("MAX_READS_IN_A_ROW");
			int maxMessageBlockSize = anonNode.QUEUE_BLOCK_SIZE;
			
			while (true) {
				synchronized (acceptorThread) { // handle new connections
					if (newConnections.size() != 0) {
						channels.addAll(newConnections);
						newConnections = new Vector<ChannelData>(50); // TODO 50?
					}	
				}
				Vector<Vector<Request>> newRequests = null;
				Vector<Request> newRequestsForCurrentChannel = null;
				for (ChannelData ch:channels) {// try to read data from existing channels
					try {
						for (int i=0; i<maxReadsPerChannelInARow; i++) {
							if (ch.requestLength == ChannelData.NOT_SET) {
								if (ch.inputStream.available() >= 4) {
									byte[] len = new byte[4];
									int read = ch.inputStream.read(len);
									assert read == 4; // should not be different due to buffered stream; check anyways...
									ch.requestLength = Util.byteArrayToInt(len);
									if (ch.requestLength > maxRequestLength) {
										System.err.println("warning: user " +ch.user +" sent a too large message");
									}
								} else {
									break;
								}
							}
							if (ch.requestLength != ChannelData.NOT_SET) { // length header already read
								if (ch.inputStream.available() >= ch.requestLength) {
									byte[] msg = new byte[ch.requestLength];
									int read = ch.inputStream.read(msg);
									assert read == ch.requestLength; // should not be different due to buffered stream; check anyways...
									ch.requestLength = ChannelData.NOT_SET;
									Request r = MixMessage.getInstanceRequest(msg, ch.user);
									if (ch.user.prevHopAddress == Util.NOT_SET)
										ch.user.prevHopAddress = MixMessage.CLIENT;
									if (anonNode.DISPLAY_ROUTE_INFO)
										System.out.println("" +anonNode +" received message on layer1"); 
									if (newRequestsForCurrentChannel == null)
										newRequestsForCurrentChannel = new Vector<Request>(maxReadsPerChannelInARow);
									newRequestsForCurrentChannel.add(r);
								} else {
									break;
								}
							}
						}
						if (newRequestsForCurrentChannel != null) {
							if (newRequests == null)
								newRequests = new Vector<Vector<Request>>(maxMessageBlockSize);
							newRequests.add(newRequestsForCurrentChannel);
							newRequestsForCurrentChannel = null;
						}	
					} catch (IOException e) {
						e.printStackTrace();
						dropChannel(ch);
						continue;
					}
					
					if (newRequests != null && newRequests.size() >= maxMessageBlockSize) {
						for (Vector<Request> requests:newRequests)
							anonNode.putInRequestInputQueue(requests.toArray(new Request[0])); // might block
						newRequests = new Vector<Vector<Request>>(maxMessageBlockSize);
					}
				}
				if (newRequests == null) {
					try {Thread.sleep(1);} catch (InterruptedException e) {continue;} // TODO: 1 ms adequate?
					continue;
				}
				
				for (Vector<Request> requests:newRequests)
					anonNode.putInRequestInputQueue(requests.toArray(new Request[0])); // might block
			}
		}
	}
	
	
	private class ConnectionBasedReplyThread extends Thread {

		@Override
		public void run() {
			if (anonNode.NUMBER_OF_MIXES == 1) { // all replies are for clients
				while (true) {
					Reply[] replies = anonNode.getFromReplyOutputQueue();
					for (Reply reply: replies)
						writeReply(reply);
				}
			} else { // only replies not for other mixes are for clients (prevMixHandler decides)
				while (true)
					writeReply(clientReplyProvider.getReplyForClient());
			}
		}
		
		
		private void writeReply(Reply reply) {
			try {
				assert reply != null;
				assert reply.getOwner() != null;
				//System.out.println("sende auf layer 0 fuer " +reply.getOwner().toString() +": " +Util.md5(reply.getByteMessage())); 
				ChannelData channel = reply.getOwner().getAttachment(attachmentKey, ChannelData.class);
				assert channel != null;
				assert channel.inputStream != null;
				if (anonNode.DISPLAY_ROUTE_INFO)
					System.out.println("" +anonNode +" sending reply on layer 1 to client (" +channel.socket.getRemoteSocketAddress() +")"); 
				channel.outputStream.write(Util.intToByteArray(reply.getByteMessage().length));
				channel.outputStream.write(reply.getByteMessage());
				channel.outputStream.flush();
			} catch (IOException e) {
				System.err.println("warning: connection to " +reply.getOwner() +" lost");
				e.printStackTrace();
				System.exit(0);
				// TODO: disconnect etc
			}
		}
	}
	
	
	public class ChannelData extends UserAttachment {
		
		final static int NOT_SET = -2;
		BufferedInputStream inputStream;
		BufferedOutputStream outputStream;
		Socket socket;
		User user;
		int requestLength = NOT_SET;
		int replyLength = NOT_SET;
		
		InetAddress replyAddress;
		short replyPort;
		
		
		public ChannelData(User user) {
			super(user, attachmentKey);
			this.user = user;
		}

	}
	
}
