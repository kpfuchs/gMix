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
package plugIns.layer5application.loadGeneratorPlugIn_v0_001;

import java.io.IOException;

import evaluation.loadGenerator.ExitNodeClientData;
import evaluation.loadGenerator.ExitNodeRequestReceiver;
import evaluation.loadGenerator.LoadGenerator;
import framework.core.AnonNode;
import framework.core.controller.Layer5ApplicationMixController;
import framework.core.message.Request;
import framework.core.routing.RoutingMode;
import framework.core.socket.socketInterfaces.IO_EventObserver_Stream;
import framework.core.socket.socketInterfaces.NoneBlockingAnonSocketOptions.IO_Mode;
import framework.core.socket.socketInterfaces.StreamAnonSocketMix;
import framework.core.socket.socketInterfaces.AnonSocketOptions.CommunicationDirection;
import framework.core.util.Util;


public class ApplicationLevelHandler implements IO_EventObserver_Stream {

	private ExitNodeRequestReceiver requestReceiver;
	private Layer5ApplicationMixController owner;
	
	
	public ApplicationLevelHandler(AnonNode owner) {
		System.out.println("ApplicationLevelHandler started"); 
		this.owner = owner.getApplicationLayerControllerMix();
		CommunicationDirection cd = owner.IS_DUPLEX ? CommunicationDirection.DUPLEX : CommunicationDirection.SIMPLEX_RECEIVER;
		IO_Mode ioMode = IO_Mode.OBSERVER_PATTERN;
		owner.createStreamAnonServerSocket(owner.getSettings().getPropertyAsInt("SERVICE_PORT1"), cd, ioMode, this, owner.ROUTING_MODE != RoutingMode.CASCADE);
		this.requestReceiver = LoadGenerator.createExitNodeRequestReceiver(owner);
	}
	
	
	@Override
	public void incomingConnection(StreamAnonSocketMix socket) {
		requestReceiver.createClientDataInstance(socket.getUser(), socket, owner);
	}

	
	@Override
	public void dataAvailable(StreamAnonSocketMix socket) {
		try {
			ExitNodeClientData client = socket.getUser().getAttachment(owner, ExitNodeClientData.class);
			int available = socket.getInputStream().available();
			byte[] dataReceived = Util.forceRead(socket.getInputStream(), available);
			assert dataReceived.length == available;
			requestReceiver.dataReceived(client, dataReceived);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	@Override
	public void incomingRequest(Request request) {
		throw new RuntimeException("this is a stream socket");  
	}
}
