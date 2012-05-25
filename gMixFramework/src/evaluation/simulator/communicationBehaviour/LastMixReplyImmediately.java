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
import evaluation.simulator.message.NoneMixMessage;
import evaluation.simulator.networkComponent.Mix;
import evaluation.simulator.networkComponent.NetworkNode;


public class LastMixReplyImmediately extends LastMixCommunicationBehaviour {

	
	protected LastMixReplyImmediately(NetworkNode owner, Simulator simulator, ReplyReceiver replyReceiver) {
		
		super(owner, simulator, replyReceiver);
		
	}

	
	// must call mix.addReply(MixMessage mixMessage)
	@Override
	public void incomingDataFromDistantProxy(NoneMixMessage noneMixMessage) {
		
		if (owner instanceof Mix && !((Mix)owner).isLastMix())
			throw new RuntimeException("ERROR: Batch only supports NoneMixMessages as reply from distant proxy! " +noneMixMessage); 
		
		MixMessage mixMessage = MixMessage.getInstance(false, owner, noneMixMessage.getOwner(), noneMixMessage.getOwner(), Simulator.getNow(), false);
		
		if (mixMessage.getFreeSpace() >= noneMixMessage.getLength()) { // noneMixMessage fits in mixMessage completely
			
			mixMessage.addPayloadObject(noneMixMessage);
			replyReceiver.incomingReply(mixMessage);
			
		} else {
			
			while (noneMixMessage.hasNextFragment()) {
			
				MessageFragment fragment = noneMixMessage.getFragment(mixMessage.getFreeSpace());
				mixMessage.addPayloadObject(fragment);
				
				if (fragment.isLastFragment()) { // last fragment -> send message (even if it is not "full")
					
					replyReceiver.incomingReply(mixMessage);
					
				} else if (mixMessage.getFreeSpace() == 0) { // still data to send, but no more space -> send last and create new mixMessage
					
					replyReceiver.incomingReply(mixMessage);
					mixMessage = MixMessage.getInstance(false, owner, noneMixMessage.getOwner(), noneMixMessage.getOwner(), Simulator.getNow(), false);
					
				}

			}
				
		}
		
	}
	
}
