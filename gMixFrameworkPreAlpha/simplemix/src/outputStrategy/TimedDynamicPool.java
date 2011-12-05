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
//"The mix fires every t seconds, provided there are n + f(min) messages in the 
//mix; however, instead of sending n messages (as in a timed- and-threshold
//constant-pool mix), the mix sends the greater of 1 and m * frac messages,
//and retains the rest in the pool, where m + fmin is the number of messages
//in the mix (m >=n). If n = 1, this is the mix that has been used in the 
//Mixmaster remailer system for years."
//implemented as described in "Generalising Mixes" (Diaz)
public class TimedDynamicPool extends Implementation implements OutputStrategyInterface {

	private static SecureRandom secureRandom = new SecureRandom();
	private SimplexTimedDynamicPool requestPool;
	private SimplexTimedDynamicPool replyPool;

	
	@Override
	public void constructor() {
		int sendingRate = 1000; // TODO: property file!
		int minMessages = 100; // TODO: property file!
		double fraction = 0.2; // TODO: property file!
		this.requestPool = new SimplexTimedDynamicPool(true, sendingRate, minMessages, fraction);
		this.replyPool = new SimplexTimedDynamicPool(false, sendingRate, minMessages, fraction);
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

	
	public class SimplexTimedDynamicPool {

		private boolean isRequestPool;
		private Vector<Message> collectedMessages;
		private boolean isFirstMessage = true;
		private int sendingRate;
		private double minMessages;
		private double fraction;
		private Timer timer = new Timer();
		
		
		public SimplexTimedDynamicPool(boolean isRequestPool, int sendingRate, int minMessages, double fraction) {
			
			this.collectedMessages = new Vector<Message>(100);	// TODO property file
			this.isRequestPool = isRequestPool;
			this.sendingRate = sendingRate;
			this.minMessages = minMessages;
			this.fraction = fraction;
			
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
				
				if (collectedMessages.size() > minMessages) {
					
					int numberOfMessagesToPutOut = (int) Math.floor(fraction * (collectedMessages.size() - minMessages));
					
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

			private SimplexTimedDynamicPool linkedPool;
			
			protected TimeoutTask(SimplexTimedDynamicPool linkedPool) {
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
