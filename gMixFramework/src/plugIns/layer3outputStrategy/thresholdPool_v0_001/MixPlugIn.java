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
package plugIns.layer3outputStrategy.thresholdPool_v0_001;

import java.security.SecureRandom;
import java.util.Vector;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer3OutputStrategyMix;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;


//Dingledine 2002:
//The mix fires when n + f messages accumulate in the mix. A pool of f 
//messages, chosen uniformly at random from all the messages, is retained in 
//the mix. (Consider these messages as feedback into the mix.) The other n 
//are forwarded on.
// f -> MIN_POOL_SIZE
// f + n -> THRESHOLD
public class MixPlugIn extends Implementation implements Layer3OutputStrategyMix {

	private static SecureRandom secureRandom = new SecureRandom();
	private SimplexTresholdPool requestPool;
	private SimplexTresholdPool replyPool;
	private int MIN_POOL_SIZE;
	private int THRESHOLD;
			
	
	@Override
	public void constructor() {
		this.MIN_POOL_SIZE = settings.getPropertyAsInt("THRESHOLD_POOL_MIN_POOL_SIZE");
		this.THRESHOLD = settings.getPropertyAsInt("THRESHOLD_POOL_THRESHOLD");
		this.requestPool = new SimplexTresholdPool(true);
		this.replyPool = new SimplexTresholdPool(false);
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

	
	public class SimplexTresholdPool {
		
		private boolean isRequestPool;
		private Vector<MixMessage> collectedMessages;
		private int numberOfMessagesToPutOut;
		
		
		public SimplexTresholdPool(boolean isRequestPool) {
			this.collectedMessages = new Vector<MixMessage>(MIN_POOL_SIZE*2);
			this.isRequestPool = isRequestPool;
			this.numberOfMessagesToPutOut = THRESHOLD - MIN_POOL_SIZE;
		}
		
		
		public synchronized void addMessage(MixMessage mixMessage) {
			collectedMessages.add(mixMessage);
			if (collectedMessages.size() == THRESHOLD) 
				putOutMessages();
		}
		
		
		public void putOutMessages() {
			for (int i=0; i<numberOfMessagesToPutOut; i++) {
				int chosen = secureRandom.nextInt(collectedMessages.size());
				if (isRequestPool)
					anonNode.putOutRequest((Request)collectedMessages.remove(chosen));
				else
					anonNode.putOutReply((Reply)collectedMessages.remove(chosen));
			}
			collectedMessages.clear();
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
