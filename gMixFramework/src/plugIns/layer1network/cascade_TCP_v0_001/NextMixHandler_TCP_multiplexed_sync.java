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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import framework.core.controller.SubImplementation;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.routing.MixList;
import framework.core.userDatabase.DatabaseEventListener;
import framework.core.userDatabase.User;
import framework.core.util.Util;


public class NextMixHandler_TCP_multiplexed_sync extends SubImplementation implements DatabaseEventListener {	

	private InetAddress nextMixAddress;
	private int nextMixPort;
	private Socket nextMixSocket; // TODO: ssl
	private BufferedInputStream nextMixInputStream;
	private BufferedOutputStream nextMixOutputStream;
	private AtomicBoolean replyThreadWaiting = new AtomicBoolean(false);
	//private int queueBlockSize;
	
	private RequestThread requestThread;
	private ReplyThread replyThread;
	
	private int requestBufferSize;
	private int replyBufferSize;
	
	private HashMap<User,Integer> thisToNextMixIDs;
	private HashMap<Integer,User> nextMixToThisIDs;
	private SecureRandom random = new SecureRandom();
	
	
	@Override
	public void constructor() {
		//this.queueBlockSize = settings.getPropertyAsInt("QUEUE_BLOCK_SIZE");
		this.requestThread = new RequestThread();
		this.replyThread = new ReplyThread();
		this.thisToNextMixIDs = new  HashMap<User,Integer>((int)Math.round((double)anonNode.EXPECTED_NUMBER_OF_USERS * 1.3d));
		this.nextMixToThisIDs = new  HashMap<Integer,User>((int)Math.round((double)anonNode.EXPECTED_NUMBER_OF_USERS * 1.3d));
		this.requestBufferSize = settings.getPropertyAsInt("MULTIPLEXED_REQUEST_BUFFER_SIZE");
		this.replyBufferSize = settings.getPropertyAsInt("MULTIPLEXED_REPLY_BUFFER_SIZE");
	}

	
	@Override
	public void initialize() {
		MixList mixList = anonNode.mixList;
		if (anonNode.PUBLIC_PSEUDONYM+1 < mixList.numberOfMixes) { // TODO
			this.nextMixAddress = mixList.addresses[anonNode.PUBLIC_PSEUDONYM+1];
			this.nextMixPort = mixList.ports[anonNode.PUBLIC_PSEUDONYM+1];
		}
		userDatabase.registerEventListener(this);
	}

	
	@Override
	public void begin() {
		this.requestThread.setPriority(Thread.MAX_PRIORITY);
		this.replyThread.setPriority(Thread.MAX_PRIORITY);
		this.requestThread.start();
		this.replyThread.start();
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
			connectToNextMix();
			while (true) {
				Request[] requests = anonNode.getFromRequestOutputQueue();
				for (int i=0; i<requests.length; i++) {
					try {
						synchronized (nextMixToThisIDs) {
							nextMixOutputStream.write(Util.intToByteArray(thisToNextMixIDs.get(requests[i].getOwner())));
						}
						nextMixOutputStream.write(Util.intToByteArray(requests[i].getByteMessage().length));
						nextMixOutputStream.write(requests[i].getByteMessage());
						nextMixOutputStream.flush();
					} catch (IOException e) {
						System.out.println(anonNode +" connection to next mix (" +nextMixAddress +":" +nextMixPort +") lost"); 
						connectToNextMix(); // reestablish connection
						continue;	
					} catch (NullPointerException e) {
						e.printStackTrace();
						continue;
					}
				} 
			}
		}	
		
		
		private void connectToNextMix() {
			synchronized (replyThreadWaiting) {
				System.out.println(anonNode +" trying to connect to next mix (" +nextMixAddress +":" +nextMixPort +")"); 
				while (true) { // try to connect to next mix
					try {
						nextMixSocket = new Socket();
						nextMixSocket.setKeepAlive(true); // permanent connection
						SocketAddress receiverAddress = new InetSocketAddress(nextMixAddress, nextMixPort);
						nextMixSocket.connect(receiverAddress);
						nextMixOutputStream = new BufferedOutputStream(nextMixSocket.getOutputStream(), requestBufferSize);
						nextMixInputStream = new BufferedInputStream(nextMixSocket.getInputStream(), replyBufferSize);
						//System.out.println(".");
						if (replyThreadWaiting.get() == true)
							replyThreadWaiting.notify();
						break; 	// exit loop, when no IOException has occurred 
								// (= connection is established successfully)
					} catch (Exception e) { // connection failed
						System.out.println(anonNode +" connection to next mix (" +nextMixAddress +":" +nextMixPort +") could not be established; trying to connect again"); 
						//e.printStackTrace(); // TODO: remove
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e1) {
							continue;
						}
					}
				}
				System.out.println(anonNode +" connection to next mix (" +nextMixAddress +":" +nextMixPort +") established"); 
			}
		}
	}
	
	
	private class ReplyThread extends Thread {
		
		@Override
		public void run() {
			while (true) { // receive messages from next mix
				try {
					waitForConnection();
					int channelIdentifier = Util.forceReadInt(nextMixInputStream);
					int messageLength = Util.forceReadInt(nextMixInputStream);
					if (messageLength < 1) {
						System.out.println(anonNode +" wrong size for reply message received"); 
						nextMixSocket.close();
						continue;
					}
					User user = null;
					synchronized (nextMixToThisIDs) {
						user = nextMixToThisIDs.get(channelIdentifier);
					}
					if (user == null) {
						System.out.println(anonNode +" received reply from (" +nextMixAddress +":" +nextMixPort +") for an unknown channel id"); 
						nextMixInputStream.skip(messageLength);
						continue;
					}
					byte[] message = Util.forceRead(nextMixInputStream, messageLength);
					Reply reply = MixMessage.getInstanceReply(message, user);
					anonNode.putInReplyInputQueue(reply);
				} catch (IOException e) {
					waitForConnection();
					continue;
				}
			}
		}
		
		
		private void waitForConnection() {
			synchronized (replyThreadWaiting) {
				while (nextMixSocket == null || !nextMixSocket.isConnected()) {
					replyThreadWaiting.set(true);
					try {
						// wait for new connection
						replyThreadWaiting.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
						continue;
					}
				}
				replyThreadWaiting.set(false);	
			}
		}
	}
	
}
