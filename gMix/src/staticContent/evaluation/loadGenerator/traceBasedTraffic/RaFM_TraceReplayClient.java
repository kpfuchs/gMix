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
package staticContent.evaluation.loadGenerator.traceBasedTraffic;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import staticContent.evaluation.loadGenerator.scheduler.ScheduleTarget;
import staticContent.evaluation.loadGenerator.scheduler.Scheduler;
import staticContent.evaluation.loadGenerator.traceBasedTraffic.event.Event;
import staticContent.evaluation.loadGenerator.traceBasedTraffic.event.IncomingReplyEvent;
import staticContent.evaluation.loadGenerator.traceBasedTraffic.event.ReplayNextFlowEvent;
import staticContent.evaluation.loadGenerator.traceBasedTraffic.event.ReplayNextTransactionEvent;
import staticContent.evaluation.traceParser.engine.dataStructure.Flow;
import staticContent.evaluation.traceParser.engine.dataStructure.Transaction;
import staticContent.evaluation.traceParser.engine.dataStructure.Flow.Restriction;
import staticContent.evaluation.traceParser.engine.fileReader.FlowGroupFlowIterator;
import staticContent.evaluation.traceParser.engine.fileReader.FlowReader;
import staticContent.framework.logger.OutputCap;
import staticContent.framework.socket.socketInterfaces.StreamAnonSocket;
import staticContent.framework.util.Util;
import userGeneratedContent.simulatorPlugIns.plugins.trafficSource.ActiveFlow;
import gnu.trove.TLongArrayList;


/**
 * This class represents a client and provides methods which allow the client to
 * schedule flows and transactions considering dependencies of other
 * transactions and flows. If the point in time has come, regarding the
 * offset/delay, the scheduled transactions will be flushed to the network.
 * 
 * @author Johannes Wendel, Simon Lecheler, kpf
 * 
 */
public class RaFM_TraceReplayClient implements ScheduleTarget<Event> {
	
	private int clientId;
	private boolean isDuplex;
	private final boolean DEBUG_ON;
	private FlowSource flowSource;
	private int idOfFirstFlowOfCurrentFlowGroup = 0;
	private int idOfLatestStartedFlow = Util.NOT_SET;
	private HashSet<Integer> finishedFlows;
	private HashMap<Integer, ActiveFlow> activeFlowsByFlowId = new HashMap<Integer, ActiveFlow>();
	private HashMap<Integer, ActiveFlow> activeFlowsByTransactionId = new HashMap<Integer, ActiveFlow>();
	private OutputCap noMoreFlowsMessage = new OutputCap(this.toString() +": no more flows to schedule", Long.MAX_VALUE);
	private boolean noMoreFlowsToSchedule = false;
	private RaFM_LoadGenerator owner;
	private Scheduler<Event> scheduler;
	private StreamAnonSocket socket;
	private BufferedOutputStream outputStream;
	
	public static boolean DISPLAY_MESSAGE_SIZES;
	public static boolean DISPLAY_THROUGHPUT;
	private long dataTransmitted = 0;
	private TLongArrayList msgSizes;
	
	
	// Counts how many flows have been scheduled, but not yet received for this
	// client to get to know for the receiving part if all scheduled flows have
	// been received "= 0".
	private int scheduledFlows = 0;

	
	public RaFM_TraceReplayClient(FlowReader trace, int clientId, boolean isDuplex, boolean debugOn) {
		this.clientId = clientId;
		this.isDuplex = isDuplex;
		this.DEBUG_ON = debugOn;
		this.flowSource = new FlowSource(trace);
		if (DISPLAY_MESSAGE_SIZES)
			this.msgSizes = new TLongArrayList(10000);
	}

	
	public synchronized void startSending(RaFM_LoadGenerator owner, Scheduler<Event> scheduler, StreamAnonSocket socket) {
		this.owner = owner;
		this.scheduler = scheduler;
		this.socket = socket;
		try {
			this.outputStream = new BufferedOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		tryScheduleNextFlows();
	}
	
	
	/**
	 * This method is called when a reply was received (by TBT_LoadGenerator)
	 * and increases the reply counter of the transaction which counts the
	 * received replies
	 * 
	 * @param transactionId
	 *            transactionId, the received reply belongs to
	 */
	public synchronized void incomingReply(int transactionId) {
		if (DEBUG_ON)
		    System.out.println("Client " +clientId +": received reply for transaction " +transactionId); 
		ActiveFlow relatedFlow = activeFlowsByTransactionId.get(transactionId);
		relatedFlow.replyReceived();
		if (DISPLAY_THROUGHPUT || DISPLAY_MESSAGE_SIZES) {
			int length = relatedFlow.getCurrentTransaction().getDistinctReplySizes()[relatedFlow.getIdOfLatestFinishedReply()];
			if (DISPLAY_THROUGHPUT)
				this.dataTransmitted += length;
			if (DISPLAY_MESSAGE_SIZES)
				this.msgSizes.add(length);
		}
		tryScheduleNextTransaction(relatedFlow);
	}
	
	
	
	/**
	 * This method checks if a next transaction or flow can be scheduled. If the
	 * current transaction has been finished (all replies received) the next
	 * transaction of the current flow will be scheduled. If there are no more
	 * flows to schedule and no active flows left, the client will vote for stop
	 * (all traffic of client defined in trace file has been processed),
	 * otherwise the next flow will be scheduled.
	 * 
	 * @param activeFlow
	 */
	private void tryScheduleNextTransaction(ActiveFlow activeFlow) {
		// If all replies have been received for the current transaction of
		// activeFlow, the next transaction of the activeFlow will be scheduled
		// by being passed to the scheduler with the corresponding sendDelay
		if (activeFlow.allRepliesForCurrentTransactionReceived()) {
			if (activeFlow.getCurrentTransaction() != null)
				activeFlowsByTransactionId.remove(activeFlow.getCurrentTransaction().getTransactionId());
			if (activeFlow.hasNextTransaction()) { // next transaction available -> schedule its send
				Transaction nextTransaction = activeFlow.getNextTransaction();
				activeFlowsByTransactionId.put(nextTransaction.getTransactionId(), activeFlow);
				ReplayNextTransactionEvent replayNextTransactionEvent = new ReplayNextTransactionEvent(activeFlow);
				scheduler.executeIn(TimeUnit.MILLISECONDS.toNanos(nextTransaction.getSendDelay()), this, replayNextTransactionEvent);
			} else { // no next transaction (end of flow): If there is no next transaction in the activeFlow it will be checked, if there are more flows to schedule. If there are no more flows to schedule and no more activeFlows in the list activeFlowsByFlowId, this client has sent and received all data defined in the trace file --> client votes for stop.
				finishedFlows.add(activeFlow.getFlow().flowId);
				activeFlowsByFlowId.remove(activeFlow.getFlow().flowId); 
				scheduledFlows -= 1;
				if (noMoreFlowsToSchedule && activeFlowsByFlowId.size() == 0 && scheduledFlows == 0) { // nothing left to schedule and no more active flows -> end simulation
					if (DISPLAY_THROUGHPUT) 
						Stats.addResult(dataTransmitted);
					if (DISPLAY_MESSAGE_SIZES)
						Stats.addResult(msgSizes);
					owner.voteForStop(socket);
				}
			}
		} else if (!isDuplex) {  // simplex simulation -> schedule arrival of next reply (-> assume replies arrive as in original trace)
			Transaction transaction = activeFlow.getCurrentTransaction();
			if (transaction.hasMoreDistinctSimplexWithFeedbackReplyDelays()) {
				IncomingReplyEvent replyEvent = new IncomingReplyEvent(transaction.getTransactionId());
				scheduler.executeIn(transaction.getNextDistinctSimplexWithFeedbackReplyDelay(), this, replyEvent);
			}
		}
		tryScheduleNextFlows();
	}
	
	
	/**
	 * This method checks if the next flow can be scheduled (passed to the
	 * scheduler) regarding the restrictions (chronology) defined in the trace
	 * file. If the flow to schedule is not blocked by another one and the
	 * offset (e.g. user think time read from trace file) is reached/has passed, 
	 * the next flow will be scheduled by passing it to the scheduler with the
	 * corresponding delay.
	 */
	private void tryScheduleNextFlows() {
		while (flowSource.peekNextFlow() != null) { 
			if (flowSource.peekNextFlow().restriction == Restriction.NONE) { // schedule none restricted flow
				Flow nextFlow = flowSource.readNextFlow();
				ReplayNextFlowEvent replayNextFlowEvent = new ReplayNextFlowEvent(nextFlow); // Note: we do not call "replayFlow(nextFlow)" directly since the order of actions should not be changed
				scheduledFlows++;
				scheduler.executeIn(1l, this, replayNextFlowEvent);
			} else if (flowSource.peekNextFlow().restriction == Restriction.SIMPLE_DELAY) {
				Flow nextFlow = flowSource.readNextFlow();
				ReplayNextFlowEvent replayNextFlowEvent = new ReplayNextFlowEvent(nextFlow); // Note: we do not call "replayFlow(nextFlow)" directly since the order of actions should not be changed
				scheduledFlows++;
				scheduler.executeIn(TimeUnit.MILLISECONDS.toNanos(nextFlow.offsetFromRestriction), this, replayNextFlowEvent);
			} else if (flowSource.peekNextFlow().restriction == Restriction.NOT_BEFORE_END_OF_OTHER_FLOW) {
				if (flowSource.peekNextFlow().idOfRestrictingFlow < idOfFirstFlowOfCurrentFlowGroup || finishedFlows.contains(flowSource.peekNextFlow().idOfRestrictingFlow)) { // no more blocked (restricting flow is finished now)
					Flow nextFlow = flowSource.readNextFlow();
					ReplayNextFlowEvent replayNextFlowEvent = new ReplayNextFlowEvent(nextFlow); // Note: we do not call "replayFlow(nextFlow)" directly since the order of actions should not be changed
					scheduledFlows++;
					scheduler.executeIn(TimeUnit.MILLISECONDS.toNanos(nextFlow.offsetFromRestriction), this, replayNextFlowEvent);
				} else { // next flow is blocked
					break;
				}
			} else if (flowSource.peekNextFlow().restriction == Restriction.NOT_BEFORE_END_OF_TRANSACTION) {
				int idOfRestrictingFlow = flowSource.peekNextFlow().idOfRestrictingFlow;
				int idOfRestrictingTransaction = flowSource.peekNextFlow().idOfRestrictingTransaction;
				ActiveFlow restrictingFlow = activeFlowsByFlowId.get(idOfRestrictingFlow);
				if (	(restrictingFlow == null && idOfLatestStartedFlow >= idOfRestrictingFlow) // restricting flow is finished already
						|| (restrictingFlow != null && idOfRestrictingTransaction < restrictingFlow.getArrayOffsetOfCurrentTransaction()) // restricting transaction is already finished
						|| (restrictingFlow != null && (idOfRestrictingTransaction == restrictingFlow.getArrayOffsetOfCurrentTransaction() // restricting transaction is currently being replayed
							&& flowSource.peekNextFlow().idOfRestrictingReply <= restrictingFlow.getIdOfLatestFinishedReply())) // the restricting reply of the restricting transaction is already replayed
					) { // restriction no longer given
					Flow nextFlow = flowSource.readNextFlow();
					ReplayNextFlowEvent replayNextFlowEvent = new ReplayNextFlowEvent(nextFlow); // Note: we do not call "replayFlow(nextFlow)" directly since the order of actions should not be changed
					scheduledFlows++;
					scheduler.executeIn(TimeUnit.MILLISECONDS.toNanos(nextFlow.offsetFromRestriction), this, replayNextFlowEvent);
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

	
	/**
	 * This method replays a flow by setting the flow to active and starts
	 * the scheduling of the first transaction of the flow.
	 * 
	 * @param flow flow to replay
	 */
	private void replayFlow(Flow flow) {
		ActiveFlow activeFlow = new ActiveFlow(flow);
		activeFlowsByFlowId.put(flow.flowId, activeFlow);
		idOfLatestStartedFlow = flow.flowId;
		assert activeFlow.hasNextTransaction();
		tryScheduleNextTransaction(activeFlow);
	}
	
	
	
	/**
	 * This method is responsible for the sending process of the client by
	 * creating a SendableTransaction out of the transaction and flushing the
	 * bytes of the SendableTransaction to the network.
	 * 
	 * @param activeFlow
	 */
	private void replayTransaction(ActiveFlow activeFlow) {
		assert activeFlow.getCurrentTransaction() != null;
		Transaction transaction = activeFlow.getCurrentTransaction();
		transaction.attachment = activeFlow;
		if (transaction.getRequestSize() == 0) { // -> server shall send first message
			transaction.setRequestSize(16); // we assume that the client must first send a message to the final mix to open a socket that can receive data from the server (e.g. socks 5 bind command) 
		} else {
			if (DISPLAY_THROUGHPUT)
				this.dataTransmitted += transaction.getRequestSize();
			if (DISPLAY_MESSAGE_SIZES)
				this.msgSizes.add(transaction.getRequestSize());
		}
		byte[] message = transaction.createSendableTransaction(clientId);
		try {
			outputStream.write(message);
			outputStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
			owner.stopSimulation(e.getLocalizedMessage());
		}
		if (!isDuplex) { // simplex simulation -> schedule arrival of next reply (-> assume replies arrive as in original trace)
			if (transaction.containsReplies()) {
				IncomingReplyEvent replyEvent = new IncomingReplyEvent(transaction.getTransactionId());
				scheduler.executeIn(transaction.getNextDistinctSimplexWithFeedbackReplyDelay(), this, replyEvent);
            } else { // no reply
				// nothing to do
			}
		}
		if (!transaction.containsReplies()) { // try to schedule next transaction if we do not have to wait for a reply from the server
			tryScheduleNextTransaction(activeFlow);
		}
	}
	
	
	/**
	 * This method will be called by the scheduler when the delay/offset, which
	 * has been set when calling this method, is over. Then, this method calls
	 * the corresponding method to replay the flow, transaction or incoming
	 * reply (simplex only).
	 */
	@Override
	public synchronized void execute(Event event) {
		if (event instanceof ReplayNextFlowEvent) {
			replayFlow(((ReplayNextFlowEvent)event).nextFlow);
		} else if (event instanceof ReplayNextTransactionEvent) {
			replayTransaction(((ReplayNextTransactionEvent)event).correspondingFlow);
		} else if (event instanceof IncomingReplyEvent) { // SIMPLEX ONLY
			incomingReply(((IncomingReplyEvent)event).transactionID);
		} else {
			throw new RuntimeException("ERROR: received unknown Event: " +event.toString());
		}
	}
	
	
	/**
	 * This inner-class represents a flow and provides methods to read the next
	 * flow from the trace file.
	 * 
	 * @author Johannes Wendel, Simon Lecheler, kpf
	 * 
	 */
	private class FlowSource {
		
		private FlowReader trace;
		private FlowGroupFlowIterator currentFlowGroup;
		private Flow nextFlow = null;
		private boolean wasPeekNextFlowCalled = false;
		private boolean newFlowGroup = false;
		
		
		private FlowSource(FlowReader trace) {
			this.trace = trace;
		}
		
		
		/**
		 * This method returns the next flow of the client, if there is a next
		 * flow, by checking the trace file.
		 * 
		 * @return next flow
		 */
		
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
		
		
		/**
		 * This method returns the next flow of the client, if there is a next
		 * flow, by checking the trace file.
		 * 
		 * @return
		 */
		private Flow readNextFlow() {
			if (!wasPeekNextFlowCalled)
				peekNextFlow();
			wasPeekNextFlowCalled = false;
			Flow result = nextFlow;
			if (newFlowGroup) {
				if (finishedFlows != null)
					finishedFlows.clear();
				finishedFlows = new HashSet<Integer>();
				idOfFirstFlowOfCurrentFlowGroup = result.flowId;
				newFlowGroup = false;
			}
			nextFlow = null;
			return result;
		}
		
	}


	public static void displayResults(long durationInMs) {
		if (DISPLAY_THROUGHPUT || DISPLAY_MESSAGE_SIZES)
			Stats.displayResults(durationInMs);
	}
	
	
	private static class Stats {
		
		static Vector<Long> writtenBytes;
		static Vector<TLongArrayList> msgSizes; //TLongArrayList msgSizes

		public static void addResult(long bytesWritten) {
			if (writtenBytes == null) 
				writtenBytes = new Vector<Long>(10000);
			writtenBytes.add(bytesWritten);
		}
		
		
		public static void addResult(TLongArrayList results) {
			if (msgSizes == null) 
				msgSizes = new Vector<TLongArrayList>(10000);
			msgSizes.add(results);
		}
		
		public static void displayResults(long durationInMs) {
			if (DISPLAY_THROUGHPUT) {
				System.out.println("Measured throughput per client in KB/sec: "); 
				for (Long res:writtenBytes)
					System.out.println((double)res/(double)durationInMs); 
				System.out.println("\n\n"); 
			}
			if (DISPLAY_MESSAGE_SIZES) {
				System.out.println("ADU-Sizes (in Byte):"); 
				for (TLongArrayList res:msgSizes) {
					long[] all = res.toNativeArray();
					for (Long l:all) 
						System.out.println(l);
				}
				System.out.println("\n\n"); 
			}
		}
		
	}
}