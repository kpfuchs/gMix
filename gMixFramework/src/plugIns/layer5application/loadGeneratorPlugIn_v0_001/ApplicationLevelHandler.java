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

import java.io.IOException;
import java.util.Vector;

import evaluation.loadGenerator.ExitNodeRequestReceiver;
import evaluation.loadGenerator.LoadGenerator;
import evaluation.loadGenerator.ExitNodeRequestReceiver.ClientData;
import framework.core.AnonNode;
import framework.core.routing.RoutingMode;
import framework.core.socket.socketInterfaces.StreamAnonServerSocket;
import framework.core.socket.socketInterfaces.StreamAnonSocketMix;
import framework.core.socket.socketInterfaces.AnonSocketOptions.CommunicationMode;
import framework.core.util.Util;


public class ApplicationLevelHandler {

	private StreamAnonServerSocket serverSocket;
	private Vector<ClientData> clients;
	private Vector<ClientData> newClients;
	private RequestThread requestThread;
	private ExitNodeRequestReceiver requestReceiver;
	
	
	public ApplicationLevelHandler(AnonNode owner) {
		System.out.println("ApplicationLevelHandler started"); 
		this.clients = new Vector<ClientData>(owner.EXPECTED_NUMBER_OF_USERS);
		this.newClients = new Vector<ClientData>(50);
		this.requestThread = new RequestThread();
		CommunicationMode cm = owner.IS_DUPLEX ? CommunicationMode.DUPLEX : CommunicationMode.SIMPLEX_RECEIVER;
		this.serverSocket = owner.createStreamAnonServerSocket(owner.getSettings().getPropertyAsInt("SERVICE_PORT1"), cm, owner.ROUTING_MODE != RoutingMode.CASCADE);
		this.requestReceiver = LoadGenerator.createExitNodeRequestReceiver(owner);
		new AcceptorThread().start(); 
		this.requestThread.start(); 
	}
	
	
	private class AcceptorThread extends Thread {

		@Override
		public void run() {
			while (true) {
				StreamAnonSocketMix newSocket = serverSocket.accept();
				ClientData client = requestReceiver.createClientDataInstance(newSocket.getUser(), newSocket);
				synchronized(newClients) {
					newClients.add(client);
				}
			}	
		}
	}

	
	private class RequestThread extends Thread {

		@Override
		public void run() {
			while (true) {
				// add new clients
				synchronized(newClients) {
					for (ClientData client: newClients)
						clients.add(client);
				}
				// read data from sockets
				int readCtr = 0;
				for (ClientData client: clients) {
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
