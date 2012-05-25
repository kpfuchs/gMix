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
import simulator.message.NetworkMessage;
import simulator.message.NoneMixMessage;
import simulator.networkComponent.Client;


public class ClientWaitForFurtherDataBeforeSend extends ClientCommunicationBehaviour implements EventExecutor {

	private MixMessage mixMessage;
	private int timeToWaitForFurtherDataFromUser;
	private Event timeoutEvent;
	
	
	protected ClientWaitForFurtherDataBeforeSend(Client owner, Simulator simulator) {
		
		super(owner, simulator);
		this.timeToWaitForFurtherDataFromUser = new Integer(Settings.getProperty("TIME_TO_WAIT_FOR_FURTHER_DATA_FROM_USER")); // in ms
		

	}
	
	
	@Override
	public void incomingRequestFromUser(NoneMixMessage request) {
		
		if (mixMessage == null) {
			mixMessage = MixMessage.getInstance(true, owner, simulator.getDistantProxy(), owner, Simulator.getNow(), false);
			timeoutEvent = new Event(this, Simulator.getNow() + timeToWaitForFurtherDataFromUser, CommunicationBehaviourEvent.TIMEOUT_SEND_CURRENT_MIX_REQUEST);
			simulator.scheduleEvent(timeoutEvent, this);
		}
		
		if (mixMessage.getFreeSpace() >= request.getLength()) { // incomingData fits in mixMessage
			
			mixMessage.addPayloadObject(request);
			
			if (mixMessage.getFreeSpace() == 0) {
				simulator.unscheduleEvent(timeoutEvent);
				owner.sendRequest(mixMessage);
				mixMessage = null;
				
			}
			
		} else { // incomingData does not fit in mixMessage -> fragment incomingData
		
			while (request.hasNextFragment()) {
				
				MessageFragment fragment = request.getFragment(mixMessage.getFreeSpace());
				mixMessage.addPayloadObject(fragment);
				
				if (mixMessage.getFreeSpace() == 0) { // still data to send, but no more space -> send last and create new mixMessage
					simulator.unscheduleEvent(timeoutEvent);
					owner.sendRequest(mixMessage);
					mixMessage = MixMessage.getInstance(true, owner, simulator.getDistantProxy(), owner, Simulator.getNow(), false);
					timeoutEvent = new Event(this, Simulator.getNow() + timeToWaitForFurtherDataFromUser, CommunicationBehaviourEvent.TIMEOUT_SEND_CURRENT_MIX_REQUEST);
					simulator.scheduleEvent(timeoutEvent, this);
				
				}
				
			}
			
		}
		
	}
	

	@Override
	public void incomingDecryptedReply(NetworkMessage reply) {

	}

	
	@Override
	public void executeEvent(Event event) {
		
		if ((CommunicationBehaviourEvent)event.getEventType() == CommunicationBehaviourEvent.TIMEOUT_SEND_CURRENT_MIX_REQUEST) {
			
			owner.sendRequest(mixMessage);
			mixMessage = null;
			
		} else 
			throw new RuntimeException("ERROR: ClientWaitForFurtherDataBeforeSend received unknown Event: " +event.toString());
		
	}
	
}