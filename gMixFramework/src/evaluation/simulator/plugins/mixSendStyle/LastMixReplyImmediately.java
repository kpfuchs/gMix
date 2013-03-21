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
package evaluation.simulator.plugins.mixSendStyle;

import evaluation.simulator.Simulator;
import evaluation.simulator.core.message.MessageFragment;
import evaluation.simulator.core.message.MixMessage;
import evaluation.simulator.core.message.TransportMessage;
import evaluation.simulator.core.networkComponent.Mix;
import evaluation.simulator.core.networkComponent.NetworkNode;


public class LastMixReplyImmediately extends MixSendStyleImpl {

	
	public LastMixReplyImmediately(NetworkNode owner, Simulator simulator, ReplyReceiver replyReceiver) {
		
		super(owner, simulator, replyReceiver);
		
	}

	
	// must call mix.addReply(MixMessage mixMessage)
	@Override
	public void incomingDataFromServer(TransportMessage transportMessage) {
		
		if (owner instanceof Mix && !((Mix)owner).isLastMix())
			throw new RuntimeException("ERROR: only supports TransportMessage as reply from distant proxy! " +transportMessage); 
		
		MixMessage mixMessage = MixMessage.getInstance(false, owner, transportMessage.getOwner(), transportMessage.getOwner(), Simulator.getNow(), false);
		
		if (mixMessage.getFreeSpace() >= transportMessage.getLength()) { // transportMessage fits in mixMessage completely
			
			mixMessage.addPayloadObject(transportMessage);
			replyReceiver.incomingReply(mixMessage);
			
		} else {
			MessageFragment fragment = null;
			assert transportMessage.hasNextFragment();
			
			while (transportMessage.hasNextFragment()) {
				
				fragment = transportMessage.getFragment(mixMessage.getFreeSpace());
				//System.out.println("adding fragment to mix message " +fragment); 
				mixMessage.addPayloadObject(fragment);
				
				if (fragment.isLastFragment()) { // last fragment -> send message (even if it is not "full")
					
					replyReceiver.incomingReply(mixMessage);
					
				} else if (mixMessage.getFreeSpace() == 0) { // still data to send, but no more space -> send last and create new mixMessage
					
					replyReceiver.incomingReply(mixMessage);
					mixMessage = MixMessage.getInstance(false, owner, transportMessage.getOwner(), transportMessage.getOwner(), Simulator.getNow(), false);
					
				}

			}
			assert fragment.isLastFragment() : ""+fragment.getAssociatedTransportMessage().reltedEndToEndMessage.getPayload().getTransactionId();
				
		}
		
	}
	
}
