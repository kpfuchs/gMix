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
import userGeneratedContent.simulatorPlugIns.plugins.trafficSource.JansenClient.ClientType;


/**
 * 
 * logic: paper "Never Been KIST: Tor’s Congestion Management Blossoms with 
 * Kernel-Informed Socket Transport", 2014, Rob Jansen, John Geddes, Chris 
 * Wacek, Micah Sherr, Paul Syverson
 * 
 * web-clients: 78.26%:
 * 		1: wait [1,60000] ms (uniform)
 *		2: download 320 KiB
 *		3: goto 1
 *	dl-clients: 8.69%
 *		1: download 5 MiB
 *		2: goto
 *	tor-perf1: 4.35%
 *		1: wait 60000 ms
 *		2. download 50 KiB
 *		3. goto 1
 *	tor-perf2: 4.35%
 *		1: wait 60000 ms
 *		2. download 1 MiB
 *		3. goto 1
 *	tor-perf3: 4.35%
 *		1: wait 60000 ms
 *		2. download 5 MiB
 *		3. goto 1
 * Total: one hour simulation time
 *
 */
@Plugin(pluginKey = "JANSEN", pluginName="Jansen Model")
public class JansenModel extends TrafficSourceImplementation {

	private JansenClient[] clients;
	@IntSimulationProperty( name = "Number of Clients", 
			key = "JANSEN_MODEL_NUMBER_OF_CLIENTS_TO_SIMULATE",
			min = 1)
	private int numberOfClients;
	
	@Override
	public AbstractClient[] createClientsArray() {
		if (!Simulator.settings.getProperty("COMMUNICATION_MODE").equals("DUPLEX"))
			throw new RuntimeException("ERROR: JansenModel requires COMMUNICATION_MODE to be DUPLEX (see experiment config file)!"); 
		this.numberOfClients = Simulator.settings.getPropertyAsInt("JANSEN_MODEL_NUMBER_OF_CLIENTS_TO_SIMULATE");
		clients = new JansenClient[numberOfClients];
		int webClients = (int) Math.round(0.7826d*(double)numberOfClients);
		int dlClients = (int) Math.round(0.0869d*(double)numberOfClients);
		int perf1Clients = (int) Math.round(0.0435d*(double)numberOfClients);
		int perf2Clients = (int) Math.round(0.0435d*(double)numberOfClients);
		int perf3Clients = (int) Math.round(0.0435d*(double)numberOfClients);
		int sum = webClients + dlClients + perf1Clients + perf2Clients + perf3Clients; 
		if (sum < numberOfClients)
			webClients += numberOfClients - sum;
		else if (sum > numberOfClients)
			webClients--;
		int ctr = 0;
		for (int i=0; i<webClients; i++, ctr++)
			clients[ctr] = new JansenClient(ClientType.WEB, "Client" +ctr, Simulator.getSimulator(), ctr);
		for (int i=0; i<dlClients; i++, ctr++)
			clients[ctr] = new JansenClient(ClientType.DOWNLOAD, "Client" +ctr, Simulator.getSimulator(), ctr);
		for (int i=0; i<perf1Clients; i++, ctr++)
			clients[ctr] = new JansenClient(ClientType.PERF1, "Client" +ctr, Simulator.getSimulator(), ctr);
		for (int i=0; i<perf2Clients; i++, ctr++)
			clients[ctr] = new JansenClient(ClientType.PERF2, "Client" +ctr, Simulator.getSimulator(), ctr);
		for (int i=0; i<perf3Clients; i++, ctr++)
			clients[ctr] = new JansenClient(ClientType.PERF3, "Client" +ctr, Simulator.getSimulator(), ctr);
		return this.clients;
	}

	
	@Override
	public void startSending() {
		for (int i=0; i<clients.length; i++) 
			clients[i].startSending();
	}
	
	
	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		Integer[] test = new Integer[] {1,2,5,10,20,50,100,200,500};
		for (int i=0; i<test.length; i++) {
			int numberOfClients = test[i];
			int webClients = (int) Math.round(0.7826d*(double)numberOfClients);
			int dlClients = (int) Math.round(0.0869d*(double)numberOfClients);
			int perf1Clients = (int) Math.round(0.0435d*(double)numberOfClients);
			int perf2Clients = (int) Math.round(0.0435d*(double)numberOfClients);
			int perf3Clients = (int) Math.round(0.0435d*(double)numberOfClients);
			int sum = webClients + dlClients + perf1Clients + perf2Clients + perf3Clients; 
			if (sum < numberOfClients)
				webClients += numberOfClients - sum;
			else if (sum > numberOfClients)
				webClients--;
			System.out.println();
			System.out.println(numberOfClients + " clients:");
			System.out.println("web: " +webClients); 
			System.out.println("dlClients: " +dlClients); 
			System.out.println("perf1Clients: " +perf1Clients); 
			System.out.println("perf2Clients: " +perf2Clients); 
			System.out.println("perf3Clients: " +perf3Clients); 
			System.out.println(); 
		} 
		
	} 
	
}
