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

package simulator;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.*;
import outputStrategy.OutputStrategy;
import logger.Logger;
import misc.DiskIOHelper;
import misc.PropertyFileHelper;
import communicationBehaviour.ClientCommunicationBehaviour;
import statistics.GeneralStatistics;
import statistics.Statistics;
import statistics.StatisticsType;
import statistics.EvaluationType;
import trafficSource.TrafficSource;
import delayBox.DelayBox;
import networkComponent.Client;
import networkComponent.DistantProxy;
import networkComponent.Mix;
import networkComponent.NetworkConnection;


public class Simulator {

	private static int now = 0;
	private static Simulator currentSimulator = null;
	private LinkedList<Event> eventQueue = new LinkedList<Event>();
	private int correctPosition;
	private Map<String, Client> clients = new HashMap<String, Client>();
	private Map<String, Mix> mixes = new HashMap<String, Mix>();
	private Map<String, NetworkConnection> networkConnections = new HashMap<String, NetworkConnection>();
	private DistantProxy distantProxy;
	private TrafficSource trafficSource;
	private Client[] clientByIdArray;
	
	
	public Simulator() {
		now = 0;
		Statistics.setSimulator(this);
		Simulator.currentSimulator = this;
	}


	private void executeSimulationScript(String simulationScript) {

		if (simulationScript.compareTo("CLIENT-MIX-MIX-MIX-DISTANT_PROXY") == 0)
			scriptClientMixMixMixDistantProxy();
		else if (simulationScript.compareTo("CLIENT-MIX-DISTANT_PROXY") == 0)
			scriptClientMixDistantProxy();
		else if (simulationScript.compareTo("CLIENT-DISTANT_PROXY") == 0)
			scriptClientDistantProxy();
		else
			throw new RuntimeException("ERROR: Simulation Script " +simulationScript +" not found!");
		
		clientByIdArray = new Client[clients.size()];
		
		for (Client c: clients.values())
			clientByIdArray[c.getClientId()] = c;
			
	}
	
	
	private void scriptClientMixMixMixDistantProxy() {
		
		// create network nodes
		mixes.put("Mix:Mix1", new Mix("Mix1", this, DelayBox.getInstance(100, 100, 7), true, false));
		mixes.put("Mix:Mix2", new Mix("Mix2", this, DelayBox.getInstance(100, 100, 7), false, false));
		mixes.put("Mix:Mix3", new Mix("Mix3", this, DelayBox.getInstance(100, 100, 7), false, true));
		
		distantProxy = DistantProxy.getInstance("DistantProxy", this, DelayBox.getInstance(100, 100, 7));
			
		// create network lines
		networkConnections.put("NetworkConnection:NetworkConnection1:Mix3<->DistantProxy", new NetworkConnection(mixes.get("Mix:Mix3"), distantProxy, this));
		networkConnections.put("NetworkConnection:NetworkConnection2:Mix2<->Mix3", new NetworkConnection(mixes.get("Mix:Mix2"), mixes.get("Mix:Mix3"), this));
		networkConnections.put("NetworkConnection:NetworkConnection3:Mix1<->Mix2", new NetworkConnection(mixes.get("Mix:Mix1"), mixes.get("Mix:Mix2"), this));
		
	}


	private void scriptClientMixDistantProxy() {
	
		// create delay boxes
		DelayBox delayBoxMix = DelayBox.getInstance(3, 3, 7);
	
		// create network nodes
		mixes.put("Mix:Mix1", new Mix("Mix1", this, delayBoxMix, true, true));
	
		distantProxy = DistantProxy.getInstance("DistantProxy", this, DelayBox.getInstance(100, 100, 7));
	
		// create network lines
		networkConnections.put("NetworkConnection:NetworkConnection1:Mix<->DistantProxy", new NetworkConnection(mixes.get("Mix:Mix1"), distantProxy, this));

	}
	
	
	private void scriptClientDistantProxy() {
		
		distantProxy = DistantProxy.getInstance("DistantProxy", this, DelayBox.getInstance(100, 100, 7));

	}
	
	
	private void performSimulation() {
		
		Event event;
		trafficSource = new TrafficSource(this); 
		trafficSource.simulateTraffic();
		
		while (true) {
			
			event = eventQueue.pollFirst();
			
			if (event == null) {
				System.out.println("### simulation finished");
				OutputStrategy.reset();
				ClientCommunicationBehaviour.reset();
				return;
			}
			
			now = event.getExecutionTime();
			 
			if (event.getTarget() == null) {
				System.out.println("event.getTarget() == null");
				System.out.println(event.getEventType()); 
				System.out.println(event.getAttachment()); 
			}
			
			event.getTarget().executeEvent(event);

		}
		
	}
	
	// callingInstance = DEBUG
	public void scheduleEvent(Event e, Object callingInstance) {
		
		//System.out.println("Received ScheduleTask from " +callingInstance +" (now: " +e.getExecutionTime() +") " +e.getAttachment()); 
		if (e.getExecutionTime() < now) {
			
			throw new RuntimeException("ERROR: executionTime < now");
			
		} else {
			
			findCorrectPosition(e);
			eventQueue.add(correctPosition, e);
			
		}
		
	}
	
	
	public void unscheduleEvent(Event e) {
		
		eventQueue.removeFirstOccurrence(e);
		
	}
	
	
	private void findCorrectPosition(Event e) {
		findCorrectPosition(e, 0, (eventQueue.size() - 1));
	}
	
	
	private void findCorrectPosition(Event e, int startIndex, int endIndex) {
		
		if (eventQueue.size() == 0) { // first message

			correctPosition = 0;

		} else {

			if (startIndex <= endIndex) {

				int mid = (startIndex + endIndex) / 2;

				switch (e.compareTo(eventQueue.get(mid))) {
				
					case -1:
						
						correctPosition = mid;
						findCorrectPosition(e, startIndex, mid - 1);
						break;					
						
					case  0:
						
						correctPosition = mid;
						startIndex = endIndex; // stop execution
						break;						
						
					case  1:
						
						correctPosition = mid + 1;
						findCorrectPosition(e, mid + 1, endIndex);
						break;
	
				}

			}

		}
		
	}
	
	
	/**
	 * @return the now
	 */
	public static int getNow() {
		return now;
	}


	public static Simulator getSimulator() {
		return currentSimulator;
	}
	
	
	/**
	 * @return the eventQueue
	 */
	public LinkedList<Event> getEventQueue() {
		return eventQueue;
	}

	/**
	 * @param eventQueue the eventQueue to set
	 */
	public void setEventQueue(LinkedList<Event> eventQueue) {
		this.eventQueue = eventQueue;
	}

	/**
	 * @return the correctPosition
	 */
	public int getCorrectPosition() {
		return correctPosition;
	}

	/**
	 * @param correctPosition the correctPosition to set
	 */
	public void setCorrectPosition(int correctPosition) {
		this.correctPosition = correctPosition;
	}

	/**
	 * @return the clients
	 */
	public Map<String, Client> getClients() {
		return clients;
	}
	
	
	public Client getClientById(int clientId) {
		return clientByIdArray[clientId];
	}
	

	/**
	 * @param clients the clients to set
	 */
	public void setClients(Map<String, Client> clients) {
		this.clients = clients;
	}

	/**
	 * @return the mixes
	 */
	public Map<String, Mix> getMixes() {
		return mixes;
	}

	/**
	 * @param mixes the mixes to set
	 */
	public void setMixes(Map<String, Mix> mixes) {
		this.mixes = mixes;
	}

	/**
	 * @return the networkConnections
	 */
	public Map<String, NetworkConnection> getNetworkConnections() {
		return networkConnections;
	}

	/**
	 * @param networkConnections the networkConnections to set
	 */
	public void setNetworkConnections(
			Map<String, NetworkConnection> networkConnections) {
		this.networkConnections = networkConnections;
	}

	/**
	 * @return the trafficSource
	 */
	public TrafficSource getTrafficSource() {
		return trafficSource;
	}

	/**
	 * @param trafficSource the trafficSource to set
	 */
	public void setTrafficSource(TrafficSource trafficSource) {
		this.trafficSource = trafficSource;
	}

	/**
	 * @param now the now to set
	 */
	public static void setNow(int now) {
		Simulator.now = now;
	}

	/**
	 * @return the distantProxy
	 */
	public DistantProxy getDistantProxy() {
		return distantProxy;
	}


	/**
	 * @param distantProxy the distantProxy to set
	 */
	public void setDistantProxy(DistantProxy distantProxy) {
		this.distantProxy = distantProxy;
	}



	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//String path = (args.length == 0) ? "/experimentDefinitions/properties.txt" : args[0];
		//performExperiment(path);
		
		performDesiredExperiments();
	
	}
	
	
	
	public static void performDesiredExperiments() {
		
		String desiredExperiments = PropertyFileHelper.getPropertyFromFile("settings.txt", "EXPERIMENTS_TO_PERFORM");
		
		for (String desiredExperiment: desiredExperiments.split(","))
			performExperiment("./experimentDefinitions/"+desiredExperiment);
		
	}
	
	
	private static void performExperiment(String pathToPropertyFile) {

		ExperimentConfig ep = new ExperimentConfig(pathToPropertyFile);
		Logger.init("output/all/" +ep.experimentStart +"-consoleOutput.txt");
		saveConfigToDisk(ep);

		BigDecimal[][][][] results = generateResultSet(ep);
		saveResultsToDisk(results, ep);
		plotResults(results, ep);
		
		StatisticsType.reset();

	}
	
	
	static class ExperimentConfig {
		
		String simulationScript;
		StatisticsType[] statisticsType;
		String propertyToVary;
		String[] values;
		boolean useSecondPropertyToVary;
		String secondPropertyToVary;
		String[] valuesForSecondProperty;
		int runs;
		String experimentStart;
		String pathToPropertyFile;
		boolean plotErrorBars;
		
		
		// load experiment config from property file
		public ExperimentConfig(String pathToPropertyFile) {
			
			Settings.initialize(pathToPropertyFile);
			this.pathToPropertyFile = pathToPropertyFile;
			this.simulationScript = Settings.getProperty("SIMULATION_SCRIPT");
				
			if (Settings.getProperty("DESIRED_EVALUATIONS").equals("ALL")) {
				
				this.statisticsType =  StatisticsType.values();
			
			} else {
				
				Vector<StatisticsType> tmp = new Vector<StatisticsType>();
				
				for (String desiredEvaluation: Settings.getProperty("DESIRED_EVALUATIONS").split(","))
					for (StatisticsType cur: StatisticsType.values())
						if (cur.toString().equals(desiredEvaluation)) {
							tmp.add(cur);
							cur.isActivated = true;
							break;
						}
				
				this.statisticsType = tmp.toArray(new StatisticsType[0]);
							
			}

			this.propertyToVary = Settings.getProperty("PROPERTY_TO_VARY");
			this.values = Settings.getProperty("VALUES_FOR_THE_PROPERTY_TO_VARY").split(",");
			
			this.useSecondPropertyToVary = 
				Settings.getProperty("USE_SECOND_PROPERTY_TO_VARY").equals("TRUE");
			
			this.secondPropertyToVary = null;
			this.valuesForSecondProperty = null;
			
			if (useSecondPropertyToVary) {
				this.secondPropertyToVary = 
					Settings.getProperty("SECOND_PROPERTY_TO_VARY");
				this.valuesForSecondProperty = 
					Settings.getProperty(
							"VALUES_FOR_THE_SECOND_PROPERTY_TO_VARY").split(","
						);	
				
			}
			
			this.runs = new Integer(Settings.getProperty("VALIDATION_RUNS")) +1;
			
			this.plotErrorBars =  Settings.getProperty("PLOT_ERROR_BARS").equals("TRUE") && runs != 1;
			
			this.experimentStart = "";
			Calendar calendar = Calendar.getInstance();
			this.experimentStart += calendar.get(Calendar.YEAR) + "-";
			this.experimentStart += (calendar.get(Calendar.MONTH)+1) + "-";
			this.experimentStart += calendar.get(Calendar.DAY_OF_MONTH) + "_";
			this.experimentStart += calendar.get(Calendar.HOUR_OF_DAY) + "-";
			this.experimentStart += calendar.get(Calendar.MINUTE) + "_";
			this.experimentStart += calendar.get(Calendar.MILLISECOND);
			
			if (	this.useSecondPropertyToVary && 
					this.valuesForSecondProperty.length != this.values.length
					)
				
				throw new RuntimeException(	"ERROR: the same number of " +
											"values must be specified for " +
											"PROPERTY_TO_VARY and " +
											"SECOND_PROPERTY_TO_VARY in " +
											"property file!");
			
		}

	}
		
	
	private static void saveConfigToDisk(ExperimentConfig ep) {
		
		String contentsOfPropertyfile = DiskIOHelper.getFileContent(ep.pathToPropertyFile);
		String description =	"# " +ep.experimentStart 
											+" experiment:\n";
		
		if (!ep.useSecondPropertyToVary)
			description += 	"# effect of " +ep.propertyToVary +" on " 
										+ep.statisticsType[0].measurand +" (" 
										+ep.statisticsType[0] +")\n";
		else
			description += 	"# effect of " +ep.propertyToVary +" and " 
										+ep.secondPropertyToVary +" on " 
										+ep.statisticsType[0].measurand +" (" 
										+ep.statisticsType[0] +")\n";
		
		
		String setupWithoutDescription = "";
		for (String property: Settings.getProperties().stringPropertyNames())
			setupWithoutDescription += property + " = " +Settings.getProperty(property) + "\n";
		
		DiskIOHelper.writeToFile(	description + contentsOfPropertyfile, 
									"./output/all/" + ep.experimentStart 
									+ "-config.txt");
		
		DiskIOHelper.writeToFile(	description + contentsOfPropertyfile, 
									"./output/last-config.txt");
		
		DiskIOHelper.writeToFile(	description + setupWithoutDescription, 
									"./output/all/" + ep.experimentStart 
									+ "-configShort.txt");
		
		DiskIOHelper.writeToFile(	description + setupWithoutDescription, 
									"./output/last-configShort.txt");
		
	}

	
	private static BigDecimal[][][][] generateResultSet(ExperimentConfig ep) {
		
		Simulator simulator;
		BigDecimal[][][][] results = new BigDecimal[ep.runs][ep.values.length][StatisticsType.values().length][EvaluationType.values().length]; // [validation_run][variing_value][statisticsType][evaluationType]
		long startOfExperiment = System.currentTimeMillis();
		
		// generate results and save them to BigDecimal[][][] results
		for (int i=0; i<ep.values.length; i++) { // for each value of the parameter(s) to vary
			
			// set value(s) of the parameter(s) to vary
			String value = ep.values[i];
			Settings.setProperty(ep.propertyToVary, value);
			String secondValue = null;
			if (ep.useSecondPropertyToVary) {
				secondValue = ep.valuesForSecondProperty[i];
				Settings.setProperty(ep.secondPropertyToVary, secondValue);
			}
			
			System.out.println();
			if (!ep.useSecondPropertyToVary)
				System.out.println("### STARTING NEW RUN SERIES (" +(i+1) +"/" +(ep.values.length) +"): " +ep.propertyToVary +"=" +value);
			else
				System.out.println("### STARTING NEW RUN SERIES (" +(i+1) +"/" +(ep.values.length) +"): " +ep.propertyToVary +"=" +value +", " +ep.secondPropertyToVary +"=" +secondValue);
			
			for (int j=0; j<ep.runs; j++) { // for each validation run

				System.out.println("### STARTING RUN " +(j+1) +"/" +ep.runs);

				simulator = new Simulator();
				simulator.executeSimulationScript(ep.simulationScript);
				
				long start = System.currentTimeMillis();
				simulator.performSimulation();
				System.out.println("### FINISHED RUN " +(j+1) +"/" +ep.runs +" (execution time: " +(System.currentTimeMillis() - start) +" ms)");
				
				// calculate results
				for (StatisticsType st: ep.statisticsType) {
					for(EvaluationType et: st.intendedEvaluations) { // for each type of evaluation intended for the current statisticsType
						try {
							results[j][i][st.ordinal()][et.ordinal()] = GeneralStatistics.getCumulativeResult(st, et);
						} catch (NullPointerException e) {
							if (Settings.DEBUG_ON)
								e.printStackTrace();
						}
					}	
				}
				
				GeneralStatistics.reset();
				Client.reset();
				
			} // end for each validation run
	
		} // end for each value of the parameter to vary
		
		System.out.println("### total execution time of experiment: " +(System.currentTimeMillis() - startOfExperiment)); 
		
		return results;
		
	}
	
	
	private static void saveResultsToDisk(BigDecimal[][][][] results, ExperimentConfig ep) {
		
		String contentForFileExplainedResultsTxt = "";
		
		HashMap<String, String> contentForResultsTxts = new HashMap<String, String>(); // contentes for the different result files (one file for each StatisticsType.measurand) 
		String[] measurands = StatisticsType.getDistinctMeasurandsFromList(ep.statisticsType);
	
		for (int i=0; i<ep.values.length; i++) { // for each value of the parameter(s) to vary
			
			contentForFileExplainedResultsTxt += "\n";
			
			if (!ep.useSecondPropertyToVary)
				contentForFileExplainedResultsTxt += "\nResults for " +ep.propertyToVary +" " +ep.values[i] +": ";
			else
				contentForFileExplainedResultsTxt += "\nResults for " +ep.propertyToVary +" " +ep.values[i]  +" and " +ep.secondPropertyToVary +" " +ep.valuesForSecondProperty[i] +": ";
			
			for (String measurand: measurands) { // for each result file (= each StatisticsType.measurand chosen for this experiment)
				
				String resultLine = contentForResultsTxts.get(measurand);
				
				if (resultLine == null)
					resultLine = ep.values[i];
				else
					resultLine += ep.values[i];
					
				for(StatisticsType st: ep.statisticsType) {
					
					if (st.measurand.equals(measurand)) {
						
						for(EvaluationType et: st.intendedEvaluations) {
							
							BigDecimal[] resultSet = getResultsForAllRuns(i,st,et,results);
							
							if (resultSet == null) { // nothing recorded -> possible error
								
								System.err.println("POSSIBLE ERROR: No results recorded for " +st +", " +et);
								contentForFileExplainedResultsTxt += "\nPOSSIBLE ERROR: No results recorded for " +st +", " +et +" ("+ep.propertyToVary +" = " +ep.values[i] +")";
								resultLine += " 0";
								
							} else if (resultSet.length == 1) { // experiment without validation runs
								
								resultLine += " "+resultSet[0];
								contentForFileExplainedResultsTxt += "\n" +ep.propertyToVary +" = " +ep.values[i]  +" "+et.description +" " +st.measurand +": " +resultSet[0] +" " +st.unit;
							
							} else { // experiment with validation runs
								
								resultLine += " "+getAvg(resultSet);
								resultLine += " "+getMin(resultSet);
								resultLine += " "+getMax(resultSet);
								
								contentForFileExplainedResultsTxt += "\n" +ep.propertyToVary +" = " +ep.values[i]  +" "+et.description +" " +st.measurand +" (AVG/MIN/MAX): " +getAvg(resultSet) +"/" +getMin(resultSet) +"/" +getMax(resultSet) +" " +st.unit;
								
							}
							
						}
						
					}
					
				}
				
				resultLine += "\n";
				contentForResultsTxts.put(measurand, resultLine);
				
			}
		
		}

		// write files
		contentForFileExplainedResultsTxt += "\n";
		DiskIOHelper.writeToFile(contentForFileExplainedResultsTxt, "./output/all/" + ep.experimentStart + "-explained-results.txt");
		DiskIOHelper.writeToFile(contentForFileExplainedResultsTxt, "./output/last-explained-results.txt");
		for (String measurand:measurands) {
			DiskIOHelper.writeToFile(contentForResultsTxts.get(measurand), "./output/all/" + ep.experimentStart +"-results-" +measurand +".txt");
			DiskIOHelper.writeToFile(contentForResultsTxts.get(measurand), "./output/last-results-" +measurand +".txt");
		}
		
	}
	
	
	private static void plotResults(BigDecimal[][][][] results, ExperimentConfig ep) {
		
		String contentForFilePlotOutput = "";
		String[] measurands = StatisticsType.getDistinctMeasurandsFromList(ep.statisticsType);
		
		for (String measurand : measurands) { // generate diagram for each measurand
			
			String experimentDescription = "effect of " + ep.propertyToVary
					+ " on " + measurand;
			String plotScript = DiskIOHelper.getFileContent("./plotscripts/"
					+ Settings.getProperty("NAME_OF_PLOT_SCRIPT"));
			plotScript = plotScript.replace(
					"varTitle = \"WILL_BE_SET_AUTOMATICALLY\"", "varTitle = \""
							+ experimentDescription + "\"");
			plotScript = plotScript.replace(
					"varXLabel = \"WILL_BE_SET_AUTOMATICALLY\"",
					"varXLabel = \"" + ep.propertyToVary + "\"");
			plotScript = plotScript.replace(
					"varYLabel = \"WILL_BE_SET_AUTOMATICALLY\"",
					"varYLabel = \"" + measurand + " in "
							+ StatisticsType.getUnit(measurand) + "\"");

			plotScript = plotScript.replace(
					"varInputFile = \"WILL_BE_SET_AUTOMATICALLY\"",
					"varInputFile = \"./output/all/" + ep.experimentStart + "-results-"
							+ measurand + ".txt\"");

			String plotInstruction = "plot";
			String plotstyle = (ep.runs != 1 && ep.plotErrorBars) ? "yerrorlines" : "linespoints";
				
			int i = 1;
			
			for (StatisticsType st : ep.statisticsType) {
				if (st.measurand.equals(measurand)) {
					for (EvaluationType et : st.intendedEvaluations) {
						if (ep.runs != 1 && ep.plotErrorBars) {
							plotInstruction +=	" varInputFile using 1:" +(++i) +":" +(++i) +":" +(++i) + " w " 
												+plotstyle +" title '" + et.description + " " +st.toString() 
												+" (max dev:" +getMaxError(st, et, results) +"%; abs: " +getMaxErrorAbs(st, et, results) +")',";
						} else {
							plotInstruction += 	" varInputFile using 1:" + (++i) + " w " +plotstyle +" title '" + et.description
												+ " " +st.toString() + "',";
						}
					}
				}
			}

			plotInstruction += "xyz";
			plotInstruction = plotInstruction.replace(",xyz", "");
			plotScript = plotScript.replace(
					"plot \"WILL_BE_SET_AUTOMATICALLY\"", plotInstruction);
			plotScript = plotScript.replace(
					"set output \"WILL_BE_SET_AUTOMATICALLY\"",
					"set output \"./output/all/" + ep.experimentStart + "-diagram-"
							+ measurand + ".eps\"");

			DiskIOHelper.writeToFile(plotScript, "./output/all/" + ep.experimentStart
					+ "-plotscript-" + measurand + ".txt");	

			String gnuplotOutput = "";
			String gnuplotErrorOutput = ""; 
			
			try {

				String line;
				String gnuPlotFolder = PropertyFileHelper.getPropertyFromFile("settings.txt", "GNUPLOT_FOLDER");
				
				Process process = Runtime.getRuntime().exec(
						gnuPlotFolder + "gnuplot"
						+ " ./output/all/" + ep.experimentStart + "-plotscript-"
						+ measurand + ".txt"); 
				
				process.waitFor(); // TODO: paralellisieren (auf ende warten und ausgaben lesen... au§erdem: ausgaben gleich mit out.print ausgeben)
				
				BufferedReader input = new BufferedReader(
						new InputStreamReader(process.getInputStream()));
				BufferedReader errorInput = new BufferedReader(
						new InputStreamReader(process.getErrorStream()));

				while ((line = input.readLine()) != null)
					gnuplotOutput += line;

				while ((line = errorInput.readLine()) != null)
					gnuplotErrorOutput += line;

				input.close();

				plotScript = plotScript.replace(
						"varInputFile = \"./output/all/" + ep.experimentStart + "-results-" + measurand + ".txt\""
						,
						"varInputFile = \"" + ep.experimentStart + "-results-" + measurand + ".txt\""
					);
						
				plotScript = plotScript.replace(
						"set output \"./output/all/" + ep.experimentStart + "-diagram-" + measurand + ".eps\""
						,
						"set output \"./" + ep.experimentStart + "-diagram-" + measurand + ".eps\""
					);
				
				DiskIOHelper.writeToFile(plotScript, "./output/all/" + ep.experimentStart + "-plotscript-" + measurand + ".txt");

				
				plotScript = plotScript.replace(
						"varInputFile = \"" + ep.experimentStart + "-results-" + measurand + ".txt\""
						,
						"varInputFile = \"last-results-" + measurand + ".txt\""
					);
				
				plotScript = plotScript.replace(
						"set output \"./" + ep.experimentStart + "-diagram-" + measurand + ".eps\""
						,
						"set output \"last-diagram-" + measurand + ".eps\""
					);
				
				DiskIOHelper.writeToFile(plotScript, "./output/last-plotscript-" + measurand + ".txt");

				try {
					process = Runtime.getRuntime().exec("./activateAquaTerm.sh");
				} catch (Exception e) {
				}

			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("ERROR"); 
			}

			if (!gnuplotOutput.equals(""))
				System.out.println("Gnuplot output (stdout):\n" + gnuplotOutput);
			if (!gnuplotErrorOutput.equals(""))
				System.err.println("Gnuplot output (error):\n" + gnuplotErrorOutput);

			contentForFilePlotOutput += "\nGnuplot output (stdout):\n" + gnuplotOutput;
			contentForFilePlotOutput += "\nGnuplot output (error):\n" + gnuplotErrorOutput;

		}

		DiskIOHelper.writeToFile(	contentForFilePlotOutput,
									"./output/last-gnuPlotOutput.txt");
		
		DiskIOHelper.writeToFile(	contentForFilePlotOutput, 
									"./output/all/" +ep.experimentStart +"-gnuPlotOutput.txt");
		
	}

	
	private static BigDecimal[] getResultsForAllRuns(
				int valueId,
				StatisticsType statisticsType, 
				EvaluationType evaluationType, 
				BigDecimal[][][][] results
				) {
		
		BigDecimal[] filteredResults = new BigDecimal[results.length];
		
		for (int i=0; i<filteredResults.length; i++)
			if (results[i][valueId][statisticsType.ordinal()][evaluationType.ordinal()] == null)
				return null;
			else
				filteredResults[i] = results[i][valueId][statisticsType.ordinal()][evaluationType.ordinal()];
			
		return filteredResults;

	}
	
	// TODO: veriation mit string statt int (z.b. versch. sim-skripte)
	/*private static void performExperiment(String simulationScript, 
			StatisticsType[] statisticsType, String propertyToVary, 
			int from, int to, int stepLength) {
		
		int entries = 0;
		for (int i=from; i<=to; i+=stepLength)
			entries++;
		
		int[] values = new int[entries];
		for (int i=from, j=0; i<=to; i+=stepLength, j++)
			values[j] = i;
		
		performExperiment(	simulationScript, 
							statisticsType, 
							propertyToVary, 
							values
							);

	}*/
	
	
	// TODO
	// values mit komma trennen
	/*public static void performExperiment(String experimentDescription, 
			String simulationScript, StatisticsType statisticsType, 
			String propertyToVary, String values) {
		
		throw new RuntimeException("ERROR: Note yet implemented!");
		
	}*/
	
	
	private static BigDecimal getMaxError(StatisticsType st, EvaluationType et, BigDecimal[][][][] results) {
		
		BigDecimal[] resultSet = new BigDecimal[results[0].length]; // for each value of the variing parameter
		
		for (int i=0; i<resultSet.length; i++) {  //for each value of the variing parameter
			BigDecimal[] values = new BigDecimal[results.length];
			for (int j=0; j<results.length; j++)
				values[j] = results[j][i][st.ordinal()][et.ordinal()];
			
			// calculate max deviation
			BigDecimal avg = getAvg(values);
			BigDecimal devMinAvg = avg.subtract(getMin(values));
			BigDecimal devMaxAvg = getMax(values).subtract(avg); 
			BigDecimal maxDev = (devMinAvg.compareTo(devMaxAvg) < 0) ? devMaxAvg : devMinAvg;
			
			if (avg.compareTo(BigDecimal.ZERO) == 0) // prevent division through zero
				resultSet[i] = BigDecimal.ZERO;
			else
				resultSet[i] = ((maxDev.divide(avg, 5, BigDecimal.ROUND_HALF_UP)).multiply(new BigDecimal(100))).setScale(0, BigDecimal.ROUND_HALF_UP);
			
		}
		
		return getMax(resultSet);
		
	}


	private static BigDecimal getMaxErrorAbs(StatisticsType st, EvaluationType et, BigDecimal[][][][] results) {
		
		BigDecimal[] resultSet = new BigDecimal[results[0].length]; // for each value of the variing parameter
		
		for (int i=0; i<resultSet.length; i++) {  //for each value of the variing parameter
			BigDecimal[] values = new BigDecimal[results.length];
			for (int j=0; j<results.length; j++)
				values[j] = results[j][i][st.ordinal()][et.ordinal()];
			
			// calculate max deviation
			BigDecimal avg = getAvg(values);
			BigDecimal devMinAvg = avg.subtract(getMin(values));
			BigDecimal devMaxAvg = getMax(values).subtract(avg); 
			resultSet[i] = ((devMinAvg.compareTo(devMaxAvg) < 0) ? devMaxAvg : devMinAvg).setScale(2, BigDecimal.ROUND_HALF_UP);
		}
		
		return getMax(resultSet);
		
	}
		
	
	private static BigDecimal getAvg(BigDecimal[] values) {
		
		try {
			
			BigDecimal sum = BigDecimal.ZERO;
			for (BigDecimal bd:values)
				sum = sum.add(bd);
			
			return sum.divide(new BigDecimal(values.length), 5, BigDecimal.ROUND_HALF_UP);
			
		} catch (NullPointerException e) {
			
			if (Settings.DEBUG_ON)
				e.printStackTrace();
			
			return BigDecimal.ZERO;
		}

	}
	
	
	private static BigDecimal getMin(BigDecimal[] values) {
		
		try {
			
			BigDecimal min = null;
			for (BigDecimal bd:values)
				if (min == null || bd.compareTo(min) == -1)
					min = bd;
					
			return (min == null) ? BigDecimal.ZERO : min;
			
		} catch (NullPointerException e) {
			
			if (Settings.DEBUG_ON)
				e.printStackTrace();
			
			return BigDecimal.ZERO;
		}

	}


	private static BigDecimal getMax(BigDecimal[] values) {
		
		try {
			
			BigDecimal max = null;
			for (BigDecimal bd:values)
				if (max == null || bd.compareTo(max) == 1)
					max = bd;
					
			return (max == null) ? BigDecimal.ZERO : max;
			
		} catch (NullPointerException e) {
			
			if (Settings.DEBUG_ON)
				e.printStackTrace();
			
			return BigDecimal.ZERO;
		}
		
		
		
	}
	

	
	/*private static double[] getValueListFromPropertyFile(String nameOfPropertyToLoadListFrom) {
		double[] result;
		String[] values = Settings.getProperty(nameOfPropertyToLoadListFrom).split(",");
		result = new double[values.length];
		for (int i=0; i<values.length; i++)
			result[i] = new Double(values[i]);
		return result;
	}
	*/
	
}
