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

import evaluation.simulator.Simulator;
import evaluation.simulator.annotations.plugin.Plugin;
import evaluation.simulator.annotations.property.IntSimulationProperty;
import evaluation.simulator.core.message.MixMessage;
import evaluation.simulator.core.networkComponent.AbstractClient;
import evaluation.simulator.core.networkComponent.Mix;
import evaluation.simulator.core.statistics.Statistics;
import evaluation.simulator.pluginRegistry.ClientSendStyle;
import evaluation.simulator.pluginRegistry.MixSendStyle;
import evaluation.simulator.pluginRegistry.StatisticsType;
import evaluation.simulator.plugins.clientSendStyle.ClientSendStyleImpl;
import evaluation.simulator.plugins.mixSendStyle.MixSendStyleImpl;

// batch as described by chaum 1981; Dingledine 2002: "Threhold Mix"
// collects messages until "batchSize" messages are reached
// when "batchSize" messages are reached, all messages are sent (in random
// order)
@Plugin(pluginKey = "BASIC_BATCH", pluginName = "Basic Batch")
public class Batch extends OutputStrategyImpl {

	@IntSimulationProperty( 
			name = "Batch size (requests)", 
			key = "BASIC_BATCH_BATCH_SIZE", 
			min = 1)
	private int batchSize;

	private final SimplexBatch replyBatch;

	private final SimplexBatch requestBatch;
	
	private Statistics statistics;
	

	public Batch(Mix mix, Simulator simulator) {
		super(mix, simulator);
		this.statistics = super.getOwner().getStatistics();
		batchSize = Simulator.settings.getPropertyAsInt("BASIC_BATCH_BATCH_SIZE");
		this.requestBatch = new SimplexBatch(batchSize, true);
		this.replyBatch = new SimplexBatch(batchSize, false);
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

	
	public class SimplexBatch {

		private final int batchSize;
		private final MixMessage[] collectedMessages;
		private final boolean isRequestBatch;
		private int nextFreeSlot = 0;
		
		public SimplexBatch(int batchSize, boolean isRequestBatch) {

			this.batchSize = batchSize;
			this.isRequestBatch = isRequestBatch;
			this.collectedMessages = new MixMessage[batchSize];

		}

		public void addMessage(MixMessage mixMessage) {

			this.collectedMessages[this.nextFreeSlot++] = mixMessage;

			if (this.nextFreeSlot == this.batchSize) {
				statistics.addValue(this.batchSize, StatisticsType.AVG_BATCH_SIZE);


				for (MixMessage m : this.collectedMessages) {
					if (this.isRequestBatch) {
						Batch.this.mix.putOutRequest(m);
					} else {
						Batch.this.mix.putOutReply(m);
					}
				}

				this.nextFreeSlot = 0;

			}

		}

	}
	
}
