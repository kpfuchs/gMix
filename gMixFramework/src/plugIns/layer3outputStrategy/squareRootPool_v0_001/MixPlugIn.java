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
package plugIns.layer3outputStrategy.squareRootPool_v0_001;

import java.security.SecureRandom;
import java.util.Vector;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer3OutputStrategyMix;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;


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
	public int getMaxSizeOfNextReply() {
		return super.recodingLayerMix.getMaxSizeOfNextReply();
	}


	@Override
	public int getMaxSizeOfNextRequest() {
		return super.recodingLayerMix.getMaxSizeOfNextRequest();
	}
	
}
