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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

import staticContent.evaluation.testbed.deploy.coordinator.Coordinator.DiscoveryMode;
import staticContent.evaluation.testbed.deploy.registry.DiscoveryRegistry;
import staticContent.evaluation.testbed.deploy.testnode.ITestNode;
import staticContent.evaluation.testbed.deploy.utility.SimpleSSHClient;
import staticContent.evaluation.testbed.deploy.utility.SimpleSftpClient;
import staticContent.evaluation.testbed.plan.XMLConfigReader;
import staticContent.evaluation.testbed.plan.node.NodeExecutionPlanException;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

public class ModelnetExperiment extends SingleExperiment {
	boolean isModelFileGenerated = false;
	boolean isRouteFileGenerated = false;
	
	@Override
	protected boolean setupEnvironment() throws InterruptedException, FileNotFoundException, NotBoundException, IOException, JSchException, SftpException {
		logger.debug("Setup Modelnet started.");
	
		Set<String> expectedNodeNames = plan.getPlannedNodeNames();
		Set<String> expectedNodeAddresses = plan.getNodeAddresses();
		
		
		// start RMI registry
		DiscoveryRegistry.startRegistryServer();
		
		// trigger node registrations
		logger.debug("Trigger physical node registrations.");
		coordinator.triggerNodesToRegister(expectedNodeAddresses);
		
		// wait for physical node registrations
		coordinator.waitForNodeRegistrations(expectedNodeNames, expectedNodeAddresses);
		
		// distribute code
		Set<ITestNode> setOfTestnodes = coordinator.getAvailableTestnodes(DiscoveryMode.REGISTRY);
		
		ITestNode firstTestnode = setOfTestnodes.iterator().next();
		
		plan.setTestnodes(setOfTestnodes);
		
		// copy Modelnet real and virtual topology to lastTestnode to generate model and route file
		coordinator.InstallOnTestNode(firstTestnode, plan.getRealTopologyFile(), "real_topology.xml");
		coordinator.InstallOnTestNode(firstTestnode, plan.getVirtualTopologyFile(), "virtual_topology.xml");
		
		// generate modelnet route file
		File modelnetRouteFile = generateModelnetRouteFile(firstTestnode);
		
		// generate modelnet model file
		File modelnetModelFile = generateModelnetModelFile(firstTestnode);
		
		// extract virtual IP settings from model file
		Map<String, Set<String>> map = XMLConfigReader.createNodeVirtualIpAssignment(modelnetModelFile);
		
		try {
			plan.configureVirtualIps(map);
		} catch (NodeExecutionPlanException e) {
			logger.error("Configuration of virtual IPs failed.", e);
		}
		
		for(ITestNode testnode: setOfTestnodes) {
			String testnodeName = testnode.getName();
			
			logger.debug("Setup node on: " + testnode.getHostName() + " node name: " + testnodeName);
			
			// install modelnet files
			coordinator.InstallOnTestNode(testnodeName, modelnetModelFile);
			coordinator.InstallOnTestNode(testnodeName, modelnetRouteFile);
			
			// deploy modelnet configuration on testnode
			deployModelnet(testnode);
			
			logger.debug("Finished setup for node on: " + testnode.getHostName());
		}
		
		// send Modelnet model and route file also to emulators
		sendModelnetConfigurationToEmulators();
		
		// deploy on emulators
		deployModelnetConfigurationOnEmulators();
		
		logger.debug("Finished setup.");
		return true;
	}

	@Override
	public void cleanup() throws RemoteException, FileNotFoundException, JSchException, SftpException {		
		logger.debug("Cleanup testnodes.");
		
		Set<ITestNode> setOfTestnodes = coordinator.getAvailableTestnodes(DiscoveryMode.REGISTRY);
		
		// TODO parallelize this
		for(ITestNode testnode: setOfTestnodes) {			
			// delete all created modelnet virtual network interfaces and routes on testnode
			testnode.executeCommand(new String[] {"/bin/bash", "-c", "<testbedRoot>/scripts/cleanUpModelnetHost"}, false);
			
			// clear installDir
			testnode.executeCommand(new String[] {"/bin/bash", "-c", "/bin/rm -r <testbedRoot>/install/*"}, false);
		}
		
		removeModelnetConfigurationOnEmulators();
		
		logger.debug("Finished cleanup of testnodes.");
	}
	
	/**
	 * Deploys the Modelnet configuration of the experiment on the given testnode.
	 * 
	 * @param ITestNode testnode
	 * @throws RemoteException
	 */
	protected void deployModelnet(ITestNode testnode) throws RemoteException {
		logger.debug("Deploy Modelnet setup on the cluster node: " + testnode.getName());
		
		// deploy Modelnet on the cluster
		String[] modelnetCommand = {"/usr/local/bin/deployhost", "<testbedRoot>/install/virtual_model.xml", "<testbedRoot>/install/virtual_route.xml"};
		testnode.executeCommand(modelnetCommand, true);		
		
		if (!isModelnetDeployed(testnode)) {
			logger.error("Modelnet deployment failed. Try again ...");
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			
			deployModelnet(testnode);
		}
		
		logger.debug("Modelnet deployment finished.");
	}
	
	/**
	 * Returns true if the Modelnet configuration of the experiment is successfully deployed on the given testnode.
	 * 
	 * @param ITestNode testnode
	 * @return boolean
	 * @throws RemoteException
	 */
	protected boolean isModelnetDeployed(ITestNode testnode) throws RemoteException {				
		Set<String> assignedIps = testnode.getAssignedVirtualAddresses();
		Set<String> plannedIps = plan.getVirtualIpsPerNode(testnode);		
		
		return assignedIps.containsAll(plannedIps);
	}

	protected void sendModelnetConfigurationToEmulators() throws JSchException, FileNotFoundException, SftpException {
		logger.debug("Send Modelnet configuration to emulator nodes.");
		
		String tmpDir = System.getProperty("user.dir") +"/inputOutput/testbed/tmp";
		String hostProjectRootDir = config.getString("hostProjectRoot");
		
		// TODO parallelize this
		for (String emulatorName: plan.getModelnetEmulatorNames()) {
			logger.debug("Send Modelnet configuration to emulator node "+emulatorName+".");
			
			String username = config.getString(emulatorName+"SSHUser");
			String password = config.getString(emulatorName+"SSHPassword");
			
			SimpleSftpClient client = new SimpleSftpClient(username, password, emulatorName, 22);
			
			client.putFile(tmpDir + "/virtual_model.xml", hostProjectRootDir +"/inputOutput/testbed/install");
			client.putFile(tmpDir + "/virtual_route.xml", hostProjectRootDir +"/inputOutput/testbed/install");
			
			client.disconnect();
		}
	}
	
	protected void removeModelnetConfigurationOnEmulators() throws JSchException, FileNotFoundException, SftpException {
		logger.debug("Remove Modelnet configuration from emulator nodes.");
		
		String hostProjectRootDir = config.getString("hostProjectRoot");		
		
		// TODO parallelize this
		for (String emulatorName: plan.getModelnetEmulatorNames()) {
			logger.debug("Remove Modelnet configuration from emulator node "+emulatorName+".");
			
			String username = config.getString(emulatorName+"SSHUser");
			String password = config.getString(emulatorName+"SSHPassword");
			
			SimpleSftpClient client = new SimpleSftpClient(username, password, emulatorName, 22);
			
			client.removeFile(hostProjectRootDir+"/inputOutput/testbed/install", "virtual_model.xml");
			client.removeFile(hostProjectRootDir+"/inputOutput/testbed/install", "virtual_route.xml");
			
			client.disconnect();
		}
	}
	
	public void generateModelnetRealTopologyFile() {
		Set<String> emulatorNodes = plan.getModelnetEmulatorNames();
		Set<String> testnodes = plan.getTestnodeNames();
		
		String topologyFileName = System.getProperty("user.dir") +"/inputOutput/testbed/tmp/real_topology.xml";
		
		Writer writer = null;

		try {
		    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(topologyFileName), "utf-8"));
		    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		    writer.write("<hardware>\n");
		    
		    for (String emulatorNodeName: emulatorNodes) {
		    	writer.write("<emul hostname=\""+emulatorNodeName+"\" />\n");
		    }
		    
		    for (String testnodeName: testnodes) {
		    	writer.write("<host hostname=\""+testnodeName+"\" />\n");
		    }
		    
		    writer.write("</hardware>\n");
		} catch (IOException ex) {
		  // report
		} finally {
		   try {writer.close();} catch (Exception ex) {}
		}
		
		plan.setRealTopologyFile(new File(topologyFileName));
	}
	
	protected void deployModelnetConfigurationOnEmulators() throws JSchException {
		logger.debug("Deploy Modelnet configuration on emulator nodes.");
		
		String hostProjectRootDir = config.getString("hostProjectRoot");
		
		// TODO parallelize this
		for (String emulatorName: plan.getModelnetEmulatorNames()) {
			logger.debug("Deploy Modelnet configuration on emulator node "+emulatorName+".");
			
			String username = config.getString(emulatorName+"SSHUser");
			String password = config.getString(emulatorName+"SSHPassword");
			
			if (username == null || password == null) {
				
			}
			
			SimpleSSHClient client = new SimpleSSHClient(username, password, emulatorName, 22);
			
			client.executeCommand("/usr/local/bin/deployhost "+hostProjectRootDir+"/inputOutput/testbed/install/virtual_model.xml "+hostProjectRootDir+"/inputOutput/testbed/install/virtual_route.xml >> /var/log/gmixTest.log");
			
			client.disconnect();
		}		
	}
	
	protected File generateModelnetModelFile(ITestNode testnode) throws RemoteException {
		logger.debug("Generate Modelnet model file on cluster node: " + testnode.getName());
		
		// deploy Modelnet on the cluster
		String[] modelnetCommand = {"/bin/bash", "-c", "/usr/local/bin/mkmodel <testbedRoot>/install/virtual_topology.xml <testbedRoot>/install/real_topology.xml > <testbedRoot>/install/virtual_model.xml"};
		testnode.executeCommand(modelnetCommand, true);

		File result = coordinator.copyFileFromTestnode(testnode, "<testbedRoot>/install/virtual_model.xml", "virtual_model.xml");
		
		if (result != null) {
			isModelFileGenerated = true;
		}
		
		if (!isModelFileGenerated) {
			logger.error("Generation of Modelnet model file failed. Try again ...");
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			
			result = generateModelnetModelFile(testnode);
		}
		
		logger.debug("Generation of Modelnet model file finished.");
		
		return result;
	}
	
	protected File generateModelnetRouteFile(ITestNode testnode) throws RemoteException {
		logger.debug("Generate Modelnet route file on cluster node: " + testnode.getName());
		
		// deploy Modelnet on the cluster
		String[] modelnetCommand = {"/bin/bash", "-c", "/usr/local/bin/allpairs <testbedRoot>/install/virtual_topology.xml > <testbedRoot>/install/virtual_route.xml"};
		testnode.executeCommand(modelnetCommand, true);
		
		File result = coordinator.copyFileFromTestnode(testnode, "<testbedRoot>/install/virtual_route.xml", "virtual_route.xml");
		
		if (result != null) {
			isRouteFileGenerated = true;
		}
		
		if (!isRouteFileGenerated) {
			logger.error("Generation of Modelnet route file failed. Try again ...");
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			
			result = generateModelnetRouteFile(testnode);
		}
		
		logger.debug("Generation of Modelnet route file finished.");
		
		return result;
	}
}
