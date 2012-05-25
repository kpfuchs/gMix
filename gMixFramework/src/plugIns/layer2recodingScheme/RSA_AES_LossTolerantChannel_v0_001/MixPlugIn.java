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
package plugIns.layer2recodingScheme.RSA_AES_LossTolerantChannel_v0_001;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer2RecodingSchemeMix;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.userDatabase.User;
import framework.core.userDatabase.UserAttachment;


public class MixPlugIn extends Implementation implements Layer2RecodingSchemeMix {

	private RSA_AES_LossTolerantChannel_Config config;
	private WorkerThread[] workerThreads;
	
	
	@Override
	public void constructor() {
		if (anonNode.IS_FREE_ROUTE)
			throw new RuntimeException("not supported"); // TODO: support it...
		this.config = new RSA_AES_LossTolerantChannel_Config(anonNode, false);
		this.workerThreads = new WorkerThread[config.NUMBER_OF_THREADS];
		for (int i=0; i<config.NUMBER_OF_THREADS; i++) {
			RSA_AES_LossTolerantChannel recodingScheme = new RSA_AES_LossTolerantChannel(anonNode, config);
			workerThreads[i] = new WorkerThread(recodingScheme);
		}
	}

	
	@Override
	public void initialize() {
		assert !anonNode.IS_FREE_ROUTE;
		for (int i=0; i<workerThreads.length; i++)
			workerThreads[i].recodingScheme.initAsRecoder();
	}
	

	@Override
	public void begin() {
		for (int i=0; i<workerThreads.length; i++)
			this.workerThreads[i].start();
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
		
		//Cipher encryptCipher; // for reply channel
		//int threadId;
		
		public ChannelData(User owner) {
			super(owner, getThis());
			//this.threadId = threadId;
		}
	}
	
	
	/*class RequestDistributorThread extends Thread {
	
	private int next = -1;
	
	@Override
	public void run() {
		while (true) { // process messages
			Request request = inputOutputHandler.getRequest();
			if (request.getOwner().getAttachment(getThis(), ChannelData.class) == null) {
				int threadId = getNextThreadId();
				new ChannelData(request.getOwner(), threadId);
				try {
					workerThreads[threadId].queue.put(request);
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				}
			} else {
				ChannelData cd = request.getOwner().getAttachment(getThis(), ChannelData.class);
				try {
					workerThreads[cd.threadId].queue.put(request);
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				}
			}
		}	
	}
	
	private int getNextThreadId() {
		if (++next == workerThreads.length)
			next = 0;
		return next;
	}
	}*/


	class WorkerThread extends Thread {
	
		RSA_AES_LossTolerantChannel recodingScheme;
	
		public WorkerThread(RSA_AES_LossTolerantChannel recodingScheme) {
			this.recodingScheme = recodingScheme;
		}

	
		@Override
		public void run() {
		
			while (true) {
				Request[] requests = anonNode.getFromRequestInputQueue();
				for (Request request:requests) {
					synchronized (getThis()) {
						if (request.getOwner().getAttachment(getThis(), ChannelData.class) == null)// {
							new ChannelData(request.getOwner());
						/*} else {
							request.getOwner().getAttachment(getThis(), ChannelData.class);
						}*/
					}
					request = recodingScheme.recodeMessage(request);
					if (request != null)
						outputStrategyLayerMix.addRequest(request);
				}
			
			
			/*while (true) { // process messages
				Request request = inputOutputHandler.getRequest();
				synchronized (getThis()) {
					if (request.getOwner().getAttachment(getThis(), ChannelData.class) == null)// {
						new ChannelData(request.getOwner());
					/*} else {
						request.getOwner().getAttachment(getThis(), ChannelData.class);
					}*/
				/*}
				request = recodingScheme.recodeMessage(request);
				if (request != null)
					outputStrategy.addRequest(request);
				
				/*if (message instanceof Request) {
					Request recodedMsg = recodingScheme.recodeMessage((Request)message);
					if (recodedMsg != null)
						outputStrategy.addRequest(recodedMsg);
				}/* else {
					Reply recodedMsg = recodingScheme.recodeReply((Reply)message);
					if (recodedMsg != null)
						outputStrategy.addReply(recodedMsg);
				}*/
				
			//}	
			}
		}
	}
	
	
	private MixPlugIn getThis() {
		return this;
	}

}
