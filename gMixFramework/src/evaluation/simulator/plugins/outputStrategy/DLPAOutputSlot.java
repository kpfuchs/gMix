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

import java.util.Collections;
import java.util.HashMap;
import java.util.Vector;

import evaluation.simulator.Simulator;
import evaluation.simulator.core.event.Event;
import evaluation.simulator.core.event.EventExecutor;
import evaluation.simulator.core.message.MixMessage;
import evaluation.simulator.core.networkComponent.AbstractClient;
import evaluation.simulator.core.networkComponent.Mix;
import evaluation.simulator.core.networkComponent.NetworkNode;
import evaluation.simulator.pluginRegistry.StatisticsType;


public class DLPAOutputSlot implements EventExecutor {

	private HashMap<AbstractClient, MixMessage> messagesToSend;
	private long timeOfOutput;
	private boolean isRequestSlot;
	private Simulator simulator;
	private Mix mix;
	private DLPABasic dLPABasic;
	private Event relatedOutputEvent;

	
	public DLPAOutputSlot(boolean isRequestSlot, int maxDelay, Simulator simulator, Mix mix, DLPABasic dLPABasic) {
		this.dLPABasic = dLPABasic;
		this.mix = mix;
		this.simulator = simulator;
		this.messagesToSend = new HashMap<AbstractClient, MixMessage>(simulator.getNumberOfClients()*2);
		this.isRequestSlot = isRequestSlot;
		this.setTimeOfOutput(Simulator.getNow() + maxDelay);
		this.relatedOutputEvent = new Event(this, getTimeOfOutput(), OutputStrategyEvent.DLPA_TIMEOUT);
		simulator.scheduleEvent(relatedOutputEvent, this);
	}
	
	
	public boolean isUsedBy(AbstractClient client) {
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
		Vector<MixMessage> messages = new Vector<MixMessage>(simulator.getNumberOfClients());
		int dummyCounter = 0;
		int noneDummyCounter = 0;
		for (AbstractClient client: simulator.getClients().values()) {
			
			MixMessage mixMessage = messagesToSend.get(client);
			if (mixMessage == null) {
				mixMessage = createDummyMessage(client, isRequestSlot);
				dummyCounter++;
				dLPABasic.statistics.addValue(true, StatisticsType.DLPA_REQUEST_MESSAGE_DROP_PERCENTAGE);
			} else {
				noneDummyCounter++;
				dLPABasic.statistics.addValue(false, StatisticsType.DLPA_REQUEST_MESSAGE_DROP_PERCENTAGE);
			}
			messages.add(mixMessage);
		}
		messagesToSend.clear();
		Collections.shuffle(messages);
		for (MixMessage m:messages) {
			if (isRequestSlot) {
				dLPABasic.statistics.increment(1, StatisticsType.DLPA_REQUEST_SENDING_RATE_PER_MIX);
				dLPABasic.statistics.increment(1, StatisticsType.DLPA_REQUEST_SENDING_RATE_PER_MIX_AND_CLIENT);
				mix.putOutRequest(m);
			} else { // reply slot
				dLPABasic.statistics.increment(1, StatisticsType.DLPA_REPLY_SENDING_RATE_PER_MIX);
				mix.putOutReply(m);
			}
			dLPABasic.statistics.increment(1, StatisticsType.DLPA_REQUEST_AND_REPLY_SENDING_RATE_PER_MIX);
		}
		if (isRequestSlot) {
			double totalMessages = dummyCounter + noneDummyCounter;
			dLPABasic.statistics.addValue(((double)dummyCounter/totalMessages)*100d, StatisticsType.DLPA_REQUEST_DUMMY_PERCENTAGE_PER_MIX);
			
		}
	}
	
	
	private MixMessage createDummyMessage(AbstractClient owner, boolean isRequest) {
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


	public void setTimeOfOutput(long timeOfOutput) {
		this.timeOfOutput = timeOfOutput;
	}


	public long getTimeOfOutput() {
		return timeOfOutput;
	}


	public int getNumerOfMessagesContained() {
		return messagesToSend.size();
	}


	public Event getTimeoutEvent() {
		return this.relatedOutputEvent;
	}
	
}
