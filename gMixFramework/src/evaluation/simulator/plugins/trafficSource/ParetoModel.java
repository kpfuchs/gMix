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
import evaluation.simulator.core.event.Event;
import evaluation.simulator.core.event.EventExecutor;
import evaluation.simulator.core.event.SimulationEvent;
import evaluation.simulator.core.networkComponent.AbstractClient;


public class ParetoModel extends TrafficSourceImplementation implements EventExecutor {

	private ParetoClient[] clients;
	private Simulator simulator;
	
	
	@Override
	public AbstractClient[] createClientsArray() {
		this.simulator = Simulator.getSimulator();
		int numberOfClients = Simulator.settings.getPropertyAsInt("NUMBER_OF_CLIENTS_TO_SIMULATE");
		clients = new ParetoClient[numberOfClients];
		for (int i=0; i<clients.length; i++) 
			clients[i] = new ParetoClient("Client" +i, Simulator.getSimulator(), i);
		return this.clients;
	}

	
	@Override
	public void startSending() {
		scheduleNextSends();
	}
	
	
	private void scheduleNextSends() {
		for (ParetoClient client: clients)
			client.scheduleNextSends();
		simulator.scheduleEvent(new Event(this, (Simulator.getNow() + 1000), ParetoClientEvent.SCHEDULE_NEXT_SENDS), this);	
	}
	
	
	private enum ParetoClientEvent implements SimulationEvent {
		SCHEDULE_NEXT_SENDS;
	}
	
	
	@Override
	public void executeEvent(Event event) {
		if (event.getEventType() instanceof ParetoClientEvent) {
			if (event.getEventType() == ParetoClientEvent.SCHEDULE_NEXT_SENDS) {
				scheduleNextSends();
			} else
				throw new RuntimeException("ERROR: received unknown Event: " +event.toString()); 
		} else {
			throw new RuntimeException("ERROR: received unknown Event: " +event.toString()); 
		}
	}
	
}