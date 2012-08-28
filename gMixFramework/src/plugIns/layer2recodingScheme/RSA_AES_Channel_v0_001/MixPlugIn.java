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

import java.util.concurrent.ArrayBlockingQueue;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer2RecodingSchemeMix;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.routing.RoutingMode;
import framework.core.userDatabase.User;
import framework.core.userDatabase.UserAttachment;


public class MixPlugIn extends Implementation implements Layer2RecodingSchemeMix {

	private RSA_AES_Channel_Config config;
	private RequestDistributorThread requestDistributorThread;
	private ReplyDistributorThread replyDistributorThread;
	
	private RequestWorkerThread[] requestWorkerThreads;
	private ReplyWorkerThread[] replyWorkerThreads;
	
	private ArrayBlockingQueue<ChannelData> channelsWithRequestsReady;
	private ArrayBlockingQueue<ChannelData> channelsWithRepliesReady;
	
	private long waitCounter = 0;
	private long noWaitCounter = 0;
	
	
	@Override
	public void constructor() {
		if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_DYNAMIC_ROUTING) 
			throw new RuntimeException("RoutingMode FREE_ROUTE_DYNAMIC_ROUTING not supported, only CASADE and FREE_ROUTE_SOURCE_ROUTING"); 
		this.config = new RSA_AES_Channel_Config(anonNode, false);
		this.channelsWithRequestsReady = new ArrayBlockingQueue<ChannelData>(config.NUMBER_OF_THREADS * 5);
		this.requestWorkerThreads = new RequestWorkerThread[config.NUMBER_OF_THREADS];
		this.requestDistributorThread = new RequestDistributorThread();

		if (anonNode.IS_DUPLEX) {
			this.channelsWithRepliesReady = new ArrayBlockingQueue<ChannelData>(config.NUMBER_OF_THREADS * 5);
			this.replyWorkerThreads = new ReplyWorkerThread[config.NUMBER_OF_THREADS];
			this.replyDistributorThread = new ReplyDistributorThread();
		}
		
		for (int i=0; i<config.NUMBER_OF_THREADS; i++) {
			RSA_AES_Channel recodingScheme = new RSA_AES_Channel(anonNode, config);
			requestWorkerThreads[i] = new RequestWorkerThread(recodingScheme);
			if (anonNode.IS_DUPLEX) {
				recodingScheme = new RSA_AES_Channel(anonNode, config);
				replyWorkerThreads[i] = new ReplyWorkerThread(recodingScheme);
			}
		}
	}

	
	@Override
	public void initialize() {
		for (int i=0; i<requestWorkerThreads.length; i++) {
			requestWorkerThreads[i].recodingScheme.initAsRecoder();
			if (anonNode.IS_DUPLEX)
				replyWorkerThreads[i].recodingScheme.initAsRecoder();
				
		}
	}
	

	@Override
	public void begin() {
		for (int i=0; i<requestWorkerThreads.length; i++) {
			this.requestWorkerThreads[i].start();
			if (anonNode.IS_DUPLEX)
				this.replyWorkerThreads[i].start();
		}
		this.requestDistributorThread.start();
		if (anonNode.IS_DUPLEX)
			this.replyDistributorThread.start();
		new StatisticsThread().start();
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
		ArrayBlockingQueue<Request> requestQueue = new ArrayBlockingQueue<Request>(config.RS_THREAD_QUEUE_LENGTH);
		ArrayBlockingQueue<Reply> replyQueue = new ArrayBlockingQueue<Reply>(config.RS_THREAD_QUEUE_LENGTH);
		Object requestSynchronizer = new Object();
		Object replySynchronizer = new Object();
		int nextHopAddress;
		
		
		public ChannelData(User owner) {
			super(owner, getThis());
		}
		
	}
	
	
	class RequestDistributorThread extends Thread {
		
		@Override
		public void run() {
			while (true) { // process messages
				for (Request request: anonNode.getFromRequestInputQueue()) {
					ChannelData channelData = null;
					try {
						if (request.getOwner().layer2Id == User.NOT_SET) { // not set
							request.getOwner().layer2Id = 1;
							channelData = new ChannelData(request.getOwner());
						} else {
							channelData = request.getOwner().getAttachment(getThis(), ChannelData.class);
							assert channelData != null;
						}
						channelData.requestQueue.put(request);
						channelsWithRequestsReady.put(channelData);
					} catch (InterruptedException e) {
						e.printStackTrace();
						continue;
					}
				} 
			}	
		}
	}
	
	
	class ReplyDistributorThread extends Thread {
		
		@Override
		public void run() {	
			while (true) { // process messages
				for (Reply reply: anonNode.getFromReplyInputQueue()) {
					ChannelData channelData = reply.getOwner().getAttachment(getThis(), ChannelData.class);
					if (channelData == null) {
						System.err.println("no channel data stored for " +reply.getOwner());
						userDatabase.removeUser(reply.getOwner());
						continue;
					}
					try {
						channelData.replyQueue.put(reply);
						channelsWithRepliesReady.put(channelData);
					} catch (InterruptedException e) {
						e.printStackTrace();
						continue;
					}
				} 
			}	
		}
	}
	

	class RequestWorkerThread extends Thread {
		
		public RSA_AES_Channel recodingScheme;
		
		public RequestWorkerThread(RSA_AES_Channel recodingScheme) {
			this.recodingScheme = recodingScheme;
		}

		
		@Override
		public void run() {
			while (true) { // process messages
				ChannelData channelData;
				try {
					channelData = channelsWithRequestsReady.take();
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				}
				long enter = System.nanoTime();
				synchronized (channelData.requestSynchronizer) {
					if (System.nanoTime() - enter > 10000)
						waitCounter++;
					else
						noWaitCounter++;
					while (true) {
						Request request = channelData.requestQueue.poll();
						if (request == null) {
							break;
						} else { 
							request = recodingScheme.recodeMessage(request, channelData); 
							if (request != null) {
								outputStrategyLayerMix.addRequest(request); 
							}
						}	
					}
				}
			}	
		}
		
	}

	
	class ReplyWorkerThread extends Thread {
		
		public RSA_AES_Channel recodingScheme;
		
		public ReplyWorkerThread(RSA_AES_Channel recodingScheme) {
			this.recodingScheme = recodingScheme;
		}

		
		@Override
		public void run() {
			while (true) { // process messages
				ChannelData channelData;
				try {
					channelData = channelsWithRepliesReady.take();
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				}
				synchronized (channelData.replySynchronizer) {
					while (true) {
						Reply reply = channelData.replyQueue.poll();
						if (reply == null) {
							break;
						} else {
							reply = recodingScheme.recodeReply(reply, channelData);
							if (reply != null)
								outputStrategyLayerMix.addReply(reply);
						}	
					}
				}
			}	
		}
		
	}
	
	
	class StatisticsThread extends Thread {
		@Override
		public void run() {
			while (true) {
				try {Thread.sleep(5000);} catch (InterruptedException e) {e.printStackTrace();continue;}
				System.out.println("recoding threads had to wait in " +((double)waitCounter/(double)(waitCounter+noWaitCounter))*100 +" % of cases");
				waitCounter = 0;
				noWaitCounter = 0;
			}
		}
	}
	
	
	private MixPlugIn getThis() {
		return this;
	}

}
