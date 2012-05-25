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
package evaluation.simulator.outputStrategy;

import java.security.SecureRandom;
import java.util.Vector;

import evaluation.simulator.communicationBehaviour.LastMixCommunicationBehaviour;
import evaluation.simulator.core.Event;
import evaluation.simulator.core.EventExecutor;
import evaluation.simulator.core.OutputStrategyEvent;
import evaluation.simulator.core.Simulator;
import evaluation.simulator.message.MixMessage;
import evaluation.simulator.message.NoneMixMessage;
import evaluation.simulator.networkComponent.Mix;


//Cottrell 1995 ("Mixmaster & Remailer Attacks")
//"The mix fires every t seconds, provided there are n + f(min) messages in the 
//mix; however, instead of sending n messages (as in a timed- and-threshold
//constant-pool mix), the mix sends the greater of 1 and m * frac messages,
//and retains the rest in the pool, where m + fmin is the number of messages
//in the mix (m >=n). If n = 1, this is the mix that has been used in the 
//Mixmaster remailer system for years."
// implemented as described in "Generalising Mixes" (Diaz)
public class TimedDynamicPool extends OutputStrategy {

	private SimplexTimedDynamicPool requestPool;
	private SimplexTimedDynamicPool replyPool;
	private LastMixCommunicationBehaviour lastMixCommunicationBehaviour = null;
	private static SecureRandom secureRandom = new SecureRandom();
	
	
	protected TimedDynamicPool(Mix mix, Simulator simulator) {

		super(mix, simulator);
		int sendingRate = Simulator.settings.getPropertyAsInt("TIMED_DYNAMIC_POOL_SENDING_RATE");
		int minMessages = Simulator.settings.getPropertyAsInt("TIMED_DYNAMIC_POOL_MIN_MESSAGES_IN_POOL");
		double fraction = Simulator.settings.getPropertyAsDouble("TIMED_DYNAMIC_POOL_FRACTION");
		this.requestPool = new SimplexTimedDynamicPool(true, sendingRate, minMessages, fraction);
		this.replyPool = new SimplexTimedDynamicPool(false, sendingRate, minMessages, fraction);
		this.lastMixCommunicationBehaviour = LastMixCommunicationBehaviour.getInstance(mix, simulator, this);
		
	}
	

	@Override
	public void incomingRequest(MixMessage mixMessage) {
		requestPool.addMessage(mixMessage);
	}


	@Override
	public void incomingReply(MixMessage mixMessage) {
		replyPool.addMessage(mixMessage);
	}


	@Override
	public void incomingReply(NoneMixMessage noneMixMessage) {
		lastMixCommunicationBehaviour.incomingDataFromDistantProxy(noneMixMessage);
	}
	
	
	public class SimplexTimedDynamicPool implements EventExecutor {

		private boolean isRequestPool;
		private Vector<MixMessage> collectedMessages;
		private boolean isFirstMessage = true;
		private int sendingRate;
		private double minMessages;
		private double fraction;
		
		
		public SimplexTimedDynamicPool(boolean isRequestPool, int sendingRate, int minMessages, double fraction) {
			
			this.collectedMessages = new Vector<MixMessage>(simulator.getClients().size()*2);	
			this.isRequestPool = isRequestPool;
			this.sendingRate = sendingRate;
			this.minMessages = minMessages;
			this.fraction = fraction;
			
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			
			if (isFirstMessage) {
				isFirstMessage = false;
				scheduleNextOutput();
			}
			
			collectedMessages.add(mixMessage);

		}

		
		public void putOutMessages() {
			
			if (collectedMessages.size() > minMessages) {
				
				int numberOfMessagesToPutOut = (int) Math.floor(fraction * (collectedMessages.size() - minMessages));
				
				for (int i=0; i<numberOfMessagesToPutOut; i++) {
					
					int chosen = secureRandom.nextInt(collectedMessages.size());
					
					if (isRequestPool)
						mix.putOutRequest(collectedMessages.remove(chosen));
					else
						mix.putOutReply(collectedMessages.remove(chosen));
				}
				
			}
	
		}
		
		
		private void scheduleNextOutput() {
			
			simulator.scheduleEvent(new Event(this, Simulator.getNow() + sendingRate, OutputStrategyEvent.TIMEOUT), this);
		
		}

		
		@Override
		public void executeEvent(Event e) {
			
			if (e.getEventType() == OutputStrategyEvent.TIMEOUT) {
				putOutMessages();
				if(!stopReplying)
					scheduleNextOutput();
			} else 
				throw new RuntimeException("ERROR: TimedBatch received unknown Event: " +e); 
			
		}
		
	}

}
