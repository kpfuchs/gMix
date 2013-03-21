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
package evaluation.simulator.plugins.outputStrategy;

import java.util.Vector;


import evaluation.simulator.Simulator;
import evaluation.simulator.core.event.Event;
import evaluation.simulator.core.event.EventExecutor;
import evaluation.simulator.core.message.MixMessage;
import evaluation.simulator.core.networkComponent.AbstractClient;
import evaluation.simulator.core.networkComponent.Mix;
import evaluation.simulator.pluginRegistry.ClientSendStyle;
import evaluation.simulator.pluginRegistry.MixSendStyle;
import evaluation.simulator.plugins.clientSendStyle.ClientSendStyleImpl;
import evaluation.simulator.plugins.mixSendStyle.MixSendStyleImpl;


//Dingledine 2002: Timed Mix
//"The mix fires (flushes all messages) every t seconds or when n messages 
// accumulate in the mix."
// see also: "BatchWithTimeout.java"
public class ThresholdOrTimedBatch extends OutputStrategyImpl {

	private SimplexThresholdOrTimedBatch requestBatch;
	private SimplexThresholdOrTimedBatch replyBatch;
	
	
	public ThresholdOrTimedBatch(Mix mix, Simulator simulator) {
		super(mix, simulator);
		int sendingRate = Simulator.settings.getPropertyAsInt("THRESHOLD_OR_TIMED_BATCH_SENDING_RATE");
		int batchSize = Simulator.settings.getPropertyAsInt("THRESHOLD_OR_TIMED_BATCH_BATCH_SIZE");
		this.requestBatch = new SimplexThresholdOrTimedBatch(true, sendingRate,batchSize );
		this.replyBatch = new SimplexThresholdOrTimedBatch(false, sendingRate, batchSize);
	}
	
	
	@Override
	public void incomingRequest(MixMessage mixMessage) {
		requestBatch.addMessage(mixMessage);
	}


	@Override
	public void incomingReply(MixMessage mixMessage) {
		replyBatch.addMessage(mixMessage);
	}
	
	
	public class SimplexThresholdOrTimedBatch implements EventExecutor {
		
		private boolean isRequestBatch;
		private Vector<MixMessage> collectedMessages;
		private boolean isFirstMessage = true;
		private int sendingRate;
		private int batchSize;
		
		
		public SimplexThresholdOrTimedBatch(boolean isRequestBatch, int sendingRate, int batchSize) {
			
			this.collectedMessages = new Vector<MixMessage>(batchSize);	
			this.isRequestBatch = isRequestBatch;
			this.sendingRate = sendingRate;
			this.batchSize = batchSize;
			
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			
			if (isFirstMessage) {
				isFirstMessage = false;
				scheduleNextOutput();
			}
			
			collectedMessages.add(mixMessage);
			
			if (collectedMessages.size() == batchSize)
				putOutMessages();

		}

		
		public void putOutMessages() {
			
			if (isRequestBatch)
				for (MixMessage m:collectedMessages)
					mix.putOutRequest(m);
			else
				for (MixMessage m:collectedMessages)
					mix.putOutReply(m);
			
			this.collectedMessages = new Vector<MixMessage>(batchSize);	
				
		}
		
		
		private void scheduleNextOutput() {
			
			simulator.scheduleEvent(new Event(this, Simulator.getNow() + sendingRate, OutputStrategyEvent.TIMEOUT), this);
		
		}

		
		@Override
		public void executeEvent(Event e) {
			
			if (e.getEventType() == OutputStrategyEvent.TIMEOUT) {
				putOutMessages();
				scheduleNextOutput();	
			} else 
				throw new RuntimeException("ERROR: TimedBatch received unknown Event: " +e); 
			
		}
		
	}
	
	
	@Override
	public ClientSendStyleImpl getClientSendStyle(AbstractClient client) {
		return ClientSendStyle.getInstance(client);
	}


	@Override
	public MixSendStyleImpl getMixSendStyle() {
		return MixSendStyle.getInstance(mix, mix);
	}

}