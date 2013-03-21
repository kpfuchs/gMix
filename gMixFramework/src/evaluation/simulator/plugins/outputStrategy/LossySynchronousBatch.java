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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import evaluation.simulator.Simulator;
import evaluation.simulator.core.event.Event;
import evaluation.simulator.core.event.EventExecutor;
import evaluation.simulator.core.message.MessageFragment;
import evaluation.simulator.core.message.MixMessage;
import evaluation.simulator.core.message.TransportMessage;
import evaluation.simulator.core.networkComponent.AbstractClient;
import evaluation.simulator.core.networkComponent.IdGenerator;
import evaluation.simulator.core.networkComponent.Identifiable;
import evaluation.simulator.core.networkComponent.Mix;
import evaluation.simulator.core.networkComponent.NetworkNode;
import evaluation.simulator.core.statistics.Statistics;
import evaluation.simulator.pluginRegistry.ClientSendStyle;
import evaluation.simulator.pluginRegistry.StatisticsType;
import evaluation.simulator.plugins.clientSendStyle.ClientSendStyleImpl;
import evaluation.simulator.plugins.mixSendStyle.MixSendStyleImpl;
import evaluation.simulator.plugins.mixSendStyle.ReplyReceiver;


public class LossySynchronousBatch extends OutputStrategyImpl implements Identifiable {

	private Statistics statistics;
	private int numericIdentifier;
	private SimplexLossySynchronousBatch requestBatch;
	private SimplexLossySynchronousBatch replyBatch;
	private Map<String, Vector<TransportMessage>> clientReplyWaitingQueues;
	
	
	public LossySynchronousBatch(Mix mix, Simulator simulator) {
		super(mix, simulator);
		this.statistics = new Statistics(this);
		this.numericIdentifier = IdGenerator.getId();
		int numberOfClients = Simulator.getSimulator().getNumberOfClients();
		double requestRate = Simulator.settings.getPropertyAsDouble("LSB_REQUEST_RATE");
		this.requestBatch = new SimplexLossySynchronousBatch(true, requestRate, numberOfClients);
		if (Simulator.settings.getProperty("COMMUNICATION_MODE").equals("SIMPLEX_REPLY") || Simulator.settings.getProperty("COMMUNICATION_MODE").equals("DUPLEX")) {
			double replyRate = Simulator.settings.getPropertyAsDouble("LSB_REPLY_RATE");
			this.replyBatch = new SimplexLossySynchronousBatch(true, replyRate, numberOfClients);
			if (mix.isLastMix())
				clientReplyWaitingQueues = new HashMap<String, Vector<TransportMessage>>();
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
	
	
	public class SimplexLossySynchronousBatch implements EventExecutor {
		
		private final boolean isRequestBatch;
		private int sendInterval;
		private MixMessage[] batch;
		private boolean isFirstMessage = true;
		
		
		public SimplexLossySynchronousBatch(boolean isRequestBatch, double sendingRate, int numberOfClients) {
			this.isRequestBatch = isRequestBatch;
			double interval = 1d/sendingRate * 1000;
			this.sendInterval = (int)Math.floor(interval + 0.5d);
			System.out.println("SENDINTERVAL: " +sendInterval); 
			this.batch = new MixMessage[numberOfClients];
		}


		public void addMessage(MixMessage mixMessage) {
			if (isFirstMessage) {
				isFirstMessage = false;
				scheduleNextOutput();
			}
			int index = mixMessage.getOwner().getClientId();
			boolean drop;
			if (batch[index] == null) { // user has no message in current batch -> add his message
				batch[index] = mixMessage;
				drop = true;
			} else { // user has message in current batch -> drop message
				drop = false;
				statistics.addValue(drop, StatisticsType.DLPA_REQUEST_MESSAGE_DROP_PERCENTAGE_INCL_DUMMIES);
				statistics.addValue(drop, StatisticsType.DLPA_MESSAGE_DROP_PERCENTAGE_INCL_DUMMIES);
			}
			// record statistics
			if (isRequestBatch) {
				statistics.addValue(drop, StatisticsType.DLPA_REQUEST_MESSAGE_DROP_PERCENTAGE);
				statistics.addValue(drop, StatisticsType.DLPA_MESSAGE_DROP_PERCENTAGE);
			} else {
				statistics.addValue(drop, StatisticsType.DLPA_REPLY_MESSAGE_DROP_PERCENTAGE);
				statistics.addValue(drop, StatisticsType.DLPA_MESSAGE_DROP_PERCENTAGE);
			}
		}


		@Override
		public void executeEvent(Event e) {
			putOutMessages();
			scheduleNextOutput();
		}
		
		
		private void putOutMessages() {
			if (mix.isLastMix() && !isRequestBatch) {
				createReplyBatchFromUserData();
			} else {
				for (int i=0; i<batch.length; i++) {
					if (batch[i] == null)
						batch[i] = createDummyMessage(simulator.getClientById(i));
					if (isRequestBatch) {
						statistics.addValue(true, StatisticsType.DLPA_REQUEST_MESSAGE_DROP_PERCENTAGE_INCL_DUMMIES);
						statistics.addValue(true, StatisticsType.DLPA_MESSAGE_DROP_PERCENTAGE_INCL_DUMMIES);
					} else {
						statistics.addValue(true, StatisticsType.DLPA_REPLY_MESSAGE_DROP_PERCENTAGE_INCL_DUMMIES);
						statistics.addValue(true, StatisticsType.DLPA_MESSAGE_DROP_PERCENTAGE_INCL_DUMMIES);
					}
				} 
				List<MixMessage> result = Arrays.asList(batch);
				Collections.shuffle(result);
				if (isRequestBatch) {
					for (MixMessage msg:result)
						mix.putOutRequest(msg);
				} else {
					for (MixMessage msg:result)
						mix.putOutReply(msg);
				}
				batch = new MixMessage[batch.length];
			}
		}
		
		
		private void scheduleNextOutput() { 
			Event outputEvent = new Event(this, Simulator.getNow() + sendInterval, OutputStrategyEvent.TIMEOUT);
			simulator.scheduleEvent(outputEvent, this);
		}
		
		private void createReplyBatchFromUserData() {
			Vector<MixMessage> batch = new Vector<MixMessage>(simulator.getClients().size());
			for (AbstractClient client: simulator.getClients().values()) {
				Vector<TransportMessage> replyWaitingQueue =  clientReplyWaitingQueues.get(client.getIdentifier());
				boolean isDummy =  replyWaitingQueue.size() == 0 ? true: false;
				MixMessage mixMessage = createDummyMessage(client);
				if (isDummy) {
					batch.add(mixMessage);
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
					batch.add(mixMessage);
				}	
			}
			Collections.shuffle(batch);
			for (MixMessage mixMessage:batch)
				mix.putOutRequest(mixMessage);
		}
		
		
		private MixMessage createDummyMessage(AbstractClient owner) {
			NetworkNode source = isRequestBatch ? owner : mix;
			NetworkNode destination = isRequestBatch ? simulator.getDistantProxy() : owner;
			return MixMessage.getInstance(isRequestBatch, source, destination, owner, Simulator.getNow(), true);
		}
		
	}


	private class ReplyStyle extends MixSendStyleImpl {

		public ReplyStyle(NetworkNode owner, Simulator simulator, ReplyReceiver replyReceiver) {
			super(owner, simulator, replyReceiver);
		}

		@Override
		public void incomingDataFromServer(TransportMessage transportMessage) {
			if (!mix.isLastMix())
				throw new RuntimeException("ERROR: BasicSynchronousBatch only supports NoneMixMessages as reply from distant proxy!");
			clientReplyWaitingQueues.get(transportMessage.getOwner().getIdentifier()).add(transportMessage);
		}
		
	}


	@Override
	public ClientSendStyleImpl getClientSendStyle(AbstractClient client) {
		return ClientSendStyle.getInstance(client);
	}


	@Override
	public MixSendStyleImpl getMixSendStyle() {
		return new ReplyStyle(mix, Simulator.getSimulator(), mix);
	}
	
	
	@Override
	public int getGlobalId() {
		return numericIdentifier;
	}

}
