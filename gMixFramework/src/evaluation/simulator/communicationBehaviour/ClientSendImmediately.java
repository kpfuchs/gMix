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
package evaluation.simulator.communicationBehaviour;

import evaluation.simulator.core.Simulator;
import evaluation.simulator.message.MessageFragment;
import evaluation.simulator.message.MixMessage;
import evaluation.simulator.message.NetworkMessage;
import evaluation.simulator.message.TransportMessage;
import evaluation.simulator.networkComponent.AbstractClient;


public class ClientSendImmediately extends ClientCommunicationBehaviour {

	
	protected ClientSendImmediately(AbstractClient owner, Simulator simulator) {
		
		super(owner, simulator);

	}


	@Override
	public void incomingRequestFromUser(TransportMessage request) {
		
		MixMessage mixMessage = MixMessage.getInstance(true, owner, simulator.getDistantProxy(), owner, Simulator.getNow(), false);
		
		if (mixMessage.getFreeSpace() >= request.getLength()) { // incomingData fits in mixMessage
			
			mixMessage.addPayloadObject(request);
			owner.sendRequest(mixMessage);
			
		} else { // incomingData does not fit in mixMessage -> fragment incomingData
			
			while (request.hasNextFragment()) {
				
				MessageFragment fragment = request.getFragment(mixMessage.getFreeSpace());
				mixMessage.addPayloadObject(fragment);
				
				if (fragment.isLastFragment()) { // last fragment -> send message (even if it is not "full")
					
					owner.sendRequest(mixMessage);
					
				} else if (mixMessage.getFreeSpace() == 0) { // still data to send, but no more space -> send last and create new mixMessage
					
					owner.sendRequest(mixMessage);
					mixMessage = MixMessage.getInstance(true, owner, simulator.getDistantProxy(), owner, Simulator.getNow(), false);
				
				}
				
			}

		}	
		
	}


	@Override
	public void incomingDecryptedReply(NetworkMessage reply) {

	}
	
	
	@Override
	public void messageReachedServer(TransportMessage request) {

	}

}
