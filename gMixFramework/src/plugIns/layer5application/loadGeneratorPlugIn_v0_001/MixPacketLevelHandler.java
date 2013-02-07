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
package plugIns.layer5application.loadGeneratorPlugIn_v0_001;

import java.util.Random;

import framework.core.AnonNode;
import framework.core.routing.RoutingMode;
import framework.core.socket.datagram.DatagramAnonServerSocketImpl;
import framework.core.socket.socketInterfaces.AnonMessage;
import framework.core.socket.socketInterfaces.AnonSocketOptions.CommunicationMode;


public class MixPacketLevelHandler {

	private DatagramAnonServerSocketImpl socket;
	private AnonNode owner;
	private final boolean IS_DUPLEX;
	
	
	public MixPacketLevelHandler(AnonNode anonNode) {
		System.out.println("MixPacketLevelHandler started"); 
		this.owner = anonNode;
		this.IS_DUPLEX = anonNode.IS_DUPLEX;
		CommunicationMode cm = IS_DUPLEX ? CommunicationMode.DUPLEX : CommunicationMode.SIMPLEX_RECEIVER;
		this.socket = (DatagramAnonServerSocketImpl) anonNode.createDatagramServerSocket(
				owner.getSettings().getPropertyAsInt("SERVICE_PORT1"), 
				cm, 
				true, 
				true, 
				owner.ROUTING_MODE != RoutingMode.CASCADE
			);
		new WorkerThread().start();
	}

	
	private class WorkerThread extends Thread {
		
		private Random random = new Random();
		
		
		@Override
		public void run() {
			while (true) {
				AnonMessage message = socket.receiveMessage();
				//System.out.println("DISTANT_PROXY: received request"); 
				if (IS_DUPLEX) {
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
	}
	
}
