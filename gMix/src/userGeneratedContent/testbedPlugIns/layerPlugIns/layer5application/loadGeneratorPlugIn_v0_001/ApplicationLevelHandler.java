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

import java.io.IOException;

import staticContent.evaluation.loadGenerator.ExitNodeClientData;
import staticContent.evaluation.loadGenerator.ExitNodeRequestReceiver;
import staticContent.evaluation.loadGenerator.LoadGenerator;
import staticContent.framework.AnonNode;
import staticContent.framework.controller.Layer5ApplicationMixController;
import staticContent.framework.message.Request;
import staticContent.framework.routing.RoutingMode;
import staticContent.framework.socket.socketInterfaces.IO_EventObserver_Stream;
import staticContent.framework.socket.socketInterfaces.StreamAnonSocketMix;
import staticContent.framework.socket.socketInterfaces.AnonSocketOptions.CommunicationDirection;
import staticContent.framework.socket.socketInterfaces.NoneBlockingAnonSocketOptions.IO_Mode;
import staticContent.framework.util.Util;


public class ApplicationLevelHandler implements IO_EventObserver_Stream {

	private ExitNodeRequestReceiver requestReceiver;
	private Layer5ApplicationMixController owner;
	
	
	public ApplicationLevelHandler(AnonNode owner) {
		System.out.println("ApplicationLevelHandler started"); 
		this.owner = owner.getApplicationLayerControllerMix();
		CommunicationDirection cd = owner.IS_DUPLEX ? CommunicationDirection.DUPLEX : CommunicationDirection.SIMPLEX_RECEIVER;
		IO_Mode ioMode = IO_Mode.OBSERVER_PATTERN;
		owner.createStreamAnonServerSocket(owner.getSettings().getPropertyAsInt("SERVICE_PORT1"), cd, ioMode, this, owner.ROUTING_MODE != RoutingMode.GLOBAL_ROUTING);
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
