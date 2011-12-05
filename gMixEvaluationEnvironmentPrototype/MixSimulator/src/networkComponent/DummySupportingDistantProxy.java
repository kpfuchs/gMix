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

package networkComponent;

import communicationBehaviour.LastMixCommunicationBehaviour;
import communicationBehaviour.ReplyReceiver;
import message.MessageFragment;
import message.MixMessage;
import message.NoneMixMessage;
import message.PayloadObject;
import simulator.EventExecutor;
import simulator.MixEvent;
import simulator.Simulator;
import statistics.StatisticsType;
import delayBox.DelayBox;


public class DummySupportingDistantProxy extends DistantProxy implements EventExecutor, ReplyReceiver {
	
	private LastMixCommunicationBehaviour lastMixCommunicationBehaviour = null;
	
	
	protected DummySupportingDistantProxy(String identifier, Simulator simulator, DelayBox delayBox) {
		
		super(identifier, simulator, delayBox, true);
		this.lastMixCommunicationBehaviour = LastMixCommunicationBehaviour.getInstance(this, simulator, this);
		
	}


	protected void incomingRequest(MixMessage mixMessage) {
		
		statistics.addValue(1, StatisticsType.DISTANTPROXY_MIXMESSAGES_RECEIVED);
		
		if (!mixMessage.isDummy())
			for (PayloadObject payloadObject:mixMessage.getPayloadObjectsContained())
				if (payloadObject instanceof NoneMixMessage)
					incomingRequest((NoneMixMessage)payloadObject);
				else if (payloadObject instanceof MessageFragment) {
					if (((MessageFragment)payloadObject).isLastFragment())
						incomingRequest(((MessageFragment)payloadObject).getAssociatedNoneMixMessage());
				} else {
					throw new RuntimeException("ERROR: unknown PayloadObject type! " +payloadObject); 
				}
		
	}
	
	
	@Override
	protected void incomingRequest(NoneMixMessage noneMixMessage) {
		
		noneMixMessage.setRequest(false);
		noneMixMessage.setSource(this);
		noneMixMessage.setDestination(noneMixMessage.getOwner());
		noneMixMessage.setLength(noneMixMessage.getReplyLength());
		statistics.addValue(noneMixMessage.getLength(), StatisticsType.DISTANTPROXY_DATAVOLUME_SENDANDRECEIVE);
		statistics.addValue(noneMixMessage.getReplyLength(), StatisticsType.DISTANTPROXY_DATAVOLUME_SENDANDRECEIVE);
		super.callRequestAnsweredIn(noneMixMessage.getResolveTimeAtDistantProxy(), noneMixMessage);

	}

	
	@Override
	protected void requestAnswered(NoneMixMessage noneMixMessage) {
		
		lastMixCommunicationBehaviour.incomingDataFromDistantProxy(noneMixMessage);
		
	}


	@Override
	public void incomingReply(MixMessage mixMessage) {
		
		sendToPreviousHop(mixMessage, 0, MixEvent.INCOMING_MIX_MESSAGE_OF_TYPE_REPLY);
	}
	
}
