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


import communicationBehaviour.LastMixCommunicationBehaviour;
import message.MixMessage;
import message.NoneMixMessage;
import networkComponent.Mix;
import simulator.Event;
import simulator.EventExecutor;
import simulator.OutputStrategyEvent;
import simulator.Settings;
import simulator.Simulator;


//Cottrell 1995 ("Mixmaster & Remailer Attacks")
//every "outputRate" ms, send x (= "numberOfMessagesInPool" - "minPoolSize") 
//randomly chosen messages (if x >= 1)
public class CottrellTimedPool extends OutputStrategy {

	private SimplexCottrellTimedPool requestBatch;
	private SimplexCottrellTimedPool replyBatch;
	private LastMixCommunicationBehaviour lastMixCommunicationBehaviour = null;
	private static SecureRandom secureRandom = new SecureRandom();
	
	protected CottrellTimedPool(Mix mix, Simulator simulator) {

		super(mix, simulator);
		int sendingRate = new Integer(Settings.getProperty("COTTRELL_TIMED_POOL_SENDING_RATE"));
		int poolSize = new Integer(Settings.getProperty("COTTRELL_TIMED_POOL_POOL_SIZE"));
		
		this.requestBatch = new SimplexCottrellTimedPool(true, sendingRate, poolSize);
		this.replyBatch = new SimplexCottrellTimedPool(false, sendingRate, poolSize);
		this.lastMixCommunicationBehaviour = LastMixCommunicationBehaviour.getInstance(mix, simulator, this);
		
	}
	
	
	@Override
	public void incomingRequest(MixMessage mixMessage) {
		requestBatch.addMessage(mixMessage);
	}


	@Override
	public void incomingReply(MixMessage mixMessage) {
		replyBatch.addMessage(mixMessage);
	}


	@Override
	public void incomingReply(NoneMixMessage noneMixMessage) {
		lastMixCommunicationBehaviour.incomingDataFromDistantProxy(noneMixMessage);
	}
	
	
	public class SimplexCottrellTimedPool implements EventExecutor {
		
		private boolean isRequestPool;
		private Vector<MixMessage> collectedMessages;
		private boolean isFirstMessage = true;
		private int sendingRate;
		private int poolSize;
		
		
		public SimplexCottrellTimedPool(boolean isRequestPool, int sendingRate, int poolSize) {
			
			this.collectedMessages = new Vector<MixMessage>(simulator.getClients().size()*2);	
			this.isRequestPool = isRequestPool;
			this.sendingRate = sendingRate;
			this.poolSize = poolSize;
			
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			
			if (isFirstMessage) {
				isFirstMessage = false;
				scheduleNextOutput();
			}
			
			collectedMessages.add(mixMessage);

		}

		
		public void putOutMessages() {
			
			int numberOfMessagesToPutOut = collectedMessages.size() - poolSize;
			
			if (numberOfMessagesToPutOut > 0) {
				
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