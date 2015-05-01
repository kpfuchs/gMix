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
package userGeneratedContent.simulatorPlugIns.plugins.trafficSource;

import staticContent.evaluation.simulator.Simulator;
import staticContent.evaluation.simulator.annotations.plugin.Plugin;
import staticContent.evaluation.simulator.annotations.property.IntSimulationProperty;
import staticContent.evaluation.simulator.core.networkComponent.AbstractClient;

@Plugin(pluginKey = "REQUEST_REPLY", pluginName="Request Reply")
public class RequestReplyModel extends TrafficSourceImplementation {
	
	private RequestReplyClient[] clients;
	@IntSimulationProperty( name = "Number of Clients", 
			key = "REQUEST_REPLY_NUMBER_OF_CLIENTS_TO_SIMULATE",
			min = 1)
	private int numberOfClients;
	
	@Override
	public AbstractClient[] createClientsArray() {
		if (!Simulator.settings.getProperty("COMMUNICATION_MODE").equals("DUPLEX"))
			throw new RuntimeException("ERROR: RequestReplyModel requires COMMUNICATION_MODE to be DUPLEX (see experiment config file)!"); 
		this.numberOfClients = Simulator.settings.getPropertyAsInt("REQUEST_REPLY_NUMBER_OF_CLIENTS_TO_SIMULATE");
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