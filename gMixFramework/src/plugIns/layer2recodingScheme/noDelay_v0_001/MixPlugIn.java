/*******************************************************************************
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2014  SVS
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
 *******************************************************************************/
package plugIns.layer2recodingScheme.noDelay_v0_001;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import framework.core.AnonNode;
import framework.core.controller.Implementation;
import framework.core.interfaces.Layer2RecodingSchemeMix;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.message.ExternalMessage.DummyStatus;
import framework.core.routing.MixList;
import framework.core.routing.RoutingMode;
import framework.core.routing.UnpackedIdArray;
import framework.core.userDatabase.User;
import framework.core.util.Util;


public class MixPlugIn extends Implementation implements Layer2RecodingSchemeMix {

	private SecureRandom secureRandom;
	
	
	@Override
	public void constructor() {
		
	}

	
	@Override
	public void initialize() {
		try {
			this.secureRandom = SecureRandom.getInstance(settings.getProperty("PRNG_ALGORITHM"));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			System.exit(1);
		}
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
		return anonNode.MAX_PAYLOAD;
		/*removed, because the payload should always be the same (headers are allowed to become bigger)
		 * if (anonNode.ROUTING_MODE == RoutingMode.CASCADE) {
			return anonNode.MAX_PAYLOAD;
		} else if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) {
			return anonNode.MAX_PAYLOAD - getRouteHeaderSize(anonNode);
		} else if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_DYNAMIC_ROUTING) {
			return anonNode.MAX_PAYLOAD;
		} else {
			throw new RuntimeException("not supported routing mode: " +anonNode.ROUTING_MODE); 
		}*/
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
		byte[] padding = new byte[anonNode.MAX_PAYLOAD];
		secureRandom.nextBytes(padding);
		byte[] lengthHeader = Util.shortToByteArray(0);
		byte[] message = Util.concatArrays(lengthHeader, padding);
		Request request = MixMessage.getInstanceRequest(message);
		request.setOwner(user);
		request.setDummyStatus(DummyStatus.DUMMY);
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
		Reply reply;
		if (!anonNode.IS_LAST_MIX) {
			byte[] padding = new byte[anonNode.MAX_PAYLOAD];
			secureRandom.nextBytes(padding);
			byte[] lengthHeader = Util.shortToByteArray(0);
			byte[] message = Util.concatArrays(lengthHeader, padding);
			reply = MixMessage.getInstanceReply(message);
		} else { // will be done later by ReplyThread
			reply = MixMessage.getInstanceReply(new byte[0]);
		}
		reply.setOwner(user);
		reply.isFirstReplyHop = true;
		reply.setDummyStatus(DummyStatus.DUMMY);
		return reply;
	}
	
	
	class RequestThread extends Thread {
		
		@Override
		public void run() {
			while (true) {
				Request[] requests = anonNode.getFromRequestInputQueue();
				for (Request request:requests) {
					if (anonNode.ROUTING_MODE == RoutingMode.CASCADE) {
						// no need to do anything
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
					} else if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_DYNAMIC_ROUTING) {
						request.nextHopAddress = anonNode.mixList.getRandomMixId();
					} else {
						throw new RuntimeException("not supported routing mode: " +anonNode.ROUTING_MODE); 
					}
					
					if (request.isFinalHop(anonNode)) { // remove padding
						int msgLength = Util.byteArrayToShort(Arrays.copyOfRange(request.getByteMessage(), 0, 2));
						if (msgLength == 0) {
							request.setDummyStatus(DummyStatus.DUMMY);
							request.setByteMessage(new byte[0]);
						} else {
							request.setDummyStatus(DummyStatus.NO_DUMMY);
							request.setByteMessage(Arrays.copyOfRange(request.getByteMessage(), 2, msgLength+2));
						}
					}
					outputStrategyLayerMix.addRequest(request);
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
					if (reply.isFirstReplyHop) {
						if (reply.getByteMessage().length > anonNode.MAX_PAYLOAD)
							throw new RuntimeException("can't send more than " +anonNode.MAX_PAYLOAD +" bytes in one message");
						byte[] lengthHeader = Util.shortToByteArray(reply.getByteMessage().length);
						if (reply.getByteMessage().length < anonNode.MAX_PAYLOAD) { // add padding
							int paddingLength = anonNode.MAX_PAYLOAD - reply.getByteMessage().length;
							byte[] padding = new byte[paddingLength];
							secureRandom.nextBytes(padding);
							reply.setByteMessage(Util.concatArrays(reply.getByteMessage(), padding));
						}
						reply.setByteMessage(Util.concatArrays(lengthHeader, reply.getByteMessage()));
						//System.out.println("mix sends reply: " +Util.toHex(reply.getByteMessage())); // TODO: remove
					}
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
