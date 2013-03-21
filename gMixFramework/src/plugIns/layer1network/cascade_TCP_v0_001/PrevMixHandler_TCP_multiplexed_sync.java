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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import framework.core.controller.SubImplementation;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.userDatabase.User;
import framework.core.util.Util;


public class PrevMixHandler_TCP_multiplexed_sync extends SubImplementation implements framework.core.userDatabase.DatabaseEventListener  {

	private int port; 
	private InetAddress bindAddress;
	private Socket previousMixSocket;
	private BufferedInputStream previousMixInputStream;
	private BufferedOutputStream previousMixOutputStream;
	private AtomicBoolean replyThreadWaiting = new AtomicBoolean(false);
	private ServerSocket serverSocket;
	private int maxRequestLength;
	//private int queueBlockSize;
	
	private HashMap<User,Integer> thisToPrevMixIDs;
	
	private int requestBufferSize;
	private int replyBufferSize;
	
	private RequestThread requestThread;
	private ReplyThread replyThread;
	
	
	@Override
	public void constructor() {
		//this.queueBlockSize = settings.getPropertyAsInt("QUEUE_BLOCK_SIZE");
		this.bindAddress = settings.getPropertyAsInetAddress("GLOBAL_MIX_BIND_ADDRESS");
		this.port = settings.getPropertyAsInt("GLOBAL_MIX_BIND_PORT");
		this.maxRequestLength = settings.getPropertyAsInt("MAX_REQUEST_LENGTH");
		thisToPrevMixIDs = new HashMap<User,Integer>((int)Math.round((double)settings.getPropertyAsInt("MAX_CONNECTIONS") * 1.3d));
		this.requestThread = new RequestThread();
		this.replyThread = new ReplyThread();
		this.requestBufferSize = settings.getPropertyAsInt("MULTIPLEXED_REQUEST_BUFFER_SIZE");
		this.replyBufferSize = settings.getPropertyAsInt("MULTIPLEXED_REPLY_BUFFER_SIZE");
	}
	

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}

	
	@Override
	public void begin() {
		this.requestThread.setPriority(Thread.MAX_PRIORITY);
		this.replyThread.setPriority(Thread.MAX_PRIORITY);
		this.requestThread.start();
		this.replyThread.start();
	}
	
	
	private class RequestThread extends Thread {
		
		@Override
		public void run() {
			waitForConnectionFromPreviousMix();
			while (true) {
				try {	// TODO: give blocks of messages to inputOutputHandlerInternal, not only single messages
					int channelIdentifier = Util.forceReadInt(previousMixInputStream);
					User user = userDatabase.getUser(channelIdentifier);
					if (user == null) {
						user = userDatabase.generateUser(channelIdentifier);
						userDatabase.addUser(user);
						thisToPrevMixIDs.put(user, channelIdentifier);
					}
					int messageLength = Util.forceReadInt(previousMixInputStream);
					if (messageLength < 1 || messageLength > maxRequestLength) {
						System.out.println(anonNode +" wrong size for request received"); 
						previousMixSocket.close();
						continue;
					}
					byte[] message = Util.forceRead(previousMixInputStream, messageLength);
					Request request = MixMessage.getInstanceRequest(message, user);
					//System.out.println(mix +" received this message (ciphertext): " +Util.md5(request.getByteMessage())); // TODO
					anonNode.putInRequestInputQueue(request);			
				} catch (IOException e) { // connection is lost
					System.out.println(anonNode +" connection to previous mix lost"); 
					waitForConnectionFromPreviousMix();
					continue;
				}
			}
		}
		
		
		private void waitForConnectionFromPreviousMix() {
			
			if (serverSocket == null) {
				try {
					serverSocket = new ServerSocket(port, 1, bindAddress);
					System.out.println(anonNode + " bound to " +bindAddress + ":" +port); 	
				} catch (IOException e) {
					System.out.println(anonNode + " couldn't bind socket " +bindAddress + ":" +port);
					e.printStackTrace();
					System.exit(1);
				}
				
			}
			
			System.out.println(anonNode +" waiting for connection from previous mix"); 
			
			while (true) {
				synchronized (replyThreadWaiting) {
					try {
						previousMixSocket = serverSocket.accept();
						previousMixSocket.setKeepAlive(true);
						previousMixInputStream = new BufferedInputStream(previousMixSocket.getInputStream(), requestBufferSize);
						previousMixOutputStream = new BufferedOutputStream(previousMixSocket.getOutputStream(), replyBufferSize);
						System.out.println(anonNode +" connection established");
						if (replyThreadWaiting.get())
							replyThreadWaiting.notify();
						break;		
					} catch (IOException e) {
						System.out.println(anonNode +" connection attempt from previous mix failed");
						e.printStackTrace();
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e1) {
							continue;
						}
						continue; // wait again	
					}
				}
			}
			
		}
		
	}
	
	
	private class ReplyThread extends Thread {
		
		@Override
		public void run() {
			waitForConnection();
			while (true) {
				try {
					Reply[] replies = anonNode.getFromReplyOutputQueue(); // TODO: add message-type-header ?
					for (int i=0; i<replies.length; i++) {
						Integer id = thisToPrevMixIDs.get(replies[i].getOwner());
						if (id == null) {
							System.out.println(anonNode +" no id for " +replies[i].getOwner() +" available" );
							continue;
						}	
						//System.out.println("id: " +id +", len: " +replies[i].getByteMessage().length); 
						assert replies[i].getByteMessage().length > 0;
						previousMixOutputStream.write(Util.intToByteArray(id));
						previousMixOutputStream.write(Util.intToByteArray(replies[i].getByteMessage().length));
						previousMixOutputStream.write(replies[i].getByteMessage());
						previousMixOutputStream.flush();
					}
				} catch (IOException e) {
					waitForConnection();
					continue;	
				}
			}
		}
		
		
		private void waitForConnection() {
			synchronized (replyThreadWaiting) {
				while (previousMixSocket == null || !previousMixSocket.isConnected()) {
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
	
	
	@Override
	public void userAdded(User user) {

	}

	
	@Override
	public void userRemoved(User user) {
		thisToPrevMixIDs.remove(user);
	}

}
