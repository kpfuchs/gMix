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

import java.util.Vector;

import evaluation.simulator.Simulator;
import evaluation.simulator.core.event.Event;
import evaluation.simulator.core.event.EventExecutor;
import evaluation.simulator.core.message.MixMessage;
import evaluation.simulator.core.networkComponent.AbstractClient;
import evaluation.simulator.core.networkComponent.IdGenerator;
import evaluation.simulator.core.networkComponent.Identifiable;
import evaluation.simulator.core.networkComponent.Mix;
import evaluation.simulator.core.networkComponent.NetworkNode;
import evaluation.simulator.core.statistics.Statistics;
import evaluation.simulator.pluginRegistry.ClientSendStyle;
import evaluation.simulator.pluginRegistry.MixSendStyle;
import evaluation.simulator.pluginRegistry.StatisticsType;
import evaluation.simulator.plugins.clientSendStyle.ClientSendStyleImpl;
import evaluation.simulator.plugins.mixSendStyle.MixSendStyleImpl;


/**
 * DLP Heuristic Algorithm (2008: Wei Wang, Mehul Motani, Vikram Srinivasan: 
 * Dependent Link Padding Algorithms for Low Latency Anonymity Systems) CCS'08
 * 
 * "DLP Heuristic Algorithm
 * Parameters: Packet arrival time tij for all flows fi element F
 * Utility threshold U.
 * Output: A sending schedule with utility of at least U
 * 01: Put new packet Pij into a FIFO queue for the flow fi
 * 02: Repeat step 01 until there is a packet P has been in the queue for Delta time units:
 * 03: if more than U*|F| queues are non-empty
 * 04:		Add a new token and send one packet for each flow immediately
 * 05: else
 * 06: 		Drop the packet P.
 * 07: endif
 * 08: Go to step 01 until no more packet arrives."
*/
public class DLPAHeuristic2 extends OutputStrategyImpl implements Identifiable {

	private Statistics statistics;
	private int numericIdentifier;
	private DLPAHeuristicSimplex requestHandler;
	private DLPAHeuristicSimplex replyHandler;
	
	
	public DLPAHeuristic2(Mix mix, Simulator simulator) {
		super(mix, simulator);
		this.statistics = new Statistics(this);
		this.numericIdentifier = IdGenerator.getId();
		int maxRequestDelay = Simulator.settings.getPropertyAsInt("MAX_DLPA_REQUEST_DELAY");
		int maxReplyDelay = Simulator.settings.getPropertyAsInt("MAX_DLPA_REPLY_DELAY");
		double requestUtilityThreshold = Simulator.settings.getPropertyAsDouble("REQUEST_UTILITY_THRESHOLD");
		double replyUtilityThreshold = Simulator.settings.getPropertyAsDouble("REPLY_UTILITY_THRESHOLD");
		this.requestHandler = new DLPAHeuristicSimplex(true, maxRequestDelay, requestUtilityThreshold);
		this.replyHandler = new DLPAHeuristicSimplex(false, maxReplyDelay, replyUtilityThreshold);
		}
	

	@Override
	public void incomingRequest(MixMessage mixMessage) {
		this.requestHandler.addMessage(mixMessage);
	}


	@Override
	public void incomingReply(MixMessage mixMessage) {
		this.replyHandler.addMessage(mixMessage);
	}

	
	private class DLPAHeuristicSimplex implements EventExecutor {
		
		boolean isRequestHandler;
		int MAX_DELAY;
		double UTILITY_THRESHOLD;
		Vector<Vector<MixMessage>> messageQueues;
		Event outputEvent = null;


		public DLPAHeuristicSimplex(boolean isRequestHandler, int maxDelay, double utilityThreshold) {
			this.isRequestHandler = isRequestHandler;
			this.MAX_DELAY = maxDelay;
			this.UTILITY_THRESHOLD = utilityThreshold;
			int numberOfClients = Simulator.getSimulator().getNumberOfClients();
			messageQueues = new Vector<Vector<MixMessage>>();
			for (int i=0; i<numberOfClients; i++)
				messageQueues.add(new Vector<MixMessage>());
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			mixMessage.timeOfArrival = Simulator.getNow();
			// "01: Put new packet Pij into a FIFO queue for the flow fi":
			messageQueues.get(mixMessage.getOwner().getClientId()).add(mixMessage); 
			if (outputEvent == null) { // set timeout
				long timeOfOutput = mixMessage.timeOfArrival + MAX_DELAY;
				outputEvent = new Event(this, timeOfOutput, OutputStrategyEvent.DLPA_TIMEOUT);
				simulator.scheduleEvent(outputEvent, this);
			}
		}


		// called by scheduler on timeout ("a packet P has been in the queue for delta time units")
		@Override
		public void executeEvent(Event e) { 
			putOutSlot(e);
		}
		
		
		public void putOutSlot(Event e) {
			// 03: if more than U*|F| queues are non-empty
			double notEmptyQueues = 0;
			for (int i=0; i<messageQueues.size(); i++)
				if (messageQueues.get(i).size() != 0)
					notEmptyQueues++;
			if (notEmptyQueues >= (UTILITY_THRESHOLD * (double)messageQueues.size())) { // dlpa-paper says "more than U*|F|" and that U must be <= 1. if we set u=1 and all queues are not empty, this would result in the algorithm dropping the token also all slots are in use. must be an error in the paper -> we use "at least U*|F|" instead of "more than U*|F|" 
				for (int i=0; i<messageQueues.size(); i++) { // "Add a new token and send one packet for each flow immediately" 
					MixMessage m;
					if (messageQueues.get(i).size() == 0) {
						m = createDummyMessage(simulator.getClientById(i), isRequestHandler);
					} else {
						m = messageQueues.get(i).remove(0);
						if (isRequestHandler) {
							statistics.addValue(true, StatisticsType.DLPA_REQUEST_MESSAGE_DROP_PERCENTAGE);
							statistics.addValue(true, StatisticsType.DLPA_MESSAGE_DROP_PERCENTAGE);
						} else {
							statistics.addValue(true, StatisticsType.DLPA_REPLY_MESSAGE_DROP_PERCENTAGE);
							statistics.addValue(true, StatisticsType.DLPA_MESSAGE_DROP_PERCENTAGE);
						}
					}
					// TODO: sort messages
					if (isRequestHandler) {
						statistics.addValue(true, StatisticsType.DLPA_REQUEST_SENDING_RATE_PER_MIX);
						statistics.addValue(true, StatisticsType.DLPA_REQUEST_SENDING_RATE_PER_MIX_AND_CLIENT);
						mix.putOutRequest(m);
					} else {
						statistics.addValue(true, StatisticsType.DLPA_REPLY_SENDING_RATE_PER_MIX);
						mix.putOutReply(m);
					}
					statistics.addValue(true, StatisticsType.DLPA_REQUEST_AND_REPLY_SENDING_RATE_PER_MIX);
				}
			} else { // "Drop the packet P."
				for (int i=0; i<messageQueues.size(); i++) { // drop all packets that are outdated (only check longest waiting packet per user)
					if (messageQueues.get(i).size() != 0) {
						if ((Simulator.getNow() - messageQueues.get(i).get(0).timeOfArrival) >= MAX_DELAY) { // message is outdated
							messageQueues.get(i).remove(0);
							if (isRequestHandler) {
								statistics.addValue(false, StatisticsType.DLPA_REQUEST_MESSAGE_DROP_PERCENTAGE);
								statistics.addValue(false, StatisticsType.DLPA_MESSAGE_DROP_PERCENTAGE);
							} else {
								statistics.addValue(false, StatisticsType.DLPA_REPLY_MESSAGE_DROP_PERCENTAGE);
								statistics.addValue(false, StatisticsType.DLPA_MESSAGE_DROP_PERCENTAGE);
							}
						}
					}
				}
			}
			// schedule next output if messages are still available
			MixMessage longestWaitingMessage = null; // determine the longest waiting message
			for (int i=0; i<messageQueues.size(); i++) {
				if (messageQueues.get(i).size() != 0) {
					if (longestWaitingMessage == null)
						longestWaitingMessage = messageQueues.get(i).get(0);
					else if (longestWaitingMessage.timeOfArrival > messageQueues.get(i).get(0).timeOfArrival)
						longestWaitingMessage = messageQueues.get(i).get(0);
				}
			}
			if (longestWaitingMessage == null) { // no messages available
				outputEvent = null; // schedule will happen with the next arriving message (see above)
			} else {
				//int timeOfOutput = longestWaitingMessage.timeOfArrival + MAX_DELAY;
				long timeOfOutput = Simulator.getNow() + MAX_DELAY;
				outputEvent = new Event(this, timeOfOutput, OutputStrategyEvent.DLPA_TIMEOUT);
				simulator.scheduleEvent(outputEvent, this);
			}
			
		}
	}
	
	private MixMessage createDummyMessage(AbstractClient owner, boolean isRequest) {
		NetworkNode source = isRequest ? owner : mix;
		NetworkNode destination = isRequest ? simulator.getDistantProxy() : owner;
		return MixMessage.getInstance(isRequest, source, destination, owner, Simulator.getNow(), true);
	}

	@Override
	public int getGlobalId() {
		return numericIdentifier;
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
