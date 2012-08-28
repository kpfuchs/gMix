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
package plugIns.layer3outputStrategy.sendImmediately_v0_001;

import java.util.Vector;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer1NetworkClient;
import framework.core.interfaces.Layer2RecodingSchemeClient;
import framework.core.interfaces.Layer3OutputStrategyClient;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.routing.MixList;
import framework.core.routing.RoutingMode;
import framework.core.util.Util;


public class ClientPlugIn extends Implementation implements Layer3OutputStrategyClient {

	private Layer1NetworkClient layer1;
	private Layer2RecodingSchemeClient layer2;
	private Vector<Reply> replyCache;
	private int availableReplyPayload = 0;
	private MixList route;
	
	
	@Override
	public void constructor() {

	}

	
	@Override
	public void initialize() {
		if (anonNode.IS_DUPLEX)
			this.replyCache = new Vector<Reply>();
	}

	
	@Override
	public void begin() {
		
	}

	
	@Override
	public void setReferences(
			Layer1NetworkClient layer1,
			Layer2RecodingSchemeClient layer2, 
			Layer3OutputStrategyClient layer3) {
		this.layer2 = layer2;
		this.layer1 = layer1;
		assert this == layer3;
	}
	
	
	@Override
	public void connect() {
		if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) {
			this.route = anonNode.mixList.getRandomRoute(anonNode.FREE_ROUTE_LENGTH);
			if (anonNode.DISPLAY_ROUTE_INFO)
				System.out.println(""+anonNode +" generated random route: " +this.route); 
			this.layer1.connect(this.route);
		} else {
			this.layer1.connect();
		}
	}

	
	@Override
	public void connect(int destPseudonym) {
		if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) {
			this.route = anonNode.mixList.getRandomRoute(anonNode.FREE_ROUTE_LENGTH, destPseudonym);
			if (anonNode.DISPLAY_ROUTE_INFO)
				System.out.println(""+anonNode +" generated random route: " +this.route); 
			this.layer1.connect(this.route);
		} else {
			this.layer1.connect();
		}
	}
	
	
	@Override
	public void disconnect() {
		layer1.disconnect();
	}

	
	@Override
	public void sendMessage(Request request) {
		if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) {
			if (!anonNode.IS_CONNECTION_BASED) { // new route for every message
				if (request.destinationPseudonym == Util.NOT_SET)
					this.route = anonNode.mixList.getRandomRoute(anonNode.FREE_ROUTE_LENGTH);
				else
					this.route = anonNode.mixList.getRandomRoute(anonNode.FREE_ROUTE_LENGTH, request.destinationPseudonym);
			} 
			request.destinationPseudonym = this.route.mixIDs[route.mixIDs.length-1];
			request.nextHopAddress = this.route.mixIDs[0];
			request.route = this.route.mixIDs;
		}
		request = layer2.applyLayeredEncryption(request);
		layer1.sendMessage(request);
	}
	
	
	@Override
	public Reply receiveReply() {
		if (replyCache.size() > 0) {
			Reply result = replyCache.remove(0);
			availableReplyPayload -= result.getByteMessage().length;
			return result;
		} else {
			Reply reply = layer1.receiveReply();
			return layer2.extractPayload(reply);
		}
	}


	@Override
	public int getMaxSizeOfNextRequest() {
		return layer2.getMaxPayloadForNextMessage();
	}


	@Override
	public int getMaxSizeOfNextReply() {
		return layer2.getMaxPayloadForNextReply();
	}


	@Override
	public int availableReplies() {
		int available = layer1.availableReplies();
		for (int i=0; i<available; i++) {
			Reply reply = layer1.receiveReply();
			replyCache.add(layer2.extractPayload(reply));
			availableReplyPayload += reply.getByteMessage().length;
		} 
		return replyCache.size();
	}


	@Override
	public int availableReplyPayload() {
		int available = layer1.availableReplies();
		for (int i=0; i<available; i++) {
			Reply reply = layer1.receiveReply();
			replyCache.add(layer2.extractPayload(reply));
			availableReplyPayload += reply.getByteMessage().length;
		} 
		return availableReplyPayload;
	}
	
}
