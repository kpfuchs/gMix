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
package staticContent.evaluation.testbed.plan.node;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import staticContent.evaluation.testbed.deploy.coordinator.Coordinator;
import staticContent.evaluation.testbed.deploy.testnode.ITestNode;
import staticContent.evaluation.testbed.plan.global.GlobalExecutionPlan;

public class NodeExecutionPlan {
	protected Logger logger = Logger.getLogger(this.getClass());
	protected String realIpAddress;
	protected File sourceFile;
	protected Coordinator coordinator;
	protected ITestNode testnode;
	protected Set<Application> applications = new HashSet<Application>();
	protected boolean isInstalledOnTestnode = false;
	protected GlobalExecutionPlan globalPlan;
	
	public NodeExecutionPlan(GlobalExecutionPlan globalPlan, String realIpAddress, String sourceFilePath) {
		this(globalPlan, realIpAddress, new File(sourceFilePath));
	}

	public NodeExecutionPlan(GlobalExecutionPlan globalPlan, String realIpAddress, File sourceFile) {
		this.globalPlan = globalPlan;
		this.realIpAddress = realIpAddress;
		this.sourceFile = sourceFile;
		this.coordinator = Coordinator.getInstance();
	}
	
	public String getNodeIpAddress() {
		return realIpAddress;
	}
	
	public void setTestnode(ITestNode testnode) {
		this.testnode = testnode;
		
		for (Application app: applications) {
			app.setTestnode(testnode);
		}
	}
	
	public void addApplication(Application application) {
		applications.add(application);
	}
	
	public void execute(int runIndex) throws IOException {
		logger.debug("Setup node on: " + realIpAddress);
		
		// install sourceFile on testnode
		if (!isInstalledOnTestnode) {
			coordinator.InstallZipOnTestNode(testnode, sourceFile);
			isInstalledOnTestnode = true;
		}		
		
		logger.debug("Finished setup for node on: " + realIpAddress);
		
		for(Application application: applications) {
			application.execute(runIndex);
		}
	}

	public void configureVirtualIps(Set<String> virtualIps) throws NodeExecutionPlanException {
		if (virtualIps.isEmpty()) {
			throw new NodeExecutionPlanException("For the real node with IP: "+realIpAddress+" the set of virtual IPs is empty.");
		}

		/*  If number of virtual IPs is greater or equal the number of applications, every application gets a unique virtual IP.
		 *  Otherwise the number of applications per virtual IP is uniformly distributed.
		 */
		
		List<String> virtualIpsList = new ArrayList<String>(virtualIps);
		List<Application> applicationsList = new ArrayList<Application>(applications);

		if (virtualIps.size() >= applications.size()) {
			for (int i = 0; i < applications.size(); i++) {
				if (! (applicationsList.get(i) instanceof VirtualNodeApplication)) {
					continue;
				}
				
				VirtualNodeApplication virtualApplication = (VirtualNodeApplication) applicationsList.get(i);
				String virtualIp = virtualIpsList.get(i);
				
				virtualApplication.setVirtualIp(virtualIp);
				
				if (virtualApplication.isInfoService()) {
					globalPlan.setInfoServiceAddress(virtualIp);
				}
			}
		}
		else {
			int counter = 0;
			
			for (int i = 0; i < applications.size(); i++) {
				if (! (applicationsList.get(i) instanceof VirtualNodeApplication)) {
					continue;
				}
				
				if (counter == virtualIps.size()) {
					counter = 0;
				}
				
				VirtualNodeApplication virtualApplication = (VirtualNodeApplication) applicationsList.get(i);
				String virtualIp = virtualIpsList.get(counter);
				
				virtualApplication.setVirtualIp(virtualIp);
				
				if (virtualApplication.isInfoService()) {
					globalPlan.setInfoServiceAddress(virtualIp);
				}
				
				counter++;
			}
		}		
	}

	public void setInfoServiceAddress(String infoServiceAddress) {
		for(Application application: applications) {			
			application.setInfoServiceAddress(infoServiceAddress);
		}
	}

	public String getInfoServiceAddress() {
		for(Application application: applications) {			
			if (application.isInfoService()) {
				if (application instanceof VirtualNodeApplication) {
					return ((VirtualNodeApplication) application).getVirtualIp();
				}
				else {
					return application.getRealIp();
				}
			}
		}
		return null;
	}
}
