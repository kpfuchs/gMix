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
package evaluation.simulator.trafficSource;

import java.math.BigDecimal;
import java.security.SecureRandom;

import org.apache.commons.math.random.RandomDataImpl;

import evaluation.simulator.core.ClientEvent;
import evaluation.simulator.core.CommunicationBehaviourEvent;
import evaluation.simulator.core.Event;
import evaluation.simulator.core.EventExecutor;
import evaluation.simulator.core.Simulator;
import evaluation.simulator.core.TrafficSourceEvent;
import evaluation.simulator.delayBox.DelayBox;
import evaluation.simulator.delayBox.DelayBox.TypeOfNode;
import evaluation.simulator.message.EndToEndMessage;
import evaluation.simulator.networkComponent.AbstractClient;
import evaluation.simulator.networkComponent.Client;
import evaluation.simulator.networkComponent.IdGenerator;
import evaluation.simulator.networkComponent.Identifiable;
import evaluation.simulator.networkComponent.NetworkConnection;
import evaluation.simulator.statistics.Statistics;
import evaluation.traceParser.engine.dataStructure.Transaction;


public class TrafficSource implements EventExecutor, Identifiable {

	private double simulationInterval;
	private Simulator simulator;
	private AbstractClient[] clients;
	private int numericIdentifier;
	private RandomDataImpl randomDataImpl;
	private String SIMULATION_SCRIPT;
	private TrafficModel trafficModel;
	private int REQUEST_SIZE;
	private int REPLY_SIZE;
	private int RESOLVE_TIME; // in ms
	public static Statistics statistics;
	private TraceFileModel traceFileModel;
	private static SecureRandom secureRandom = new SecureRandom();
	
	
	public TrafficSource(Simulator simulator) {
		this.simulator = simulator;
		TrafficSource.statistics = new Statistics(this);
		this.numericIdentifier = IdGenerator.getId();
		this.randomDataImpl = new RandomDataImpl();
		this.randomDataImpl.reSeed(secureRandom.nextLong());
		this.SIMULATION_SCRIPT = Simulator.settings.getProperty("SIMULATION_SCRIPT");
		if (Simulator.settings.getProperty("REQUEST_SIZE").equals("AUTO"))
			this.REQUEST_SIZE = Simulator.settings.getPropertyAsInt("MIX_REQUEST_PAYLOAD_SIZE");
		else
			this.REQUEST_SIZE = Simulator.settings.getPropertyAsInt("REQUEST_SIZE");
		if (Simulator.settings.getProperty("REPLY_SIZE").equals("AUTO"))
			this.REPLY_SIZE = Simulator.settings.getPropertyAsInt("MIX_REPLY_PAYLOAD_SIZE");
		else
			this.REPLY_SIZE = Simulator.settings.getPropertyAsInt("REPLY_SIZE");
		this.RESOLVE_TIME = Simulator.settings.getPropertyAsInt("RESOLVE_TIME");
		if (Simulator.settings.getProperty("SIMULATION_END").equals("REAL_TIME_END"))
			new StopSimulationTimer(simulator);
		this.simulationInterval = Simulator.settings.getPropertyAsDouble("SIMULATION_INTERVAL");
		if (Simulator.settings.getProperty("TYPE_OF_TRAFFIC_GENERATOR").compareTo("POISSON") == 0) {
			trafficModel = TrafficModel.POISSON;
			this.clients = new AbstractClient[Simulator.settings.getPropertyAsInt("NUMBER_OF_CLIENTS_TO_SIMULATE")];
		} else if (Simulator.settings.getProperty("TYPE_OF_TRAFFIC_GENERATOR").compareTo("PARETO") == 0) {
			trafficModel = TrafficModel.PARETO;
			this.clients = new AbstractClient[Simulator.settings.getPropertyAsInt("NUMBER_OF_CLIENTS_TO_SIMULATE")];
		} else if (Simulator.settings.getProperty("TYPE_OF_TRAFFIC_GENERATOR").compareTo("TRACE_FILE") == 0) {
			trafficModel = TrafficModel.TRACE_FILE;
			this.traceFileModel = new TraceFileModel(simulator);
			this.clients = traceFileModel.createClientsArray();
		} else
			throw new RuntimeException("ERROR! Unknown TYPE_OF_TRAFFIC_GENERATOR!");
		if (SIMULATION_SCRIPT.compareTo("CLIENT-MIX-MIX-MIX-DISTANT_PROXY") == 0)
			scriptClientMixMixMixDistantProxy();
		else if (SIMULATION_SCRIPT.compareTo("CLIENT-MIX-DISTANT_PROXY") == 0)
			scriptClientMixDistantProxy();
		else if (SIMULATION_SCRIPT.compareTo("CLIENT-DISTANT_PROXY") == 0)
			scriptClientDistantProxy();
		else
			throw new RuntimeException("ERROR! Unknown SIMULATION_SCRIPT!");
	}
	
	
	public int getNumberOfClients() {
		return clients.length;
	}
	
	
	private void scriptClientMixMixMixDistantProxy() {
		if (trafficModel != TrafficModel.TRACE_FILE)
			for (int i=0; i<clients.length; i++)
				clients[i] = new Client("Client" +i, simulator, DelayBox.getInstance(TypeOfNode.CLIENT));	
		for (int i=0; i<clients.length; i++) {
			simulator.getClients().put("Client:Client" +i, clients[i]);
			simulator.getNetworkConnections().put("NetworkConnection:NetworkConnection:Client"+i +"<->Mix1", new NetworkConnection(clients[i], simulator.getMixes().get("Mix:Mix1"), simulator));
			if (Simulator.settings.getProperty("CLIENT_COMMUNICATION_BEHAVIOUR").compareTo("SEND_SYNCHRONOUSLY") == 0 && Simulator.settings.getPropertyAsBoolean("SIMULATE_REQUEST_CHANNEL"))
				simulator.scheduleEvent(new Event(clients[i].getClientCommunicationBehaviour(), Simulator.getNow() +1, CommunicationBehaviourEvent.INVITATION_TO_SEND_NEXT_MIX_MESSAGE), this);
		}
		if (trafficModel!= TrafficModel.TRACE_FILE && trafficModel!= TrafficModel.PARETO && trafficModel!= TrafficModel.POISSON)
			throw new RuntimeException("ERROR! Unknown TYPE_OF_TRAFFIC_GENERATOR!");	
	}
	
	
	private void scriptClientMixDistantProxy() {
		scriptClientMixMixMixDistantProxy();
	}
	
	
	private void scriptClientDistantProxy() {
		if (trafficModel != TrafficModel.TRACE_FILE)
			for (int i=0; i<clients.length; i++)
				clients[i] = new Client("Client" +i, simulator, DelayBox.getInstance(TypeOfNode.CLIENT));	
		for (int i=0; i<clients.length; i++) {
			simulator.getClients().put("Client:Client" +i, clients[i]);
			simulator.getNetworkConnections().put("NetworkConnection:NetworkConnection:Client"+i +"<->DistnantProxy", new NetworkConnection(clients[i], simulator.getDistantProxy(), simulator));
			if (Simulator.settings.getProperty("CLIENT_COMMUNICATION_BEHAVIOUR").compareTo("SEND_SYNCHRONOUSLY") == 0 && Simulator.settings.getPropertyAsBoolean("SIMULATE_REQUEST_CHANNEL"))
				simulator.scheduleEvent(new Event(clients[i].getClientCommunicationBehaviour(), Simulator.getNow() +1, CommunicationBehaviourEvent.INVITATION_TO_SEND_NEXT_MIX_MESSAGE), this);
		}
		if (trafficModel!= TrafficModel.TRACE_FILE && trafficModel!= TrafficModel.PARETO && trafficModel!= TrafficModel.POISSON)
			throw new RuntimeException("ERROR! Unknown TYPE_OF_TRAFFIC_GENERATOR!");	
	}
	

	public void simulateTraffic() {
		if (trafficModel == TrafficModel.POISSON || trafficModel == TrafficModel.PARETO) {
			long nextExecutionTime = Math.round(Simulator.getNow() + Simulator.settings.getPropertyAsDouble("SIMULATION_INTERVAL"));
			if (nextExecutionTime <= 0) { // ctr limit reached
				System.err.println("warning: stopping simulation as simulation time variable has reached its maximum value"); 
				simulator.stopSimulation("simulation time variable overflow");
				return;
			}
			//System.out.println("### beginning new simulation interval");
			if (trafficModel == TrafficModel.POISSON)
				simulatePoissonTraffic();
			else if (trafficModel == TrafficModel.PARETO)
				simulateParetoTraffic();
			simulator.scheduleEvent(new Event(this, nextExecutionTime, TrafficSourceEvent.GENERATE_REQUESTS_FOR_NEXT_INTERVAL, null), this);
		} else if (trafficModel == TrafficModel.TRACE_FILE) {
			this.traceFileModel.startSending();
		} else {
			throw new RuntimeException("ERROR! Unknown TYPE_OF_TRAFFIC_GENERATOR!");
		}
	}
	
	
	private void simulateParetoTraffic() {
		long end = Math.round((double)Simulator.getNow()+simulationInterval);
		for (AbstractClient client: simulator.getClients().values()) { // simulate traffic for each client
			double ALPHA = Simulator.settings.getPropertyAsDouble("PARETO_ALPHA");
			double avgRequestsPerSecond = Simulator.settings.getPropertyAsDouble("PARETO_AVG_REQUESTS_PER_SECOND");
			//double avgSendInterval = (1000d/avgRequestsPerPulse);
			//NormalizedPareto pareto = new NormalizedPareto(ALPHA, avgSendInterval);
			Pareto pareto = new Pareto(ALPHA, 1d);
			while (true) {
				client.latest += Math.round(pareto.drawSample(avgRequestsPerSecond) * 1000d);
				//System.out.println(client.latest); 
				//System.out.println(interval); 
				if (client.latest <= end) {
					Transaction at = new Transaction(0, /*client.getClientId(),*/ 0, REQUEST_SIZE, REPLY_SIZE, RESOLVE_TIME);
					EndToEndMessage eteMessage = new EndToEndMessage(0, at, true);
					simulator.scheduleEvent(new Event(client, client.latest, ClientEvent.REQUEST_FROM_USER, eteMessage), this);	
				} else {
					break;
				}
			} 
		}	
		
		/*
		 * 		double generateTrafficDuration = 900000000;
		double numberOfClients = 10;
		double alpha = 1.3d;
		double avgRequestsPerPulse = 1d;
		double[] latestScheduledSends = new double[(int) numberOfClients];
		double[] numberOfSends = new double[(int) numberOfClients];
		for (int i=0; i<numberOfClients; i++) { // simulate traffic for each client
			double avgSendInterval = (1d/avgRequestsPerPulse)*1000d;
			double min = (avgSendInterval * (alpha - 1d)) / alpha;
			//double min = avgSendInterval / (avgSendInterval - alpha);
			System.out.println(avgSendInterval); 
			//double min = avgSendInterval/(avgSendInterval-alpha);
			//System.out.println(min); 
			NormalizedPareto pareto = new NormalizedPareto(alpha, avgSendInterval);
			while (true) {
				double interval = pareto.drawSample();
				if ((latestScheduledSends[i] + interval) > generateTrafficDuration)
					break;
				latestScheduledSends[i] += interval;
				numberOfSends[i]++;
				//System.out.println(interval); 
			} 
		}
		// calc avg:
		for (int i=0; i<numberOfSends.length; i++) {
			System.out.println("avg sends: " +(numberOfSends[i]/(generateTrafficDuration/1000d))); 
		} 
		 */
	}
	
	/*private void simulateParetoTraffic() {
		double numberOfClients = simulator.getClients().size();
		for (Client client: simulator.getClients().values()) { // simulate traffic for each client
			double ALPHA = Simulator.settings.getPropertyAsDouble("PARETO_ALPHA");
			double timePassedInCurrentSimulationInterval = 0;
			double pulseLength = 1000d;
			double numberOfPulses = Simulator.settings.getPropertyAsDouble("SIMULATION_INTERVAL") / pulseLength;
			double meanRequestsPerPulse = Simulator.settings.getPropertyAsDouble("PARETO_AVG_REQUESTS_PER_SECOND");
			
			meanRequestsPerPulse = meanRequestsPerPulse / numberOfClients; // TODO: test
			double sendInterval = pulseLength/meanRequestsPerPulse;
			//double sendRate = 1d/sendInterval;
			
			double min = (sendInterval * (ALPHA - 1d)) / ALPHA;
			Pareto pareto = new Pareto(ALPHA, min);
			
			for (int i=0; i<numberOfPulses; i++) { // for each pulse
				double numberOfRequestsInThisPulse = Math.round(pareto.drawSample() / 1000d); // TODO: check
				
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
	}*/
	
	
	private void simulatePoissonTraffic() {
		
		for (AbstractClient client: simulator.getClients().values()) { // simulate traffic for each client

			// each SIMULATION_INTERVAL is split into "pulses"
			// the number of messages to send in each pulse is drawn from a 
			// Poisson distribution (with the mean 
			// "AVERAGE_REQUESTS_PER_SECOND")
			// for each message, a uniformly distributed random value is 
			// generated to determine its time of sending in within the pulse
			double timePassedInCurrentSimulationInterval = 0;
			double pulseLength;
			double numberOfPulses;
			double meanRequestsPerPulse;
			
			if (Simulator.settings.getProperty("POISSON_MODE") == null || 
				Simulator.settings.getProperty("POISSON_MODE").equals("") ||
				Simulator.settings.getProperty("POISSON_MODE").equals("AVERAGE_REQUESTS_PER_SECOND")
				) { // use parameter AVERAGE_REQUESTS_PER_SECOND to determine the sending rate
				pulseLength = Simulator.settings.getPropertyAsDouble("PULSE_LENGTH");
				numberOfPulses = Simulator.settings.getPropertyAsDouble("SIMULATION_INTERVAL") / pulseLength;
				meanRequestsPerPulse = Simulator.settings.getPropertyAsDouble("AVERAGE_REQUESTS_PER_SECOND") / 1000d * pulseLength;
			} else if (Simulator.settings.getProperty("POISSON_MODE").equals("SENDING_RATE")) { // use parameter POISSON_SENDING_RATE  to determine the sending rate
				pulseLength = 1000d;
				numberOfPulses = Simulator.settings.getPropertyAsDouble("SIMULATION_INTERVAL") / pulseLength;
				meanRequestsPerPulse = (1.0d/Simulator.settings.getPropertyAsDouble("POISSON_SENDING_RATE"));
			} else {
				throw new RuntimeException("not supported POISSON_MODE: " +Simulator.settings.getProperty("POISSON_MODE")); 
			}

			for (int i=0; i<numberOfPulses; i++) { // for each pulse
				double numberOfRequestsInThisPulse = randomDataImpl.nextPoisson(meanRequestsPerPulse);
				double startOfPulse = (double)Simulator.getNow() + timePassedInCurrentSimulationInterval;
				double endOfPulse = (double)Simulator.getNow() + timePassedInCurrentSimulationInterval + pulseLength;
				
				for (int j=0; j<numberOfRequestsInThisPulse; j++) {
					double whenToSend = randomDataImpl.nextUniform(startOfPulse, endOfPulse);
					Transaction at = new Transaction(0, /*client.getClientId(),*/ 0, REQUEST_SIZE, REPLY_SIZE, RESOLVE_TIME);
					EndToEndMessage eteMessage = new EndToEndMessage(0, at, true);
					simulator.scheduleEvent(new Event(client, (long)whenToSend, ClientEvent.REQUEST_FROM_USER, eteMessage), this);	
				}
				timePassedInCurrentSimulationInterval += pulseLength;
			}
		}	
	}
	
	//double last = 0;
	
	/*private void simulatePoissonTraffic() {
		
		for (Client client: simulator.getClients().values()) { // simulate traffic for each client

			// each SIMULATION_INTERVAL is split into "pulses"
			// the number of messages to send in each pulse is drawn from a 
			// Poisson distribution (with the mean 
			// "AVERAGE_REQUESTS_PER_SECOND")
			// for each message, a uniformly distributed random value is 
			// generated to determine its time of sending in within the pulse
			double timePassedInCurrentSimulationInterval = 0;
			double pulseLength;
			double numberOfPulses;
			double meanRequestsPerPulse;
			
			if (Simulator.settings.getProperty("POISSON_MODE") == null || 
				Simulator.settings.getProperty("POISSON_MODE").equals("") ||
				Simulator.settings.getProperty("POISSON_MODE").equals("AVERAGE_REQUESTS_PER_SECOND")
				) { // use parameter AVERAGE_REQUESTS_PER_SECOND to determine the sending rate
				pulseLength = Simulator.settings.getPropertyAsDouble("PULSE_LENGTH");
				numberOfPulses = Simulator.settings.getPropertyAsDouble("SIMULATION_INTERVAL") / pulseLength;
				meanRequestsPerPulse = Simulator.settings.getPropertyAsDouble("AVERAGE_REQUESTS_PER_SECOND") / 1000d * pulseLength;
			} else if (Simulator.settings.getProperty("POISSON_MODE").equals("SENDING_RATE")) { // use parameter POISSON_SENDING_RATE  to determine the sending rate
				pulseLength = 1000d;
				numberOfPulses = Simulator.settings.getPropertyAsDouble("SIMULATION_INTERVAL") / pulseLength;
				meanRequestsPerPulse = (1.0d/Simulator.settings.getPropertyAsDouble("POISSON_SENDING_RATE"));
			} else {
				throw new RuntimeException("not supported POISSON_MODE: " +Simulator.settings.getProperty("POISSON_MODE")); 
			}
			System.out.println("meanRequestsPerPulse: " +meanRequestsPerPulse); 
			double totalPulses = 0;
			double totalMessages = 0;
			double zeros = 0;
			double ones = 0;
			double twos = 0;
			double threes = 0;
			double largers = 0;
			
			for (int i=0; i<numberOfPulses; i++) { // for each pulse
				double numberOfRequestsInThisPulse = randomDataImpl.nextPoisson(meanRequestsPerPulse);
				totalPulses++;
				totalMessages += numberOfRequestsInThisPulse;
				if (numberOfRequestsInThisPulse == 0)
					zeros++;
				else if (numberOfRequestsInThisPulse == 1)
					ones++;
				else if (numberOfRequestsInThisPulse == 2)
					twos++;
				else if (numberOfRequestsInThisPulse == 3)
					threes++;
				else if (numberOfRequestsInThisPulse > 3)
					largers++;
				
				double startOfPulse = (double)Simulator.getNow() + timePassedInCurrentSimulationInterval;
				double endOfPulse = (double)Simulator.getNow() + timePassedInCurrentSimulationInterval + pulseLength;
					
				for (int j=0; j<numberOfRequestsInThisPulse; j++) {
					
					double whenToSend = randomDataImpl.nextUniform(startOfPulse, endOfPulse);
					
					//if (last != 0 && (whenToSend - last) < 1000d) {
					//	System.out.println("abstand klein"); 
					//}
					//last = whenToSend;
						
					simulator.scheduleEvent(new Event(client, (int)whenToSend, ClientEvent.REQUEST_FROM_USER, new NoneMixMessage(true, client, distantProxy, (""+i+"-"+j), (int)whenToSend, client, REQUEST_SIZE, RESOLVE_TIME, REPLY_SIZE, null)), this);	
					statistics.addValue(1, StatisticsType.TRAFFICSOURCE_SENDING_RATE_PER_CLIENT);

				}
				
				timePassedInCurrentSimulationInterval += pulseLength;
				
				if ((Simulator.getNow() + timePassedInCurrentSimulationInterval) > generateTrafficDuration)
					break;
			}
			PoissonDistributionImpl p = new PoissonDistributionImpl(meanRequestsPerPulse);
			System.out.println("total pulses (seconds): " +totalPulses);
			System.out.println("total messages: " +totalMessages);
			System.out.println("wie wahrscheinlich war es, dass x nachrichten in einem intervall auftraten:"); 
			System.out.println("zeros: " +zeros +" (" +(zeros/totalPulses)*100d +" %), expected: " +p.probability(0)*100d +" %"); 
			System.out.println("ones: " +ones +" (" +(ones/totalPulses)*100d +"%), expected: " +p.probability(1)*100d +" %"); 
			System.out.println("twos: " +twos +" (" +(twos/totalPulses)*100d +"%), expected: " +p.probability(2)*100d +" %"); 
			System.out.println("threes: " +threes +" (" +(threes/totalPulses)*100d +"%), expected: " +p.probability(3)*100d +" %"); 
			try {
				System.out.println("largers: " +largers +" (" +(largers/totalPulses)*100d +"%), expected: " +(1d-(double)p.cumulativeProbability(3))*100d +" %");
			} catch (MathException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			try {
				
				double wahrscheinlichkeitDassInEinemPulsMehrAlsEineNachrichtGesendetWerdenSoll = 1d - (p.probability(0) + p.probability(1));
				double dropWahrscheinlichkeit = wahrscheinlichkeitDassInEinemPulsMehrAlsEineNachrichtGesendetWerdenSoll;
				System.out.println("XCXCXC dropWahrscheinlichkeit: " +dropWahrscheinlichkeit); 
				
				double expectedPercentageOfPulsesWithoutDrop = 1d - (((p.probability(0)*totalPulses)+(p.probability(1)*totalPulses))/totalPulses);
				double expectedNumberOfDroppedMessage = expectedPercentageOfPulsesWithoutDrop * totalMessages;
				double expectedNumberOfDroppedMessageInclDummies = expectedNumberOfDroppedMessage / totalPulses ;
				
				System.out.println("xxx expectedPercentageOfPulsesWithoutDrop: " +expectedPercentageOfPulsesWithoutDrop); 
				System.out.println("xxx expectedNumberOfDroppedMessage: " +expectedNumberOfDroppedMessage); 
				System.out.println("xxx expectedNumberOfDroppedMessageInclDummies: " +expectedNumberOfDroppedMessageInclDummies); 
				
				
				
				double expectedNumberOfPulsesWithoutDrop = p.cumulativeProbability(1d) * totalPulses;
				double expectedNumberOfPulsesWitDrop = (1d-p.cumulativeProbability(1d)) * totalPulses;
				//double expectedNumberOfDroppedMessages = totalMessages - expectedNumberOfPulsesWithoutDrop;
				double expectedNumberOfDroppedMessagesInclDummies = totalPulses - expectedNumberOfPulsesWithoutDrop;
				System.out.println("expectedNumberOfPulsesWithoutDrop: " +expectedNumberOfPulsesWithoutDrop); 
				System.out.println("expectedNumberOfPulsesWitDrop: " +expectedNumberOfPulsesWitDrop); 
				//System.out.println("expectedNumberOfDroppedMessages: " +expectedNumberOfDroppedMessages); 
				System.out.println("expectedNumberOfDroppedMessagesInclDummies: " +expectedNumberOfDroppedMessagesInclDummies); 
				
				
			} catch (MathException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			double mixSendingRate = Simulator.settings.getPropertyAsDouble("LSB_REQUEST_RATE");
			double mixSendInterval = (1d/mixSendingRate)*1000d; // in ms
			double dropProbabilityPerInterval = 1d - (p.probability(0) + p.probability(1));
			System.out.println("erwartungswert fuer drop: " +dropProbabilityPerInterval*100d +"%"); 
			
			double t = 1d - ( 
					((p.probability(0)*totalPulses)+(p.probability(1)*totalPulses))
					/
					(totalPulses/(mixSendInterval))
					)*100d; // %
			
			double t2= 1d-((zeros+ones)/(totalPulses/(mixSendInterval)))*100d; // %	
			System.out.println("erwartungswert fuer drop (gemessen bei traffic source): " +(1-((zeros+ones)/(totalPulses/((1d/(mixSendingRate*1000d))))))*100d +"%, = " +(1-((zeros+ones)/totalPulses))*totalMessages +" messages"); 
			
			
			System.out.println("erwartungswert fuer drop (gemessen bei traffic source): " +(1d-((zeros+ones)/(totalPulses/((1d/(mixSendingRate*1000d))))))*100d +"%, = " +(1-((zeros+ones)/totalPulses))*totalMessages +" messages"); 
			System.out.println("erwartungswert analytisch: " +(1d-(((p.probability(0)*totalPulses)+(p.probability(1)*totalPulses))/totalPulses))*100d +"%, = " +(1d-(((p.probability(0)*totalPulses)+(p.probability(1)*totalPulses))/totalPulses))*totalMessages +" messages"); 
			
			System.out.println("erwartungswert fuer drop mit dummies (gemessen bei traffic source): " +((1-((zeros+ones)/totalPulses))*totalMessages)/totalMessages); 
			System.out.println("erwartungswert analytisch mit dummies: " +((1d-(((p.probability(0)*totalPulses)+(p.probability(1)*totalPulses))/totalPulses))*totalMessages)/totalMessages); 
		}	
		
	}
	*/
	
	
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
	
	
	private class StopSimulationTimer extends Thread {
		
		private Simulator simulator;
		private long sleepUntil;
		
		
		public StopSimulationTimer(Simulator simulator) {
			this.simulator = simulator;
			this.start();
		}
		
		
		@Override
		public void run() {
			int delay = Simulator.settings.getPropertyAsInt("REAL_TIME_LIMIT_IN_SEC") * 1000;
			sleepUntil = System.currentTimeMillis() + delay;
			while (System.currentTimeMillis() < sleepUntil) {
				try {
					Thread.sleep(Math.max(0, sleepUntil - System.currentTimeMillis()));
				} catch (InterruptedException e) {
					continue;
				}
			}
			simulator.stopSimulation("real-time limit reached (variable REAL_TIME_LIMIT_IN_SEC in experiment config)");
		}
		
	}
	
	
	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		
		double generateTrafficDuration = 1000000d;
		double numberOfClients = 10;
		double alpha = 1.1d;
		double avgRequestsPerPulse = 1.0d;
		
		double[] latestScheduledSends = new double[(int) numberOfClients];
		//double[] numberOfSends = new double[(int) numberOfClients];
		//double avgSendInterval = (1000d/avgRequestsPerPulse);
		BigDecimal sum;
		int ctr;
		//NormalizedPareto pareto = new NormalizedPareto(ALPHA, avgSendInterval);
		Pareto pareto = new Pareto(alpha, avgRequestsPerPulse);
		for (int i=0; i<numberOfClients; i++) { // simulate traffic for each client
			sum = new BigDecimal("0");
			ctr = 0;
			while (true) {
				double interval = pareto.drawSample(avgRequestsPerPulse);
				if ((latestScheduledSends[i] + interval) > generateTrafficDuration)
					break;
				latestScheduledSends[i] += interval;
				//numberOfSends[i]++;
				sum = sum.add(new BigDecimal(interval));
				ctr++;
			}
			System.out.println("avg: " +sum.divide(new BigDecimal(ctr), 5, BigDecimal.ROUND_HALF_UP) +" (should be " +avgRequestsPerPulse +")"); 
		}
		// calc avg:
		//for (int i=0; i<numberOfSends.length; i++) {
		//	System.out.println("avg sends: " +(numberOfSends[i]/(generateTrafficDuration/1000d)) +"(" +numberOfSends[i] +"/" +(generateTrafficDuration/1000d) +")"); 
		//} 

	} 
	

}
