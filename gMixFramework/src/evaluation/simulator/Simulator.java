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
package evaluation.simulator;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import evaluation.simulator.annotations.property.BoolSimulationProperty;
import evaluation.simulator.annotations.property.DoubleSimulationProperty;
import evaluation.simulator.annotations.property.IntSimulationProperty;
import evaluation.simulator.annotations.property.StringSimulationProperty;
import evaluation.simulator.annotations.property.requirements.SimulationEndRealTimeEndRequirement;
import evaluation.simulator.core.ExperimentConfig;
import evaluation.simulator.core.binding.gMixBinding;
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
import framework.core.config.Paths;
import framework.core.config.Settings;
import framework.core.launcher.CommandLineParameters;
import framework.core.launcher.GMixTool;
import framework.core.util.Util;
import gnu.trove.TDoubleArrayList;


public class Simulator extends GMixTool implements Identifiable {

	//private static Logger logger = Logger.getLogger(Simulator.class);

	private final int numericIdentifier;
	public static Settings settings;
	gMixBinding gmixbind = null;
	
	
	@BoolSimulationProperty( name = "Debug output",
			key = "DEBUG_OUTPUT",
			inject = "0:SIMULATION,Simulation",
//			value=false,
			isStatic = true,
			info = "If this option is enabled the simulator will generate debug output. Simulations with debug output may perform significantly longer!")
	public static boolean DEBUG_ON = true;
	private static long now = 0;
	private static Simulator currentSimulator = null;
	private static CommandLineParameters commandLineParameters;
	private final PriorityQueue<Event> eventQueue = new PriorityQueue<Event>();
	private HashMap<String, AbstractClient> clients;
	private HashMap<String, Mix> mixes;
	private HashMap<String, NetworkConnection> networkConnections;
	private DistantProxy distantProxy;
	private AbstractClient[] clientByIdArray;
	private static boolean firstRun = true;
	//private static XMLResource generalConfig;
	private volatile boolean stopSimulation = false;
	private int voteStopCounter = 0;
	
	@IntSimulationProperty( name = "Recording start (ms)",
			key = "START_RECORDING_STATISTICS_AT",
			inject = "1:SIMULATION,Simulation",
			isStatic = true,
			min = 0)
	public long ts_recordStatisticsStart = Util.NOT_SET; // timestamp
	
	
	public long ts_recordStatisticsEnd = Util.NOT_SET;
	
	@DoubleSimulationProperty( name = "Real time limit (s)",
			key = "REAL_TIME_LIMIT_IN_SEC",
			inject = "5:SIMULATION,Simulation",
			isStatic = true,
			min = 0,
			enable_requirements = SimulationEndRealTimeEndRequirement.class)
	int delay; // TODO: pass through constructor
	
	@StringSimulationProperty( name = "Experiments to perform",
			key = "DESIRED_EVALUATIONS",
			inject = "2:SIMULATION,Simulation",
			possibleValues = "@StatisticsType",
			multiSelection = true,
			isStatic = true,
			info = "Multiselection is possible")
	private static String desiredExperiments;
	
	// Requirement
	@StringSimulationProperty( name = "Simulation end condition",
			key = "SIMULATION_END",
			inject = "3:SIMULATION,Simulation",
			possibleValues = "REAL_TIME_END,SIMULATION_TIME_END,END_OF_TRACE_FILE_REACHED",
			isStatic = true)
	private static String endCondition;
	
	public static Statistics trafficSourceStatistics;
	private Topology topology;
	private TrafficSource trafficSource;
	public ResultSet results;

	public static void reset() {
		Simulator.firstRun = true;
	}

	
	/*public Simulator(CommandLineParameters params, boolean isGUI) {
		this(params.setSimGuiTrue());
	}*/
	
	
	public Simulator(CommandLineParameters params) {
		Simulator.commandLineParameters = params;
		now = 0;
		this.numericIdentifier = IdGenerator.getId();
		this.results = null;
		if (firstRun) {
			firstRun = false;
			if (commandLineParameters.useSimGui){ // GUI call
				Simulator.settings = new Settings(Paths.SIM_PROPERTY_FILE_PATH);
				Simulator.settings.addProperties(commandLineParameters.passthroughParameters);
				Simulator.DEBUG_ON = Simulator.settings.getPropertyAsBoolean("DEBUG_OUTPUT");
				Simulator.currentSimulator = this;
				Statistics.setSimulator(this);
				Simulator.trafficSourceStatistics = new Statistics(this);
				this.results = performExperimentReturnResults(Simulator.settings);
			} else if (commandLineParameters.globalConfigFile != null) {
				Simulator.settings = new Settings(Paths.SIM_PROPERTY_FILE_PATH);
				Simulator.settings.addProperties(Paths.SIM_EXPERIMENT_DEFINITION_FOLDER_PATH +commandLineParameters.globalConfigFile);
				Simulator.DEBUG_ON = Simulator.settings.getPropertyAsBoolean("DEBUG_OUTPUT");
				Simulator.currentSimulator = this;
				Statistics.setSimulator(this);
				Simulator.trafficSourceStatistics = new Statistics(this);
				performExperiment(Simulator.settings);
			} else {
				Simulator.settings = new Settings(Paths.SIM_PROPERTY_FILE_PATH);
				Simulator.desiredExperiments = settings.getProperty("EXPERIMENTS_TO_PERFORM");
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
		this.clientByIdArray = this.trafficSource.createClientsArray();
		// init topology
		this.topology = Topology.getTopology();
		this.topology.constructor(this.clientByIdArray);
		this.clients = this.topology.getClients();
		this.mixes = this.topology.getMixes();
		this.networkConnections = this.topology.getNetworkConnections();
		this.distantProxy = this.topology.getDistantProxy();
		// create output strategies and sending styles:
		if (this.mixes.size() == 0) {
			for (AbstractClient client:this.clients.values()) {
				client.setSendStyle(ClientSendStyle.getInstance(client));
			}
		} else {
			for (Mix mix:this.mixes.values()) {
				OutputStrategyImpl os = OutputStrategy.getInstance(mix);
				mix.setOutputStrategy(os);
				if (mix.isFirstMix()) {
					for (AbstractClient client:this.clients.values()) {
						client.setSendStyle(os.getClientSendStyle(client));
					}
				}
			}
		}
		
		Simulator.endCondition = Simulator.settings.getProperty("SIMULATION_END");
		if (Simulator.endCondition.equals("REAL_TIME_END")) {
			new StopSimulationTimer(this);
		}
	}


	public int getNumberOfClients() {
		return this.clientByIdArray.length;
	}


	private void performSimulation() {
		Event event;
		this.trafficSource.startSending();
		while (true) {
			
			if (gMixBinding.shouldStop()){
				return;
			}
			
			event = this.eventQueue.poll(); // get next event
			if ((event == null) || this.stopSimulation) { // stop simulation
				if (event == null) {
					System.out.println("### simulation finished (no more events to simulate)");
				} else {
					System.out.println("### simulation finished");
				}
				if (this.ts_recordStatisticsEnd == Util.NOT_SET) {
					this.ts_recordStatisticsEnd = getNow();
				}
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
			if (this.sequenceCounter == Long.MAX_VALUE) {
				System.out.println("RESETTING SEQUENCE_COUNTER");
				this.sequenceCounter = this.eventQueue.size() + 1;
				long diff = Long.MAX_VALUE - this.eventQueue.size();
				for (Event event:this.eventQueue) {
					event.setSequenceNumber(event.getSequenceNumber() - diff);
				}

			}
			this.sequenceCounter++;
			e.setSequenceNumber(this.sequenceCounter);
			this.eventQueue.add(e);
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
		return this.clients;
	}


	public AbstractClient getClientById(int clientId) {
		return this.clientByIdArray[clientId];
	}


	public HashMap<String, Mix> getMixes() {
		return this.mixes;
	}


	/**
	 * @return the networkConnections
	 */
	public Map<String, NetworkConnection> getNetworkConnections() {
		return this.networkConnections;
	}


	public DistantProxy getDistantProxy() {
		return this.distantProxy;
	}



	public void stopSimulation(String reason) {
		if (this.stopSimulation) {
			return;
		}
		this.stopSimulation = true;
		System.out.println("### stopping simulation. reason: " +reason);
	}


	public void voteForStop() {
		this.voteStopCounter++;
		if (this.voteStopCounter == this.clients.size()) {
			this.stopSimulation("end of trace reached (variable SIMULATION_END in experiment config)");
		}
	}


	private static void performExperiment(Settings settings) {

		ExperimentConfig ep = new ExperimentConfig(settings);
		saveConfigToDisk(ep);

		ResultSet resultSet = generateResultSet(ep);
		storeAndPlotResults(resultSet);

		StatisticsType.reset();

	}

	private static ResultSet performExperimentReturnResults(Settings settings) {

		ExperimentConfig ep = new ExperimentConfig(settings);
		saveConfigToDisk(ep);

		ResultSet resultSet = generateResultSet(ep);
		storeAndPlotResults(resultSet);

		StatisticsType.reset();
		return resultSet;

	}

	private static void saveConfigToDisk(ExperimentConfig ep) {
		String description =	"# " +ep.experimentStart +" experiment:\n";
		if (!ep.useSecondPropertyToVary) {
			description += 	"# effect of " +ep.propertyToVary +" on [";
			for (StatisticsType st: ep.desiredStatisticsTypes) {
				description += " " +st;
			}
			description += "]\n";
		} else {
			description += 	"# effect of " +ep.propertyToVary +" and " +ep.secondPropertyToVary +" on [";
			for (StatisticsType st: ep.desiredStatisticsTypes) {
				description += " " +st;
			}
			description += "]\n";
		}
		String config = "";
		for (String property: settings.getPropertiesObject().stringPropertyNames()) {
			config += property + " = " +settings.getProperty(property) + "\n";
		}
		Util.writeToFile(	description + config,
				Paths.SIM_OUTPUT_FOLDER_PATH + ep.experimentStart
				+ "-configShort.txt");
	}


	private static void storeAndPlotResults(ResultSet resultSet) {
		for (PlotType plotType:resultSet.getDesiredPlotTypes()) {
			plotType.plot(resultSet);
		}
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
			if (!ep.useSecondPropertyToVary) {
				System.out.println("### STARTING NEW RUN SERIES (" +(i+1) +"/" +(ep.values.length) +"): " +ep.propertyToVary +"=" +value);
			} else {
				System.out.println("### STARTING NEW RUN SERIES (" +(i+1) +"/" +(ep.values.length) +"): " +ep.propertyToVary +"=" +value +", " +ep.secondPropertyToVary +"=" +secondValue);
			}

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
				for (StatisticsType st: ep.desiredStatisticsTypes) {
					results[i][st.ordinal()][j] = GeneralStatistics.getResult(st);
				}
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

		private final Simulator simulator;
		private long sleepUntil;


		public StopSimulationTimer(Simulator simulator) {
			this.simulator = simulator;
			this.start();
		}


		@Override
		public void run() {
			int delay = (int) Math.round(Simulator.settings.getPropertyAsDouble("REAL_TIME_LIMIT_IN_SEC") * 1000d);
			this.sleepUntil = System.currentTimeMillis() + delay;
			while (System.currentTimeMillis() < this.sleepUntil) {
				try {
					Thread.sleep(Math.max(0, this.sleepUntil - System.currentTimeMillis()));
				} catch (InterruptedException e) {
					continue;
				}
			}
			this.simulator.stopSimulation("real-time limit reached (variable REAL_TIME_LIMIT_IN_SEC in experiment config)");
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

	public void setBinging(gMixBinding gMixBinding) {
		gmixbind = gMixBinding;
	}
}
