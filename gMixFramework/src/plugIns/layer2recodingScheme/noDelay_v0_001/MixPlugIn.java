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
package plugIns.layer2recodingScheme.noDelay_v0_001;

import framework.core.AnonNode;
import framework.core.controller.Implementation;
import framework.core.interfaces.Layer2RecodingSchemeMix;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.routing.MixList;
import framework.core.routing.RoutingMode;
import framework.core.routing.UnpackedIdArray;
import framework.core.userDatabase.User;
import framework.core.util.Util;


public class MixPlugIn extends Implementation implements Layer2RecodingSchemeMix {

	
	@Override
	public void constructor() {
		
	}

	
	@Override
	public void initialize() {

	}
	

	@Override
	public void begin() {
		if (anonNode.IS_DUPLEX)
			new ReplyThread().start();
		new RequestThread().start();
	}

	
	@Override
	public int getMaxSizeOfNextReply() {
		return anonNode.MAX_PAYLOAD;
	}

	
	@Override
	public int getMaxSizeOfNextRequest() {
		if (anonNode.ROUTING_MODE == RoutingMode.CASCADE) {
			return anonNode.MAX_PAYLOAD;
		} else if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) {
			return anonNode.MAX_PAYLOAD - getRouteHeaderSize(anonNode);
		} else if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_DYNAMIC_ROUTING) {
			return anonNode.MAX_PAYLOAD;
		} else {
			throw new RuntimeException("not supported routing mode: " +anonNode.ROUTING_MODE); 
		}
	}

	
	@Override
	public Request generateDummy(int[] route, User user) {
		throw new RuntimeException("not supported");
		/*Request request = MixMessage.getInstanceRequest(new byte[0], settings);
		request.setOwner(user);
		request.route = route;
		return request;*/
	}

	
	@Override
	public Request generateDummy(User user) {
		Request request = MixMessage.getInstanceRequest(new byte[0]);
		request.setOwner(user);
		return request;
	}
	

	@Override
	public Reply generateDummyReply(int[] route, User user) {
		throw new RuntimeException("not supported");
		/*Reply reply = MixMessage.getInstanceReply(new byte[0], settings);
		reply.setOwner(user);
		reply.route = route;
		return reply;*/
	}

	
	@Override
	public Reply generateDummyReply(User user) {
		Reply reply = MixMessage.getInstanceReply(new byte[0]);
		reply.setOwner(user);
		return reply;
	}
	
	
	class RequestThread extends Thread {
		
		@Override
		public void run() {
			while (true) {
				Request[] requests = anonNode.getFromRequestInputQueue();
				for (Request request:requests) {
					if (anonNode.ROUTING_MODE == RoutingMode.CASCADE) {
						outputStrategyLayerMix.addRequest(request);
					} else if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) {
						byte[][] splitted = Util.split(getRouteHeaderSize(anonNode), request.getByteMessage());
						UnpackedIdArray routeInfo = MixList.unpackIdArrayWithPos(splitted[0]);
						if (routeInfo.pos >= routeInfo.route.length) {
							if (anonNode.DISPLAY_ROUTE_INFO)
								System.out.println(""+anonNode +" setting nextHopAddress to \"LAST HOP\" (pos: " +routeInfo.pos +")"); 
							request.nextHopAddress = MixMessage.NONE;
							request.setByteMessage(splitted[1]);
						} else {
							if (anonNode.DISPLAY_ROUTE_INFO)
								System.out.println(""+anonNode +" setting nextHopAddress to " +routeInfo.route[routeInfo.pos] +", pos: " +routeInfo.pos); 
							request.nextHopAddress = routeInfo.route[routeInfo.pos];
							routeInfo.pos++;
							System.arraycopy(MixList.packIdArray(routeInfo), 0, request.getByteMessage(), 0, getRouteHeaderSize(anonNode));
						}
						outputStrategyLayerMix.addRequest(request);
					} else if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_DYNAMIC_ROUTING) {
						request.nextHopAddress = anonNode.mixList.getRandomMixId();
						outputStrategyLayerMix.addRequest(request);
					} else {
						throw new RuntimeException("not supported routing mode: " +anonNode.ROUTING_MODE); 
					}
					
				}

			}	
		}
	}
	
	
	class ReplyThread extends Thread {
		
		@Override
		public void run() {
			while (true) {
				Reply[] replies = anonNode.getFromReplyInputQueue();
				for (Reply reply:replies) {
					if (anonNode.ROUTING_MODE == RoutingMode.CASCADE) {
						outputStrategyLayerMix.addReply(reply);
					} else if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) {
						outputStrategyLayerMix.addReply(reply);
					} else if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_DYNAMIC_ROUTING) {
						reply.nextHopAddress = anonNode.mixList.getRandomMixId();
						outputStrategyLayerMix.addReply(reply);
					} else {
						throw new RuntimeException("not supported routing mode: " +anonNode.ROUTING_MODE); 
					}
				}
			}
		}
	}

	
	protected static int getRouteHeaderSize(AnonNode anonNode) {
		return ((anonNode.FREE_ROUTE_LENGTH-1)*4)+2;
	}
}
