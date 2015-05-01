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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer3outputStrategy.stopAndGo_v0_001;

import java.util.Vector;

import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer1NetworkClient;
import staticContent.framework.interfaces.Layer2RecodingSchemeClient;
import staticContent.framework.interfaces.Layer3OutputStrategyClient;
import staticContent.framework.interfaces.Layer4TransportClient;
import staticContent.framework.message.MixMessage;
import staticContent.framework.message.Reply;
import staticContent.framework.message.Request;
import staticContent.framework.message.ExternalMessage.DummyStatus;
import staticContent.framework.routing.MixList;
import staticContent.framework.routing.RoutingMode;
import staticContent.framework.util.Util;


//Kesdogan et. al. 1998: Stop-and-Go MIXes: Providing Probabilistic Anonymity in an Open System
public class ClientPlugIn extends Implementation implements Layer3OutputStrategyClient {

	private Layer1NetworkClient layer1;
	private Layer2RecodingSchemeClient layer2;
	
	private Vector<byte[]> replyCache;
	private int availableReplyPayload = 0;
	private MixList route;
	private int dstPseudonym = Util.NOT_SET;
	private Helper helper;
	
	
	@Override
	public void constructor() {
		this.helper = new Helper(settings, clock);
	}

	
	@Override
	public void initialize() {
		if (anonNode.IS_DUPLEX)
			this.replyCache = new Vector<byte[]>();
	}

	
	@Override
	public void begin() {
		
	}

	
	@Override
	public void setReferences(
			Layer1NetworkClient layer1,
			Layer2RecodingSchemeClient layer2, 
			Layer3OutputStrategyClient layer3,
			Layer4TransportClient layer4) {
		this.layer2 = layer2;
		this.layer1 = layer1;
		assert this == layer3;
	}
	
	
	@Override
	public void connect() {
		if (anonNode.ROUTING_MODE == RoutingMode.SOURCE_ROUTING)
			this.route = sourceRoutingPlugInClient.choseRoute();
		this.layer1.connect();
	}

	
	@Override
	public void connect(int destPseudonym) {
		if (anonNode.ROUTING_MODE == RoutingMode.SOURCE_ROUTING) {
			this.route = sourceRoutingPlugInClient.choseRoute(destPseudonym);
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
	public void write(byte[] data) {
		if (data == null || data.length == 0)
			throw new RuntimeException("write(null) and write(byte[0]) are not allowed");
		Request request = MixMessage.getInstanceRequest(data);
		int numberOfMixes;
		if (anonNode.ROUTING_MODE == RoutingMode.GLOBAL_ROUTING) {
			numberOfMixes = anonNode.mixList.numberOfMixes;
		} else { // free route
			if (!anonNode.IS_CONNECTION_BASED) { // new route for every message
				if (this.dstPseudonym == Util.NOT_SET)
					this.route = sourceRoutingPlugInClient.choseRoute();
				else
					this.route = sourceRoutingPlugInClient.choseRoute(this.dstPseudonym);
			} 
			request.destinationPseudonym = this.route.mixIDs[route.mixIDs.length-1];
			request.nextHopAddress = this.route.mixIDs[0];
			request.route = this.route.mixIDs;
			numberOfMixes = request.route.length;
		}
		request.headers = helper.createHeaders(numberOfMixes);
		request = layer2.applyLayeredEncryption(request);
		layer1.sendMessage(request);
	}


	@Override
	public void write(byte[] data, int destPseudonym) {
		this.dstPseudonym = destPseudonym;
		write(data);
	}


	@Override
	public byte[] receive() {
		if (replyCache.size() > 0) {
			byte[] result = replyCache.remove(0);
			availableReplyPayload -= result.length;
			return result;
		} else {
			Reply reply = layer1.receiveReply();
			reply = layer2.extractPayload(reply);
			assert reply.getDummyStatus() != DummyStatus.UNKNOWN;
			while (reply.getDummyStatus() == DummyStatus.DUMMY) {
				reply = layer1.receiveReply();
				reply = layer2.extractPayload(reply);
			}
			assert reply.getByteMessage() != null && reply.getByteMessage().length != 0;
			return reply.getByteMessage();
		}
	}


	@Override
	public int getMaxSizeOfNextWrite() {
		return layer2.getMaxPayloadForNextMessage();
	}


	@Override
	public int getMaxSizeOfNextReceive() {
		return layer2.getMaxPayloadForNextReply();
	}


	@Override
	public int availableReplies() {
		for (int i=0; i<layer1.availableReplies(); i++) {
			Reply reply = layer1.receiveReply();
			reply = layer2.extractPayload(reply);
			assert reply.getDummyStatus() != DummyStatus.UNKNOWN;
			if (reply.getDummyStatus() == DummyStatus.NO_DUMMY) {
				replyCache.add(reply.getByteMessage());
				availableReplyPayload += reply.getByteMessage().length;
				assert reply.getByteMessage() != null && reply.getByteMessage().length != 0;
			}
		} 
		return replyCache.size();
	}


	@Override
	public int availableReplyData() {
		for (int i=0; i<layer1.availableReplies(); i++) {
			Reply reply = layer1.receiveReply();
			reply = layer2.extractPayload(reply);
			assert reply.getDummyStatus() != DummyStatus.UNKNOWN;
			if (reply.getDummyStatus() == DummyStatus.NO_DUMMY) {
				replyCache.add(reply.getByteMessage());
				availableReplyPayload += reply.getByteMessage().length;
				assert reply.getByteMessage() != null && reply.getByteMessage().length != 0;
			}
		} 
		return availableReplyPayload;
	}


}
