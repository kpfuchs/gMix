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
import java.util.Vector;

import evaluation.simulator.core.Event;
import evaluation.simulator.core.EventExecutor;
import evaluation.simulator.core.OutputStrategyEvent;
import evaluation.simulator.core.Simulator;
import evaluation.simulator.message.MixMessage;
import evaluation.simulator.networkComponent.Client;
import evaluation.simulator.networkComponent.Mix;
import evaluation.simulator.networkComponent.NetworkNode;
import evaluation.simulator.statistics.StatisticsType;


public class DLPAOutputSlot implements EventExecutor {

	private HashMap<Client, MixMessage> messagesToSend;
	private int timeOfOutput;
	private boolean isRequestSlot;
	private Simulator simulator;
	private Mix mix;
	private DLPABasic dLPABasic;
	private Event relatedOutputEvent;

	
	public DLPAOutputSlot(boolean isRequestSlot, int maxDelay, Simulator simulator, Mix mix, DLPABasic dLPABasic) {

		this.dLPABasic = dLPABasic;
		this.mix = mix;
		this.simulator = simulator;
		this.messagesToSend = new HashMap<Client, MixMessage>();
		this.isRequestSlot = isRequestSlot;
		this.setTimeOfOutput(Simulator.getNow() + maxDelay);
		this.relatedOutputEvent = new Event(this, getTimeOfOutput(), OutputStrategyEvent.DLPA_TIMEOUT);
		simulator.scheduleEvent(relatedOutputEvent, this);
		
	}
	
	
	public boolean isUsedBy(Client client) {
		
		return messagesToSend.containsKey(client);
		
	}
	
	
	public void addMessage(MixMessage mixMessage) {

		messagesToSend.put(mixMessage.getOwner(), mixMessage);
		
	}
	
	
	public void putOutMessages() {

		if (isRequestSlot)
			dLPABasic.removeRequestOutputslot(this);
		else
			dLPABasic.removeReplyOutputslot(this);
		
		Vector<MixMessage> messages = new Vector<MixMessage>(); // TODO: ohne vector... debug

		for (Client client: simulator.getClients().values()) {
			
			MixMessage mixMessage = messagesToSend.get(client);
			if (mixMessage == null)
				mixMessage = createDummyMessage(client, isRequestSlot);

			
			messages.add(mixMessage);
			/*if (isRequestSlot)
				mix.putOutRequest(mixMessage);	
			else
				mix.putOutReply(mixMessage);
			*/
			
		}

		//for (MixMessage mixMessage:messagesToSend.values()) {
			
			//TODO: record statistics (dlpa-overhead) System.out.println("overhead: " +((double)simulator.getClients().size() / messagesToSend.values().size() * 100 - 100) +"%");
			/*if (isRequestSlot)
				mix.putOutRequest(mixMessage);
			else
				mix.putOutReply(mixMessage);
			*/
			//messages.add(mixMessage);
		//}
		
		//TODO: record statistics (delay durch dlpa) 

		
		messagesToSend.clear();

		
		for (MixMessage m:messages) {
			
			if (isRequestSlot) {
				dLPABasic.statistics.addValue(1, StatisticsType.DLPA_REQUEST_SENDING_RATE);
				dLPABasic.statistics.addValue(1, StatisticsType.DLPA_REQUEST_SENDING_RATE_PER_CLIENT);
				mix.putOutRequest(m);
				
			} else {
				
				dLPABasic.statistics.addValue(1, StatisticsType.DLPA_REPLY_SENDING_RATE);
				mix.putOutReply(m);
				
			}
			
			dLPABasic.statistics.addValue(1, StatisticsType.DLPA_REQUEST_AND_REPLY_SENDING_RATE);
			
		}
		
		messages.clear();

	}
	
	
	private MixMessage createDummyMessage(Client owner, boolean isRequest) {
		
		NetworkNode source = isRequest ? owner : mix;
		NetworkNode destination = isRequest ? simulator.getDistantProxy() : owner;
		
		return MixMessage.getInstance(isRequest, source, destination, owner, Simulator.getNow(), true);
		
	}


	@Override
	public void executeEvent(Event e) {
		
		if (e.getEventType() == OutputStrategyEvent.DLPA_TIMEOUT) {
			putOutMessages();
		} else
			throw new RuntimeException("ERROR! Received unknown event! " +e);
		
	}


	public void setTimeOfOutput(int timeOfOutput) {
		this.timeOfOutput = timeOfOutput;
	}


	public int getTimeOfOutput() {
		return timeOfOutput;
	}


	public int getNumerOfMessagesContained() {
		return messagesToSend.size();
	}


	public Event getTimeoutEvent() {
		return this.relatedOutputEvent;
	}
	
}
