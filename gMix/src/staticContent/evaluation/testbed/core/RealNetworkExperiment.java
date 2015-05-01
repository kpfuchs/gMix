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
import java.rmi.RemoteException;
import java.util.Set;

import staticContent.evaluation.testbed.deploy.coordinator.Coordinator.DiscoveryMode;
import staticContent.evaluation.testbed.deploy.registry.DiscoveryRegistry;
import staticContent.evaluation.testbed.deploy.testnode.ITestNode;

public class RealNetworkExperiment extends SingleExperiment {
	@Override
	protected boolean setupEnvironment() throws InterruptedException, FileNotFoundException, NotBoundException, IOException {
		logger.debug("Setup RealNetwork started.");
		
		Set<String> expectedNodeNames = plan.getPlannedNodeNames();
		Set<String> expectedNodeAddresses = plan.getNodeAddresses();		
		
		// start RMI registry
		DiscoveryRegistry.startRegistryServer();
		
		// trigger node registrations
		logger.debug("Trigger physical node registrations.");		
		coordinator.triggerNodesToRegister(expectedNodeAddresses);
		
		// wait for physical node registrations
		coordinator.waitForNodeRegistrations(expectedNodeNames, expectedNodeAddresses);
		
		Set<ITestNode> setOfTestnodes = coordinator.getAvailableTestnodes(DiscoveryMode.REGISTRY);
		
		plan.setTestnodes(setOfTestnodes);
		
		plan.setInfoServiceAddress();
		
		logger.debug("Finished setup.");
		return true;
	}

	@Override
	public void cleanup() throws RemoteException {
		logger.debug("Cleanup testnodes.");
		
		Set<ITestNode> setOfTestnodes = coordinator.getAvailableTestnodes(DiscoveryMode.REGISTRY);
		
		// TODO parallelize this
		for(ITestNode testnode: setOfTestnodes) {			
			// clear installDir
			testnode.executeCommand(new String[] {"/bin/bash", "-c", "/bin/rm -r <testbedRoot>/install/*"}, false);
		}
		
		logger.debug("Finished cleanup of testnodes.");
	}

}
