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
package evaluation.simulator.plugins.trafficSource;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import evaluation.simulator.Simulator;
import evaluation.simulator.core.event.Event;
import evaluation.simulator.core.message.EndToEndMessage;
import evaluation.simulator.core.message.TransportMessage;
import evaluation.simulator.core.networkComponent.AbstractClient;
import evaluation.traceParser.engine.dataStructure.Flow;
import evaluation.traceParser.engine.dataStructure.ExtendedTransaction;
import evaluation.traceParser.engine.dataStructure.Flow.Restriction;
import evaluation.traceParser.engine.fileReader.FlowGroupFlowIterator;
import evaluation.traceParser.engine.fileReader.FlowReader;
import framework.core.logger.OutputCap;
import framework.core.util.Util;


public class TraceReplayClient extends AbstractClient {
	
	private FlowSource flowSource;
	private int idOfFirstFlowOfCurrentFlowGroup = 0;
	private int idOfLatestStartedFlow = Util.NOT_SET;
	private HashSet<Integer> finishedFlows;
	private HashMap<Integer, ActiveFlow> activeFlows = new HashMap<Integer, ActiveFlow>();
	private OutputCap noMoreFlowsMessage = new OutputCap(this.toString() +": no more flows to schedule", Long.MAX_VALUE);
	private boolean noMoreFlowsToSchedule = false;
	
	
	public TraceReplayClient(String identifier, Simulator simulator, FlowReader trace, int clientId) {
		super(identifier, simulator);
		this.clientId = clientId;
		this.flowSource = new FlowSource(trace);
	}

	
	public void startSending() {
		tryScheduleNextFlows();
	}

	
	@Override
	public void messageReachedServer(EndToEndMessage message) {
		if (closedLoopSending)
			messageTransmitted(message);
		
	}
	
	private void messageTransmitted(EndToEndMessage message) {
		ExtendedTransaction at = (ExtendedTransaction)message.getPayload();
		if (!simulateReplyChannel) { // simplex simulation -> schedule replies here -> assume replies arrive as in original trace (NOT_BEFORE_END_OF_TRANSACTION-restriction requires knowledge of reply times)
			if (at.containsReplies()) { // create and schedule replies: 
				int[] replySizes = at.getDistinctReplySizes();
				for (int i=0; i<replySizes.length; i++) {
					EndToEndMessage reply = message.createReplyForThisMessage(at, replySizes[i]);
					long replyDelay = at.endsOfReplies[i] - at.startOfRequest; // delay as observed in original trace
					Event incomingReplyEvent = new Event(this, Simulator.getNow() + replyDelay, TraceReplayClientEvent.CALL_INCOMING_MESSAGE, reply);
					simulator.scheduleEvent(incomingReplyEvent, this);
					//System.out.println("simplex, scheduling reply for transaction " +at.getTransactionId() +" for t=" +(Simulator.getNow() + replyDelay)); 
				}
			} else { // no reply
				// nothing to do
			}
		}
		if (!at.containsReplies()) { // try to schedule next transaction if we do not have to wait for a reply from the server
			tryScheduleNextTransaction((ActiveFlow) message.getPayload().attachment);
		}
	}
	
	
	@Override
	public void incomingMessage(EndToEndMessage message) {
		ActiveFlow relatedFlow = (ActiveFlow) message.getPayload().attachment;
		//System.out.println(super.toString() +": received reply for transaction " +message.getPayload().getTransactionId() +" (flow " +relatedFlow.flow.flowId + ", now: " +Simulator.getNow() +", object-ref: " +message +")");
		relatedFlow.replyReceived();
		tryScheduleNextTransaction(relatedFlow);
	}
	
	
	private void tryScheduleNextTransaction(ActiveFlow activeFlow) {
		if (activeFlow.allRepliesForCurrentTransactionReceived()) {
			if (activeFlow.hasNextTransaction()) { // next transaction available -> schedule its send
				ExtendedTransaction nextTransaction = activeFlow.getNextTransaction();
				//System.out.println(nextTransaction.getSendDelay()); 
				Event replayNextTransactionEvent = new Event(this, Simulator.getNow() + nextTransaction.getSendDelay(), TraceReplayClientEvent.REPLAY_NEXT_TRANSACTION, activeFlow);
				//System.out.println(super.toString() +": scheduling next transaction (id: " +nextTransaction.getTransactionId() +", arrayOffset: " +activeFlow.getArrayOffsetOfCurrentTransaction() +", flow: " +activeFlow.flow.flowId +", time:" +Simulator.getNow() +") for " +(Simulator.getNow() + nextTransaction.getSendDelay()));
				simulator.scheduleEvent(replayNextTransactionEvent, this);
			} else { // no next transaction (end of flow)
				//System.out.println(super.toString() +": flow " +activeFlow.flow.flowId +" is finished (now: " +Simulator.getNow() +")");
				finishedFlows.add(activeFlow.flow.flowId);
				activeFlows.remove(activeFlow.flow.flowId); 
				if (noMoreFlowsToSchedule && activeFlows.size() == 0) { // nothing left to schedule and no more active flows -> end simulation
					simulator.voteForStop();
					//simulator.stopSimulation("end of trace reached (variable SIMULATION_END in experiment config)");
					//System.out.println("thats it now " +Simulator.getNow()); 
				}
			}
		}
		tryScheduleNextFlows();
	}
	
	
	private void tryScheduleNextFlows() {
		while (flowSource.peekNextFlow() != null) { 
			if (flowSource.peekNextFlow().restriction == Restriction.NONE) { // schedule none restricted flow
				Flow nextFlow = flowSource.readNextFlow();
				Event replayNextFlowEvent = new Event(this, Simulator.getNow() , TraceReplayClientEvent.REPLAY_FLOW, nextFlow);
				//System.out.println(super.toString() +": scheduling next flow (no restriction, id: " +nextFlow.flowId +", time:" +Simulator.getNow() +") for " +Simulator.getNow());
				simulator.scheduleEvent(replayNextFlowEvent, this);
			} else if (flowSource.peekNextFlow().restriction == Restriction.SIMPLE_DELAY) {
				Flow nextFlow = flowSource.readNextFlow();
				Event replayNextFlowEvent = new Event(this, Simulator.getNow() + nextFlow.offsetFromRestriction, TraceReplayClientEvent.REPLAY_FLOW, nextFlow);
				//System.out.println(super.toString() +": scheduling next flow (SIMPLE_DELAY restriction, id: " +nextFlow.flowId +", time:" +Simulator.getNow() +") for " +(Simulator.getNow() + nextFlow.offsetFromRestriction));
				simulator.scheduleEvent(replayNextFlowEvent, this);
			} else if (flowSource.peekNextFlow().restriction == Restriction.NOT_BEFORE_END_OF_OTHER_FLOW) {
				if (flowSource.peekNextFlow().idOfRestrictingFlow < idOfFirstFlowOfCurrentFlowGroup || finishedFlows.contains(flowSource.peekNextFlow().idOfRestrictingFlow)) { // no more blocked (restricting flow is finished now)
					Flow nextFlow = flowSource.readNextFlow();
					Event replayNextFlowEvent = new Event(this, Simulator.getNow() + nextFlow.offsetFromRestriction, TraceReplayClientEvent.REPLAY_FLOW, nextFlow);
					//System.out.println(super.toString() +": scheduling next flow (NOT_BEFORE_END_OF_OTHER_FLOW-restriction, id: " +nextFlow.flowId +", id of restricting flow: " +nextFlow.idOfRestrictingFlow +", time:" +Simulator.getNow() +", delay: " +nextFlow.offsetFromRestriction +") for " +(Simulator.getNow() + nextFlow.offsetFromRestriction));
					simulator.scheduleEvent(replayNextFlowEvent, this);
				} else { // next flow is blocked
					break;
				}
			} else if (flowSource.peekNextFlow().restriction == Restriction.NOT_BEFORE_END_OF_TRANSACTION) {
				int idOfRestrictingFlow = flowSource.peekNextFlow().idOfRestrictingFlow;
				int idOfRestrictingTransaction = flowSource.peekNextFlow().idOfRestrictingTransaction;
				ActiveFlow restrictingFlow = activeFlows.get(idOfRestrictingFlow);
				if (	(restrictingFlow == null && idOfLatestStartedFlow >= idOfRestrictingFlow) // restricting flow is finished already
						|| (restrictingFlow != null && idOfRestrictingTransaction < restrictingFlow.getArrayOffsetOfCurrentTransaction()) // restricting transaction is already finished
						|| (restrictingFlow != null && (idOfRestrictingTransaction == restrictingFlow.getArrayOffsetOfCurrentTransaction() // restricting transaction is currently being replayed
							&& flowSource.peekNextFlow().idOfRestrictingReply <= restrictingFlow.getIdOfLatestFinishedReply())) // the restricting reply of the restricting transaction is already replayed
					) { // restriction no longer given
					Flow nextFlow = flowSource.readNextFlow();
					Event replayNextFlowEvent = new Event(this, Simulator.getNow() + nextFlow.offsetFromRestriction, TraceReplayClientEvent.REPLAY_FLOW, nextFlow);
					//System.out.println(super.toString() +": scheduling next flow (NOT_BEFORE_END_OF_TRANSACTION-restriction, id: " +nextFlow.flowId +", time:" +Simulator.getNow() +") for " +(Simulator.getNow() + nextFlow.offsetFromRestriction));
					//String rsfid = restrictingFlow == null ? "none" : ""+restrictingFlow.flow.flowId;
					//System.out.println("restrictingFlow: " +rsfid); 
					simulator.scheduleEvent(replayNextFlowEvent, this);
				} else { // next flow is blocked
					break;
				}
			} else {
				throw new RuntimeException("no handler implemented for restriction " +flowSource.peekNextFlow().restriction); 
			}
		}
		if (flowSource.peekNextFlow() == null) { // no more flows to schedule
			noMoreFlowsMessage.putOut();
			noMoreFlowsToSchedule = true;
		}
	}

	
	/*
	 * 	private void tryScheduleNextFlows() {
		try {
			if (currentFlowGroup == null || !currentFlowGroup.hasNext()) { // need next flow group
				if (trace.hasNextFlow() && trace.peekNextFlow().senderId == clientId) { // next flow group is available -> schedule all none-restricted flows
					currentFlowGroup = trace.getFlowGroupFlowIterator();
					finishedFlows.clear();
					finishedFlows = new HashSet<Integer>();
					while (currentFlowGroup.hasNext() && trace.peekNextFlow().notBeforeRestriction == Util.NOT_SET) { geht so nicht, notBeforeRestriction wird erst durch fgiterator gesetzt..
						Flow nextFlow = currentFlowGroup.next();
						Event replayNextFlowEvent = new Event(this, Simulator.getNow() + nextFlow.offsetFromLastFlow, TraceReplayClientEvent.REPLAY_FLOW, nextFlow);
						simulator.scheduleEvent(replayNextFlowEvent, this);
					} 
				}
			} else { // check if blocked flows of the current flow group can be scheduled:
				while (currentFlowGroup.hasNext() && finishedFlows.contains(trace.peekNextFlow().notBeforeRestriction)) { geht so nicht, notBeforeRestriction wird erst durch fgiterator gesetzt..
					Flow nextFlow = currentFlowGroup.next();
					Event replayNextFlowEvent = new Event(this, Simulator.getNow() + nextFlow.offsetFromRestrictingFlow, TraceReplayClientEvent.REPLAY_FLOW, nextFlow);
					simulator.scheduleEvent(replayNextFlowEvent, this);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("could not read trace file " +e.getLocalizedMessage()); 
		}
	}
	 */
	
	private void replayFlow(Flow flow) {
		//System.out.println(super.toString() +": start replaying flow " +flow.flowId +" (now: " +Simulator.getNow() +")"); 
		ActiveFlow activeFlow = new ActiveFlow(flow);
		activeFlows.put(flow.flowId, activeFlow);
		idOfLatestStartedFlow = flow.flowId;
		assert activeFlow.hasNextTransaction();
		tryScheduleNextTransaction(activeFlow);
	}
	
	
	private void replayTransaction(ActiveFlow activeFlow) {
		assert activeFlow.getCurrentTransaction() != null;
		ExtendedTransaction transaction = activeFlow.getCurrentTransaction();
		transaction.attachment = activeFlow;
		if (transaction.getRequestSize() == 0) // -> server shall send first message
			transaction.setRequestSize(16); // we assume that the client must first send a message to the final mix to open a socket that can receive data from the server (e.g. socks 5 bind command) 
		EndToEndMessage eteMessage = new EndToEndMessage(transaction.getServerId(), transaction, !activeFlow.hasNextTransaction());
		//System.out.println(super.toString() +": sending transaction (id: " +transaction.getTransactionId() +", flow " +activeFlow.flow.flowId +", time:" +Simulator.getNow() +")");
		TransportMessage tm = super.sendMessage(eteMessage);
		if (!closedLoopSending) {
			assert tm.reltedEndToEndMessage == eteMessage;
			EndToEndMessage message = tm.reltedEndToEndMessage;
			message.transportMessage = tm;
			/*
			 * EndToEndMessage message = tm.reltedEndToEndMessage;
		message.transportMessage = tm;
		statistics.addValue(tm.getLength(), StatisticsType.DISTANTPROXY_DATAVOLUME_SENDANDRECEIVE);
		server.incomingMessage(message);
		tm.getOwner().messageReachedServer(message);
			 */
			/*EndToEndMessage message = eteMessage.r
			message.transportMessage = tm;
			statistics.addValue(tm.getLength(), StatisticsType.DISTANTPROXY_DATAVOLUME_SENDANDRECEIVE);
			server.incomingMessage(message);
			tm.getOwner().messageReachedServer(message); // notify client that his message has now reached the server
			*/
			messageTransmitted(eteMessage);
			}
	}
	
	
	@Override
	public void executeEvent(Event event) {
		if (event.getEventType() instanceof TraceReplayClientEvent) {
			if (event.getEventType() == TraceReplayClientEvent.REPLAY_FLOW) {
				replayFlow((Flow)event.getAttachment());
			} else if (event.getEventType() == TraceReplayClientEvent.REPLAY_NEXT_TRANSACTION) {
				replayTransaction((ActiveFlow)event.getAttachment());
			} else if (event.getEventType() == TraceReplayClientEvent.CALL_INCOMING_MESSAGE) {
				incomingMessage((EndToEndMessage)event.getAttachment());
			} else
				throw new RuntimeException("ERROR: received unknown Event: " +event.toString()); 
		} else {
			super.executeEvent(event);
		}
	}
	
	
	private class FlowSource {
		
		private FlowReader trace;
		private FlowGroupFlowIterator currentFlowGroup;
		private Flow nextFlow = null;
		private boolean wasPeekNextFlowCalled = false;
		private boolean newFlowGroup = false;
		
		
		private FlowSource(FlowReader trace) {
			this.trace = trace;
		}
		
		
		private Flow peekNextFlow() {
			if (wasPeekNextFlowCalled)
				return nextFlow;
			try {
				wasPeekNextFlowCalled = true;
				if (nextFlow != null) {
					return nextFlow;
				} else {
					if (trace.peekNextFlow() == null || trace.peekNextFlow().senderId != clientId) { // no more flows for this client
						return null;
					} else { // more flows available
						if (currentFlowGroup == null || !currentFlowGroup.hasNext()) { // need next flow group
							currentFlowGroup = trace.getFlowGroupFlowIterator();
							newFlowGroup = true;
						}
						nextFlow = currentFlowGroup.next();
						return nextFlow;
					}
				}
			} catch (IOException e) {
				throw new RuntimeException("could not read trace file " +e.getLocalizedMessage()); 
			}
		}
		
		
		private Flow readNextFlow() {
			if (!wasPeekNextFlowCalled)
				peekNextFlow();
			wasPeekNextFlowCalled = false;
			Flow result = nextFlow;
			if (newFlowGroup) {
				if (finishedFlows != null) {
					finishedFlows.clear();
					activeFlows.clear();
				}
				finishedFlows = new HashSet<Integer>();
				idOfFirstFlowOfCurrentFlowGroup = result.flowId;
				newFlowGroup = false;
			}
			nextFlow = null;
			return result;
		}
		
	}
	
	private class ActiveFlow {
		
		Flow flow;
		private int transactionCounter;
		private int transactions;
		private ExtendedTransaction currentTransaction;
		private int expectedReplies;
		private int replyCounter;
		private int idOfLatestReply;
		
		
		public ActiveFlow(Flow flow) {
			this.flow = flow;
			this.transactionCounter = 0;
			this.transactions = flow.transactions.size();
			this.idOfLatestReply = Util.NOT_SET;
		}
		
		
		public boolean hasNextTransaction() {
			return transactionCounter < transactions;
		}
		
		
		public ExtendedTransaction getNextTransaction() {
			currentTransaction = flow.transactions.get(transactionCounter);
			transactionCounter++;
			expectedReplies = currentTransaction.getTotalReplySize() == 0 ? 0 : currentTransaction.getDistinctReplySizes().length;
			replyCounter = 0;
			idOfLatestReply = Util.NOT_SET;
			return currentTransaction;
		}
		
		public ExtendedTransaction getCurrentTransaction() {
			return currentTransaction;
		}
		
		
		public void replyReceived() {
			idOfLatestReply = replyCounter;
			replyCounter++;
			//System.out.println("(" +replyCounter +" of " +expectedReplies +")"); 
			assert replyCounter <= expectedReplies;
		}
		
		
		public int getArrayOffsetOfCurrentTransaction() {
			return transactionCounter - 1;
		}
		
		
		public int getIdOfLatestFinishedReply() {
			return idOfLatestReply;
		}
		
		
		public boolean allRepliesForCurrentTransactionReceived() {
			return replyCounter == expectedReplies;
		}
		
	}
	
}
