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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import framework.core.controller.SubImplementation;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.routing.MixList;
import framework.core.userDatabase.DatabaseEventListener;
import framework.core.userDatabase.User;
import framework.core.util.Util;


public class NextMixHandler_TCP_RR_multiplexed_sync extends SubImplementation implements DatabaseEventListener {	

	private AtomicBoolean replyThreadWaiting = new AtomicBoolean(false);
	private RequestThread requestThread;
	private ReplyThread replyThread;
	private int requestBufferSize;
	private int replyBufferSize;
	private int maxReplyLength;
	
	private HashMap<User,Integer> thisToNextMixIDs;
	private HashMap<Integer,User> nextMixToThisIDs;
	private SecureRandom random = new SecureRandom();
	
	private HashMap<Integer, MixConnection> connectionsMap; // used by RequestThread
	private Vector<MixConnection> connections; // used by ReplyThread
	private Vector<MixConnection> newConnections; // used for synchronization between RequestThread and ReplyThread
	
	
	@Override
	public void constructor() {
		this.requestThread = new RequestThread();
		this.replyThread = new ReplyThread();
		this.thisToNextMixIDs = new  HashMap<User,Integer>((int)Math.round((double)anonNode.EXPECTED_NUMBER_OF_USERS * 1.3d));
		this.nextMixToThisIDs = new  HashMap<Integer,User>((int)Math.round((double)anonNode.EXPECTED_NUMBER_OF_USERS * 1.3d));
		this.connectionsMap = new HashMap<Integer, MixConnection>();
		this.connections = new Vector<MixConnection>();
		this.newConnections = new Vector<MixConnection>();
		this.requestBufferSize = settings.getPropertyAsInt("MULTIPLEXED_REQUEST_BUFFER_SIZE");
		this.replyBufferSize = settings.getPropertyAsInt("MULTIPLEXED_REPLY_BUFFER_SIZE");
		this.maxReplyLength = settings.getPropertyAsInt("MAX_REPLY_LENGTH");
	}

	
	@Override
	public void initialize() {
		userDatabase.registerEventListener(this);
	}

	
	@Override
	public void begin() {
		// establish connections to all known mixes:
		MixList mixList = anonNode.mixList;
		for (int i=0; i<mixList.addresses.length; i++)
			if (mixList.mixIDs[i] != anonNode.PUBLIC_PSEUDONYM) // don't connect to yourself...
				connectTo(mixList.mixIDs[i]);
		this.requestThread.start();
		if (anonNode.IS_DUPLEX)
			this.replyThread.start();
	}
	
	
	private void connectTo(int mixId) {
		MixConnection con = null;
		synchronized (replyThreadWaiting) {
			MixList mixList = anonNode.mixList;
			con = new MixConnection();
			con.mixId = mixId;
			con.nextMixAddress = mixList.addresses[mixId];
			con.nextMixPort = mixList.ports[mixId] + 1000;
			System.out.println(anonNode +" trying to connect to next mix (" +con.nextMixAddress +":" +con.nextMixPort +")"); 
			while (true) { // try to connect
				try {
					con.nextMixSocket = new Socket();
					con.nextMixSocket.setKeepAlive(true); // permanent connection
					SocketAddress receiverAddress = new InetSocketAddress(con.nextMixAddress, con.nextMixPort);
					con.nextMixSocket.connect(receiverAddress);
					con.nextMixOutputStream = new BufferedOutputStream(con.nextMixSocket.getOutputStream(), requestBufferSize);
					con.nextMixInputStream = new BufferedInputStream(con.nextMixSocket.getInputStream(), replyBufferSize);
					con.nextMixOutputStream.write(Util.intToByteArray(anonNode.PUBLIC_PSEUDONYM));
					con.nextMixOutputStream.flush();
					//System.out.println(".");
					if (replyThreadWaiting.get() == true)
						replyThreadWaiting.notify();
					break; 	// exit loop, when no IOException has occurred 
							// (= connection is established successfully)
				} catch (Exception e) { // connection failed
					System.out.println(anonNode +" connection to next mix (" +con.nextMixAddress +":" +con.nextMixPort +") could not be established; trying to connect again"); 
					//e.printStackTrace(); // TODO: remove
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						continue;
					}
				}
			}
			System.out.println(anonNode +" connection to next mix (" +con.nextMixAddress +":" +con.nextMixPort +") established"); 
		}
		connectionsMap.put(con.mixId, con);
		if (anonNode.IS_DUPLEX) {
			synchronized (newConnections) {
				newConnections.add(con);
			}
		}
	}
	
	
	@Override
	public void userAdded(User user) {
		synchronized (nextMixToThisIDs) {
			while (true) {
				int idForNextMix = random.nextInt();
				if (nextMixToThisIDs.get(idForNextMix) != null)
					continue;
				this.thisToNextMixIDs.put(user, idForNextMix);
				this.nextMixToThisIDs.put(idForNextMix, user);
				break;
			}
		}
	}
	

	@Override
	public void userRemoved(User user) {
		synchronized (nextMixToThisIDs) {
			this.nextMixToThisIDs.remove(thisToNextMixIDs.get(user));
			this.thisToNextMixIDs.remove(user);
		}
	}

	
	private class RequestThread extends Thread {
		
		@Override
		public void run() {
			while (true) {
				Request[] requests = anonNode.getFromRequestOutputQueue();
				MixConnection con = null;
				for (int i=0; i<requests.length; i++) {
					try {
						assert requests[i].nextHopAddress != Util.NOT_SET;
						con = connectionsMap.get(requests[i].nextHopAddress);
						if (con == null) { // no connection to that mix -> try to connect
							connectTo(requests[i].nextHopAddress);
							con = connectionsMap.get(requests[i].nextHopAddress);
							if (con == null) { // could not connect to that mix
								System.err.println("received message with unknown next hop address: " +requests[i].nextHopAddress);
								continue;
							}	
						}
						if (anonNode.DISPLAY_ROUTE_INFO)
							System.out.println("" +anonNode +" sending request on layer 1 to next mix (mix " +con.mixId +"); " +Util.md5(requests[i].getByteMessage()));
						synchronized (nextMixToThisIDs) {
							con.nextMixOutputStream.write(Util.intToByteArray(thisToNextMixIDs.get(requests[i].getOwner())));
						}
						con.nextMixOutputStream.write(Util.intToByteArray(requests[i].getByteMessage().length));
						//System.out.println("req-len: " +requests[i].getByteMessage().length); // TODO: remove
						con.nextMixOutputStream.write(requests[i].getByteMessage());
						con.nextMixOutputStream.flush();
					} catch (IOException e) {
						System.out.println(anonNode +" connection to next mix (" +con.nextMixAddress +":" +con.nextMixPort +") lost"); 
						connectTo(con.mixId); // reestablish connection
						continue;	
					} catch (NullPointerException e) {
						e.printStackTrace();
						continue;
					}
				}
			}
		}
	}
	
	
	private class ReplyThread extends Thread {
		
		@Override
		public void run() {
			
			int maxReadsPerChannelInARow = settings.getPropertyAsInt("MAX_READS_IN_A_ROW_MIX");
			int maxMessageBlockSize = anonNode.QUEUE_BLOCK_SIZE;
			
			while (true) { // receive messages from "next" mixes
				
				synchronized (newConnections) { // handle new connections
					if (newConnections.size() != 0) {
						connections.addAll(newConnections);
						newConnections = new Vector<MixConnection>();
					}
				}
				
				Vector<Vector<Reply>> newReplies = null;
				Vector<Reply> newRepliesForCurrentConnection = null;
				for (MixConnection con:connections) {// try to read data from existing connections
					try {
						for (int i=0; i<maxReadsPerChannelInARow; i++) {
							if (con.currentUserIdentifier == Util.NOT_SET) { // try to read id
								if (con.nextMixInputStream.available() >= 4) {
									byte[] id = new byte[4];
									int read = con.nextMixInputStream.read(id);
									assert read == 4; // should not be different due to buffered stream; check anyways...
									con.currentUserIdentifier = Util.byteArrayToInt(id);
									synchronized (nextMixToThisIDs) {
										con.currentUser = nextMixToThisIDs.get(con.currentUserIdentifier);
									}
									if (con.currentUser == null) {
										System.out.println(anonNode +" received reply from (" +con.nextMixAddress +":" +con.nextMixPort +") for an unknown channel id"); 
										dropConncetion(con);
										continue;
									}
								} else {
									break;
								}
							}
							if (con.currentReplyLength == Util.NOT_SET) { // try to read length-header
								if (con.nextMixInputStream.available() >= 4) {
									byte[] len = new byte[4];
									int read = con.nextMixInputStream.read(len);
									assert read == 4; // should not be different due to buffered stream; check anyways...
									con.currentReplyLength = Util.byteArrayToInt(len);
									//System.out.println("rep-len: " +con.currentReplyLength); // TODO: remove
									
									if (con.currentReplyLength > maxReplyLength) {
										System.err.println("warning: mix " +con.mixId +" sent a too large message");
										dropConncetion(con);
										continue;
									} else if (con.currentReplyLength < 1) {
										System.err.println("warning: mix " +con.mixId +" sent a too small message");
										dropConncetion(con);
										continue;
									}
								} else {
									break;
								}
							}
							if (con.currentReplyLength != Util.NOT_SET) { // length header already read -> try to read message
								if (con.nextMixInputStream.available() >= con.currentReplyLength) {
									byte[] msg = new byte[con.currentReplyLength];
									int read = con.nextMixInputStream.read(msg);
									assert read == con.currentReplyLength; // should not be different due to buffered stream; check anyways...
									con.currentUserIdentifier = Util.NOT_SET;
									con.currentReplyLength = Util.NOT_SET;
									if (anonNode.DISPLAY_ROUTE_INFO)
										System.out.println("" +anonNode +" received reply on layer 1 from next mix (mix " +con.mixId +")"); 
									Reply r = MixMessage.getInstanceReply(msg, con.currentUser);
									if (newRepliesForCurrentConnection == null)
										newRepliesForCurrentConnection = new Vector<Reply>(maxReadsPerChannelInARow);
									newRepliesForCurrentConnection.add(r);
								} else {
									break;
								}
							}
						}
						if (newRepliesForCurrentConnection != null) {
							if (newReplies == null)
								newReplies = new Vector<Vector<Reply>>(maxMessageBlockSize);
							newReplies.add(newRepliesForCurrentConnection);
							newRepliesForCurrentConnection = null;
						}
					} catch (IOException e) {
						e.printStackTrace();
						dropConncetion(con);
						continue;
					}
					
					if (newReplies != null && newReplies.size() >= maxMessageBlockSize) {
						for (Vector<Reply> replies:newReplies)
							anonNode.putInReplyInputQueue(replies.toArray(new Reply[0])); // might block
						newReplies = new Vector<Vector<Reply>>(maxMessageBlockSize);
					}
				}
				if (newReplies == null) {
					try {Thread.sleep(1);} catch (InterruptedException e) {continue;} // TODO: 1 ms adequate?
					continue;
				}
				
				for (Vector<Reply> requests:newReplies)
					anonNode.putInReplyInputQueue(requests.toArray(new Reply[0])); // might block
			}
		}
	}
	

	private synchronized void dropConncetion(MixConnection con) {
		connections.remove(con);
		try {
			con.nextMixInputStream.close();
			con.nextMixOutputStream.close();
			con.nextMixSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private class MixConnection {
		private int mixId;
		private InetAddress nextMixAddress;
		private int nextMixPort;
		private Socket nextMixSocket; // TODO: ssl
		private BufferedInputStream nextMixInputStream;
		private BufferedOutputStream nextMixOutputStream;
		private int currentUserIdentifier = Util.NOT_SET;
		private int currentReplyLength = Util.NOT_SET;
		private User currentUser;
	}
	
}
