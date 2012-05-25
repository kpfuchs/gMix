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

import infoService.MixList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import message.MixMessage;
import message.Reply;
import message.Request;

import framework.Util;

import userDatabase.DatabaseEventListener;
import userDatabase.User;


public class Tcp_Multiplexed_NextMixCommunicationHandler_v1 extends GeneralCommunicationHandler implements NextMixCommunicationHandler, DatabaseEventListener {

	
	private InetAddress nextMixAddress;
	private int nextMixPort;
	private Socket nextMixSocket;	// TODO: ssl
	private InputStream nextMixInputStream;
	private OutputStream nextMixOutputStream;
	private AtomicBoolean replyThreadWaiting = new AtomicBoolean(false);
	
	private RequestThread requestThread;
	private ReplyThread replyThread;
	
	private HashMap<User,Integer> thisToNextMixIDs;
	private HashMap<Integer,User> nextMixToThisIDs;
	private SecureRandom random = new SecureRandom();
	
	
	@Override
	public void constructor() {
		this.requestThread = new RequestThread();
		this.replyThread = new ReplyThread();
		this.thisToNextMixIDs = new  HashMap<User,Integer>((int)Math.round((double)settings.getPropertyAsInt("MAX_CONNECTIONS") * 1.3d));
		this.nextMixToThisIDs = new  HashMap<Integer,User>((int)Math.round((double)settings.getPropertyAsInt("MAX_CONNECTIONS") * 1.3d));
	}

	
	@Override
	public void initialize() {
		MixList mixList = infoService.getMixList();
		if (mix.getIdentifier()+1 < mixList.numberOfMixes) {
			this.nextMixAddress = mixList.addresses[mix.getIdentifier()+1];
			this.nextMixPort = mixList.ports[mix.getIdentifier()+1];
		}
		userDatabase.registerEventListener(this);
	}

	
	@Override
	public void begin() {
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
				try {
					Request request = inputOutputHandlerInternal.getProcessedRequest();	// TODO: add message-type-header ?
					synchronized (nextMixToThisIDs) {
						nextMixOutputStream.write(Util.intToByteArray(thisToNextMixIDs.get(request.getOwner())));
					}
					nextMixOutputStream.write(Util.intToByteArray(request.getByteMessage().length));
					nextMixOutputStream.write(request.getByteMessage());
					nextMixOutputStream.flush();
				} catch (IOException e) {
					System.out.println(mix +" connection to next mix (" +nextMixAddress +":" +nextMixPort +") lost"); 
					connectToNextMix(); // reestablish connection
					continue;	
				} catch (NullPointerException e) {
					e.printStackTrace();
					continue;
				}
			}
		}	
		
		
		private void connectToNextMix() {
			synchronized (replyThreadWaiting) {
				System.out.println(mix +" trying to connect to next mix (" +nextMixAddress +":" +nextMixPort +")"); 
				while (true) { // try to connect to next mix
					try {
						nextMixSocket = new Socket();
						nextMixSocket.setKeepAlive(true); // permanent connection
						SocketAddress receiverAddress = new InetSocketAddress(nextMixAddress, nextMixPort);
						nextMixSocket.connect(receiverAddress);
						nextMixOutputStream = nextMixSocket.getOutputStream();
						nextMixInputStream = nextMixSocket.getInputStream();
						//System.out.println(".");
						if (replyThreadWaiting.get() == true)
							replyThreadWaiting.notify();
						break; 	// exit loop, when no IOException has occurred 
								// (= connection is established successfully)
					} catch (IOException e) { // connection failed
						System.out.println(mix +" connection to next mix (" +nextMixAddress +":" +nextMixPort +") could not be established; trying to connect again"); 
						e.printStackTrace(); // TODO: remove
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e1) {
							continue;
						}
					}
				}
				System.out.println(mix +" connection to next mix (" +nextMixAddress +":" +nextMixPort +") established"); 
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
						System.out.println(mix +" wrong size for reply message received"); 
						nextMixSocket.close();
						continue;
					}
					User user = null;
					synchronized (nextMixToThisIDs) {
						user = nextMixToThisIDs.get(channelIdentifier);
					}
					if (user == null) {
						System.out.println(mix +" received reply from (" +nextMixAddress +":" +nextMixPort +") for an unknown channel id"); 
						nextMixInputStream.skip(messageLength);
						continue;
					}
					byte[] message = Util.forceRead(nextMixInputStream, messageLength);
					Reply reply = MixMessage.getInstanceReply(message, user, settings);
					inputOutputHandlerInternal.addUnprocessedReply(reply);
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
