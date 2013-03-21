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


import evaluation.simulator.Simulator;
import evaluation.simulator.core.message.MixMessage;
import evaluation.simulator.core.networkComponent.AbstractClient;
import evaluation.simulator.core.networkComponent.Mix;
import evaluation.simulator.pluginRegistry.ClientSendStyle;
import evaluation.simulator.pluginRegistry.MixSendStyle;
import evaluation.simulator.plugins.clientSendStyle.ClientSendStyleImpl;
import evaluation.simulator.plugins.mixSendStyle.MixSendStyleImpl;


// batch as described by chaum 1981; Dingledine 2002: "Threhold Mix"
// collects messages until "batchSize" messages are reached
// when "batchSize" messages are reached, all messages are sent (in random order)
public class Batch extends OutputStrategyImpl {

	private SimplexBatch requestBatch;
	private SimplexBatch replyBatch;
	
	
	public Batch(Mix mix, Simulator simulator) {

		super(mix, simulator);
		int batchSize = Simulator.settings.getPropertyAsInt("BATCH_SIZE");
		this.requestBatch = new SimplexBatch(batchSize, true);
		this.replyBatch = new SimplexBatch(batchSize, false);
		
	}
	
	
	@Override
	public void incomingRequest(MixMessage mixMessage) {
		requestBatch.addMessage(mixMessage);
	}


	@Override
	public void incomingReply(MixMessage mixMessage) {
		replyBatch.addMessage(mixMessage);
	}
	
	
	public class SimplexBatch {
		
		private boolean isRequestBatch;
		private int batchSize;
		private MixMessage[] collectedMessages;
		private int nextFreeSlot = 0;
		
		
		public SimplexBatch(int batchSize, boolean isRequestBatch) {
			
			this.batchSize = batchSize;	
			this.isRequestBatch = isRequestBatch;
			this.collectedMessages = new MixMessage[batchSize];
				
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			
			collectedMessages[nextFreeSlot++] = mixMessage;
			
			if (nextFreeSlot == batchSize) {
				
				for (MixMessage m: collectedMessages)
					if (isRequestBatch)
						mix.putOutRequest(m);
					else
						mix.putOutReply(m);
				
				nextFreeSlot = 0;
				
			}
			
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
