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
import evaluation.simulator.message.NoneMixMessage;
import evaluation.simulator.statistics.StatisticsType;


public class BasicDistantProxy extends DistantProxy {
	
	
	protected BasicDistantProxy(String identifier, Simulator simulator, DelayBox delayBox) {
		
		super(identifier, simulator, delayBox, false);
		
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
		
		sendReply(noneMixMessage, 0);
		
	}
	
}
