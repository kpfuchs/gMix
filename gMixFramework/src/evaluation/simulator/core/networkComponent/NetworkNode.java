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
package evaluation.simulator.core.networkComponent;


import java.util.HashMap;


import evaluation.simulator.Simulator;
import evaluation.simulator.core.event.Event;
import evaluation.simulator.core.event.EventExecutor;
import evaluation.simulator.core.event.SimulationEvent;
import evaluation.simulator.core.message.NetworkMessage;
import evaluation.simulator.core.statistics.Statistics;
import evaluation.simulator.plugins.delayBox.DelayBoxImpl;


public abstract class NetworkNode implements EventExecutor, Identifiable {
	
	private int numericIdentifier;
	private String identifier;
	private NetworkConnection connectionToNextHop;
	private NetworkConnection connectionToPreviousHop;
	private Simulator simulator;
	private DelayBoxImpl delayBox;
	private int numberOfConnectionsToNextHops = 0;
	private int numberOfConnectionsToPreviousHops = 0;
	private HashMap<String, NetworkConnection> connectionsToNextHops;
	private HashMap<String, NetworkConnection> connectionsToPreviousHops;
	protected Statistics statistics;

	
	public NetworkNode(String identifier, Simulator simulator) {
		
		this.identifier = identifier;
		this.numericIdentifier = IdGenerator.getId();
		this.simulator = simulator;
		this.statistics = new Statistics(this);
		
	}
	
	
	public void sendToNextHop(NetworkMessage networkMessage, int delay, SimulationEvent simulationEvent) {
		
		if (numberOfConnectionsToNextHops == 1)
			sendToHop(connectionToNextHop, networkMessage, delay, simulationEvent);
		else
			sendToHop(connectionsToNextHops.get(networkMessage.getDestination().getIdentifier()), networkMessage, delay, simulationEvent);

	}


	public void sendToPreviousHop(NetworkMessage networkMessage, int delay, SimulationEvent simulationEvent) {
		 
		if (numberOfConnectionsToPreviousHops == 1)
			sendToHop(connectionToPreviousHop, networkMessage, delay, simulationEvent);
		else
			sendToHop(connectionsToPreviousHops.get(networkMessage.getDestination().getIdentifier()), networkMessage, delay, simulationEvent);

	}


	public void sendToHop(NetworkConnection connectionToHop, NetworkMessage networkMessage, int delay, SimulationEvent simulationEvent) {
		
		Event event = new Event(connectionToHop, Simulator.getNow() + delay, simulationEvent, networkMessage);
		simulator.scheduleEvent(event, this);
		String destination = networkMessage.isRequest() ? connectionToHop.getDestination().toString() : connectionToHop.getSource().toString();
		if (Simulator.DEBUG_ON)
			System.out.println(identifier + ": sending " +networkMessage +" to " +destination);
		
	}

	
	/**
	 * @return the connectionToNextHop
	 */
	public NetworkConnection getConnectionToNextHop() {
		return connectionToNextHop;
	}


	/**
	 * @param connectionToNextHop the connectionToNextHop to set
	 */
	public void setConnectionToNextHop(NetworkConnection connectionToNextHop) {
		
		if (numberOfConnectionsToNextHops == 0) {
			this.connectionToNextHop = connectionToNextHop;
			numberOfConnectionsToNextHops++;
		} else if (numberOfConnectionsToNextHops == 1) {
			connectionsToNextHops = new HashMap<String, NetworkConnection>(100);
			connectionsToNextHops.put(this.connectionToNextHop.getDestination().toString(), this.connectionToNextHop);
			connectionsToNextHops.put(connectionToNextHop.getDestination().toString(), connectionToNextHop);
			this.connectionToNextHop = null;
			numberOfConnectionsToNextHops++;
		} else {
			connectionsToNextHops.put(connectionToNextHop.getDestination().toString(), connectionToNextHop);
			numberOfConnectionsToNextHops++;
		}
		
	}


	/**
	 * @return the connectionToPreviousHop
	 */
	public NetworkConnection getConnectionToPreviousHop() {
		return connectionToPreviousHop;
	}


	/**
	 * @param connectionToPreviousHop the connectionToPreviousHop to set
	 */
	public void setConnectionToPreviousHop(NetworkConnection connectionToPreviousHop) {
		if (numberOfConnectionsToPreviousHops == 0) {
			this.connectionToPreviousHop = connectionToPreviousHop;
			numberOfConnectionsToPreviousHops++;
		} else if (numberOfConnectionsToPreviousHops == 1) {
			connectionsToPreviousHops = new HashMap<String, NetworkConnection>(100); 
			connectionsToPreviousHops.put(this.connectionToPreviousHop.getSource().toString(), this.connectionToPreviousHop);
			connectionsToPreviousHops.put(connectionToPreviousHop.getSource().toString(), connectionToPreviousHop);
			this.connectionToPreviousHop = null;
			numberOfConnectionsToPreviousHops++;
		} else {
			connectionsToPreviousHops.put(connectionToPreviousHop.getSource().toString(), connectionToPreviousHop);
			numberOfConnectionsToPreviousHops++;
		}
	}
	
	
	/**
	 * @return the identifier
	 */
	public String getIdentifier() {
		
		return identifier;
		
	}
	
	
	public String toString() {
		return identifier;
	}

	
	/**
	 * @return the delayBox
	 */
	public DelayBoxImpl getDelayBox() {
		return delayBox;
	}


	/**
	 * @param delayBox the delayBox to set
	 */
	public void setDelayBox(DelayBoxImpl delayBox) {
		this.delayBox = delayBox;
	}


	@Override
	public int getGlobalId() {
		return numericIdentifier;
	}
	

	/**
	 * @return the statistics
	 */
	public Statistics getStatistics() {
		return statistics;
	}


	/**
	 * @param statistics the statistics to set
	 */
	public void setStatistics(Statistics statistics) {
		this.statistics = statistics;
	}



}
