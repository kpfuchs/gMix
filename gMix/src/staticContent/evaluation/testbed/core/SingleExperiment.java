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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Set;

import org.apache.log4j.Logger;

import staticContent.evaluation.simulator.core.statistics.ResultSet;
import staticContent.evaluation.testbed.deploy.coordinator.Coordinator;
import staticContent.evaluation.testbed.deploy.coordinator.Coordinator.DiscoveryMode;
import staticContent.evaluation.testbed.deploy.testnode.ITestNode;
import staticContent.evaluation.testbed.deploy.utility.ConfigManager;
import staticContent.evaluation.testbed.deploy.utility.ConfigManager.Type;
import staticContent.evaluation.testbed.plan.global.GlobalExecutionPlan;
import staticContent.evaluation.testbed.statistic.Statistic;
import staticContent.framework.clock.Clock;
import staticContent.framework.config.Settings;
import userGeneratedContent.simulatorPlugIns.pluginRegistry.StatisticsType;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

import gnu.trove.TDoubleArrayList;

public abstract class SingleExperiment {
	protected ConfigManager config = ConfigManager.getInstance(Type.COORDINATOR);
	protected Coordinator coordinator;
	protected Logger logger = Logger.getLogger(this.getClass());	
	protected boolean isEnvironmentSetup = false;
	protected boolean isRunning = false;	
	protected long startTime = -1;
	protected long stopTime = -1;
	protected Clock clock;	
	protected GlobalExecutionPlan plan;	
	protected TDoubleArrayList[] calculatedResults; // [statisticsType]->[TDoubleArray:calculated value(s)]
	
	public SingleExperiment() {
		this.coordinator = Coordinator.getInstance();
		
		// create clock
		Settings clockSettings = new Settings(System.getProperty("user.dir") +"/inputOutput/anonNode/defaultConfig.txt");
		this.clock = new Clock(clockSettings);
	}
	
	/**
	 * Set execution plan of the experiment.
	 * 
	 * @param GlobalExecutionPlan plan
	 */
	public void setExecutionPlan(GlobalExecutionPlan plan) {
		this.plan = plan;
	}
	
	/**
	 * Prepares the experiment environment (emulator nodes and testnodes).
	 * 
	 * @return boolean - Returns true if the preparation was successfull, false otherwise.
	 * 
	 * @throws InterruptedException
	 * @throws RemoteException
	 * @throws FileNotFoundException
	 * @throws NotBoundException
	 * @throws IOException
	 * @throws JSchException
	 * @throws SftpException
	 */
	protected abstract boolean setupEnvironment() throws InterruptedException, RemoteException, FileNotFoundException, NotBoundException, IOException, JSchException, SftpException;
	
	/**
	 * 
	 * 
	 * @throws RemoteException
	 * 
	 * @throws FileNotFoundException
	 * @throws JSchException
	 * @throws SftpException
	 */
	public abstract void cleanup() throws RemoteException, FileNotFoundException, JSchException, SftpException;
	
	protected void init() throws InterruptedException, FileNotFoundException, NotBoundException, IOException, JSchException, SftpException {		
		if (!this.isEnvironmentSetup) { 
			setupEnvironment();
			this.isEnvironmentSetup = true;
		}
		
		cleanEnvironment();
	}
	
	protected boolean cleanEnvironment() throws RemoteException {
		logger.debug("Cleanup environment.");
		
		Set<ITestNode> setOfTestnodes = coordinator.getAvailableTestnodes(DiscoveryMode.REGISTRY);
		
		// TODO parallelize this
		for(ITestNode testnode: setOfTestnodes) {
			testnode.killAllProcesses();			
			logger.debug("Killed processes on: " + testnode.getHostName());
		}
		
		logger.debug("Cleanup finished.");
		return true;
	}
	
	protected void collectSensorData() throws RemoteException{
		logger.debug("Collect sensor data.");
		
		Set<ITestNode> setOfTestnodes = coordinator.getAvailableTestnodes(DiscoveryMode.REGISTRY);
		
		// TODO parallelize this
		for(ITestNode testnode: setOfTestnodes) {
			// get terminated processes on testnode
			Set<Integer> vpids = coordinator.getStartedProcessesOnNode(testnode);
			
			// fetch sensor data for all processes
			// TODO parallelize this
			for(int vpid: vpids) {				
				String fileName = "sensor-"+vpid+".log";				
				coordinator.copySensorFileFromTestnode(testnode, vpid, fileName);
				
				// TODO calculate statistics on process base
			}		
			
			// delete sensor data files on testnode
			testnode.deleteAllLogFiles();
		}		
		
		logger.debug("Finished collection of sensor data.");
	}
	
	protected void calculateStatistics() {
		logger.debug("Calculating statistics.");
		String sensorDir = System.getProperty("user.dir") +"/inputOutput/testbed/tmp";
		
		Statistic stat = new Statistic(plan);
		
		FilenameFilter filter = new FilenameFilter() {
	        @Override
	        public boolean accept(File dir, String name) {
	            return name.toLowerCase().startsWith("sensor") && name.toLowerCase().endsWith(".log");
	        }
	    };
		
		File[] sensorFiles = new File(sensorDir).listFiles(filter);
		
		for (int i = 0; i < sensorFiles.length; i++) {
			File sensorFile = sensorFiles[i];
			
			try {
				stat.readInFile(sensorFile, startTime, stopTime);
			} catch (FileNotFoundException e) {
				logger.error("Sensor file not found.", e);
			}
			
			// delete after processing
			sensorFile.delete();
		}
		
		calculatedResults = stat.calculate(plan.getCurrentRunIndex());
		
		logger.debug("Finished calculation of statistics.");
	}
	
	public void stop() throws RemoteException {
		if (isRunning) {
			stopTime = clock.getTime();
			
			cleanEnvironment();
			
			collectSensorData();
			
			// calculate statistics on experiment base
			calculateStatistics();
			
			// reset startTime and stopTime
			startTime = -1;
			stopTime = -1;
			
			// reset coordinator state
			coordinator.reset();
			
			logger.info("Finished experiment.");
			
			isRunning = false;
		}
	}

	public boolean execute(int runIndex) throws FileNotFoundException, IOException, SftpException, InterruptedException, NotBoundException, JSchException {
		if (!isRunning) {
			init();
			
			startTime = clock.getTime() + plan.getSettlingTime();
			
			logger.info("Start experiment with run index: " + runIndex);			
			
			plan.execute(runIndex);	
			
			isRunning = true;
		}		
		
		return true;
	}

	public ResultSet getResultSet(ResultSet resultSet) {
		TDoubleArrayList[][][] results = resultSet.results;
		
		for (StatisticsType st: plan.getDesiredStatisticTypes()) {
			results[plan.getCurrentRunIndex()][st.ordinal()][0] = calculatedResults[st.ordinal()];
		}
		
		return resultSet;
	}

	public void reset() {
		calculatedResults = null;		
	}

}
