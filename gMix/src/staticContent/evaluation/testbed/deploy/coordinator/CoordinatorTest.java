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
package staticContent.evaluation.testbed.deploy.coordinator;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

import javax.rmi.ssl.SslRMIClientSocketFactory;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import staticContent.evaluation.testbed.deploy.utility.ConfigManager;
import staticContent.evaluation.testbed.deploy.utility.ConfigManager.Type;

public class CoordinatorTest{

	Registry registry;
	ConfigManager config;
	protected Logger logger = Logger.getLogger(this.getClass());

//	public void runTest() {
//		try {
//			Coordinator coordinator = new Coordinator();
//			//DiscoveryHelper disco = new DiscoveryHelper();
//
//			config = ConfigManager.getInstance(Type.COORDINATOR);
//
//			String srcDir = config.getAbsoluteFilePath(config.getString("srcDir"));
//
//			Set<String> setOfTestnodeNames = coordinator.getAvailableTestnodeNames(DiscoveryMode.MULTICAST);
////			Set<String> setOfTestnodeNames = coordinator.getAvailableTestnodeNames(DiscoveryMode.PRECONFIGURED);
////			Set<String> setOfTestnodeNames = coordinator.getAvailableTestnodeNames(DiscoveryMode.REGISTRY);
//
//			logger.debug("available nodes: "+Arrays.toString(setOfTestnodeNames.toArray()));
//
//			for(String testnodeName: setOfTestnodeNames) {
//				File sourceFile = new File(srcDir+"/test.jar");
//
//				coordinator.InstallOnTestNode(testnodeName, sourceFile);
//
//				logger.debug("installed on "+testnodeName+": "+sourceFile.getName());
//
//				String[] args = {"<installDir>", "1", "abc"};
//				String[] vmArgs = {};
//
//				coordinator.executeOnTestNode(testnodeName, "test.jar", "test.HelloWorld", args, vmArgs, null);
//
//				logger.debug("executed on "+testnodeName+": "+"test.HelloWorld");
//
//				logger.debug("running processes on "+testnodeName+": ");
//
//				Thread.sleep(1000);
//
////				for (Process p : coordinator.getProcesses(testnodeName)) {
////					logger.debug(p.getPid()+" "+p.getTestnodeName());
////					System.out.println();
////
////
////					p.kill();
////
////					logger.debug("killed process with pid "+p.getPid()+" on "+testnodeName);
////				}
//			}
//		} catch (Exception e) {
//			logger.error(e.getMessage(), e);
//		}
//	}

	public void runTest2() {
		try {
			config = ConfigManager.getInstance(Type.COORDINATOR);

			registry = LocateRegistry.getRegistry(config.getString("registryAddress"), config.getInt("registryPort"), new SslRMIClientSocketFactory());

			//IRegistryProxy registryProxy = (IRegistryProxy) registry.lookup("registryProxy");

			logger.debug("bound ids before: "+Arrays.toString(registry.list()));

		} catch (Exception e) {
			e.getCause().printStackTrace();
			logger.error(e.getMessage(), e);
		}
	}

//	public void runTest3() {
//		try {
//			Coordinator coordinator = new Coordinator();
//			//DiscoveryHelper disco = new DiscoveryHelper();
//
//			config = ConfigManager.getInstance(Type.COORDINATOR);
//
//			String srcDir = config.getAbsoluteFilePath(config.getString("srcDir"));
//
////			Set<ITestNode> setOfTestnodes = coordinator.getAvailableTestnodes(DiscoveryMode.MULTICAST);
////			Set<ITestNode> setOfTestnodes = coordinator.getAvailableTestnodes(DiscoveryMode.PRECONFIGURED);
//			Set<ITestNode> setOfTestnodes = coordinator.getAvailableTestnodes(DiscoveryMode.REGISTRY);
//
//			logger.debug("available nodes: "+Arrays.toString(setOfTestnodes.toArray()));
//
//			for(ITestNode testnode: setOfTestnodes) {
//				File sourceFile = new File(srcDir+"/test.jar");
//
//				String testnodeName = testnode.getName();
//
//				coordinator.InstallOnTestNode(testnode, sourceFile);
//
//				logger.debug("installed on "+testnodeName+": "+sourceFile.getName());
//
//				String[] args = {"<installDir>", "1", "abc"};
//				String[] vmArgs = {};
//
//				coordinator.executeOnTestNode(testnode, "test.jar", "test.HelloWorld", args, vmArgs, null);
//
//				logger.debug("executed on "+testnodeName+": "+"test.HelloWorld");
//
//				logger.debug("running processes on "+testnodeName+": ");
//
//				Thread.sleep(30000);
//
//				for (ProcessInfo p : coordinator.getProcesses(testnode)) {
//					logger.debug(p.getVpid()+" "+p.getTestnodeName());
//
//
//					p.kill();
//
//					logger.debug("killed process with pid "+p.getVpid()+" on "+testnodeName);
//				}
//			}
//
//			logger.debug("All running processes:");
//
//			for (ProcessInfo p : coordinator.getAllProcesses()) {
//				logger.debug(p.getVpid()+" "+p.getTestnodeName());
//			}
//		} catch (Exception e) {
//			logger.error(e.getMessage(), e);
//		}
//	}

	public static void main(String[] args) {
//		System.setProperty("javax.net.debug", "all");
//		System.setProperty("java.rmi.server.logCalls", "true");
		
		PropertyConfigurator.configure(ConfigManager.getDirOfExecutable()+"config/log4j.properties");
		
		ConfigManager config = ConfigManager.getInstance(Type.COORDINATOR);

		System.setProperty("java.rmi.server.hostname", config.getString("hostAddress"));
		System.setProperty("javax.net.ssl.keyStore", config.getAbsoluteFilePath(System.getProperty("user.dir") + config.getString("coordinatorKeystorePath")));
        System.setProperty("javax.net.ssl.keyStorePassword", config.getString("coordinatorKeystorePassword"));
        System.setProperty("javax.net.ssl.trustStore", config.getAbsoluteFilePath(System.getProperty("user.dir") +config.getString("coordinatorTruststorePath")));
        System.setProperty("javax.net.ssl.trustStorePassword", config.getString("coordinatorTruststorePassword"));

//		new CoordinatorStarter().runTest();

//		new CoordinatorStarter().runTest2();

//		new CoordinatorTest().runTest3();
	}

}
