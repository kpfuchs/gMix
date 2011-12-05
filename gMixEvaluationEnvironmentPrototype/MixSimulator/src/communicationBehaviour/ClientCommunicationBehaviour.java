/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2011  Karl-Peter Fuchs
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

package communicationBehaviour;

import simulator.Event;
import simulator.EventExecutor;
import simulator.Settings;
import simulator.Simulator;
import statistics.Statistics;
import message.NetworkMessage;
import message.NoneMixMessage;
import networkComponent.Client;


public abstract class ClientCommunicationBehaviour implements EventExecutor {

	protected Client owner;
	protected Simulator simulator;
	protected Statistics statistics;
	protected static boolean stopSending = false;
	
	
	public static ClientCommunicationBehaviour getInstance(Client owner, Simulator simulator) {
		
		String simulationScript = Settings.getProperty("SIMULATION_SCRIPT");
		String outputStrategy = Settings.getProperty("OUTPUT_STRATEGY");
		String communicationBehaviour = Settings.getProperty("CLIENT_COMMUNICATION_BEHAVIOUR");
		
		if (simulationScript.equals("CLIENT-DISTANT_PROXY"))
			return new ClientSendWithoutMixes(owner, simulator);
		if (outputStrategy.equals("BASIC_SYNCHRONOUS_BATCH"))
			return new ClientBasicSynchronous(owner, simulator);
		if (outputStrategy.equals("DISTINCT_USER_BATCH"))
			return new ClientWaitForReply(owner, simulator);
		else if (communicationBehaviour.equals("SEND_IMMEDIATELY"))
			return new ClientSendImmediately(owner, simulator);
		else if (communicationBehaviour.equals("WAIT_FOR_FURTHER_DATA"))
			return new ClientWaitForFurtherDataBeforeSend(owner, simulator);
		else
			throw new RuntimeException("ERROR: no suiting ClientCommunicationBehaviour: " +communicationBehaviour +"!");

	}
	
	
	protected ClientCommunicationBehaviour(Client owner, Simulator simulator) {
		
		this.simulator = simulator;
		this.owner = owner;
		this.statistics = owner.getStatistics();
		
	}
	
	
	// must call owner.sendRequest(NetworkMessage);
	// if request shall be sent to first mix, NetworkMessage must be of type "MixMessage"
	// if request shall be sent to ditantProxy directly, NetworkMessage must be of type "NoneMixMessage"
	public abstract void incomingRequestFromUser(NoneMixMessage request);
	
	
	public abstract void incomingDecryptedReply(NetworkMessage reply);

	
	
	public void setOwner(Client owner) {
		this.owner = owner;
	}


	public Client getOwner() {
		return owner;
	}
	
	
	// called when no more traffic will be generated
	public static void stopSending() {
		stopSending = true;
	}
	
	
	public static void reset() {
		stopSending = false;
	}
	
	
	// overwrite in subclass if needed
	public void executeEvent(Event e) {
		throw new RuntimeException("Not Implemented");
		
	}

	
}
