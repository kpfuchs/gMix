/*******************************************************************************
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2014  SVS
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
 *******************************************************************************/
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer3outputStrategy.squareRootPool_v0_001;

import java.security.SecureRandom;
import java.util.Vector;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer3OutputStrategyMix;
import staticContent.framework.message.MixMessage;
import staticContent.framework.message.Reply;
import staticContent.framework.message.Request;
import staticContent.framework.userDatabase.User;


// Serjantov 2007 ("A Fresh Look at the Generalized Mix Framework")
// (Section 4.2 of the paper)
public class MixPlugIn extends Implementation implements Layer3OutputStrategyMix {

	private static SecureRandom secureRandom = new SecureRandom();
	private SimplexBinomialPool requestPool;
	private SimplexBinomialPool replyPool;
	private int DEFAULT_POOL_SIZE;
	private int SEND_INTERVAL;

	
	@Override
	public void constructor() {
		this.DEFAULT_POOL_SIZE = settings.getPropertyAsInt("SQRT_POOL_DEFAULT_POOL_SIZE");
		this.SEND_INTERVAL = settings.getPropertyAsInt("SQRT_POOL_SEND_INTERVAL");
		this.requestPool = new SimplexBinomialPool(true);
		this.replyPool = new SimplexBinomialPool(false);
	}
	
	
	@Override
	public void initialize() {
		// no need to do anything
	}

	
	@Override
	public void begin() {
		// no need to do anything
	}

	
	@Override
	public void addRequest(Request request) {
		requestPool.addMessage((MixMessage) request);
	}

	
	@Override
	public void addReply(Reply reply) {
		replyPool.addMessage((MixMessage) reply);
	}

	
	public class SimplexBinomialPool implements Runnable {

		private boolean isRequestPool;
		private Vector<MixMessage> collectedMessages;
		private boolean isFirstMessage = true;
		private ScheduledThreadPoolExecutor scheduler;
		
		
		public SimplexBinomialPool(	boolean isRequestPool) {
			this.collectedMessages = new Vector<MixMessage>(DEFAULT_POOL_SIZE);
			this.isRequestPool = isRequestPool;
			this.scheduler = new ScheduledThreadPoolExecutor(1);
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			synchronized (this) {
				if (isFirstMessage) {
					isFirstMessage = false;
					scheduler.scheduleAtFixedRate(
							this, 
							SEND_INTERVAL, 
							SEND_INTERVAL, 
							TimeUnit.MILLISECONDS
							);
				}
				collectedMessages.add(mixMessage);
			}
		}

		
		public void putOutMessages() {
			synchronized (this) {
				double M = collectedMessages.size();
				double coinBias = 1d - (1d / Math.sqrt(M));
				for (int i=0; i<collectedMessages.size(); i++) {
					MixMessage m = collectedMessages.get(i);
					if (secureRandom.nextDouble() < coinBias) {
						if (isRequestPool)
							anonNode.putOutRequest((Request)m);
						else
							anonNode.putOutReply((Reply)m);
						collectedMessages.remove(i--);
					}
				}
			}
		}

		
		@Override
		public void run() {
			putOutMessages();
		}
			
	}

	
	@Override
	public int getMaxSizeOfNextWrite() {
		return super.recodingLayerMix.getMaxSizeOfNextReply();
	}


	@Override
	public int getMaxSizeOfNextRead() {
		return super.recodingLayerMix.getMaxSizeOfNextRequest();
	}


	@Override
	public void write(User user, byte[] data) {
		Reply reply = MixMessage.getInstanceReply(data, user); 
		reply.isFirstReplyHop = true;
		transportLayerMix.addLayer4Header(reply);
		anonNode.forwardToLayer2(reply);
	}
	
}