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
package staticContent.evaluation.testbed.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import staticContent.evaluation.simulator.Simulator;
import staticContent.evaluation.simulator.core.ExperimentConfig;
import staticContent.evaluation.simulator.core.statistics.ResultSet;
import staticContent.evaluation.simulator.core.statistics.Statistics;
import staticContent.evaluation.testbed.deploy.utility.ConfigManager;
import staticContent.evaluation.testbed.deploy.utility.ConfigManager.Type;
import staticContent.evaluation.testbed.plan.XMLConfigReader;
import staticContent.evaluation.testbed.plan.global.GlobalExecutionPlan;
import staticContent.framework.config.Paths;
import staticContent.framework.config.Settings;
import userGeneratedContent.simulatorPlugIns.pluginRegistry.StatisticsType;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;


public class ExperimentSeries {
	
	protected ConfigManager config = ConfigManager.getInstance(Type.COORDINATOR);
	protected Logger logger = Logger.getLogger(this.getClass());
	protected String experimentConfigurationPath;
	
	
	public ExperimentSeries(String experimentConfigurationPath) {
		this.experimentConfigurationPath = experimentConfigurationPath;
	}

	
	/**
	 * Executes an experiment series specified with the given config file.
	 * 
	 * @param configFile
	 * 
	 * @throws InterruptedException
	 * @throws FileNotFoundException
	 * @throws NotBoundException
	 * @throws IOException
	 * @throws JSchException
	 * @throws SftpException
	 */
	public void execute(String configFile) throws InterruptedException, FileNotFoundException, NotBoundException, IOException, JSchException, SftpException {
		logger.info("Start experiment series");
		
		setGmixSpecificSettings();

		SingleExperiment exp;
		//		String configFilePath = System.getProperty("user.dir") +"/examples/test2experiment.xml";
		//		String configFilePath = System.getProperty("user.dir") +"/examples/test3experiment.xml";
		//		String configFilePath = System.getProperty("user.dir") +"/examples/test4experiment.xml";
		//		String configFilePath = System.getProperty("user.dir") +"/examples/test5experiment.xml";
		//		String configFilePath = System.getProperty("user.dir") +"/examples/test6experiment.xml";
		//		String configFilePath = System.getProperty("user.dir") +"/examples/test_experiment_modelnet_uni1.xml";
		//		String configFilePath = System.getProperty("user.dir") +"/examples/test_experiment_modelnet_uni2.xml";
		//		String configFilePath = System.getProperty("user.dir") +"/examples/test_experiment_real_uni1.xml";
		
		// set plan
		GlobalExecutionPlan plan = XMLConfigReader.createPlanV1(experimentConfigurationPath);
		
		switch(plan.getNetworkMode()) {
			case MODELNET:
//				System.err.println("remove me (was inserted to prevent execution of ModelnetExperiment)"); 
				exp = new ModelnetExperiment();
				break;
			case REAL:
			default:
				exp = new RealNetworkExperiment();
		}
		
		exp.setExecutionPlan(plan);
		
		ResultSet resultSet = createResultset(plan);
		
		for (int i=0; i < plan.getNumOfExperiments(); i++) {			
			exp.execute(i);
			
			switch(plan.getTerminationMode()) {
				case EVENT:
					EventListener el1 = EventListener.getInstance(exp);
					el1.listenTo(plan.getTerminationEvents());
					break;
				case TIME:
					Thread.sleep(plan.getExecutionTime()*1000);			
					exp.stop();
					break;
				case TIME_AND_EVENT:
					EventListener el2 = EventListener.getInstance(exp);
					el2.listenTo(plan.getTerminationEvents());
					Thread.sleep(plan.getExecutionTime()*1000);
					exp.stop();
					break;
			}
			
			resultSet = exp.getResultSet(resultSet);
			
			exp.reset();
			
			Thread.sleep(10000);
		}
		
		exp.cleanup();
		
		for (StatisticsType st: plan.getDesiredStatisticTypes()) {
			st.plotType.plot(resultSet);
		}
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		logger.info("Finished experiment series.");
		
		System.exit(1);
	}
	

	/**
	 * Creates a ResultSet instance, that can be filled with experiment results.
	 * The ResultSet instance is used from the plotter to plot the experiment results.
	 * 
	 * @param GlobalExecutionPlan plan
	 * @return ResultSet
	 */
	protected ResultSet createResultset(GlobalExecutionPlan plan) {
		Map<String, List<String>> variableParamsMap = plan.getVariableParameters();
		
		String firstParameterName = null;
		String[] firstParameterValues = null;		
		
		ExperimentConfig ep = new ExperimentConfig();
		
		Iterator<String> paramsKeySetIterator = variableParamsMap.keySet().iterator();
		
		firstParameterName = paramsKeySetIterator.next();
		firstParameterValues = new String[variableParamsMap.get(firstParameterName).size()];
		firstParameterValues = variableParamsMap.get(firstParameterName).toArray(firstParameterValues);
		
		if (variableParamsMap.size() > 1) {
			String secondParameterName = paramsKeySetIterator.next();
			String[] secondParameterValues = new String[variableParamsMap.get(secondParameterName).size()];
			secondParameterValues = variableParamsMap.get(secondParameterName).toArray(secondParameterValues);
			
			ep.useSecondPropertyToVary = true;
			ep.secondPropertyToVary = secondParameterName;
			ep.valuesForSecondProperty = secondParameterValues;
		}
		
		Calendar calendar = Calendar.getInstance();
		
		ep.propertyToVary = firstParameterName;
		ep.values = firstParameterValues;		
		ep.runs = 1;
		ep.desiredStatisticsTypes = plan.getDesiredStatisticTypes();		
		ep.experimentStart = "";		
		ep.experimentStart += calendar.get(Calendar.YEAR) + "-";
		ep.experimentStart += (calendar.get(Calendar.MONTH)+1) + "-";
		ep.experimentStart += calendar.get(Calendar.DAY_OF_MONTH) + "_";
		ep.experimentStart += calendar.get(Calendar.HOUR_OF_DAY) + "-";
		ep.experimentStart += calendar.get(Calendar.MINUTE) + "_";
		ep.experimentStart += calendar.get(Calendar.MILLISECOND);
		
		ResultSet resultSet = new ResultSet(ep);
		
		// set simulation time to 1000, then it is neutral	
		for (int i=0; i<ep.values.length; i++) {
			for (int j=0; j<ep.runs; j++) {
				resultSet.simulationTime[i][j] = 1000;
			}
		}
		
		return resultSet;
	}
	
	
	/**
	 * Sets some gmix project specific simulator and statistic settings.
	 * This settings are needed to use the statistic classes of the gmix simulator
	 * without executing a simulation. 
	 */
	public static void setGmixSpecificSettings() {
		Simulator.settings = new Settings(Paths.SIM_PROPERTY_FILE_PATH);
		
		Properties properties = new Properties();		
		properties.setProperty("CALC_AVG_OF_RUNS", "false");
		properties.setProperty("IS_INVERSE", "false");
		properties.setProperty("SIM_OUTPUT_FOLDER_PATH", "./inputOutput/testbed/output/");
		properties.setProperty("NAME_OF_PLOT_SCRIPT", "simguiPlotScript.txt");
		properties.setProperty("NONE_OVERWRITABLE_PARAMETERS", "");		
		
		Simulator.settings.addProperties(properties);
		
		Statistics.setRecordStatistics(true);
	}
	

	// test_experiment_real_fallbeispiel.xml
	public static void main(String[] args) {
		if (args == null || args.length == 0)
			throw new RuntimeException("missing parameter: please specify the name of the experiment script you want to execute as command line parameter.\nexiting...");  
		try {
			// Set log4j configuration file path
			PropertyConfigurator.configure(System.getProperty("user.dir") +"/inputOutput/testbed/config/log4j.properties");
			
			ConfigManager config = ConfigManager.getInstance(Type.COORDINATOR);
			
			ExperimentSeries es = new ExperimentSeries(System.getProperty("user.dir") +"/inputOutput/testbed/experimentDefinitions/" +args[0]);

			System.setProperty("javax.net.ssl.keyStore", config.getAbsoluteFilePath(System.getProperty("user.dir") +config.getString("coordinatorKeystorePath")));
	        System.setProperty("javax.net.ssl.keyStorePassword", config.getString("coordinatorKeystorePassword"));
	        System.setProperty("javax.net.ssl.trustStore", config.getAbsoluteFilePath(System.getProperty("user.dir") +config.getString("coordinatorTruststorePath")));
	        System.setProperty("javax.net.ssl.trustStorePassword", config.getString("coordinatorTruststorePassword"));	        
			
			es.execute("");		
					
		} catch (InterruptedException | JSchException | SftpException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.exit(1);
	}
	
}
