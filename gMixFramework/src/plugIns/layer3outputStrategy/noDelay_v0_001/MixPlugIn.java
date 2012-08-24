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
package plugIns.layer3outputStrategy.noDelay_v0_001;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer3OutputStrategyMix;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.routing.RoutingMode;


public class MixPlugIn extends Implementation implements Layer3OutputStrategyMix {

	
	@Override
	public void constructor() {
		// no need to do anything
	}

	
	@Override
	public void initialize() {
		// no need to do anything
	}

	
	@Override
	public void begin() {
		// no need to do anything
	}

	
	@Override
	public void addRequest(Request request) {
		if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_DYNAMIC_ROUTING) {
			request.nextHopAddress = anonNode.mixList.getRandomMixId();
			if (request.nextHopAddress == anonNode.PUBLIC_PSEUDONYM) // this mix is the final mix for this message
				request.nextHopAddress = MixMessage.NONE;
		}
		anonNode.putOutRequest(request);
	}


	@Override
	public void addReply(Reply reply) {
		anonNode.putOutReply(reply);
	}
	
	
	@Override
	public int getMaxSizeOfNextReply() {
		return super.recodingLayerMix.getMaxSizeOfNextReply();
	}


	@Override
	public int getMaxSizeOfNextRequest() {
		return super.recodingLayerMix.getMaxSizeOfNextRequest();
	}
	
}
