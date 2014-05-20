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
package framework.core.socket.datagram;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import framework.core.AnonNode;
import framework.core.routing.RoutingMode;
import framework.core.socket.socketInterfaces.AdaptiveAnonSocket;
import framework.core.socket.socketInterfaces.AnonMessage;
import framework.core.socket.socketInterfaces.DatagramAnonSocket;
import framework.core.util.Util;


public class DatagramAnonSocketClientImpl extends AdaptiveAnonSocket implements DatagramAnonSocket {

	private HashMap<Integer, Integer> pseudonymToDestAddress = null;
	
	
	public DatagramAnonSocketClientImpl(
			AnonNode owner,
			CommunicationDirection communicationMode,
			boolean isReliable, 
			boolean isOrderPreserving, 
			boolean isFreeRoute
			) {
		super(	owner, 
				communicationMode, 
				false, 
				isReliable,
				isOrderPreserving, 
				isFreeRoute
				);
		if (isDuplex)
			this.pseudonymToDestAddress = new HashMap<Integer, Integer>(100);
		if (owner.ROUTING_MODE == RoutingMode.CASCADE)
			layer4.connect();
	}

	
	@Override
	public void sendMessage(int destPort, byte[] payload) {
		if (isFreeRoute)
			throw new RuntimeException("no destination address specified; use \"sendMessage(destinationPseudonym, destPort, payload\" instead"); 
		payload = Util.concatArrays(Util.shortToByteArray(destPort), payload); // add destination port (= which layer 5 service/ServerSocket shall be addressed)
		//Request request = MixMessage.getInstanceRequest(payload);
		layer4.write(payload);
	}


	@Override
	public void sendMessage(int destinationPseudonym, int destinationPort, byte[] payload) {
		if (!isFreeRoute)
			throw new RuntimeException("this is a fixed route socket; you cannot specify a destination address; use \"sendMessage(destPort, payload\" instead"); 
		if (isDuplex) { // add a pseudonym so the receiver's reply can be identified (the receiver will include this pseudonym in his reply)
			int endToEndPseudonym = new Random().nextInt(); // TODO
			pseudonymToDestAddress.put(endToEndPseudonym, destinationPseudonym);
			payload = Util.concatArrays(Util.intToByteArray(endToEndPseudonym), payload);
		}
		payload = Util.concatArrays(Util.shortToByteArray(destinationPort), payload); // add destination port (= which layer 5 service/ServerSocket shall be addressed)
		//Request request = MixMessage.getInstanceRequest(payload);
		//request.destinationPseudonym = destinationPseudonym;
		layer4.write(payload, destinationPseudonym);
	}


	@Override
	public AnonMessage receiveMessage() {
		if (!isDuplex)
			throw new RuntimeException("this is a simplex socket"); 
		
		byte[] reply = layer4.receive();
		assert reply != null && reply.length != 0;
		AnonMessage result = new AnonMessage(reply);
		
		// TODO: add support for multiple services (requires l4-plugin-support...)
		//int sourcePort = Util.byteArrayToShort(Arrays.copyOf(reply, 2));
		//result.setByteMessage(Arrays.copyOfRange(reply, 2, reply.length));
		//result.setSourcePort(sourcePort); 
				
		if (isFreeRoute) {
			int endToEndPseudonym = Util.byteArrayToInt(Arrays.copyOf(result.getByteMessage(), 4));
			result.setByteMessage(Arrays.copyOfRange(result.getByteMessage(), 4, result.getByteMessage().length));
			Integer sourcePseudonym = pseudonymToDestAddress.remove(endToEndPseudonym);
			if (sourcePseudonym == null) {
				System.err.println("received reply with unknown id");
				return receiveMessage();
			}
			result.setSourcePseudonym(sourcePseudonym);
		}
		return result;
	}

	
	@Override
	public int getMaxSizeForNextMessageSend() {
		if (isFreeRoute)
			return layer4.getMaxSizeOfNextWrite() -6; // -2 for port; -4 for pseudonym; see sendMessage()
		else
			return layer4.getMaxSizeOfNextWrite() -2; // -2 for port; see sendMessage()
	}

	
	@Override
	public int getMaxSizeForNextMessageReceive() {
		if (!isDuplex)
			throw new RuntimeException("this socket is simplex only"); 
		// TODO: add support for multiple services (requires l4-plugin-support...)
		//int maxSize = layer4.getMaxSizeOfNextReply() -2; // -2 for port
		//if (isFreeRoute)
		//	maxSize -= 4; // pseudonym
		//return maxSize;
		if (isFreeRoute)
			return layer4.getMaxSizeOfNextReply() - 4;
		else
			return layer4.getMaxSizeOfNextReply();
	}


	@Override
	public AdaptiveAnonSocket getImplementation() {
		return this;
	}


	@Override
	public int availableReplies() {
		return layer4.availableReplies();
	}
	
}
