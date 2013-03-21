/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2013  Karl-Peter Fuchs
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
package evaluation.simulator.plugins.trafficSource;

import evaluation.simulator.Simulator;
import evaluation.simulator.core.networkComponent.AbstractClient;


public class RequestReplyModel extends TrafficSourceImplementation {
	
	private RequestReplyClient[] clients;
	
	
	@Override
	public AbstractClient[] createClientsArray() {
		if (!Simulator.settings.getProperty("COMMUNICATION_MODE").equals("DUPLEX"))
			throw new RuntimeException("ERROR: RequestReplyModel requires COMMUNICATION_MODE to be DUPLEX (see experiment config file)!"); 
		int numberOfClients = Simulator.settings.getPropertyAsInt("NUMBER_OF_CLIENTS_TO_SIMULATE");
		clients = new RequestReplyClient[numberOfClients];
		for (int i=0; i<clients.length; i++) 
			clients[i] = new RequestReplyClient("Client" +i, Simulator.getSimulator(), i);
		return this.clients;
	}

	
	@Override
	public void startSending() {
		for (int i=0; i<clients.length; i++) 
			clients[i].startSending();
	}

}