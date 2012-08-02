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
package framework.core.socket.datagram;

import java.util.Arrays;

import framework.core.AnonNode;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.socket.socketInterfaces.AdaptiveAnonServerSocket;
import framework.core.socket.socketInterfaces.AnonMessage;
import framework.core.socket.socketInterfaces.DatagramAnonServerSocket;
import framework.core.util.Util;


public class DatagramAnonServerSocketImpl extends AdaptiveAnonServerSocket implements DatagramAnonServerSocket {

	
	public DatagramAnonServerSocketImpl(
			AnonNode owner,
			int bindPseudonym,
			int bindPort,
			CommunicationMode communicationMode,
			boolean isReliable, 
			boolean isOrderPreserving, 
			boolean isFreeRoute) {
		super(	owner, 
				bindPseudonym, 
				bindPort, 
				communicationMode, 
				false,
				isReliable, 
				isOrderPreserving, 
				isFreeRoute
				);
		if (communicationMode == CommunicationMode.DUPLEX && !owner.IS_DUPLEX)
			throw new RuntimeException("the current plug-in config does not suport duplex sockets");
		if (communicationMode == CommunicationMode.SIMPLEX_SENDER)
			throw new RuntimeException("this is a simplex socket (server backend); the server backend can only be \"CommunicationMode.SIMPLEX_RECEIVER\"");
	}

	
	@Override
	public AnonMessage receiveMessage() {
		Request request = super.getNextRequest();
		AnonMessage result = new AnonMessage(request.getByteMessage());
		result.setUser(request.getOwner());
		if (isDuplex) { // extract pseudonym
			result.setMaxReplySize(getMaxSizeForNextMessageSend());
			int endToEndPseudonym = Util.byteArrayToInt(Arrays.copyOf(result.getByteMessage(), 4));
			result.setByteMessage(Arrays.copyOfRange(result.getByteMessage(), 4, result.getByteMessage().length));
			result.setSourcePseudonym(endToEndPseudonym);
		}
		return result;
	}

	
	@Override
	public void sendMessage(AnonMessage message) {
		if (!isDuplex)
			throw new RuntimeException("this socket is simplex only"); 
		if (message.getSourcePseudonym() == AnonMessage.NOT_SET)
			throw new RuntimeException("no address data specified in the bypassed message; use the AnonMessage object you have received with \"receiveMessage()\" for this method! (use \"anonMessage.setByteMessage()\" to add the new reply payload"); 
		if (message.getUser() == null)
			throw new RuntimeException("no user object reference in the bypassed message; use the AnonMessage object you have received with \"receiveMessage()\" for this method! (use \"anonMessage.setByteMessage()\" to add the new reply payload");
		if (message.getByteMessage().length > getMaxSizeForNextMessageSend())
			throw new RuntimeException("the bypassed message is too large; use \"message.getMaxReplySize()\" to get the maximum size"); 
		byte[] payload;
		if (isFreeRoute)
			payload = Util.concatArrays(new byte[][] {
				Util.shortToByteArray(bindPort),
				Util.intToByteArray(message.getSourcePseudonym()),
				message.getByteMessage()
			});
		else
			payload = Util.concatArrays(new byte[][] {
					Util.shortToByteArray(bindPort),
					message.getByteMessage()
				});
		Reply reply = MixMessage.getInstanceReply(payload, message.getUser());
		owner.putInReplyInputQueue(reply);
	}
	
	
	@Override
	public int getMaxSizeForNextMessageSend() {
		if (!isDuplex)
			throw new RuntimeException("this socket is simplex only"); 
		if (isFreeRoute)
			return layer3.getMaxSizeOfNextReply() - 6; // -2 for port; -4 for pseudonym; see sendMessage()
		else
			return layer3.getMaxSizeOfNextReply() - 2; // -2 for port; see sendMessage()
	}

	
	@Override
	public int getMaxSizeForNextMessageReceive() {
		int maxSize = layer3.getMaxSizeOfNextRequest() -2; // -2 for port
		if (isDuplex)
			maxSize -= 4; // pseudonym
		return maxSize;
	}


	@Override
	public AdaptiveAnonServerSocket getImplementation() {
		return this;
	}

}
