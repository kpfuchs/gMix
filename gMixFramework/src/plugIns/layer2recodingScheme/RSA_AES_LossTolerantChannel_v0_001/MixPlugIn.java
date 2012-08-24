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
package plugIns.layer2recodingScheme.RSA_AES_LossTolerantChannel_v0_001;


import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer2RecodingSchemeMix;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.routing.RoutingMode;
import framework.core.userDatabase.User;
import framework.core.userDatabase.UserAttachment;


public class MixPlugIn extends Implementation implements Layer2RecodingSchemeMix {

	private RSA_AES_LossTolerantChannel_Config config;
	
	private RequestWorkerThread[] requestWorkerThreads;
	private ReplyWorkerThread[] replyWorkerThreads;
	
	private long waitCounter = 0;
	private long noWaitCounter = 0;
	
	
	@Override
	public void constructor() {
		if (anonNode.ROUTING_MODE != RoutingMode.CASCADE)
			throw new RuntimeException("not supported"); // TODO: support it...
		this.config = new RSA_AES_LossTolerantChannel_Config(anonNode, false);
		this.requestWorkerThreads = new RequestWorkerThread[config.NUMBER_OF_THREADS];

		if (anonNode.IS_DUPLEX) {
			this.replyWorkerThreads = new ReplyWorkerThread[config.NUMBER_OF_THREADS];
		}
		
		for (int i=0; i<config.NUMBER_OF_THREADS; i++) {
			RSA_AES_LossTolerantChannel recodingScheme = new RSA_AES_LossTolerantChannel(anonNode, config);
			requestWorkerThreads[i] = new RequestWorkerThread(recodingScheme);
			if (anonNode.IS_DUPLEX) {
				recodingScheme = new RSA_AES_LossTolerantChannel(anonNode, config);
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
		SecretKeySpec symSessionKey;
		Cipher decryptCipher;
		boolean established = false;
		
		Cipher encryptCipher; // for reply channel
		
		Object requestSynchronizer = new Object();
		Object replySynchronizer = new Object();
		
		public ChannelData(User owner) {
			super(owner, getThis());
		}
	}
	

	class RequestWorkerThread extends Thread {
		
		public RSA_AES_LossTolerantChannel recodingScheme;
		
		public RequestWorkerThread(RSA_AES_LossTolerantChannel recodingScheme) {
			this.recodingScheme = recodingScheme;
		}

		
		@Override
		public void run() {
			while (true) { // process messages
				for (Request request: anonNode.getFromRequestInputQueue()) {
					ChannelData channelData = null;
					if (request.getOwner().layer2Id == User.NOT_SET) { // not set
						request.getOwner().layer2Id = 1;
						channelData = new ChannelData(request.getOwner());
					} else {
						channelData = request.getOwner().getAttachment(getThis(), ChannelData.class);
						assert channelData != null;
					}
					long enter = System.nanoTime();
					synchronized (channelData.requestSynchronizer) {
						if (System.nanoTime() - enter > 10000)
							waitCounter++;
						else
							noWaitCounter++;
						request = recodingScheme.recodeMessage(request, channelData); 
						if (request != null)
							outputStrategyLayerMix.addRequest(request); 
					}
				} 
			}	
		}
		
	}

	
	class ReplyWorkerThread extends Thread {
		
		public RSA_AES_LossTolerantChannel recodingScheme;
		
		public ReplyWorkerThread(RSA_AES_LossTolerantChannel recodingScheme) {
			this.recodingScheme = recodingScheme;
		}

		
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
					synchronized (channelData.replySynchronizer) {
						reply = recodingScheme.recodeReply(reply, channelData);
						if (reply != null)
							outputStrategyLayerMix.addReply(reply);
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
