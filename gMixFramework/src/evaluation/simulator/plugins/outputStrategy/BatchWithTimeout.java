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
package evaluation.simulator.plugins.outputStrategy;

import java.util.Vector;

import evaluation.simulator.Simulator;
import evaluation.simulator.annotations.plugin.Plugin;
import evaluation.simulator.annotations.property.IntSimulationProperty;
import evaluation.simulator.core.event.Event;
import evaluation.simulator.core.event.EventExecutor;
import evaluation.simulator.core.message.MixMessage;
import evaluation.simulator.core.networkComponent.AbstractClient;
import evaluation.simulator.core.networkComponent.Mix;
import evaluation.simulator.core.statistics.Statistics;
import evaluation.simulator.pluginRegistry.ClientSendStyle;
import evaluation.simulator.pluginRegistry.MixSendStyle;
import evaluation.simulator.pluginRegistry.StatisticsType;
import evaluation.simulator.plugins.clientSendStyle.ClientSendStyleImpl;
import evaluation.simulator.plugins.mixSendStyle.MixSendStyleImpl;


// Dingledine 2002: Timed Mix
// collects messages until "batchSize" messages are reached or a timeout occurs
// the timeout timer is started when the first message is added to the batch
// and gets canceled as the batch is put out (due to reaching the batch size)
// see also: "ThresholdOrTimedBatch.java"
@Plugin(pluginKey = "BATCH_WITH_TIMEOUT", pluginName = "Batch With Timeout")
public class BatchWithTimeout extends OutputStrategyImpl {

	@IntSimulationProperty( name = "Timeout (ms)", 
			key = "TIMEOUT_IN_MS", position=51,
			min = 0)
	private int timeout;
	
	@IntSimulationProperty( name = "Batch size (requests)", 
			key = "BATCH_WITH_TIMEOUT_BATCH_SIZE", 
			position=52,
			min = 0)
	private int batchSize;
	
	private Statistics statistics;

	
	private final SimplexBatchWithTimeout replyBatch;
	private final SimplexBatchWithTimeout requestBatch;

	
	public BatchWithTimeout(Mix mix, Simulator simulator) {
		super(mix, simulator);
		this.statistics = super.getOwner().getStatistics();
		timeout = Simulator.settings.getPropertyAsInt("TIMEOUT_IN_MS");
		batchSize = Simulator.settings.getPropertyAsInt("BATCH_WITH_TIMEOUT_BATCH_SIZE");
		this.requestBatch = new SimplexBatchWithTimeout(true, timeout,
				batchSize);
		this.replyBatch = new SimplexBatchWithTimeout(false, timeout, batchSize);
	}

	
	@Override
	public ClientSendStyleImpl getClientSendStyle(AbstractClient client) {
		return ClientSendStyle.getInstance(client);
	}

	
	@Override
	public MixSendStyleImpl getMixSendStyle() {
		return MixSendStyle.getInstance(this.mix, this.mix);
	}
	

	@Override
	public void incomingReply(MixMessage mixMessage) {
		this.replyBatch.addMessage(mixMessage);
	}

	
	@Override
	public void incomingRequest(MixMessage mixMessage) {
		this.requestBatch.addMessage(mixMessage);
	}

	
	public class SimplexBatchWithTimeout implements EventExecutor {

		private final int batchSize;
		private Vector<MixMessage> collectedMessages;
		private final boolean isRequestBatch;
	
		private final int timeout;
		private Event timeoutEvent;

		
		public SimplexBatchWithTimeout(boolean isRequestBatch, int timeout,
				int batchSize) {

			this.collectedMessages = new Vector<MixMessage>(batchSize);
			this.isRequestBatch = isRequestBatch;
			this.timeout = timeout;
			this.batchSize = batchSize;

		}

		
		public void addMessage(MixMessage mixMessage) {

			if (this.collectedMessages.size() == 0) {
				this.setTimeout();
			}

			this.collectedMessages.add(mixMessage);

			if (this.collectedMessages.size() == this.batchSize) {
				this.putOutMessages();
			}

		}
		

		private void cancelTimeout() {
			if (this.timeoutEvent != null) {
				BatchWithTimeout.this.simulator
						.unscheduleEvent(this.timeoutEvent);
				this.timeoutEvent = null;
			}
		}
		

		@Override
		public void executeEvent(Event e) {

			if (e.getEventType() == OutputStrategyEvent.TIMEOUT) {
				this.putOutMessages();
			} else {
				throw new RuntimeException(
						"ERROR: TimedBatch received unknown Event: " + e);
			}

		}

		
		public void putOutMessages() {

			statistics.addValue(collectedMessages.size(), StatisticsType.AVG_BATCH_SIZE);

			if (this.isRequestBatch) {
				for (MixMessage m : this.collectedMessages) {
					BatchWithTimeout.this.mix.putOutRequest(m);
				}
			} else {
				for (MixMessage m : this.collectedMessages) {
					BatchWithTimeout.this.mix.putOutReply(m);
				}
			}

			this.collectedMessages = new Vector<MixMessage>(this.batchSize);
			this.cancelTimeout();

		}

		
		private void setTimeout() {

			this.timeoutEvent = new Event(this, Simulator.getNow()
					+ this.timeout, OutputStrategyEvent.TIMEOUT);
			BatchWithTimeout.this.simulator.scheduleEvent(this.timeoutEvent,
					this);

		}

	}
	
}
