/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2011  Karl-Peter Fuchs
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

package inputOutputHandler.communicationHandler;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import message.MixMessage;
import message.Reply;
import message.Request;
import framework.Util;
import userDatabase.DatabaseEventListener;
import userDatabase.User;


public class Tcp_Multiplexed_PreviousMixCommunicationHandler_v1 extends GeneralCommunicationHandler implements PreviousMixCommunicationHandler, DatabaseEventListener  {

	private int port; 
	private InetAddress bindAddress;
	private Socket previousMixSocket;
	private InputStream previousMixInputStream;
	private OutputStream previousMixOutputStream;
	private AtomicBoolean replyThreadWaiting = new AtomicBoolean(false);
	private ServerSocket serverSocket;
	private int maxRequestLength;
	
	private HashMap<User,Integer> thisToPrevMixIDs;
	
	private RequestThread requestThread;
	private ReplyThread replyThread;
	
	
	@Override
	public void constructor() {
		this.bindAddress = settings.getPropertyAsInetAddress("BIND_ADDRESS");
		this.port = settings.getPropertyAsInt("PORT");
		this.maxRequestLength = settings.getPropertyAsInt("MAX_REQUEST_LENGTH");
		thisToPrevMixIDs = new HashMap<User,Integer>((int)Math.round((double)settings.getPropertyAsInt("MAX_CONNECTIONS") * 1.3d));
		this.requestThread = new RequestThread();
		this.replyThread = new ReplyThread();
	}
	

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}

	
	@Override
	public void begin() {
		this.requestThread.start();
		this.replyThread.start();
	}
	
	
	private class RequestThread extends Thread {
		
		@Override
		public void run() {
			waitForConnectionFromPreviousMix();
			while (true) {
				try {
					int channelIdentifier = Util.forceReadInt(previousMixInputStream);
					User user = userDatabase.getUser(channelIdentifier);
					if (user == null) {
						user = userDatabase.generateUser(channelIdentifier);
						userDatabase.addUser(user);
						thisToPrevMixIDs.put(user, channelIdentifier);
					}
					int messageLength = Util.forceReadInt(previousMixInputStream);
					if (messageLength < 1 || messageLength > maxRequestLength) {
						System.out.println(mix +" wrong size for request received"); 
						previousMixSocket.close();
						continue;
					}
					byte[] message = Util.forceRead(previousMixInputStream, messageLength);
					Request request = MixMessage.getInstanceRequest(message, user, settings);
					//System.out.println(mix +" received this message (ciphertext): " +Util.md5(request.getByteMessage())); // TODO
					inputOutputHandlerInternal.addUnprocessedRequest(request);			
				} catch (IOException e) { // connection is lost
					System.out.println(mix +" connection to previous mix lost"); 
					waitForConnectionFromPreviousMix();
					continue;
				}
			}
		}
		
		
		private void waitForConnectionFromPreviousMix() {
			
			if (serverSocket == null) {
				try {
					serverSocket = new ServerSocket(port, 1, bindAddress);
					System.out.println(mix + " bound to " +bindAddress + ":" +port); 	
				} catch (IOException e) {
					System.out.println(mix + " couldn't bind socket " +bindAddress + ":" +port);
					e.printStackTrace();
					System.exit(1);
				}
				
			}
			
			System.out.println(mix +" waiting for connection from previous mix"); 
			
			while (true) {
				synchronized (replyThreadWaiting) {
					try {
						previousMixSocket = serverSocket.accept();
						previousMixSocket.setKeepAlive(true);
						previousMixInputStream = previousMixSocket.getInputStream();
						previousMixOutputStream = previousMixSocket.getOutputStream();
						System.out.println(mix +" connection established");
						if (replyThreadWaiting.get())
							replyThreadWaiting.notify();
						break;		
					} catch (IOException e) {
						System.out.println(mix +" connection attempt from previous mix failed");
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
					Reply reply = inputOutputHandlerInternal.getProcessedReply();	// TODO: add message-type-header ?
					Integer id = thisToPrevMixIDs.get(reply.getOwner());
					if (id == null) {
						System.out.println(mix +" no id for " +reply.getOwner() +" available" );
						continue;
					}	
					previousMixOutputStream.write(Util.intToByteArray(id));
					previousMixOutputStream.write(Util.intToByteArray(reply.getByteMessage().length));
					previousMixOutputStream.write(reply.getByteMessage());
					previousMixOutputStream.flush();
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
