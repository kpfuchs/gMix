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

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import evaluation.simulator.Simulator;
import evaluation.simulator.annotations.plugin.Plugin;
import evaluation.simulator.annotations.property.IntSimulationProperty;
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
// mix that expects exactly one message from each participant per "sendingRate"
// (see class "ClientBasicSynchronous")
// (blocks until every client has sent a message!)
// will put out a reply batch every "replyRate" ms
// (creates dummies if no data available)
@Plugin(pluginKey = "BASIC_SYNCHRONOUS_BATCH", pluginName = "Basic Synchronous Batch")
public class BasicSynchronousBatch extends OutputStrategyImpl implements
		EventExecutor {

	public class SimplexSynchronousBatch {

		private final int batchSize;
		private final MixMessage[] collectedMessages;
		private final boolean isRequestBatch;
		private int nextFreeSlot = 0;

		public SimplexSynchronousBatch(int batchSize, boolean isRequestBatch) {

			this.batchSize = batchSize;
			this.isRequestBatch = isRequestBatch;
			this.collectedMessages = new MixMessage[batchSize];

		}

		public void addMessage(MixMessage mixMessage) {
			this.collectedMessages[this.nextFreeSlot++] = mixMessage;
			if (this.nextFreeSlot == this.batchSize) {
				for (MixMessage m : this.collectedMessages) {
					if (this.isRequestBatch) {
						BasicSynchronousBatch.this.mix.putOutRequest(m);
					} else {
						BasicSynchronousBatch.this.mix.putOutReply(m);
					}
				}
				this.nextFreeSlot = 0;
			}

		}

	}

	private Map<String, Vector<TransportMessage>> clientReplyWaitingQueues;
	private SimplexSynchronousBatch replyBatch;
	
	@IntSimulationProperty(
			name = "Reply interval (ms)", 
			key = "BASIC_SYNCHRONOUS_REPLY_INTERVAL_IN_MS",
			min = 0
	)
	private int replyInterval;

	private final SimplexSynchronousBatch requestBatch;

	public BasicSynchronousBatch(Mix mix, Simulator simulator) {
		super(mix, simulator);
		int batchSize = Simulator.getSimulator().getNumberOfClients();
		this.requestBatch = new SimplexSynchronousBatch(batchSize, true);
		if (super.simulateReplyChannel) {
			this.replyBatch = new SimplexSynchronousBatch(batchSize, false);
			this.replyInterval = Simulator.settings
					.getPropertyAsInt("BASIC_SYNCHRONOUS_REPLY_INTERVAL_IN_MS");
			if (mix.isLastMix()) {
				this.clientReplyWaitingQueues = new HashMap<String, Vector<TransportMessage>>();
				for (AbstractClient c : simulator.getClients().values()) {
					this.clientReplyWaitingQueues.put(c.getIdentifier(),
							new Vector<TransportMessage>(10, 10));
				}
			}
			// schedule first reply batch:
			Event putOutNextReplyBatchEvent = new Event(this,
					Simulator.getNow() + this.replyInterval,
					OutputStrategyEvent.PUT_OUT_REPLY_BATCH);
			simulator.scheduleEvent(putOutNextReplyBatchEvent, this);
		}
	}

	@Override
	public void executeEvent(Event event) {
		if (event.getEventType() != OutputStrategyEvent.PUT_OUT_REPLY_BATCH) {
			throw new RuntimeException("ERROR! received unsupported event!"
					+ event);
		}
		this.putOutReplyBatch();
	}

	@Override
	public ClientSendStyleImpl getClientSendStyle(AbstractClient client) {
		boolean noMixes = !Topology.getTopology().containsAtLeastOneMix();
		boolean noRequestChannel = Simulator.settings.getProperty(
				"COMMUNICATION_MODE").equals("SIMPLEX_REPLY");
		if (noMixes || noRequestChannel) {
			return new ClientSendWithoutMixes(client, Simulator.getSimulator());
		} else {
			return new ClientBasicSynchronous(client, Simulator.getSimulator());
		}
	}

	@Override
	public MixSendStyleImpl getMixSendStyle() {
		return MixSendStyle.getInstance(this.mix, this.mix);
	}

	@Override
	public void incomingReply(MixMessage mixMessage) {
		this.replyBatch.addMessage(mixMessage);
	}

	public void incomingReply(TransportMessage transportMessage) {
		if (!this.mix.isLastMix()) {
			throw new RuntimeException(
					"ERROR: BasicSynchronousBatch only supports TransportMessage as reply from distant proxy!");
		}
		this.clientReplyWaitingQueues.get(
				transportMessage.getOwner().getIdentifier()).add(
				transportMessage);
	}

	@Override
	public void incomingRequest(MixMessage mixMessage) {
		this.requestBatch.addMessage(mixMessage);
	}

	private void putOutReplyBatch() {

		for (AbstractClient client : this.simulator.getClients().values()) {

			Vector<TransportMessage> replyWaitingQueue = this.clientReplyWaitingQueues
					.get(client.getIdentifier());
			boolean isDummy = replyWaitingQueue.size() == 0 ? true : false;

			MixMessage mixMessage = MixMessage.getInstance(false, this.mix,
					client, client, Simulator.getNow(), isDummy);

			if (isDummy) {

				this.incomingReply(mixMessage);

			} else {

				for (int i = 0; i < replyWaitingQueue.size(); i++) {

					TransportMessage noneMixMessage = replyWaitingQueue.get(i);

					if ((mixMessage.getFreeSpace() >= noneMixMessage
							.getLength()) && !noneMixMessage.isFragmented()) { // noneMixMessage
																				// fits
																				// in
																				// mixMessage
																				// completely

						replyWaitingQueue.remove(noneMixMessage);
						i--;
						mixMessage.addPayloadObject(noneMixMessage);

					} else { // add Fragment

						if (noneMixMessage.hasNextFragment()) {

							MessageFragment messageFragment = noneMixMessage
									.getFragment(mixMessage.getFreeSpace());
							mixMessage.addPayloadObject(messageFragment);

						}

						if (!noneMixMessage.hasNextFragment()) {
							replyWaitingQueue.remove(i);
							i--;
						}

					}

					if (mixMessage.getFreeSpace() == 0) {
						break;
					}

				}

				this.incomingReply(mixMessage);

			}

		}

		Event sendNextReplyEvent = new Event(this, Simulator.getNow()
				+ this.replyInterval, OutputStrategyEvent.PUT_OUT_REPLY_BATCH);
		this.simulator.scheduleEvent(sendNextReplyEvent, this);

	}

}
