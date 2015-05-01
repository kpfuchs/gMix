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
package staticContent.framework.socket.datagram;

import java.util.Arrays;

import staticContent.framework.AnonNode;
import staticContent.framework.message.Request;
import staticContent.framework.socket.socketInterfaces.AdaptiveAnonServerSocket;
import staticContent.framework.socket.socketInterfaces.AnonMessage;
import staticContent.framework.socket.socketInterfaces.DatagramAnonServerSocket;
import staticContent.framework.socket.socketInterfaces.IO_EventObserver;
import staticContent.framework.util.Util;


public class DatagramAnonServerSocketImpl extends AdaptiveAnonServerSocket implements DatagramAnonServerSocket {

	
	public DatagramAnonServerSocketImpl(
			AnonNode owner,
			int bindPseudonym,
			int bindPort,
			CommunicationDirection communicationMode,
			IO_Mode ioMode,
			IO_EventObserver requestObserver,
			boolean isReliable, 
			boolean isOrderPreserving, 
			boolean isFreeRoute) {
		super(	owner, 
				bindPseudonym, 
				bindPort, 
				communicationMode,
				ioMode,
				requestObserver,
				false,
				isReliable, 
				isOrderPreserving, 
				isFreeRoute
				);
		if (communicationMode == CommunicationDirection.DUPLEX && !owner.IS_DUPLEX)
			throw new RuntimeException("the current plug-in config does not suport duplex sockets");
		if (communicationMode == CommunicationDirection.SIMPLEX_SENDER)
			throw new RuntimeException("this is a simplex socket (server backend); the server backend can only be \"CommunicationMode.SIMPLEX_RECEIVER\"");
	}

	
	public DatagramAnonServerSocketImpl(
			AnonNode owner,
			int bindPseudonym,
			int bindPort,
			CommunicationDirection communicationMode,
			IO_Mode ioMode,
			boolean isReliable, 
			boolean isOrderPreserving, 
			boolean isFreeRoute) {
		this(	owner, 
				bindPseudonym, 
				bindPort, 
				communicationMode,
				ioMode,
				null,
				isReliable, 
				isOrderPreserving, 
				isFreeRoute
				);
	}
	
	
	@Override
	public AnonMessage receiveMessage() {
		Request request = super.getNextRequest();
		AnonMessage result = new AnonMessage(request.getByteMessage());
		result.setUser(request.getOwner());
		if (isDuplex) {
			result.setMaxReplySize(getMaxSizeForNextMessageSend());
			if (isFreeRoute) { // extract pseudonym
				int endToEndPseudonym = Util.byteArrayToInt(Arrays.copyOf(result.getByteMessage(), 4));
				result.setByteMessage(Arrays.copyOfRange(result.getByteMessage(), 4, result.getByteMessage().length));
				result.setSourcePseudonym(endToEndPseudonym);
			}
		}
		return result;
	}

	
	@Override
	public void sendMessage(AnonMessage message) {
		if (!isDuplex)
			throw new RuntimeException("this socket is simplex only"); 
		if (isFreeRoute && message.getSourcePseudonym() == AnonMessage.NOT_SET)
			throw new RuntimeException("no address data specified in the bypassed message; use the AnonMessage object you have received with \"receiveMessage()\" for this method! (use \"anonMessage.setByteMessage()\" to add the new reply payload"); 
		if (message.getUser() == null)
			throw new RuntimeException("no user object reference in the bypassed message; use the AnonMessage object you have received with \"receiveMessage()\" for this method! (use \"anonMessage.setByteMessage()\" to add the new reply payload");
		if (message.getByteMessage().length > getMaxSizeForNextMessageSend())
			throw new RuntimeException("the bypassed message is too large; use \"message.getMaxReplySize()\" to get the maximum size"); 
		byte[] payload;
		if (isFreeRoute) {
			payload = Util.concatArrays(new byte[][] {
				//Util.shortToByteArray(bindPort), // TODO: add support for multiple services (requires l4-plugin-support...)
				Util.intToByteArray(message.getSourcePseudonym()),
				message.getByteMessage()
			});
		} else {
			//Util.shortToByteArray(bindPort), // TODO: add support for multiple services (requires l4-plugin-support...)
			//payload = Util.concatArrays(new byte[][] {
			//		Util.shortToByteArray(bindPort),
			//		message.getByteMessage()
			//	});
			payload = message.getByteMessage();
		}
		//Reply reply = MixMessage.getInstanceReply(payload, message.getUser());
		//reply.isFirstReplyHop = true;
		layer4.write(message.getUser(), payload);
	}
	
	
	@Override
	public int getMaxSizeForNextMessageSend() {
		if (!isDuplex)
			throw new RuntimeException("this socket is simplex only"); 
		if (isFreeRoute) {
			// TODO: add support for multiple services (requires l4-plugin-support...)
			//return layer4.getMaxSizeOfNextWrite() - 6; // -2 for port; -4 for pseudonym; see sendMessage()
			return layer4.getMaxSizeOfNextWrite() - 4; // -4 for pseudonym; see sendMessage()
		} else {
			return layer4.getMaxSizeOfNextWrite();
			// TODO: add support for multiple services (requires l4-plugin-support...)
			//return layer4.getMaxSizeOfNextWrite() - 2; // -2 for port; see sendMessage()
		}
	}

	
	@Override
	public int getMaxSizeForNextMessageReceive() {
		int maxSize = layer4.getMaxSizeOfNextRead() -2; // -2 for port
		if (isDuplex)
			maxSize -= 4; // pseudonym
		return maxSize;
	}


	@Override
	public AdaptiveAnonServerSocket getImplementation() {
		return this;
	}

}
