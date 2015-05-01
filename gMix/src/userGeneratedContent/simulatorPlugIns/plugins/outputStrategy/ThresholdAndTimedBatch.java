/*******************************************************************************
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2014  SVS
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
 *******************************************************************************/
package userGeneratedContent.simulatorPlugIns.plugins.outputStrategy;

import java.util.Vector;

import staticContent.evaluation.simulator.Simulator;
import staticContent.evaluation.simulator.annotations.plugin.Plugin;
import staticContent.evaluation.simulator.annotations.property.IntSimulationProperty;
import staticContent.evaluation.simulator.core.event.Event;
import staticContent.evaluation.simulator.core.event.EventExecutor;
import staticContent.evaluation.simulator.core.message.MixMessage;
import staticContent.evaluation.simulator.core.networkComponent.AbstractClient;
import staticContent.evaluation.simulator.core.networkComponent.Mix;
import userGeneratedContent.simulatorPlugIns.pluginRegistry.ClientSendStyle;
import userGeneratedContent.simulatorPlugIns.pluginRegistry.MixSendStyle;
import userGeneratedContent.simulatorPlugIns.plugins.clientSendStyle.ClientSendStyleImpl;
import userGeneratedContent.simulatorPlugIns.plugins.mixSendStyle.MixSendStyleImpl;



//Dingledine 2002: Timed Mix
// "fires (flushes all messages) every t seconds but only
// when at least n messages have accumulated in the mix.
@Plugin(pluginKey = "THRESHOLD_AND_TIMED_BATCH", pluginName = "Threshold And Timed Batch")
public class ThresholdAndTimedBatch extends OutputStrategyImpl {

	private SimplexThresholdAndTimedBatch requestBatch;
	private SimplexThresholdAndTimedBatch replyBatch;
	
	@IntSimulationProperty( name = "Sending Rate (ms)", 
			key = "THRESHOLD_AND_TIMED_BATCH_SENDING_RATE_IN_MS",
			min = 0)
	private int sendingRate;
	
	@IntSimulationProperty( name = "Batch Size (requests)", 
			key = "THRESHOLD_AND_TIMED_BATCH_BATCH_SIZE",
			min = 1)
	private int batchSize;
	
	public ThresholdAndTimedBatch(Mix mix, Simulator simulator) {
		super(mix, simulator);
		this.sendingRate = Simulator.settings.getPropertyAsInt("THRESHOLD_AND_TIMED_BATCH_SENDING_RATE_IN_MS");
		this.batchSize = Simulator.settings.getPropertyAsInt("THRESHOLD_AND_TIMED_BATCH_BATCH_SIZE");
		this.requestBatch = new SimplexThresholdAndTimedBatch(true, sendingRate,batchSize );
		this.replyBatch = new SimplexThresholdAndTimedBatch(false, sendingRate, batchSize);
	}
	
	
	@Override
	public void incomingRequest(MixMessage mixMessage) {
		requestBatch.addMessage(mixMessage);
	}


	@Override
	public void incomingReply(MixMessage mixMessage) {
		replyBatch.addMessage(mixMessage);
	}
	
	
	public class SimplexThresholdAndTimedBatch implements EventExecutor {
		
		private boolean isRequestBatch;
		private Vector<MixMessage> collectedMessages;
		private boolean isFirstMessage = true;
		private int sendingRate;
		private int batchSize;
		
		
		public SimplexThresholdAndTimedBatch(boolean isRequestBatch, int sendingRate, int batchSize) {
			
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

		}

		
		public void putOutMessages() {
			
			if (collectedMessages.size() <= batchSize)
				return;
				
			if (isRequestBatch)
				for (MixMessage m:collectedMessages)
					mix.putOutRequest(m);
			else
				for (MixMessage m:collectedMessages)
					mix.putOutReply(m);
			
			this.collectedMessages = new Vector<MixMessage>(collectedMessages.size()*2);	
				
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