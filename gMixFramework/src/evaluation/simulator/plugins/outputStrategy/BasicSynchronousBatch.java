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

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import evaluation.simulator.Simulator;
import evaluation.simulator.core.event.Event;
import evaluation.simulator.core.event.EventExecutor;
import evaluation.simulator.core.message.MessageFragment;
import evaluation.simulator.core.message.MixMessage;
import evaluation.simulator.core.message.TransportMessage;
import evaluation.simulator.core.networkComponent.AbstractClient;
import evaluation.simulator.core.networkComponent.Mix;
import evaluation.simulator.pluginRegistry.MixSendStyle;
import evaluation.simulator.pluginRegistry.Topology;
import evaluation.simulator.plugins.clientSendStyle.ClientBasicSynchronous;
import evaluation.simulator.plugins.clientSendStyle.ClientSendStyleImpl;
import evaluation.simulator.plugins.clientSendStyle.ClientSendWithoutMixes;
import evaluation.simulator.plugins.mixSendStyle.MixSendStyleImpl;


// 1991: Andreas Pfitzmann, Birgit Pfitzmann, Michael Waidner: ISDN-MIXes: 
// Untraceable Communication with Very Small Bandwidth Overhead
// simplified version; no broadcast; no anonymity for servers
// mix that expects exactly one message from each participant per "sendingRate" (see class "ClientBasicSynchronous")
// (blocks until every client has sent a message!)
// will put out a reply batch every "replyRate" ms 
// (creates dummies if no data available)
public class BasicSynchronousBatch extends OutputStrategyImpl implements EventExecutor {

	private SimplexSynchronousBatch requestBatch;
	private SimplexSynchronousBatch replyBatch;
	private int replyInterval;
	private Map<String, Vector<TransportMessage>> clientReplyWaitingQueues;
	
	
	public BasicSynchronousBatch(Mix mix, Simulator simulator) {
		super(mix, simulator);
		int batchSize = Simulator.getSimulator().getNumberOfClients();
		requestBatch = new SimplexSynchronousBatch(batchSize, true);
		if (super.simulateReplyChannel) {
			replyBatch = new SimplexSynchronousBatch(batchSize, false);
			this.replyInterval = Simulator.settings.getPropertyAsInt("BASIC_SYNCHRONOUS_REPLY_INTERVAL_IN_MS");
			if (mix.isLastMix()) {
				clientReplyWaitingQueues = new HashMap<String, Vector<TransportMessage>>();
				for (AbstractClient c: simulator.getClients().values())
					clientReplyWaitingQueues.put(c.getIdentifier(), new Vector<TransportMessage>(10,10));
			}
			// schedule first reply batch:
			Event putOutNextReplyBatchEvent = new Event(this, Simulator.getNow() + replyInterval, OutputStrategyEvent.PUT_OUT_REPLY_BATCH);
			simulator.scheduleEvent(putOutNextReplyBatchEvent, this);
		}
	}

	
	@Override
	public void incomingRequest(MixMessage mixMessage) {
		requestBatch.addMessage(mixMessage);
	}


	@Override
	public void incomingReply(MixMessage mixMessage) {
		replyBatch.addMessage(mixMessage);
	}
	
	
	public class SimplexSynchronousBatch {
		
		private boolean isRequestBatch;
		private int batchSize;
		private MixMessage[] collectedMessages;
		private int nextFreeSlot = 0;
		
		
		public SimplexSynchronousBatch(int batchSize, boolean isRequestBatch) {
			
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
	
	
	public void incomingReply(TransportMessage transportMessage) {
		if (!mix.isLastMix())
			throw new RuntimeException("ERROR: BasicSynchronousBatch only supports TransportMessage as reply from distant proxy!");
		clientReplyWaitingQueues.get(transportMessage.getOwner().getIdentifier()).add(transportMessage);
	}
	
	
	private void putOutReplyBatch() {
		 
		for (AbstractClient client: simulator.getClients().values()) {
			
			Vector<TransportMessage> replyWaitingQueue =  clientReplyWaitingQueues.get(client.getIdentifier());
			boolean isDummy =  replyWaitingQueue.size() == 0 ? true: false;
			
			MixMessage mixMessage = MixMessage.getInstance(false, mix, client, client, Simulator.getNow(), isDummy);
			
			if (isDummy) {
				
				incomingReply(mixMessage);
				
			} else {

				for (int i=0; i<replyWaitingQueue.size(); i++) {
					
					TransportMessage noneMixMessage = replyWaitingQueue.get(i);
					
					if (mixMessage.getFreeSpace() >= noneMixMessage.getLength() && !noneMixMessage.isFragmented()) { // noneMixMessage fits in mixMessage completely
						
						replyWaitingQueue.remove(noneMixMessage);
						i--;
						mixMessage.addPayloadObject(noneMixMessage);
						
					} else { // add Fragment
						
						if (noneMixMessage.hasNextFragment()) {
							
							MessageFragment messageFragment = noneMixMessage.getFragment(mixMessage.getFreeSpace());
							mixMessage.addPayloadObject(messageFragment);
		
						}
						
						if (!noneMixMessage.hasNextFragment()) {
							replyWaitingQueue.remove(i);
							i--;
						}
						
					}
					
					if (mixMessage.getFreeSpace() == 0)
						break;
				
				}
				
				incomingReply(mixMessage);
				
			}	
			
		}
		
		Event sendNextReplyEvent = new Event(this, Simulator.getNow() + replyInterval, OutputStrategyEvent.PUT_OUT_REPLY_BATCH);
		simulator.scheduleEvent(sendNextReplyEvent, this);
				
	}
	
	
	@Override
	public void executeEvent(Event event) {
		if (event.getEventType() != OutputStrategyEvent.PUT_OUT_REPLY_BATCH)
			throw new RuntimeException("ERROR! received unsupported event!" +event);
		putOutReplyBatch();	
	}


	@Override
	public ClientSendStyleImpl getClientSendStyle(AbstractClient client) {
		boolean noMixes = !Topology.getTopology().containsAtLeastOneMix();
		boolean noRequestChannel = Simulator.settings.getProperty("COMMUNICATION_MODE").equals("SIMPLEX_REPLY");
		if (noMixes || noRequestChannel) {
			return new ClientSendWithoutMixes(client, Simulator.getSimulator());
		} else {
			return new ClientBasicSynchronous(client, Simulator.getSimulator());
		}
	}


	@Override
	public MixSendStyleImpl getMixSendStyle() {
		return MixSendStyle.getInstance(mix, mix);
	}

}
