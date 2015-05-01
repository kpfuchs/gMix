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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001;

import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer5ApplicationMix;
import staticContent.framework.routing.RoutingMode;
import staticContent.framework.socket.socketInterfaces.StreamAnonServerSocket;
import staticContent.framework.socket.socketInterfaces.StreamAnonSocketMix;
import staticContent.framework.socket.socketInterfaces.AnonSocketOptions.CommunicationDirection;
import staticContent.framework.socket.socketInterfaces.NoneBlockingAnonSocketOptions.IO_Mode;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.exitClient.ExitClient;


public class MixPlugIn extends Implementation implements Layer5ApplicationMix {

	/**
	 * we will wait for anonymous connections (from socks clients) on this
	 * socket
	 */
	private StreamAnonServerSocket socket = null;

	
	@Override
	public void constructor() {
		if (!super.anonNode.IS_DUPLEX)
			throw new RuntimeException("Socks requires a DUPLEX anonymous channel");
		if (!super.anonNode.IS_CONNECTION_BASED)
			throw new RuntimeException("Socks requires a CONNECTION_BASED anonymous channel");
		if (!super.anonNode.IS_ORDER_PRESERVING)
			throw new RuntimeException("Socks requires an ORDER_PRESERVING anonymous channel");
		if (!super.anonNode.IS_RELIABLE)
			throw new RuntimeException("Socks requires a RELIABLE anonymous channel");
		//this.config = new Config(super.anonNode.getSettings());
	}

	
	@Override
	public void initialize() {

	}

	
	@Override
	public void begin() {
		this.socket = super.anonNode.createStreamAnonServerSocket(
				1080,
				CommunicationDirection.DUPLEX, IO_Mode.BLOCKING,
				super.anonNode.ROUTING_MODE != RoutingMode.GLOBAL_ROUTING);
		new AcceptorThread().start();
	}

	
	private class AcceptorThread extends Thread {

		/**
		 * wait for anonymous (multiplexed) connections from clients and hand
		 * them over to the demultiplexer.
		 */
		@Override
		public void run() {
			while (true) {
				StreamAnonSocketMix client = socket.accept();
				//System.out.println("mix: anon connection accepted");
				new ExitClient(client, settings);
			}
		}

	}
}
