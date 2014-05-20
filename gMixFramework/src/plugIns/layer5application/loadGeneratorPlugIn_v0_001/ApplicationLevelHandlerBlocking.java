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
import java.util.Vector;

import evaluation.loadGenerator.ExitNodeClientData;
import evaluation.loadGenerator.ExitNodeRequestReceiver;
import evaluation.loadGenerator.LoadGenerator;
import framework.core.AnonNode;
import framework.core.controller.Layer5ApplicationMixController;
import framework.core.routing.RoutingMode;
import framework.core.socket.socketInterfaces.NoneBlockingAnonSocketOptions.IO_Mode;
import framework.core.socket.socketInterfaces.StreamAnonServerSocket;
import framework.core.socket.socketInterfaces.StreamAnonSocketMix;
import framework.core.socket.socketInterfaces.AnonSocketOptions.CommunicationDirection;
import framework.core.util.Util;


public class ApplicationLevelHandlerBlocking {

	private StreamAnonServerSocket serverSocket;
	private Vector<ExitNodeClientData> clients;
	private Vector<ExitNodeClientData> newClients;
	private RequestThread requestThread;
	private ExitNodeRequestReceiver requestReceiver;
	private Layer5ApplicationMixController owner;
	
	
	public ApplicationLevelHandlerBlocking(AnonNode owner) {
		System.out.println("ApplicationLevelHandler started"); 
		this.owner = owner.getApplicationLayerControllerMix();
		this.clients = new Vector<ExitNodeClientData>(owner.EXPECTED_NUMBER_OF_USERS);
		this.newClients = new Vector<ExitNodeClientData>(50);
		this.requestThread = new RequestThread();
		CommunicationDirection cd = owner.IS_DUPLEX ? CommunicationDirection.DUPLEX : CommunicationDirection.SIMPLEX_RECEIVER;
		IO_Mode ioMode = IO_Mode.BLOCKING;
		this.serverSocket = owner.createStreamAnonServerSocket(owner.getSettings().getPropertyAsInt("SERVICE_PORT1"), cd, ioMode, owner.ROUTING_MODE != RoutingMode.CASCADE);
		this.requestReceiver = LoadGenerator.createExitNodeRequestReceiver(owner);
		new AcceptorThread().start(); 
		this.requestThread.start(); 
	}
	
	
	private class AcceptorThread extends Thread {

		@Override
		public void run() {
			while (true) {
				StreamAnonSocketMix newSocket = serverSocket.accept();
				ExitNodeClientData client = requestReceiver.createClientDataInstance(newSocket.getUser(), newSocket, owner);
				synchronized(newClients) {
					newClients.add(client);
				}
			}	
		}
	}

	
	// TODO: why not use none-blocking mode (observer pattern)?
	private class RequestThread extends Thread {

		@Override
		public void run() {
			while (true) {
				// add new clients
				synchronized(newClients) {
					for (ExitNodeClientData client: newClients)
						clients.add(client);
				}
				// read data from sockets
				int readCtr = 0;
				for (ExitNodeClientData client: clients) {
					try {
						int available = client.socket.getInputStream().available();
						if (available > 0) {
							readCtr++;
							byte[] dataReceived = Util.forceRead(client.socket.getInputStream(), available);
							assert dataReceived.length == available;
							requestReceiver.dataReceived(client, dataReceived);
						}
					} catch (IOException e) {
						e.printStackTrace();
						continue;
					}
				}
				if (readCtr == 0)
					try {Thread.sleep(1);} catch (InterruptedException e) {e.printStackTrace();} // TODO: wait-notify
			}
		}
	}
}
