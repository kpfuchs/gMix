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

package simulator.trafficSource;


import java.security.SecureRandom;

import org.apache.commons.math.random.RandomDataImpl;




import simulator.communicationBehaviour.ClientCommunicationBehaviour;
import simulator.core.ClientEvent;
import simulator.core.CommunicationBehaviourEvent;
import simulator.core.Event;
import simulator.core.EventExecutor;
import simulator.core.Settings;
import simulator.core.Simulator;
import simulator.core.TrafficSourceEvent;
import simulator.delayBox.DelayBox;
import simulator.message.NoneMixMessage;
import simulator.networkComponent.Client;
import simulator.networkComponent.DistantProxy;
import simulator.networkComponent.IdGenerator;
import simulator.networkComponent.Identifiable;
import simulator.networkComponent.NetworkConnection;
import simulator.outputStrategy.OutputStrategy;
import simulator.statistics.Statistics;
import simulator.statistics.StatisticsType;


public class TrafficSource implements EventExecutor, Identifiable {

	private int currentRound = 0;
	private int simulationRounds;
	private double simulationInterval;
	private double generateTrafficDuration;
	private Simulator simulator;
	private Client[] clients;
	private DistantProxy distantProxy;
	private int numericIdentifier;
	private RandomDataImpl randomDataImpl;
	private String SIMULATION_SCRIPT;
	private TrafficModel trafficModel;
	private int REQUEST_SIZE;
	private int REPLY_SIZE;
	private int RESOLVE_TIME;
	private Statistics statistics;
	private static SecureRandom secureRandom = new SecureRandom();
	
	
	public TrafficSource(Simulator simulator) {
		
		this.simulator = simulator;
		this.statistics = new Statistics(this);
		this.numericIdentifier = IdGenerator.getId();
		this.randomDataImpl = new RandomDataImpl();
		this.randomDataImpl.reSeed(secureRandom.nextLong());
		this.SIMULATION_SCRIPT = Settings.getProperty("SIMULATION_SCRIPT");
		this.REQUEST_SIZE = new Integer(Settings.getProperty("REQUEST_SIZE"));
		this.REPLY_SIZE = new Integer(Settings.getProperty("REPLY_SIZE"));
		this.RESOLVE_TIME = new Integer(Settings.getProperty("RESOLVE_TIME"));
		if (Settings.getProperty("GENERATE_TRAFFIC_DURATION").equals("AUTO"))
			this.generateTrafficDuration = new Double(Settings.getProperty("RECORD_STATISTICS_TO"));
		else
			this.generateTrafficDuration = new Double(Settings.getProperty("GENERATE_TRAFFIC_DURATION"));
		this.simulationInterval = new Double(Settings.getProperty("SIMULATION_INTERVAL"));
		this.simulationRounds = (int)Math.ceil(generateTrafficDuration / simulationInterval);

		if (SIMULATION_SCRIPT.compareTo("CLIENT-MIX-MIX-MIX-DISTANT_PROXY") == 0)
			scriptClientMixMixMixDistantProxy();
		else if (SIMULATION_SCRIPT.compareTo("CLIENT-MIX-DISTANT_PROXY") == 0)
			scriptClientMixDistantProxy();
		else if (SIMULATION_SCRIPT.compareTo("CLIENT-DISTANT_PROXY") == 0)
			scriptClientDistantProxy();
		else
			throw new RuntimeException("ERROR! Unknown SIMULATION_SCRIPT!");
		
		if (Settings.getProperty("TYPE_OF_TRAFFIC_GENERATOR").compareTo("POISSONDISTRIBUTED") == 0)
			trafficModel = TrafficModel.POISSONDISTRIBUTED;
		else
			throw new RuntimeException("ERROR! Unknown TYPE_OF_TRAFFIC_GENERATOR!");
		
	}
	
	
	private void scriptClientMixMixMixDistantProxy() {
		
		this.distantProxy = simulator.getDistantProxy();
		int numberOfClients = new Integer(Settings.getProperty("NUMBER_OF_CLIENTS_TO_SIMULATE"));
		clients = new Client[numberOfClients];
		
		for (int i=0; i<numberOfClients; i++) {
			
			clients[i] = new Client("Client" +i, simulator, DelayBox.getInstance(10, 10, 25));
			simulator.getClients().put("Client:Client" +i, clients[i]);
			simulator.getNetworkConnections().put("NetworkConnection:NetworkConnection:Client"+i +"<->Mix1", new NetworkConnection(clients[i], simulator.getMixes().get("Mix:Mix1"), simulator));
			
			if (Settings.getProperty("CLIENT_COMMUNICATION_BEHAVIOUR").compareTo("SEND_SYNCHRONOUSLY") == 0 && Settings.getProperty("SIMULATE_REQUEST_CHANNEL").compareTo("TRUE") == 0)
				simulator.scheduleEvent(new Event(clients[i].getClientCommunicationBehaviour(), Simulator.getNow() +1, CommunicationBehaviourEvent.INVITATION_TO_SEND_NEXT_MIX_MESSAGE), this);
		
		}
		
	}
	
	
	private void scriptClientMixDistantProxy() {
		
		scriptClientMixMixMixDistantProxy();
		
	}
	
	
	private void scriptClientDistantProxy() {
		
		this.distantProxy = simulator.getDistantProxy();
		int numberOfClients = new Integer(Settings.getProperty("NUMBER_OF_CLIENTS_TO_SIMULATE"));
		clients = new Client[numberOfClients];
		
		for (int i=0; i<numberOfClients; i++) {
			
			clients[i] = new Client("Client" +i, simulator, DelayBox.getInstance(10, 10, 25));
			simulator.getClients().put("Client:Client" +i, clients[i]);
			simulator.getNetworkConnections().put("NetworkConnection:NetworkConnection:Client"+i +"<->DistnantProxy", new NetworkConnection(clients[i], simulator.getDistantProxy(), simulator));
			
			if (Settings.getProperty("CLIENT_COMMUNICATION_BEHAVIOUR").compareTo("SEND_SYNCHRONOUSLY") == 0 && Settings.getProperty("SIMULATE_REQUEST_CHANNEL").compareTo("TRUE") == 0)
				simulator.scheduleEvent(new Event(clients[i].getClientCommunicationBehaviour(), Simulator.getNow() +1, CommunicationBehaviourEvent.INVITATION_TO_SEND_NEXT_MIX_MESSAGE), this);
		
		}
		
	}
	

	public void simulateTraffic() {
		
		if (++currentRound > simulationRounds) {
			ClientCommunicationBehaviour.stopSending();
			OutputStrategy.stopReplying();
			return; // simulation will stop automatically if no new events are put into event-queue
		}
		
		//System.out.println("### beginning new simulation interval");
		
		if (trafficModel == TrafficModel.POISSONDISTRIBUTED)
			simulatePoissonTraffic();
		else 
			throw new RuntimeException("ERROR! Unknown TYPE_OF_TRAFFIC_GENERATOR!");
		
		simulator.scheduleEvent(new Event(this, (int)Math.round(Simulator.getNow() + new Double(Settings.getProperty("SIMULATION_INTERVAL"))), TrafficSourceEvent.GENERATE_REQUESTS_FOR_NEXT_INTERVAL, null), this);
		
	}
	
	
	private void simulatePoissonTraffic() {
		
		for (Client client: simulator.getClients().values()) { // simulate traffic for each client

			// each SIMULATION_INTERVAL is split into "pulses"
			// the number of messages to send in each pulse is drawn from a 
			// Poisson distribution (with the mean 
			// "AVERAGE_REQUESTS_PER_SECOND")
			// for each message, a uniformly distributed random value is 
			// generated to determine its time of sending in within the pulse
			double pulseLength = new Double(Settings.getProperty("PULSE_LENGTH"));
			double timePassedInCurrentSimulationInterval = 0;
			double numberOfPulses = new Double(Settings.getProperty("SIMULATION_INTERVAL")) / pulseLength;
			double meanRequestsPerPulse = new Double(Settings.getProperty("AVERAGE_REQUESTS_PER_SECOND")) / 1000d * pulseLength;
			
			for (int i=0; i<numberOfPulses; i++) { // for each pulse
				
				double numberOfRequestsInThisPulse = randomDataImpl.nextPoisson(meanRequestsPerPulse);
				double startOfPulse = (double)Simulator.getNow() + timePassedInCurrentSimulationInterval;
				double endOfPulse = (double)Simulator.getNow() + timePassedInCurrentSimulationInterval + pulseLength;
					
				for (int j=0; j<numberOfRequestsInThisPulse; j++) {
					
					double whenToSend = randomDataImpl.nextUniform(startOfPulse, endOfPulse);
					simulator.scheduleEvent(new Event(client, (int)whenToSend, ClientEvent.REQUEST_FROM_USER, new NoneMixMessage(true, client, distantProxy, (""+i+"-"+j), (int)whenToSend, client, REQUEST_SIZE, RESOLVE_TIME, REPLY_SIZE, null)), this);	
					statistics.addValue(1, StatisticsType.TRAFFICSOURCE_SENDING_RATE_PER_CLIENT);

				}
				
				timePassedInCurrentSimulationInterval += pulseLength;
				
				if ((Simulator.getNow() + timePassedInCurrentSimulationInterval) > generateTrafficDuration)
					return;
			}
			
		}	
		
	}

	
	@Override
	public void executeEvent(Event event) {
		
		if (!(event.getEventType() instanceof TrafficSourceEvent)) {
			throw new RuntimeException("ERROR: TrafficSource received wrong Event");
		} else {				
				
			switch ((TrafficSourceEvent)event.getEventType()) {
			
				case GENERATE_REQUESTS_FOR_NEXT_INTERVAL:
					simulateTraffic();
					break;
				
				default:
					throw new RuntimeException("ERROR: TrafficSource received wrong Event");
					
			}
			
		}
		
	}


	@Override
	public int getGlobalId() {
		return numericIdentifier;
	}
	
}
