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
import message.Message;
import message.Reply;
import message.Request;
import architectureInterface.OutputStrategyInterface;
import framework.Implementation;


public class BasicPool extends Implementation implements OutputStrategyInterface {

	private static SecureRandom secureRandom = new SecureRandom();
	private SimplexBasicPool requestPool;
	private SimplexBasicPool replyPool;

	
	@Override
	public void constructor() {
		int poolSize = 4; // TODO: property file!
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
		requestPool.addMessage((Message) request);
	}

	
	@Override
	public void addReply(Reply reply) {
		replyPool.addMessage((Message) reply);
	}

	
	public class SimplexBasicPool {
		
		private boolean isRequestPool;
		private Message[] collectedMessages;
		private int poolSize;
		private int nextFreeSlot = 0;
		
		
		public SimplexBasicPool(boolean isRequestPool, int poolSize) {
			
			this.collectedMessages = new Message[poolSize];
			this.isRequestPool = isRequestPool;
			this.poolSize = poolSize;
			
		}
		
		
		public synchronized void addMessage(Message mixMessage) {
			
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
		
		
		private void putOutMessage(Message mixMessage) {
			
			if (isRequestPool)
				controller.getInputOutputHandler().addRequest((Request)mixMessage);
			else
				controller.getInputOutputHandler().addReply((Reply)mixMessage);
			
		}

	}

	
	@Override
	public String[] getCompatibleImplementations() {
		return (new String[] {	"outputStrategy.BasicBatch",
								"outputStrategy.BasicPool",
								"inputOutputHandler.BasicInputOutputHandler",
								"keyGenerator.BasicKeyGenerator",
								"messageProcessor.BasicMessageProcessor",
								"externalInformationPort.BasicExternalInformationPort",
								"networkClock.BasicSystemTimeClock",
								"userDatabase.BasicUserDatabase",
								"message.BasicMessage"	
			});
	}


	@Override
	public boolean usesPropertyFile() {
		return false;
	}
	
}
