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
package staticContent.evaluation.testbed.plan.global;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import staticContent.evaluation.testbed.deploy.coordinator.Coordinator;
import staticContent.evaluation.testbed.deploy.testnode.ITestNode;
import staticContent.evaluation.testbed.plan.node.NodeExecutionPlan;
import staticContent.evaluation.testbed.plan.node.NodeExecutionPlanException;
import staticContent.evaluation.testbed.statistic.Metric;
import userGeneratedContent.simulatorPlugIns.pluginRegistry.StatisticsType;

public abstract class GlobalExecutionPlan {
	protected Logger logger = Logger.getLogger(this.getClass());
	protected Coordinator coordinator;
	
	public static enum Network_Mode {REAL, MODELNET};
	public static enum Termination_Mode {TIME, EVENT, TIME_AND_EVENT};
	
	protected Network_Mode networkMode;	
	protected Termination_Mode terminationMode;
	
	protected Map<String, NodeExecutionPlan> nodeIpPlans = new HashMap<String, NodeExecutionPlan>();
	protected Map<String,Set<String>> virtualIpAssignments = new HashMap<String,Set<String>>();
	protected Set<Metric> evalMetrics = new HashSet<Metric>();
	protected Map<String, List<String>> variableParams;
	protected Set<String> terminationEvents;
	
	protected Set<String> emulatorNodeNames;
	protected Set<String> testnodeNames;
		
	protected long executionTime;
	protected long settlingTime = -1;
	protected int numOfExperiments = 0;
	protected int currentRunIndex = -1;
	
	protected File realTopologyFile;
	protected File virtualTopologyFile;	
	
	public GlobalExecutionPlan(Network_Mode networkMode) {
		this.networkMode = networkMode;
		this.coordinator = Coordinator.getInstance();
	}
	
	public Set<String> getVirtualIpsPerNode(ITestNode testnode) throws RemoteException {		
		return virtualIpAssignments.get(testnode.getHostName());
	}
	
	public void execute(int runIndex) throws FileNotFoundException, IOException {
		this.currentRunIndex = runIndex;
		
		for(NodeExecutionPlan plan: nodeIpPlans.values()) {
			plan.execute(runIndex);
		}
	}
	
	public void addNodePlan(NodeExecutionPlan nodePlan) {
		nodeIpPlans.put(nodePlan.getNodeIpAddress(), nodePlan);
	}
	
	public Set<String> getPlannedNodeNames() {
		Set<String> result = new HashSet<String>();
		
		for (String address: getNodeAddresses()) {
			result.add("testnode_"+address);
		}
		
		return result;
	}

	public Set<String> getNodeAddresses() {
		return nodeIpPlans.keySet();
	}

	public void setTestnodes(Set<ITestNode> testnodes) throws RemoteException {
		for (ITestNode testnode: testnodes) {
			String address = testnode.getHostName();
			
			nodeIpPlans.get(address).setTestnode(testnode);
		}
	}
	
	public Network_Mode getNetworkMode() {
		return networkMode;
	}
	
	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
		
		if (terminationMode == null) {
			terminationMode = Termination_Mode.TIME;
		}
		else if (terminationMode == Termination_Mode.EVENT) {
			terminationMode = Termination_Mode.TIME_AND_EVENT;
		}
	}

	public long getExecutionTime() {
		return executionTime;
	}
	
	public void setEvalMetrics(Set<Metric> evalMetrics) {
		this.evalMetrics = evalMetrics;
	}
	
	public Set<Metric> getEvalMetrics() {
		return this.evalMetrics;
	}
	
	public StatisticsType[] getDesiredStatisticTypes() {
		StatisticsType[] result = new StatisticsType[evalMetrics.size()];
		Metric[] metricArray    = new Metric[evalMetrics.size()];
				 metricArray    = evalMetrics.toArray(metricArray);
		
		for (int i = 0; i < metricArray.length; i++) {
			result[i] = metricArray[i].statisticsTypeReference;
		}
		
		return result;
	}
	
	public void setNumOfExperiments(int num) {
		this.numOfExperiments = num;
	}
	
	public int getNumOfExperiments() {
		return numOfExperiments;
	}
	
	public void configureVirtualIps(Map<String, Set<String>> virtualIpConfig) throws NodeExecutionPlanException {
		this.virtualIpAssignments = virtualIpConfig;
		
		for(String realIp: nodeIpPlans.keySet()) {
			NodeExecutionPlan plan = nodeIpPlans.get(realIp);
			plan.configureVirtualIps(virtualIpConfig.get(realIp));
		}
	}
	
	public void setInfoServiceAddress(String infoServiceAddress) {
		logger.debug("Set InfoService address: "+infoServiceAddress);
		
		for(String realIp: nodeIpPlans.keySet()) {
			NodeExecutionPlan plan = nodeIpPlans.get(realIp);
			plan.setInfoServiceAddress(infoServiceAddress);
		}
	}
	
	public void setInfoServiceAddress() {
		setInfoServiceAddress(getInfoServiceAddress());
	}
	
	public String getInfoServiceAddress() {		
		for(String realIp: nodeIpPlans.keySet()) {
			NodeExecutionPlan plan = nodeIpPlans.get(realIp);
			String address = plan.getInfoServiceAddress();
			if (address != null) {
				return address;
			}
		}
		
		return null;
	}
	
	public void setRealTopologyFile(File f) {
		this.realTopologyFile = f;
	}

	public void setVirtualTopologyFile(File f) {
		this.virtualTopologyFile = f;
	}

	public File getRealTopologyFile() {
		return realTopologyFile;
	}

	public File getVirtualTopologyFile() {
		return virtualTopologyFile;
	}
	
	public int getCurrentRunIndex() {
		return currentRunIndex;
	}

	public void setVariableParameters(Map<String, List<String>> variableParams) {
		this.variableParams = variableParams;		
	}
	
	public Map<String, List<String>> getVariableParameters() {
		return variableParams;
	}
	
	public Termination_Mode getTerminationMode() {
		return terminationMode;
	}
	
	public void setTerminationEvents(Set<String> terminationEvents) {
		this.terminationEvents = terminationEvents;
		
		if (terminationMode == null) {
			terminationMode = Termination_Mode.EVENT;
		}
		else if (terminationMode == Termination_Mode.TIME) {
			terminationMode = Termination_Mode.TIME_AND_EVENT;
		}
	}
	
	public Set<String> getTerminationEvents() {
		if (terminationMode == Termination_Mode.TIME) {
			return null;
		}
		
		return terminationEvents;
	}
	
	public void setModelnetEmulatorNames(Set<String> names) {
		this.emulatorNodeNames = names;
	}
	
	public Set<String> getModelnetEmulatorNames() {
		return emulatorNodeNames;
	}
	
	public void setTestnodeNames(Set<String> names) {
		this.testnodeNames = names;
	}
	
	public Set<String> getTestnodeNames() {
		return testnodeNames;
	}	

	public long getSettlingTime() {
		return (settlingTime > 0) ? settlingTime : 0;
	}
	
	public void setSettlingTime(long settlingTime) {
		this.settlingTime = settlingTime;
	}
}
