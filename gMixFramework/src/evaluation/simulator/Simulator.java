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
package evaluation.simulator;

import java.util.*;

import gnu.trove.TDoubleArrayList;

import framework.core.config.Paths;
import framework.core.config.Settings;
import framework.core.launcher.CommandLineParameters;
import framework.core.launcher.GMixTool;
import framework.core.util.Util;

import evaluation.simulator.core.ExperimentConfig;
import evaluation.simulator.core.event.Event;
import evaluation.simulator.core.networkComponent.AbstractClient;
import evaluation.simulator.core.networkComponent.DistantProxy;
import evaluation.simulator.core.networkComponent.IdGenerator;
import evaluation.simulator.core.networkComponent.Identifiable;
import evaluation.simulator.core.networkComponent.Mix;
import evaluation.simulator.core.networkComponent.NetworkConnection;
import evaluation.simulator.core.statistics.GeneralStatistics;
import evaluation.simulator.core.statistics.ResultSet;
import evaluation.simulator.core.statistics.Statistics;
import evaluation.simulator.pluginRegistry.ClientSendStyle;
import evaluation.simulator.pluginRegistry.OutputStrategy;
import evaluation.simulator.pluginRegistry.PlotType;
import evaluation.simulator.pluginRegistry.StatisticsType;
import evaluation.simulator.pluginRegistry.Topology;
import evaluation.simulator.pluginRegistry.TrafficSource;
import evaluation.simulator.plugins.outputStrategy.OutputStrategyImpl;


public class Simulator extends GMixTool implements Identifiable {

	private int numericIdentifier;
	public static Settings settings;
	public static boolean DEBUG_ON = false;
	private static long now = 0;
	private static Simulator currentSimulator = null;
	private static CommandLineParameters commandLineParameters;
	private PriorityQueue<Event> eventQueue = new PriorityQueue<Event>();
	private HashMap<String, AbstractClient> clients;
	private HashMap<String, Mix> mixes;
	private HashMap<String, NetworkConnection> networkConnections;
	private DistantProxy distantProxy;
	private AbstractClient[] clientByIdArray;
	private static boolean firstRun = true;
	//private static XMLResource generalConfig;
	private volatile boolean stopSimulation = false;
	private int voteStopCounter = 0;
	public long ts_recordStatisticsStart = Util.NOT_SET; // timestamp
	public long ts_recordStatisticsEnd = Util.NOT_SET;
	public static Statistics trafficSourceStatistics;
	private Topology topology;
	private TrafficSource trafficSource;
	
	
	public Simulator(CommandLineParameters params) {
		Simulator.commandLineParameters = params;
		now = 0;
		numericIdentifier = IdGenerator.getId();
		if (firstRun) {
			firstRun = false;
			if (commandLineParameters.globalConfigFile != null) {
				Simulator.settings = new Settings(Paths.SIM_PROPERTY_FILE_PATH);
				Simulator.settings.addProperties(Paths.SIM_EXPERIMENT_DEFINITION_FOLDER_PATH +commandLineParameters.globalConfigFile);
				Simulator.DEBUG_ON = Simulator.settings.getPropertyAsBoolean("DEBUG_OUTPUT");
				Simulator.currentSimulator = this;
				Statistics.setSimulator(this);
				Simulator.trafficSourceStatistics = new Statistics(this);
				performExperiment(Simulator.settings);
			} else {
				Simulator.settings = new Settings(Paths.SIM_PROPERTY_FILE_PATH);
				String desiredExperiments = settings.getProperty("EXPERIMENTS_TO_PERFORM");
				Simulator.DEBUG_ON = Simulator.settings.getPropertyAsBoolean("DEBUG_OUTPUT");
				for (String desiredExperiment: desiredExperiments.split(",")) {
					Simulator.settings = new Settings(Paths.SIM_PROPERTY_FILE_PATH);
					Simulator.settings.addProperties(Paths.SIM_EXPERIMENT_DEFINITION_FOLDER_PATH +desiredExperiment);
					Simulator.currentSimulator = this;
					Statistics.setSimulator(this);
					Simulator.trafficSourceStatistics = new Statistics(this);
					performExperiment(Simulator.settings);
				}
			}
		} else {
			Statistics.setSimulator(this);
			Simulator.trafficSourceStatistics = new Statistics(this);
			Simulator.currentSimulator = this;
		}
		
	}


	private void executeSimulationScript(String simulationScript) {
		// init traffic source
		this.trafficSource = TrafficSource.getTrafficSource();
		this.clientByIdArray = trafficSource.createClientsArray();
		// init topology
		this.topology = Topology.getTopology();
		this.topology.constructor(this.clientByIdArray);
		this.clients = topology.getClients();
		this.mixes = topology.getMixes();
		this.networkConnections = topology.getNetworkConnections();
		this.distantProxy = topology.getDistantProxy();
		// create output strategies and sending styles:
		if (mixes.size() == 0) {
			for (AbstractClient client:clients.values())
				client.setSendStyle(ClientSendStyle.getInstance(client));
		} else {
			for (Mix mix:mixes.values()) {
				OutputStrategyImpl os = OutputStrategy.getInstance(mix);
				mix.setOutputStrategy(os);
				if (mix.isFirstMix())
					for (AbstractClient client:clients.values())
						client.setSendStyle(os.getClientSendStyle(client));
			} 
		}
		if (Simulator.settings.getProperty("SIMULATION_END").equals("REAL_TIME_END"))
			new StopSimulationTimer(this);
	}
	
	
	public int getNumberOfClients() {
		return clientByIdArray.length;
	}
	
	
	private void performSimulation() {
		Event event;
		trafficSource.startSending();
		while (true) {
			event = eventQueue.poll(); // get next event
			if (event == null || stopSimulation) { // stop simulation
				if (event == null)
					System.out.println("### simulation finished (no more events to simulate)");
				else
					System.out.println("### simulation finished");
				if (ts_recordStatisticsEnd == Util.NOT_SET) // not yet determined
					ts_recordStatisticsEnd = getNow();
				return;
			} else if (event.isCanceled()) {
				continue;
			} else { // execute event
				now = event.getExecutionTime();
				assert event.getTarget() != null;
				event.setExecuted();
				event.getTarget().executeEvent(event);
			}
		}
	}
	
	private long sequenceCounter = 0;
	// callingInstance = DEBUG
	public void scheduleEvent(Event e, Object callingInstance) {
		//System.out.println("Received ScheduleTask from " +callingInstance +" (now: " +e.getExecutionTime() +") " +e.getAttachment()); 
		if (e.getExecutionTime() < now) {
			throw new RuntimeException("ERROR: executionTime < now (" +e.getExecutionTime() +" < " +now);
		} else {
			if (sequenceCounter == Long.MAX_VALUE) {
				System.out.println("RESETTING SEQUENCE_COUNTER"); 
				sequenceCounter = eventQueue.size() + 1;
				long diff = Long.MAX_VALUE - eventQueue.size();
				for (Event event:eventQueue)
					event.setSequenceNumber(event.getSequenceNumber() - diff);
				 
			}
			sequenceCounter++;
			e.setSequenceNumber(sequenceCounter);
			eventQueue.add(e);
		}
		
	}
	
	
	public void unscheduleEvent(Event e) {
		e.cancel();
	}


	/**
	 * @return the now
	 */
	public static long getNow() {
		return now;
	}


	public static Simulator getSimulator() {
		return currentSimulator;
	}

	
	public HashMap<String, AbstractClient> getClients() {
		return clients;
	}
	
	
	public AbstractClient getClientById(int clientId) {
		return clientByIdArray[clientId];
	}
	

	public HashMap<String, Mix> getMixes() {
		return mixes;
	}
	

	/**
	 * @return the networkConnections
	 */
	public Map<String, NetworkConnection> getNetworkConnections() {
		return networkConnections;
	}
	

	public DistantProxy getDistantProxy() {
		return distantProxy;
	}


	
	public void stopSimulation(String reason) {
		if (stopSimulation) // ignore multiple calls
			return;
		this.stopSimulation = true;
		System.out.println("### stopping simulation. reason: " +reason);
	}
	

	public void voteForStop() {
		voteStopCounter++;
		if (voteStopCounter == clients.size())
			stopSimulation("end of trace reached (variable SIMULATION_END in experiment config)");
	}
	
	
	private static void performExperiment(Settings settings) {

		ExperimentConfig ep = new ExperimentConfig(settings);
		saveConfigToDisk(ep);

		ResultSet resultSet = generateResultSet(ep);
		storeAndPlotResults(resultSet);
		
		StatisticsType.reset();

	}
	
	
	private static void saveConfigToDisk(ExperimentConfig ep) {
		String description =	"# " +ep.experimentStart +" experiment:\n";
		if (!ep.useSecondPropertyToVary) {
			description += 	"# effect of " +ep.propertyToVary +" on [";
			for (StatisticsType st: ep.desiredStatisticsTypes)
				description += " " +st;
			description += "]\n";
		} else {
			description += 	"# effect of " +ep.propertyToVary +" and " +ep.secondPropertyToVary +" on [";
			for (StatisticsType st: ep.desiredStatisticsTypes)
				description += " " +st;
			description += "]\n";
		}
		String config = "";
		for (String property: settings.getPropertiesObject().stringPropertyNames())
			config += property + " = " +settings.getProperty(property) + "\n";
		Util.writeToFile(	description + config, 
							Paths.SIM_OUTPUT_FOLDER_PATH + ep.experimentStart 
							+ "-configShort.txt");	
	}
	
	
	private static void storeAndPlotResults(ResultSet resultSet) {
		for (PlotType plotType:resultSet.getDesiredPlotTypes())
			plotType.plot(resultSet);
	} 
	
	
	private static ResultSet generateResultSet(ExperimentConfig ep) {
		Simulator simulator;
		ResultSet resultSet = new ResultSet(ep);
		TDoubleArrayList[][][] results = resultSet.results;
		long startOfExperiment = System.currentTimeMillis();
		
		// generate results and store them in ResultSet
		for (int i=0; i<ep.values.length; i++) { // for each value of the parameter(s) to vary
			// set value(s) of the parameter(s) to vary
			String value = ep.values[i];
			settings.setProperty(ep.propertyToVary, value);
			String secondValue = null;
			if (ep.useSecondPropertyToVary) {
				secondValue = ep.valuesForSecondProperty[i];
				settings.setProperty(ep.secondPropertyToVary, secondValue);
			}
			
			System.out.println();
			if (!ep.useSecondPropertyToVary)
				System.out.println("### STARTING NEW RUN SERIES (" +(i+1) +"/" +(ep.values.length) +"): " +ep.propertyToVary +"=" +value);
			else
				System.out.println("### STARTING NEW RUN SERIES (" +(i+1) +"/" +(ep.values.length) +"): " +ep.propertyToVary +"=" +value +", " +ep.secondPropertyToVary +"=" +secondValue);
			
			for (int j=0; j<ep.runs; j++) { // for each validation run

				System.out.println("### STARTING RUN " +(j+1) +"/" +ep.runs);

				simulator = new Simulator(commandLineParameters);
				simulator.executeSimulationScript(ep.simulationScript);
				
				long start = System.currentTimeMillis();
				simulator.performSimulation();
				resultSet.simulationTime[i][j] = simulator.ts_recordStatisticsEnd - simulator.ts_recordStatisticsStart;
				resultSet.numberOfClients[i] = simulator.clients.size();
				resultSet.numberOfMixes[i] = simulator.mixes.size();
				System.out.println("### FINISHED RUN " +(j+1) +"/" +ep.runs +" (execution time: " +(System.currentTimeMillis() - start) +" ms, simulation time: " +resultSet.simulationTime[i][j] +"ms)");

				// calculate results
				for (StatisticsType st: ep.desiredStatisticsTypes)
					results[i][st.ordinal()][j] = GeneralStatistics.getResult(st);
				GeneralStatistics.reset();
				AbstractClient.reset();
				
			} // end for each validation run
	
		} // end for each value of the parameter to vary
		
		System.out.println("### total execution time of experiment: " +(System.currentTimeMillis() - startOfExperiment)); 
		
		return resultSet;
		
	}


	@Override
	public int getGlobalId() {
		return this.numericIdentifier;
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
			int delay = (int) Math.round(Simulator.settings.getPropertyAsDouble("REAL_TIME_LIMIT_IN_SEC") * 1000d);
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
		new Simulator(new CommandLineParameters(args));
	}
}
