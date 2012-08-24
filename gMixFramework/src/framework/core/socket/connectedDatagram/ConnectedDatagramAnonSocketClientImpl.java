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
package framework.core.socket.connectedDatagram;

import framework.core.AnonNode;
import framework.core.message.MixMessage;
import framework.core.message.Request;
import framework.core.socket.socketInterfaces.AdaptiveAnonSocket;
import framework.core.socket.socketInterfaces.ConnectedDatagramAnonSocket;
import framework.core.util.Util;


public class ConnectedDatagramAnonSocketClientImpl extends AdaptiveAnonSocket implements ConnectedDatagramAnonSocket {

	private boolean isConnected = false;

	
	public ConnectedDatagramAnonSocketClientImpl(
			AnonNode owner,
			CommunicationMode communicationMode,
			boolean isReliable, 
			boolean isOrderPreserving, 
			boolean isFreeRoute
			) {
		super(	owner, 
				communicationMode, 
				true, 
				isReliable,
				isOrderPreserving, 
				isFreeRoute
				);
	}
	
	
	@Override
	public void connect(int destinationPort) {
		layer3.connect();
		this.destinationPort = destinationPort;
		this.isConnected = true;
	}


	@Override
	public void connect(int destinationPseudonym, int destinationPort) {
		if (!isFreeRoute)
			throw new RuntimeException("this is a fixed route socket; you cannot specify a destination address; use \"connect(destinationPort)\" instead"); 
		layer3.connect(destinationPseudonym);
		this.destinationPseudonym = destinationPseudonym;
		this.destinationPort = destinationPort;
		this.isConnected = true;
	}


	@Override
	public void disconnect() {
		layer3.disconnect();
		this.isConnected = false;
	}


	@Override
	public boolean isConnected() {
		return this.isConnected;
	}


	@Override
	public void sendMessage(byte[] payload) {
		if (!isConnected)
			throw new RuntimeException("not connected"); 
		
		if (!owner.LAYER_1_LINKS_MESSAGES) // add a pseudonym for the (final) receiver, so it can link the messages of this sender/socket 
			payload = Util.concatArrays(Util.intToByteArray(endToEndPseudonym), payload);
		
		payload = Util.concatArrays(Util.shortToByteArray(destinationPort), payload); // add destination port (= which layer 5 service/ServerSocket shall be addressed)
		Request request = MixMessage.getInstanceRequest(payload);
		layer3.sendMessage(request);
	}


	@Override
	public byte[] receiveMessage() {
		if (!isDuplex)
			throw new RuntimeException("this is a simplex socket"); 
		if (!isConnected)
			throw new RuntimeException("not connected");
		return layer3.receiveReply().getByteMessage();
	}


	@Override
	public int getMaxSizeForNextMessageSend() {
		int maxSize = layer3.getMaxSizeOfNextRequest() - 2; // -2 for port; see sendMessage()
		if (!owner.LAYER_1_LINKS_MESSAGES) // -4 for pseudonym; see sendMessage()
			maxSize -= 4;
		return maxSize;
	}


	@Override
	public int getMaxSizeForNextMessageReceive() {
		if (!isDuplex)
			throw new RuntimeException("this socket is simplex only"); 
		return layer3.getMaxSizeOfNextReply();
	}


	@Override
	public AdaptiveAnonSocket getImplementation() {
		return this;
	}

}
