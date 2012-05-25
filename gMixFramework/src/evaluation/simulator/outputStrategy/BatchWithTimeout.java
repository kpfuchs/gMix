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

package evaluation.simulator.outputStrategy;

import java.util.Vector;


import evaluation.simulator.communicationBehaviour.LastMixCommunicationBehaviour;
import evaluation.simulator.core.Event;
import evaluation.simulator.core.EventExecutor;
import evaluation.simulator.core.OutputStrategyEvent;
import evaluation.simulator.core.Simulator;
import evaluation.simulator.message.MixMessage;
import evaluation.simulator.message.NoneMixMessage;
import evaluation.simulator.networkComponent.Mix;



// Dingledine 2002: Timed Mix
// collects messages until "batchSize" messages are reached or a timeout occurs
// the timeout timer is started when the first message is added to the batch
// and gets canceled as the batch is put out (due to reaching the batch size)
// see also: "ThresholdOrTimedBatch.java"
public class BatchWithTimeout extends OutputStrategy {

	private SimplexBatchWithTimeout requestBatch;
	private SimplexBatchWithTimeout replyBatch;
	private LastMixCommunicationBehaviour lastMixCommunicationBehaviour = null;
	
	
	protected BatchWithTimeout(Mix mix, Simulator simulator) {

		super(mix, simulator);
		int timeout = Simulator.settings.getPropertyAsInt("BATCH_WITH_TIMEOUT_TIMEOUT");
		int batchSize = Simulator.settings.getPropertyAsInt("BATCH_WITH_TIMEOUT_BATCH_SIZE");
		this.requestBatch = new SimplexBatchWithTimeout(true, timeout,batchSize );
		this.replyBatch = new SimplexBatchWithTimeout(false, timeout, batchSize);
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
	
	
	public class SimplexBatchWithTimeout implements EventExecutor {
		
		private boolean isRequestBatch;
		private Vector<MixMessage> collectedMessages;
		private int timeout;
		private int batchSize;
		private Event timeoutEvent;
		
		
		public SimplexBatchWithTimeout(boolean isRequestBatch, int timeout, int batchSize) {
			
			this.collectedMessages = new Vector<MixMessage>(batchSize);	
			this.isRequestBatch = isRequestBatch;
			this.timeout = timeout;
			this.batchSize = batchSize;
			
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			
			collectedMessages.add(mixMessage);
			
			if (collectedMessages.size() == 0)
				setTimeout();
			else if (collectedMessages.size() == batchSize)
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
			cancelTimeout();
			
		}
		
		
		private void setTimeout() {
			
			this.timeoutEvent = new Event(this, Simulator.getNow() + timeout, OutputStrategyEvent.TIMEOUT);
			simulator.scheduleEvent(timeoutEvent, this);
		
		}

		
		private void cancelTimeout() {
			
			simulator.unscheduleEvent(this.timeoutEvent);
			this.timeoutEvent = null;
			
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
