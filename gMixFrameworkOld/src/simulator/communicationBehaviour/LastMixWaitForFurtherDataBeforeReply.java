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

package simulator.communicationBehaviour;

import simulator.core.CommunicationBehaviourEvent;
import simulator.core.Event;
import simulator.core.EventExecutor;
import simulator.core.Settings;
import simulator.core.Simulator;
import simulator.message.MessageFragment;
import simulator.message.MixMessage;
import simulator.message.NoneMixMessage;
import simulator.networkComponent.Client;
import simulator.networkComponent.NetworkNode;


public class LastMixWaitForFurtherDataBeforeReply extends LastMixCommunicationBehaviour implements EventExecutor {

	private int timeToWaitForFurtherDataFromDistantProxy;
	private boolean setupComplete = false;
	private MixMessage[] replies;
	private Event[] timeoutEvents;
	
 	
	protected LastMixWaitForFurtherDataBeforeReply(NetworkNode owner, Simulator simulator, ReplyReceiver replyReceiver) {
		
		super(owner, simulator, replyReceiver);
		this.timeToWaitForFurtherDataFromDistantProxy = new Integer(Settings.getProperty("TIME_TO_WAIT_FOR_DATA_FROM_DISTANT_PROXY")); // in ms
		
	}
	
	
	@Override
	public void incomingDataFromDistantProxy(NoneMixMessage noneMixMessage) {
		
		if (!setupComplete)
			setup();
		
		if (replies[noneMixMessage.getOwner().getClientId()] == null) {
			
			replies[noneMixMessage.getOwner().getClientId()] = MixMessage.getInstance(false, owner, noneMixMessage.getOwner(), noneMixMessage.getOwner(), Simulator.getNow(), false);
			timeoutEvents[noneMixMessage.getOwner().getClientId()] = new Event(this, Simulator.getNow() + timeToWaitForFurtherDataFromDistantProxy, CommunicationBehaviourEvent.TIMEOUT_SEND_CURRENT_MIX_REPLY);
			timeoutEvents[noneMixMessage.getOwner().getClientId()].setAttachment(noneMixMessage.getOwner().getClientId());
			simulator.scheduleEvent(timeoutEvents[noneMixMessage.getOwner().getClientId()], this);
		
		}
		
		if (replies[noneMixMessage.getOwner().getClientId()].getFreeSpace() >= noneMixMessage.getLength()) { // incomingData fits in mixMessage
			
			replies[noneMixMessage.getOwner().getClientId()].addPayloadObject(noneMixMessage);
			
			if (replies[noneMixMessage.getOwner().getClientId()].getFreeSpace() == 0) {
				simulator.unscheduleEvent(timeoutEvents[noneMixMessage.getOwner().getClientId()]);
				replyReceiver.incomingReply(replies[noneMixMessage.getOwner().getClientId()]);
				replies[noneMixMessage.getOwner().getClientId()] = null;
				
			}
			
		} else { // incomingData does not fit in mixMessage -> fragment incomingData
		
			while (noneMixMessage.hasNextFragment()) {
				
				MessageFragment fragment = noneMixMessage.getFragment(replies[noneMixMessage.getOwner().getClientId()].getFreeSpace());
				replies[noneMixMessage.getOwner().getClientId()].addPayloadObject(fragment);
				
				if (replies[noneMixMessage.getOwner().getClientId()].getFreeSpace() == 0) { // still data to send, but no more space -> send last and create new mixMessage
					simulator.unscheduleEvent(timeoutEvents[noneMixMessage.getOwner().getClientId()]);
					replyReceiver.incomingReply(replies[noneMixMessage.getOwner().getClientId()]);
					replies[noneMixMessage.getOwner().getClientId()] = MixMessage.getInstance(false, owner, noneMixMessage.getOwner(), noneMixMessage.getOwner(), Simulator.getNow(), false);
					timeoutEvents[noneMixMessage.getOwner().getClientId()] = new Event(this, Simulator.getNow() + timeToWaitForFurtherDataFromDistantProxy, CommunicationBehaviourEvent.TIMEOUT_SEND_CURRENT_MIX_REPLY);
					timeoutEvents[noneMixMessage.getOwner().getClientId()].setAttachment(noneMixMessage.getOwner().getClientId());
					simulator.scheduleEvent(timeoutEvents[noneMixMessage.getOwner().getClientId()], this);
				
				}
				
			}
			
		}
		
	}

	
	protected void setup() {
		
		setupComplete = true;
		replies = new MixMessage[Client.getNumberOfClients()];
		timeoutEvents = new Event[Client.getNumberOfClients()];

	}
	

	@Override
	public void executeEvent(Event event) {
		
		if ((CommunicationBehaviourEvent)event.getEventType() == CommunicationBehaviourEvent.TIMEOUT_SEND_CURRENT_MIX_REPLY) {
			
			MixMessage mixMessage = replies[(Integer)event.getAttachment()];
			replies[(Integer)event.getAttachment()] = null;
			replyReceiver.incomingReply(mixMessage);

		} else 
			throw new RuntimeException("ERROR: LastMixWaitForFurtherDataBeforeReply received unknown Event: " +event.toString());
		
	}
	
}