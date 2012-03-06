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

package outputStrategy;

import java.security.SecureRandom;

import message.MixMessage;
import message.Reply;
import message.Request;
import framework.Implementation;


public class BasicPool_v1 extends Implementation implements OutputStrategy {

	private static SecureRandom secureRandom = new SecureRandom();
	private SimplexBasicPool requestPool;
	private SimplexBasicPool replyPool;

	
	@Override
	public void constructor() {
		int poolSize = settings.getPropertyAsInt("BasicPool_v1_POOL_SIZE");
		this.requestPool = new SimplexBasicPool(true, poolSize);
		this.replyPool = new SimplexBasicPool(false, poolSize);
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

	
	public class SimplexBasicPool {
		
		private boolean isRequestPool;
		private MixMessage[] collectedMessages;
		private int poolSize;
		private int nextFreeSlot = 0;
		
		
		public SimplexBasicPool(boolean isRequestPool, int poolSize) {
			
			this.collectedMessages = new MixMessage[poolSize];
			this.isRequestPool = isRequestPool;
			this.poolSize = poolSize;
			
		}
		
		
		public synchronized void addMessage(MixMessage mixMessage) {
			
			if (nextFreeSlot < poolSize) {
				
				collectedMessages[nextFreeSlot++] = mixMessage;
				
			} else {
				
				int chosen = secureRandom.nextInt(poolSize+1);
				
				if (chosen == poolSize) {
					
					putOutMessage(mixMessage);
				
				} else {
					
					putOutMessage(collectedMessages[chosen]);
					collectedMessages[chosen] = mixMessage;
					
				}
		
			}

		}
		
		
		private void putOutMessage(MixMessage mixMessage) {
			
			if (isRequestPool)
				controller.getInputOutputHandler().addRequest((Request)mixMessage);
			else
				controller.getInputOutputHandler().addReply((Reply)mixMessage);
			
		}

	}

}
