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
package plugIns.layer2recodingScheme.RSA_AES_Channel_v0_001;

import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer2RecodingSchemeMix;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.userDatabase.User;
import framework.core.userDatabase.UserAttachment;


public class MixPlugIn extends Implementation implements Layer2RecodingSchemeMix {

	private RSA_AES_Channel_Config config;
	private WorkerThread[] workerThreads;
	private RequestDistributorThread requestDistributorThread;
	private ReplyDistributorThread replyDistributorThread;
	
	
	@Override
	public void constructor() {
		if (anonNode.IS_FREE_ROUTE)
			throw new RuntimeException("not supported"); // TODO: support it...
		this.config = new RSA_AES_Channel_Config(anonNode, false);
		this.requestDistributorThread = new RequestDistributorThread();
		this.requestDistributorThread.setPriority(Thread.MAX_PRIORITY); // TODO ?
		if (anonNode.IS_DUPLEX)
			this.replyDistributorThread = new ReplyDistributorThread();
		this.workerThreads = new WorkerThread[config.NUMBER_OF_THREADS];
		for (int i=0; i<config.NUMBER_OF_THREADS; i++) {
			RSA_AES_Channel recodingScheme = new RSA_AES_Channel(anonNode, config);
			workerThreads[i] = new WorkerThread(recodingScheme);
		}
	}

	
	@Override
	public void initialize() {
		assert !anonNode.IS_FREE_ROUTE;
		for (int i=0; i<workerThreads.length; i++)
			workerThreads[i].recodingScheme.initAsRecoder(this);
	}
	

	@Override
	public void begin() {
		for (int i=0; i<workerThreads.length; i++)
			this.workerThreads[i].start();
		this.requestDistributorThread.start();
		if (anonNode.IS_DUPLEX)
			this.replyDistributorThread.start();
	}

	
	@Override
	public int getMaxSizeOfNextReply() {
		return anonNode.MAX_PAYLOAD;
	}

	
	@Override
	public int getMaxSizeOfNextRequest() {
		return anonNode.MAX_PAYLOAD;
	}

	
	@Override
	public Request generateDummy(int[] route, User user) {
		throw new RuntimeException("not supported");
	}

	
	@Override
	public Request generateDummy(User user) {
		throw new RuntimeException("not supported");
	}
	

	@Override
	public Reply generateDummyReply(int[] route, User user) {
		throw new RuntimeException("not supported");
	}

	
	@Override
	public Reply generateDummyReply(User user) {
		throw new RuntimeException("not supported");
	}
	
	
	class ChannelData extends UserAttachment {
		SecretKey macKey;
		Cipher decryptCipher;
		Cipher encryptCipher;
		int threadId;
		
		public ChannelData(User owner, int threadId) {
			super(owner, getThis());
			this.threadId = threadId;
		}
	}
	
	
	class RequestDistributorThread extends Thread {
		
		private int next = -1;
		
		@Override
		public void run() {
			while (true) { // process messages
				Request[] requests = anonNode.getFromRequestInputQueue();
				// int threadId = requests[0].getOwner().layer2Id; // TODO: all meesages in that array must be of the same user! -> ioh-dependency
				if (requests[0].getOwner().layer2Id == -1) { // not set
					requests[0].getOwner().layer2Id = getNextThreadId(); // TODO aktuelle auslastung der threads beachten (number of channels)
					new ChannelData(requests[0].getOwner(), requests[0].getOwner().layer2Id);
				}
				try {
					workerThreads[requests[0].getOwner().layer2Id].queue.put(requests);
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				}

				
					/*old- assume messages of diff. users in array: for (int i=0; i<requests.length; i++) {
						int threadId = requests[i].getOwner().threadId;
						if (threadId == -1) { // not set
							threadId = getNextThreadId(); // TODO aktuelle auslastung der threads beachten (number of channels)
							requests[i].getOwner().threadId = threadId;
							new ChannelData(requests[i].getOwner(), threadId);
						}
						try {
							workerThreads[threadId].queue.put(requests[i]);
						} catch (InterruptedException e) {
							e.printStackTrace();
							continue;
						}
					}*/
				 
			}	
		}
		
		private int getNextThreadId() {
			if (++next == workerThreads.length)
				next = 0;
			return next;
		}
	}
	
	
	class ReplyDistributorThread extends Thread {
		
		@Override
		public void run() {
			while (true) { // process messages
				Reply[] replies = anonNode.getFromReplyInputQueue();
				//int threadId = replies[0].getOwner().layer2Id; // TODO: all meesages in that array must be of the same user! -> ioh-dependency
				if (replies[0].getOwner().layer2Id == -1) { // not set
					throw new RuntimeException("no threadID set (this should have been done by requestDistributorThread)"); 
				}
				try {
					workerThreads[replies[0].getOwner().layer2Id].queue.put(replies);
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				}
			}	
		}
		
	}
	
	
	class WorkerThread extends Thread {
		
		ArrayBlockingQueue<MixMessage[]> queue;
		RSA_AES_Channel recodingScheme;
		int queueLength = settings.getPropertyAsInt("RS_THREAD_QUEUE_LENGTH");
		
		public WorkerThread(RSA_AES_Channel recodingScheme) {
			this.recodingScheme = recodingScheme;
			this.queue = new ArrayBlockingQueue<MixMessage[]>(queueLength);
			new Thread(// TODO: remove
					new Runnable() {
						public void run() {
							while (true) {
								System.out.println("free space: " +queue.remainingCapacity()); 
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					}
				).start(); 
		}

		
		@Override
		public void run() {
			while (true) { // process messages
				MixMessage[] msg =  null;
				System.out.println(1); 
				try {msg = queue.take();} catch (InterruptedException e) {continue;}
				System.out.println(2); 
				Vector<MixMessage[]> messages = new Vector<MixMessage[]>(queue.size());
				messages.add(msg);
				System.out.println(3); 
				queue.drainTo(messages);
				System.out.println(4); 
				for (int i=0; i<messages.size(); i++) {
					MixMessage[] m = messages.get(i);
					if (m[0] instanceof Request) {
						for (int j=0; j<m.length; j++) {
							System.out.println(5); 
							Request recodedMsg = recodingScheme.recodeMessage((Request)m[j]);
							System.out.println(6); 
							if (recodedMsg != null)
								outputStrategyLayerMix.addRequest(recodedMsg);
							System.out.println(7); 
						}
					} else {
						for (int j=0; j<m.length; j++) {
							System.out.println(8); 
							Reply recodedMsg = recodingScheme.recodeReply((Reply)m[j]);
							System.out.println(9); 
							if (recodedMsg != null)
								outputStrategyLayerMix.addReply(recodedMsg);
							System.out.println(10); 
						}
					}
				}
			}	
		}
	}
	
	
	private MixPlugIn getThis() {
		return this;
	}

}
