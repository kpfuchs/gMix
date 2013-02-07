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
package evaluation.simulator.networkComponent;

import evaluation.simulator.core.Simulator;
import evaluation.simulator.delayBox.DelayBox;
import evaluation.simulator.message.EndToEndMessage;
import evaluation.simulator.message.TransportMessage;
import evaluation.simulator.statistics.StatisticsType;


public class BasicDistantProxy extends DistantProxy {
	
	
	protected BasicDistantProxy(String identifier, Simulator simulator, DelayBox delayBox) {
		this(identifier, simulator, delayBox, false);
	}

	
	protected BasicDistantProxy(String identifier, Simulator simulator, DelayBox delayBox, boolean supportsMixMessages) {
		super(identifier, simulator, delayBox, supportsMixMessages);
	}
	
	
	
	@Override
	protected void incomingRequest(TransportMessage tm) {
		EndToEndMessage message = tm.reltedEndToEndMessage;
		message.transportMessage = tm;
		statistics.addValue(tm.getLength(), StatisticsType.DISTANTPROXY_DATAVOLUME_SENDANDRECEIVE);
		server.incomingMessage(message);
		tm.getOwner().messageReachedServer(message); // notify client that his message has now reached the server
		/*noneMixMessage.setRequest(false);
		noneMixMessage.setSource(this);
		noneMixMessage.setDestination(noneMixMessage.getOwner());
		noneMixMessage.setLength(noneMixMessage.getReplyLength());
		statistics.addValue(noneMixMessage.getReplyLength(), StatisticsType.DISTANTPROXY_DATAVOLUME_SENDANDRECEIVE);
		super.callRequestAnsweredIn(noneMixMessage.getResolveTimeAtDistantProxy(), noneMixMessage);*/
	}
	

	@Override
	protected void requestAnswered(TransportMessage noneMixMessage) {
		//sendReply(noneMixMessage, 0);
	}
	
}
