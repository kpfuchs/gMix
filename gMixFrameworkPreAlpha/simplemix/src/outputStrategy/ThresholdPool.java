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
import java.util.Vector;

import message.Message;
import message.Reply;
import message.Request;
import architectureInterface.OutputStrategyInterface;
import framework.Implementation;


//Dingledine 2002:
//The mix fires when n + f messages accumulate in the mix. A pool of f 
//messages, chosen uniformly at random from all the messages, is retained in 
//the mix. (Consider these messages as feedback into the mix.) The other n 
//are forwarded on.
public class ThresholdPool extends Implementation implements OutputStrategyInterface {

	private static SecureRandom secureRandom = new SecureRandom();
	private SimplexTresholdPool requestPool;
	private SimplexTresholdPool replyPool;

	
	@Override
	public void constructor() {
		int poolSize = 4; // TODO: property file!
		int threshold = 4; // TODO: property file!
		this.requestPool = new SimplexTresholdPool(true, poolSize, threshold);
		this.replyPool = new SimplexTresholdPool(false, poolSize, threshold);
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

	
	public class SimplexTresholdPool {
		
		private boolean isRequestPool;
		private Vector<Message> collectedMessages;
		private int threshold;
		private int numberOfMessagesToPutOut;
		
		
		public SimplexTresholdPool(boolean isRequestPool, int poolSize, int threshold) {
			
			this.collectedMessages = new Vector<Message>(poolSize*2);
			this.isRequestPool = isRequestPool;
			this.threshold = threshold;
			this.numberOfMessagesToPutOut = threshold - poolSize;
			
		}
		
		
		public synchronized void addMessage(Message message) {
			
			collectedMessages.add(message);
			
			if (collectedMessages.size() == threshold)
				putOutMessages();

		}
		
		
		public void putOutMessages() {
			
			for (int i=0; i<numberOfMessagesToPutOut; i++) {
					
				int chosen = secureRandom.nextInt(collectedMessages.size());
					
				if (isRequestPool)
					controller.getInputOutputHandler().addRequest((Request)collectedMessages.remove(chosen));
				else
					controller.getInputOutputHandler().addReply((Reply)collectedMessages.remove(chosen));

			}
	
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
