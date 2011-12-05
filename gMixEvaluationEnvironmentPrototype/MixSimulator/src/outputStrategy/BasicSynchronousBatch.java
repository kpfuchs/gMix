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

package outputStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import message.MessageFragment;
import message.MixMessage;
import message.NoneMixMessage;
import networkComponent.Client;
import networkComponent.Mix;
import simulator.Event;
import simulator.EventExecutor;
import simulator.OutputStrategyEvent;
import simulator.Settings;
import simulator.Simulator;


// 1991: Andreas Pfitzmann, Birgit Pfitzmann, Michael Waidner: ISDN-MIXes: 
// Untraceable Communication with Very Small Bandwidth Overhead
// simplified version; no broadcast; no anonymity for servers
// mix that expects exactly one message from each participant per "sendingRate" (see class "ClientBasicSynchronous")
// (blocks until every client has sent a message!)
// will put out a reply batch every "replyRate" ms 
// (creates dummies if no data available)
public class BasicSynchronousBatch extends OutputStrategy implements EventExecutor {

	private SimplexSynchronousBatch requestBatch;
	private SimplexSynchronousBatch replyBatch;
	private int replyRate;
	private boolean setupComplete = false;
	private Map<String, Vector<NoneMixMessage>> clientReplyWaitingQueues;
	
	
	protected BasicSynchronousBatch(Mix mix, Simulator simulator) {
		
		super(mix, simulator);
		
		int batchSize = new Integer(Settings.getProperty("NUMBER_OF_CLIENTS_TO_SIMULATE"));
		requestBatch = new SimplexSynchronousBatch(batchSize, true);
		replyBatch = new SimplexSynchronousBatch(batchSize, false);
		this.replyRate = new Integer(Settings.getProperty("REPLY_RATE"));
		
		if (mix.isLastMix())
			clientReplyWaitingQueues = new HashMap<String, Vector<NoneMixMessage>>();
	
	}

	
	@Override
	public void incomingRequest(MixMessage mixMessage) {
		
		if (mix.isLastMix() && !setupComplete)
			setup(); // TODO: statt setup so aufzurufen: generelle methode einführen, die von simulator.java aufgerufen wird, sobald alle objekte instanziiert sind...

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
	
	
	public void incomingReply(NoneMixMessage noneMixMessage) {
		
		if (!mix.isLastMix())
			throw new RuntimeException("ERROR: BasicSynchronousBatch only supports NoneMixMessages as reply from distant proxy!");
		
		if (!setupComplete)
			setup();
		
		clientReplyWaitingQueues.get(noneMixMessage.getOwner().getIdentifier()).add(noneMixMessage);
		
	}
	
	
	protected void setup() {
		
		setupComplete = true;
		
		if (Settings.getProperty("SIMULATE_REPLY_CHANNEL").equals("TRUE")) {
			
			for (Client c: simulator.getClients().values())
				clientReplyWaitingQueues.put(c.getIdentifier(), new Vector<NoneMixMessage>(10,10));
			
			Event putOutNextReplyBatchEvent = new Event(this, Simulator.getNow() + replyRate, OutputStrategyEvent.PUT_OUT_REPLY_BATCH);
			simulator.scheduleEvent(putOutNextReplyBatchEvent, this);
			
		}
		
	}
	
	
	private void putOutReplyBatch() {
		 
		for (Client client: simulator.getClients().values()) {
			
			Vector<NoneMixMessage> replyWaitingQueue =  clientReplyWaitingQueues.get(client.getIdentifier());
			boolean isDummy =  replyWaitingQueue.size() == 0 ? true: false;
			
			MixMessage mixMessage = MixMessage.getInstance(false, mix, client, client, Simulator.getNow(), isDummy);
			
			if (isDummy) {
				
				incomingReply(mixMessage);
				
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
				
				incomingReply(mixMessage);
				
			}	
			
		}
		
		if(!stopReplying) {
			Event sendNextReplyEvent = new Event(this, Simulator.getNow() + replyRate, OutputStrategyEvent.PUT_OUT_REPLY_BATCH);
			simulator.scheduleEvent(sendNextReplyEvent, this);
		} 
				
	}
	
	
	@Override
	public void executeEvent(Event event) {
		
		if (event.getEventType() != OutputStrategyEvent.PUT_OUT_REPLY_BATCH)
			throw new RuntimeException("ERROR! received unsupported event!" +event);
		
		putOutReplyBatch();	
		
	}

}
