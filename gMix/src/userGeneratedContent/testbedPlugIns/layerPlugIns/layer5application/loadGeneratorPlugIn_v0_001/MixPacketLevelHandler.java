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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.loadGeneratorPlugIn_v0_001;

import java.util.Arrays;
import java.util.Random;

import staticContent.framework.AnonNode;
import staticContent.framework.message.Request;
import staticContent.framework.routing.RoutingMode;
import staticContent.framework.socket.datagram.DatagramAnonServerSocketImpl;
import staticContent.framework.socket.socketInterfaces.AnonMessage;
import staticContent.framework.socket.socketInterfaces.IO_EventObserver;
import staticContent.framework.socket.socketInterfaces.AnonSocketOptions.CommunicationDirection;
import staticContent.framework.socket.socketInterfaces.NoneBlockingAnonSocketOptions.IO_Mode;
import staticContent.framework.util.Util;


public class MixPacketLevelHandler implements IO_EventObserver {

	private DatagramAnonServerSocketImpl socket;
	private AnonNode owner;
	private final boolean IS_DUPLEX;
	private Random random = new Random();
	
	
	public MixPacketLevelHandler(AnonNode anonNode) {
		System.out.println("MixPacketLevelHandler started"); 
		this.owner = anonNode;
		this.IS_DUPLEX = anonNode.IS_DUPLEX;
		CommunicationDirection cd = IS_DUPLEX ? CommunicationDirection.DUPLEX : CommunicationDirection.SIMPLEX_RECEIVER;
		IO_Mode ioMode = IO_Mode.OBSERVER_PATTERN;
		this.socket = (DatagramAnonServerSocketImpl) anonNode.createDatagramServerSocket(
				owner.getSettings().getPropertyAsInt("SERVICE_PORT1"), 
				cd, 
				ioMode,
				this,
				true, 
				true, 
				owner.ROUTING_MODE != RoutingMode.GLOBAL_ROUTING
			);
	}

	@Override
	public void incomingRequest(Request request) {
		//System.out.println("DISTANT_PROXY: received request"); 
		AnonMessage message = new AnonMessage(request.getByteMessage());
		message.setUser(request.getOwner());
		if (this.IS_DUPLEX) { // extract pseudonym
			message.setMaxReplySize(socket.getMaxSizeForNextMessageSend());
			int endToEndPseudonym = Util.byteArrayToInt(Arrays.copyOf(message.getByteMessage(), 4));
			message.setByteMessage(Arrays.copyOfRange(message.getByteMessage(), 4, message.getByteMessage().length));
			message.setSourcePseudonym(endToEndPseudonym);
			System.err.println("test"); 
			// send reply:
			if (message.getByteMessage().length == message.getMaxReplySize()) {
				socket.sendMessage(message);
			} else {
				byte[] replyPayload = new byte[message.getMaxReplySize()];
				random.nextBytes(replyPayload);
				message.setByteMessage(replyPayload);
				socket.sendMessage(message);
			}
		}

	}

	
}
