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

import java.util.Vector;

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


public class ClientBasicSynchronous extends ClientCommunicationBehaviour implements EventExecutor {

	private int sendingRate;
	private Vector<NoneMixMessage> requestWaitingQueue = new Vector<NoneMixMessage>(10,10);
	
	
	protected ClientBasicSynchronous(Client owner, Simulator simulator) {
		
		super(owner, simulator);
		this.sendingRate = new Integer(Settings.getProperty("SENDING_RATE"));
	
	}

	
	@Override
	public void incomingRequestFromUser(NoneMixMessage request) {
		requestWaitingQueue.add(request);
	}


	@Override
	public void incomingDecryptedReply(NetworkMessage reply) {
		
	}
	
	
	private void sendMessage() {

		if (requestWaitingQueue.size() == 0) { // no data to send -> send dummy
			
			owner.sendRequest(MixMessage.getInstance(true, owner, simulator.getDistantProxy(), owner, Simulator.getNow(), true));
		
		} else { // data available to send -> send as much data as available (limit: free space in mixMessage)
			
			MixMessage mixMessage = MixMessage.getInstance(true, owner, simulator.getDistantProxy(), owner, Simulator.getNow(), false);

			for (int i=0; i<requestWaitingQueue.size(); i++) {
				
				NoneMixMessage noneMixMessage = requestWaitingQueue.get(i);
				
				if (mixMessage.getFreeSpace() >= noneMixMessage.getLength() && !noneMixMessage.isFragmented()) { // noneMixMessage fits in mixMessage completely
					
					requestWaitingQueue.remove(i);
					i--;
					mixMessage.addPayloadObject(noneMixMessage);
					
				} else { // add Fragment
					
					if (noneMixMessage.hasNextFragment()) {
						
						MessageFragment messageFragment = noneMixMessage.getFragment(mixMessage.getFreeSpace());
						mixMessage.addPayloadObject(messageFragment);
	
					}
					
					if (!noneMixMessage.hasNextFragment()) {
						requestWaitingQueue.remove(i);
						i--;
					}

				}
				
				if (mixMessage.getFreeSpace() == 0)
					break;
			
			}
			
			owner.sendRequest(mixMessage);

		}
		
		if (!stopSending) {
			Event sendNextMessageEvent = new Event(this, Simulator.getNow() + sendingRate, CommunicationBehaviourEvent.INVITATION_TO_SEND_NEXT_MIX_MESSAGE);
			simulator.scheduleEvent(sendNextMessageEvent, this);
		}
		
	}


	@Override
	public void executeEvent(Event event) {
		
		if (event.getEventType() != CommunicationBehaviourEvent.INVITATION_TO_SEND_NEXT_MIX_MESSAGE)
			throw new RuntimeException("ERROR! received unsupported event!" +event);

		sendMessage();	
		
	}

}
