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
import evaluation.simulator.message.NoneMixMessage;
import evaluation.simulator.networkComponent.Client;
import evaluation.simulator.networkComponent.Mix;


// output strategy, that collects messages until "batchSize" messages are reached
// when "batchSize" messages are reached, all messages are sent (in random order)
// accepts only one message per participant for each batch
public class DistinctUserBatch extends OutputStrategy implements EventExecutor {
	
	private boolean setupComplete;
	private int batchSize;
	private MixMessage[] collectedRequests;
	private MixMessage[] collectedReplies;
	private int requestCounter = 0;
	private int replyCounter = 0;
	private int timeout;
	private Event timeoutEvent = null;
	private Map<String, Vector<NoneMixMessage>> clientReplyWaitingQueues;
	private boolean outputAllowed = false;
	
	private final static int HAS_SENT_DUMMY = 0;
	private final static int DATA_AVAILABLE = 1;
	private final static int TRUE = 1;
	private final static int FALSE = 0;
	private int[][] replyInfo;
	

	
	protected DistinctUserBatch(Mix mix, Simulator simulator) {
		
		super(mix, simulator);
		this.timeout = Simulator.settings.getPropertyAsInt("MAX_REPLY_DELAY_DISTINCT_USER_BATCH");
		
	}


	private void setup() {
		
		setupComplete = true;
		batchSize = Client.getNumberOfClients();
		collectedRequests = new MixMessage[batchSize];
		
		if (Simulator.settings.getPropertyAsBoolean("SIMULATE_REPLY_CHANNEL")) {
			
			collectedReplies = new MixMessage[batchSize];
			
			if (mix.isLastMix()) {
				
				this.replyInfo = new int[Client.getNumberOfClients()][2];
				
				clientReplyWaitingQueues = new HashMap<String, Vector<NoneMixMessage>>();
				
				for (Client c: simulator.getClients().values()) {
					clientReplyWaitingQueues.put(c.getIdentifier(), new Vector<NoneMixMessage>(10,10));
				}
				
			}

		}
		
	}


	@Override
	public void incomingRequest(MixMessage mixMessage) {
		
		if (!setupComplete)
			setup();
		
		if (collectedRequests[mixMessage.getOwner().getClientId()] != null)
			throw new RuntimeException("ERROR! two messages from the same client for one batch! " +mixMessage); // ggf. nachrichtenqueue einfügen, wenn das unterstützt werden soll
		
		collectedRequests[mixMessage.getOwner().getClientId()] = mixMessage;
		
		if (++requestCounter == batchSize) { // put out batch
			
			if (mix.isLastMix()) {
				
				for (MixMessage m: collectedRequests) {
					replyInfo[m.getOwner().getClientId()][HAS_SENT_DUMMY] = m.isDummy() ? TRUE : FALSE;
					mix.putOutRequest(m); 
				}
				
				if(!stopReplying) {
					this.timeoutEvent = new Event(this, Simulator.getNow() + timeout, OutputStrategyEvent.TIMEOUT);
					simulator.scheduleEvent(timeoutEvent, this);
					outputAllowed = true;
				} 
				
			} else {
				
				for (MixMessage m: collectedRequests)
					mix.putOutRequest(m);
			}
	
			requestCounter = 0;
			collectedRequests = new MixMessage[batchSize];

		}
		
	}


	@Override
	public void incomingReply(MixMessage mixMessage) {
		
		if (!setupComplete)
			setup();
		
		if (collectedReplies[mixMessage.getOwner().getClientId()] != null)
			throw new RuntimeException("ERROR! two messages from the same client for one batch!"); // ggf. nachrichtenqueue einfügen, wenn das unterstützt werden soll
		
		collectedReplies[mixMessage.getOwner().getClientId()] = mixMessage;
		
		if (++replyCounter == batchSize) {
			
			for (MixMessage m: collectedReplies)
				mix.putOutReply(m);
			
			replyCounter = 0;
			collectedReplies = new MixMessage[batchSize];
			
		}
		
	}
	
	
	private void timeout() {
		
		putOutReplyBatch(true);
		/*if (doOutput()) {
			
			putOutReplyBatch();
			
		} else {
			
			Event timeoutEvent = new Event(this, Simulator.getNow() + timeout, OutputStrategyEvent.TIMEOUT);
			simulator.scheduleEvent(timeoutEvent, this);
			
		}*/
			
	}
	
	
	
	private boolean doOutput() {
		
		for (int[] data: replyInfo)
			if (data[HAS_SENT_DUMMY] == FALSE && data[DATA_AVAILABLE] == 0)
				return false;
			
		return true;
		
	}
	
	
	private void putOutReplyBatch(boolean timeoutReached) {
		
		if (!outputAllowed)
			return;
		
		outputAllowed = false;
		
		for (Client client: simulator.getClients().values()) {
			
			Vector<NoneMixMessage> replyWaitingQueue =  clientReplyWaitingQueues.get(client.getIdentifier());
			boolean isDummy =  replyWaitingQueue.size() == 0 ? true: false;
			
			MixMessage mixMessage = MixMessage.getInstance(false, mix, client, client, Simulator.getNow(), isDummy);
			
			if (isDummy) {
				
				mix.putOutReply(mixMessage);
				
			} else {

				for (int i=0; i<replyWaitingQueue.size(); i++) {
					
					NoneMixMessage noneMixMessage = replyWaitingQueue.get(i);
					
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
				
				replyInfo[mixMessage.getOwner().getClientId()][DATA_AVAILABLE] -= mixMessage.getPayloadLength();
				mix.putOutReply(mixMessage);
				
			}	
			
		}
		
		replyCounter = 0;
		collectedReplies = new MixMessage[batchSize];
		
		if (!timeoutReached) {
			simulator.unscheduleEvent(this.timeoutEvent);
		}

	}


	@Override
	public void incomingReply(NoneMixMessage noneMixMessage) {
		
		if (!mix.isLastMix())
			throw new RuntimeException("ERROR: BasicSynchronousBatch only supports NoneMixMessages as reply from distant proxy!");
		
		if (!setupComplete)
			setup();
		
		clientReplyWaitingQueues.get(noneMixMessage.getOwner().getIdentifier()).add(noneMixMessage);
		replyInfo[noneMixMessage.getOwner().getClientId()][DATA_AVAILABLE] += noneMixMessage.getLength();
		
		if (doOutput())
			putOutReplyBatch(false);
		
	}


	@Override
	public void executeEvent(Event event) {
		
		if (event.getEventType() != OutputStrategyEvent.TIMEOUT)
			throw new RuntimeException("ERROR! received unsupported event!" +event);
		
		timeout();
		
	}

}
