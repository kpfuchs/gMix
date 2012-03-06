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

package simulator.outputStrategy;


import simulator.communicationBehaviour.LastMixCommunicationBehaviour;
import simulator.core.Settings;
import simulator.core.Simulator;
import simulator.message.MixMessage;
import simulator.message.NoneMixMessage;
import simulator.networkComponent.Mix;


// batch as described by chaum 1981; Dingledine 2002: "Threhold Mix"
// collects messages until "batchSize" messages are reached
// when "batchSize" messages are reached, all messages are sent (in random order)
public class Batch extends OutputStrategy {

	private SimplexBatch requestBatch;
	private SimplexBatch replyBatch;
	private LastMixCommunicationBehaviour lastMixCommunicationBehaviour;
	
	
	protected Batch(Mix mix, Simulator simulator) {

		super(mix, simulator);
		int batchSize = new Integer(Settings.getProperty("BATCH_SIZE"));
		this.requestBatch = new SimplexBatch(batchSize, true);
		this.replyBatch = new SimplexBatch(batchSize, false);
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
	
}
