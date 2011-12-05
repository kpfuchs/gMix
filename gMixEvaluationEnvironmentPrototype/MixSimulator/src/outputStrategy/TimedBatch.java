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

import java.util.Vector;

import message.MixMessage;
import message.NoneMixMessage;
import networkComponent.Mix;
import simulator.Event;
import simulator.EventExecutor;
import simulator.OutputStrategyEvent;
import simulator.Settings;
import simulator.Simulator;
import communicationBehaviour.LastMixCommunicationBehaviour;


// Dingledine 2002: Timed Mix
// "The mix fires (flushes all messages) every t seconds"
public class TimedBatch extends OutputStrategy {

	private SimplexTimedMix requestBatch;
	private SimplexTimedMix replyBatch;
	private LastMixCommunicationBehaviour lastMixCommunicationBehaviour = null;
	
	
	protected TimedBatch(Mix mix, Simulator simulator) {

		super(mix, simulator);
		int sendingRate = new Integer(Settings.getProperty("TIMED_BATCH_SENDING_RATE"));
		this.requestBatch = new SimplexTimedMix(true, sendingRate);
		this.replyBatch = new SimplexTimedMix(false, sendingRate);
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
	
	
	public class SimplexTimedMix implements EventExecutor {
		
		private boolean isRequestTimedMix;
		private Vector<MixMessage> collectedMessages;
		private boolean isFirstMessage = true;
		private int sendingRate;
		
		
		public SimplexTimedMix(boolean isRequestTimedMix, int sendingRate) {
			
			this.collectedMessages = new Vector<MixMessage>(simulator.getClients().size()*2);	
			this.isRequestTimedMix = isRequestTimedMix;
			this.sendingRate = sendingRate;
			
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			
			if (isFirstMessage) {
				isFirstMessage = false;
				scheduleNextOutput();
			}
			
			collectedMessages.add(mixMessage);

		}

		
		public void putOutMessages() {
			
			for (MixMessage m:collectedMessages)
				if (isRequestTimedMix)
					mix.putOutRequest(m);
				else
					mix.putOutReply(m);
			
			this.collectedMessages = new Vector<MixMessage>(collectedMessages.size() * 2);	
			if(!stopReplying)
				scheduleNextOutput();		
			
		}
		
		
		private void scheduleNextOutput() {
			
			simulator.scheduleEvent(new Event(this, Simulator.getNow() + sendingRate, OutputStrategyEvent.TIMEOUT), this);
		
		}

		
		@Override
		public void executeEvent(Event e) {
			
			if (e.getEventType() == OutputStrategyEvent.TIMEOUT) {
				putOutMessages();
			} else 
				throw new RuntimeException("ERROR: TimedBatch received unknown Event: " +e); 
			
		}
		
	}

}