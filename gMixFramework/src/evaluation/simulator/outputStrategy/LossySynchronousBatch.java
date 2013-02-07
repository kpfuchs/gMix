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
package evaluation.simulator.outputStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import evaluation.simulator.core.Event;
import evaluation.simulator.core.EventExecutor;
import evaluation.simulator.core.OutputStrategyEvent;
import evaluation.simulator.core.Simulator;
import evaluation.simulator.message.MessageFragment;
import evaluation.simulator.message.MixMessage;
import evaluation.simulator.message.TransportMessage;
import evaluation.simulator.networkComponent.AbstractClient;
import evaluation.simulator.networkComponent.IdGenerator;
import evaluation.simulator.networkComponent.Identifiable;
import evaluation.simulator.networkComponent.Mix;
import evaluation.simulator.networkComponent.NetworkNode;
import evaluation.simulator.statistics.Statistics;
import evaluation.simulator.statistics.StatisticsType;


public class LossySynchronousBatch extends OutputStrategy implements Identifiable {

	private Statistics statistics;
	private int numericIdentifier;
	private SimplexLossySynchronousBatch requestBatch;
	private SimplexLossySynchronousBatch replyBatch;
	private Map<String, Vector<TransportMessage>> clientReplyWaitingQueues;
	
	
	protected LossySynchronousBatch(Mix mix, Simulator simulator) {
		super(mix, simulator);
		this.statistics = new Statistics(this);
		this.numericIdentifier = IdGenerator.getId();
		int numberOfClients = Simulator.getSimulator().getNumberOfClients();
		double requestRate = Simulator.settings.getPropertyAsDouble("LSB_REQUEST_RATE");
		this.requestBatch = new SimplexLossySynchronousBatch(true, requestRate, numberOfClients);
		if (Simulator.settings.getPropertyAsBoolean("SIMULATE_REPLY_CHANNEL")) {
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
			double drop;
			if (batch[index] == null) { // user has no message in current batch -> add his message
				batch[index] = mixMessage;
				drop = 1;
			} else { // user has message in current batch -> drop message
				drop = -1;
				statistics.addValue(-1, StatisticsType.REQUEST_MESSAGE_DROP_PERCENTAGE_INCL_DUMMIES);
				statistics.addValue(-1, StatisticsType.MESSAGE_DROP_PERCENTAGE_INCL_DUMMIES);
			}
			// record statistics
			if (isRequestBatch) {
				statistics.addValue(drop, StatisticsType.REQUEST_MESSAGE_DROP_PERCENTAGE);
				statistics.addValue(drop, StatisticsType.MESSAGE_DROP_PERCENTAGE);
			} else {
				statistics.addValue(drop, StatisticsType.REPLY_MESSAGE_DROP_PERCENTAGE);
				statistics.addValue(drop, StatisticsType.MESSAGE_DROP_PERCENTAGE);
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
				// TODO: sort batch before output
				for (int i=0; i<batch.length; i++) {
					if (batch[i] == null)
						batch[i] = createDummyMessage(simulator.getClientById(i));
					if (isRequestBatch) {
						//statistics.addValue(1, StatisticsType.REQUEST_MESSAGE_DROP_PERCENTAGE_INCL_DUMMIES);
						//statistics.addValue(1, StatisticsType.MESSAGE_DROP_PERCENTAGE_INCL_DUMMIES);
						statistics.addValue(1, StatisticsType.REQUEST_MESSAGE_DROP_PERCENTAGE_INCL_DUMMIES);
						statistics.addValue(1, StatisticsType.MESSAGE_DROP_PERCENTAGE_INCL_DUMMIES);
						mix.putOutRequest(batch[i]);
					} else {
						statistics.addValue(1, StatisticsType.REPLY_MESSAGE_DROP_PERCENTAGE_INCL_DUMMIES);
						statistics.addValue(1, StatisticsType.MESSAGE_DROP_PERCENTAGE_INCL_DUMMIES);
						mix.putOutReply(batch[i]);
					}
				} 
				batch = new MixMessage[batch.length];
			}
		}
		
		
		private void scheduleNextOutput() { 
			if (!stopReplying) {
				Event outputEvent = new Event(this, Simulator.getNow() + sendInterval, OutputStrategyEvent.TIMEOUT);
				simulator.scheduleEvent(outputEvent, this);
			}
		}
		
		private void createReplyBatchFromUserData() {
			for (AbstractClient client: simulator.getClients().values()) {
				Vector<TransportMessage> replyWaitingQueue =  clientReplyWaitingQueues.get(client.getIdentifier());
				boolean isDummy =  replyWaitingQueue.size() == 0 ? true: false;
				MixMessage mixMessage = createDummyMessage(client);
				// TODO: sort batch before output
				if (isDummy) {
					mix.putOutRequest(mixMessage);
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
					mix.putOutRequest(mixMessage);
				}	
			}
		}
		
		
		private MixMessage createDummyMessage(AbstractClient owner) {
			NetworkNode source = isRequestBatch ? owner : mix;
			NetworkNode destination = isRequestBatch ? simulator.getDistantProxy() : owner;
			return MixMessage.getInstance(isRequestBatch, source, destination, owner, Simulator.getNow(), true);
		}
		
	}
	
	
	public void incomingReply(TransportMessage noneMixMessage) {
		
		if (!mix.isLastMix())
			throw new RuntimeException("ERROR: BasicSynchronousBatch only supports NoneMixMessages as reply from distant proxy!");
		clientReplyWaitingQueues.get(noneMixMessage.getOwner().getIdentifier()).add(noneMixMessage);
		
	}


	@Override
	public int getGlobalId() {
		return numericIdentifier;
	}

}
