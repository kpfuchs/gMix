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
package evaluation.simulator.core.networkComponent;

import evaluation.simulator.Simulator;
import evaluation.simulator.core.event.MixEvent;
import evaluation.simulator.core.message.EndToEndMessage;
import evaluation.simulator.core.message.MessageFragment;
import evaluation.simulator.core.message.MixMessage;
import evaluation.simulator.core.message.PayloadObject;
import evaluation.simulator.core.message.TransportMessage;
import evaluation.simulator.pluginRegistry.MixSendStyle;
import evaluation.simulator.pluginRegistry.StatisticsType;
import evaluation.simulator.plugins.mixSendStyle.MixSendStyleImpl;
import evaluation.simulator.plugins.mixSendStyle.ReplyReceiver;


public class BasicDistantProxy extends DistantProxy implements ReplyReceiver {
	
	private MixSendStyleImpl lastMixCommunicationBehaviour = null;
	
	
	protected BasicDistantProxy(String identifier, Simulator simulator) {
		this(identifier, simulator, false);
	}

	
	protected BasicDistantProxy(String identifier, Simulator simulator, boolean supportsMixMessages) {
		super(identifier, simulator, supportsMixMessages);
	}
	
	
	
	@Override
	protected void incomingRequest(TransportMessage transportMessage) {
		EndToEndMessage message = transportMessage.reltedEndToEndMessage;
		message.transportMessage = transportMessage;
		statistics.increment(transportMessage.getLength(), StatisticsType.SUM_DISTANTPROXY_DATAVOLUME_SENDANDRECEIVE);
		transportMessage.getOwner().statistics.addValue(Simulator.getNow() - transportMessage.getCreationTime(), StatisticsType.AVG_CLIENT_LATENCY_LAYER5MESSAGE);
		transportMessage.getOwner().statistics.addValue(Simulator.getNow() - transportMessage.getCreationTime(), StatisticsType.AVG_CLIENT_LATENCY_LAYER5MESSAGE_HIST);
		transportMessage.getOwner().statistics.addValue(Simulator.getNow() - transportMessage.getCreationTime(), StatisticsType.CF_AVG_LATENCY_PER_CLIENT_SEND);
		server.incomingMessage(message);
		transportMessage.getOwner().messageReachedServer(message); // notify client that his message has now reached the server
	}
	
	
	protected void incomingRequest(MixMessage mixMessage) {
		if (this.lastMixCommunicationBehaviour == null)
			this.lastMixCommunicationBehaviour = MixSendStyle.getInstance(this, this);
		statistics.increment(1, StatisticsType.SUM_DISTANTPROXY_MIXMESSAGES_RECEIVED);
		if (!mixMessage.isDummy())
			for (PayloadObject payloadObject:mixMessage.getPayloadObjectsContained())
				if (payloadObject instanceof TransportMessage)
					incomingRequest((TransportMessage)payloadObject);
				else if (payloadObject instanceof MessageFragment) {
					if (((MessageFragment)payloadObject).isLastFragment())
						incomingRequest(((MessageFragment)payloadObject).getAssociatedTransportMessage());
				} else {
					throw new RuntimeException("ERROR: unknown PayloadObject type! " +payloadObject); 
				}
	}
	

	@Override
	protected void requestAnswered(TransportMessage transportMessage) {
		if (lastMixCommunicationBehaviour != null)
			lastMixCommunicationBehaviour.incomingDataFromServer(transportMessage);
		else
			sendToPreviousHop(transportMessage, 0, MixEvent.INCOMING_REPLY_FROM_DISTANT_PROXY);
	}


	@Override
	public void incomingReply(MixMessage mixMessage) {
		sendToPreviousHop(mixMessage, 0, MixEvent.INCOMING_MIX_MESSAGE_OF_TYPE_REPLY);
	}
	
}
