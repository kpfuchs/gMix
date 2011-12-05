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
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import message.Message;
import message.Reply;
import message.Request;
import architectureInterface.OutputStrategyInterface;
import framework.Implementation;


//Cottrell 1995 ("Mixmaster & Remailer Attacks")
//every "outputRate" ms, send x (= "numberOfMessagesInPool" - "minPoolSize") 
//randomly chosen messages (if x >= 1)
public class CottrellTimedPool extends Implementation implements OutputStrategyInterface {

	private SimplexCottrellTimedPool requestPool;
	private SimplexCottrellTimedPool replyPool;
	private static SecureRandom secureRandom = new SecureRandom();
	
	
	@Override
	public void constructor() {
		
		int sendingRate = 1000; // TODO: property file!
		int minPoolSize = 10; // TODO: property file!
		
		this.requestPool = new SimplexCottrellTimedPool(true, sendingRate, minPoolSize);
		this.replyPool = new SimplexCottrellTimedPool(false, sendingRate, minPoolSize);

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
		requestPool.addMessage((Message)request);
	}

	
	@Override
	public void addReply(Reply reply) {
		replyPool.addMessage((Message)reply);
	}

	
	public class SimplexCottrellTimedPool {

		private boolean isRequestPool;
		private Vector<Message> collectedMessages;
		private boolean isFirstMessage = true;
		private int sendingRate;
		private int minPoolSize;
		private Timer timer = new Timer();
		
		
		public SimplexCottrellTimedPool(boolean isRequestPool, int sendingRate, int minPoolSize) {
			
			this.collectedMessages = new Vector<Message>(100);	// TODO property file
			this.isRequestPool = isRequestPool;
			this.sendingRate = sendingRate;
			this.minPoolSize = minPoolSize;
			
		}
		
		
		public void addMessage(Message mixMessage) {
			
			synchronized (collectedMessages) {
				
				if (isFirstMessage) {
					isFirstMessage = false;
					timer.scheduleAtFixedRate(new TimeoutTask(this), sendingRate, sendingRate);
				}
				
				collectedMessages.add(mixMessage);
				
			}

		}

		
		public void putOutMessages() {
			
			synchronized (collectedMessages) {
				
				int numberOfMessagesToPutOut = collectedMessages.size() - minPoolSize;
				
				if (numberOfMessagesToPutOut > 0) {
					
					for (int i=0; i<numberOfMessagesToPutOut; i++) {
						
						int chosen = secureRandom.nextInt(collectedMessages.size());
						
						if (isRequestPool)
							controller.getInputOutputHandler().addRequest((Request)collectedMessages.remove(chosen));
						else
							controller.getInputOutputHandler().addReply((Reply)collectedMessages.remove(chosen));
						
					}
				
				}
	
			}
				
		}
	
		
		private final class TimeoutTask extends TimerTask {

			private SimplexCottrellTimedPool linkedPool;
			
			protected TimeoutTask(SimplexCottrellTimedPool linkedPool) {
				this.linkedPool = linkedPool;
			}
			
			@Override 
			public void run() {
				linkedPool.putOutMessages();
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
