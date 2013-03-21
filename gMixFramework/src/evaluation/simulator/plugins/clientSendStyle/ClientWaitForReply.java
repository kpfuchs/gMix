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
package evaluation.simulator.plugins.clientSendStyle;

import java.util.Vector;

import evaluation.simulator.Simulator;
import evaluation.simulator.core.message.MessageFragment;
import evaluation.simulator.core.message.MixMessage;
import evaluation.simulator.core.message.NetworkMessage;
import evaluation.simulator.core.message.TransportMessage;
import evaluation.simulator.core.networkComponent.AbstractClient;


public class ClientWaitForReply extends ClientSendStyleImpl {

	private Vector<TransportMessage> requestWaitingQueue = new Vector<TransportMessage>(10,10);
	private boolean isFirstCall;
	
	
	public ClientWaitForReply(AbstractClient owner, Simulator simulator) {
		super(owner, simulator);
		this.isFirstCall = true;
	}
	

	@Override
	public void incomingRequestFromUser(TransportMessage request) {
		
		requestWaitingQueue.add(request);
		
		if (isFirstCall) {
			isFirstCall = false;
			sendMessage();
		}
		
	}

	
	private void sendMessage() {
		
		if (requestWaitingQueue.size() == 0) { // no data to send -> send dummy
			
			owner.sendRequest(MixMessage.getInstance(true, owner, simulator.getDistantProxy(), owner, Simulator.getNow(), true));
		
		} else { // data available to send -> send as much data as available (limit: free space in mixMessage)
			
			MixMessage mixMessage = MixMessage.getInstance(true, owner, simulator.getDistantProxy(), owner, Simulator.getNow(), false);

			for (int i=0; i<requestWaitingQueue.size(); i++) {
				
				TransportMessage noneMixMessage = requestWaitingQueue.get(i);
				
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
		
	}
	
	
	@Override
	public void incomingDecryptedReply(NetworkMessage reply) {
		sendMessage();
	}


	@Override
	public void messageReachedServer(TransportMessage request) {
		if (!simulateReplyChannel)
			sendMessage();
	}

}
