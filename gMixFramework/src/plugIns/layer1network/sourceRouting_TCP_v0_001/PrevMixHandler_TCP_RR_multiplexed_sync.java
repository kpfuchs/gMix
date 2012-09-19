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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;

import framework.core.controller.SubImplementation;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.userDatabase.DatabaseEventListener;
import framework.core.userDatabase.User;
import framework.core.util.Util;


public class PrevMixHandler_TCP_RR_multiplexed_sync extends SubImplementation implements DatabaseEventListener, ClientReplyProvider  {

	private int port; 
	private InetAddress bindAddress;
	private ServerSocket serverSocket;
	private int maxRequestLength;
	
	private HashMap<User,Integer> thisToPrevMixIDs;
	
	private int requestBufferSize;
	private int replyBufferSize;
	
	private RequestThread requestThread;
	private ReplyThread replyThread;
	private AcceptorThread acceptorThread;
	
	private HashMap<Integer, MixConnection> connectionsMap; // used by ReplyThread
	private Vector<MixConnection> connections; // used by RequestThread
	private Vector<MixConnection> newConnections; // used for synchronization between RequestThread and ReplyThread
	
	private ArrayBlockingQueue<Reply> repliesForClients;
	
	
	@Override
	public void constructor() {
		this.bindAddress = settings.getPropertyAsInetAddress("GLOBAL_MIX_BIND_ADDRESS");
		this.port = settings.getPropertyAsInt("GLOBAL_MIX_BIND_PORT") +1000;
		this.maxRequestLength = settings.getPropertyAsInt("MAX_REQUEST_LENGTH");
		this.thisToPrevMixIDs = new HashMap<User,Integer>((int)Math.round((double)settings.getPropertyAsInt("MAX_CONNECTIONS") * 1.3d));
		this.connectionsMap = new HashMap<Integer, MixConnection>();
		this.connections = new Vector<MixConnection>();
		this.newConnections = new Vector<MixConnection>();
		this.acceptorThread = new AcceptorThread();
		this.requestThread = new RequestThread();
		if (anonNode.IS_DUPLEX)
			this.replyThread = new ReplyThread();
		this.requestBufferSize = settings.getPropertyAsInt("MULTIPLEXED_REQUEST_BUFFER_SIZE");
		this.replyBufferSize = settings.getPropertyAsInt("MULTIPLEXED_REPLY_BUFFER_SIZE");
		this.repliesForClients = new ArrayBlockingQueue<Reply>(settings.getPropertyAsInt("GLOBAL_REPLY_OUTPUT_QUEUE_SIZE"));
	}
	

	@Override
	public void initialize() {
		
	}

	
	@Override
	public void begin() {
		this.acceptorThread.start();
		this.requestThread.start();
		if (anonNode.IS_DUPLEX)
			this.replyThread.start();
	}
	
	
	private class AcceptorThread extends Thread {
		
		@Override
		public void run() {
			try {
				serverSocket = new ServerSocket(port, 5, bindAddress);
				System.out.println(anonNode + " bound to " +bindAddress + ":" +port); 	
			} catch (IOException e) {
				System.out.println(anonNode + " couldn't bind socket " +bindAddress + ":" +port);
				e.printStackTrace();
				System.exit(1);
			}
			while (true) {
				try {
					Socket socket = serverSocket.accept();
					socket.setKeepAlive(true);
					MixConnection con = new MixConnection();
					con.previousMixInputStream = new BufferedInputStream(socket.getInputStream(), requestBufferSize);
					con.previousMixOutputStream = new BufferedOutputStream(socket.getOutputStream(), replyBufferSize);
					con.mixId = Util.forceReadInt(con.previousMixInputStream); // TODO: check certificate
					if (anonNode.mixList.getAddress(con.mixId) == null) { // TODO: reload (possibly updated) list from info-service...
						System.err.println("received connection from unknown host: " +socket.getInetAddress() +":" +socket.getPort());
						socket.close();
						continue;
					}
					synchronized (newConnections) {
						newConnections.add(con);
					}
					synchronized (connectionsMap) {
						connectionsMap.put(con.mixId, con);
					}
					System.out.println(anonNode +" connection accepted from " +socket.getInetAddress() +":" +socket.getPort());	
				} catch (IOException e) {
					System.err.println(anonNode +" connection attempt failed");
					e.printStackTrace();
					continue;	
				}
			}
		}
	}
	
	
	private class RequestThread extends Thread {
		
		
		@Override
		public void run() {
			
			int maxReadsPerChannelInARow = settings.getPropertyAsInt("MAX_READS_IN_A_ROW_MIX");
			int maxMessageBlockSize = anonNode.QUEUE_BLOCK_SIZE;
			
			while (true) { // receive messages from "prev" mixes
				
				synchronized (newConnections) { // handle new connections
					if (newConnections.size() != 0) {
						connections.addAll(newConnections);
						newConnections = new Vector<MixConnection>();
					}
				}
				
				Vector<Vector<Request>> newRequests = null;
				Vector<Request> newRequestsForCurrentConnection = null;
				for (MixConnection con:connections) {// try to read data from existing connections
					try {
						for (int i=0; i<maxReadsPerChannelInARow; i++) {
							if (con.currentUserIdentifier == Util.NOT_SET) { // try to read id
								if (con.previousMixInputStream.available() >= 4) {
									byte[] id = new byte[4];
									int read = con.previousMixInputStream.read(id);
									assert read == 4; // should not be different due to buffered stream; check anyways...
									con.currentUserIdentifier = Util.byteArrayToInt(id);
									User user = userDatabase.getUser(con.currentUserIdentifier);
									if (user == null) {
										user = userDatabase.generateUser(con.currentUserIdentifier);
										userDatabase.addUser(user);
										thisToPrevMixIDs.put(user, con.currentUserIdentifier);
										user.prevHopAddress = con.mixId;
									}
									con.currentUser = user;
								} else {
									break;
								}
							}
							if (con.currentRequestLength == Util.NOT_SET) { // try to read length-header
								if (con.previousMixInputStream.available() >= 4) {
									byte[] len = new byte[4];
									int read = con.previousMixInputStream.read(len);
									assert read == 4; // should not be different due to buffered stream; check anyways...
									con.currentRequestLength = Util.byteArrayToInt(len);
									if (con.currentRequestLength < 1 || con.currentRequestLength > maxRequestLength) {
										System.out.println(anonNode +" wrong size for request received"); 
										dropConncetion(con);
										continue;
									}
								} else {
									break;
								}
							}
							if (con.currentRequestLength != Util.NOT_SET) { // length header already read -> try to read message
								if (con.previousMixInputStream.available() >= con.currentRequestLength) {
									byte[] msg = new byte[con.currentRequestLength];
									int read = con.previousMixInputStream.read(msg);
									assert read == con.currentRequestLength; // should not be different due to buffered stream; check anyways...
									//System.err.println("next mix received: id: " +con.currentUserIdentifier +"; msg-length: " +con.currentRequestLength +"; msg: " +Util.md5(msg)); 
									
									con.currentUserIdentifier = Util.NOT_SET;
									con.currentRequestLength = Util.NOT_SET;
									Request r = MixMessage.getInstanceRequest(msg, con.currentUser);
									if (con.currentUser.prevHopAddress == Util.NOT_SET)
										con.currentUser.prevHopAddress = con.mixId;
									if (anonNode.DISPLAY_ROUTE_INFO)
										System.out.println("" +anonNode +" received request on layer 1 from prev mix (mix " +con.mixId +")"); 
									assert con.currentUser != null;
									if (newRequestsForCurrentConnection == null)
										newRequestsForCurrentConnection = new Vector<Request>(maxReadsPerChannelInARow);
									newRequestsForCurrentConnection.add(r);
								} else {
									break;
								}
							}
						}
						if (newRequestsForCurrentConnection != null) {
							if (newRequests == null)
								newRequests = new Vector<Vector<Request>>(maxMessageBlockSize);
							newRequests.add(newRequestsForCurrentConnection);
							newRequestsForCurrentConnection = null;
						}
					} catch (IOException e) {
						e.printStackTrace();
						dropConncetion(con);
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
					anonNode.putInRequestInputQueue(requests.toArray(new Request[0])); // might block				}
			}
		}
		
	}
	
	
	
	
	private class ReplyThread extends Thread {
		
		@Override
		public void run() {
			while (true) {
				Reply[] replies = anonNode.getFromReplyOutputQueue();
				MixConnection con = null;
				for (int i=0; i<replies.length; i++) {
					if (replies[i].nextHopAddress == MixMessage.CLIENT) {
						putInRepliesForClient(replies[i]);
						continue;
					}
					Integer id;
					synchronized (thisToPrevMixIDs) {
						id = thisToPrevMixIDs.get(replies[i].getOwner());
					}
					if (id == null) {
						System.err.println(anonNode +" no id for " +replies[i].getOwner() +" available" );
						continue;
					}	
					try {
						synchronized (connectionsMap) {
							con = connectionsMap.get(replies[i].nextHopAddress);
						}
						if (con == null) { // no connection to that mix
							System.err.println("received reply with unknown next hop address: " +replies[i].nextHopAddress);
							continue;
						}
						if (anonNode.DISPLAY_ROUTE_INFO)
							System.out.println("" +anonNode +" sending reply on layer 1 to prev mix (mix " +con.mixId +")"); 
						con.previousMixOutputStream.write(Util.intToByteArray(id));
						con.previousMixOutputStream.write(Util.intToByteArray(replies[i].getByteMessage().length));
						con.previousMixOutputStream.write(replies[i].getByteMessage());
						con.previousMixOutputStream.flush();
					} catch (IOException e) {
						System.out.println(anonNode +" connection to prev mix (" +con.mixId +") lost"); 
						continue;	
					} catch (NullPointerException e) {
						e.printStackTrace();
						continue;
					}
				}
			}
		}

		
		private void putInRepliesForClient(Reply reply) {
			try {
				repliesForClients.put(reply);
			} catch (InterruptedException e) {
				e.printStackTrace();
				putInRepliesForClient(reply);
			}
		}
	}
	
	
	@Override
	public Reply getReplyForClient() {
		try {
			return repliesForClients.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return getReplyForClient();
		}
	}
	
	
	@Override
	public void userAdded(User user) {

	}

	
	@Override
	public void userRemoved(User user) {
		thisToPrevMixIDs.remove(user);
	}
	
	
	private synchronized void dropConncetion(MixConnection con) {
		connections.remove(con);
		try {
			con.previousMixOutputStream.close();
			con.previousMixInputStream.close();
			con.previousMixSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private class MixConnection {
		private int mixId;
		private Socket previousMixSocket;
		private BufferedInputStream previousMixInputStream;
		private BufferedOutputStream previousMixOutputStream;
		private int currentRequestLength = Util.NOT_SET;
		private int currentUserIdentifier = Util.NOT_SET;
		private User currentUser;
	}

}
