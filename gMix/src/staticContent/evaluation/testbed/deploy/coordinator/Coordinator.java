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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.rmi.ssl.SslRMIClientSocketFactory;

import org.apache.log4j.Logger;

import staticContent.evaluation.testbed.deploy.discovery.DiscoveryHelper;
import staticContent.evaluation.testbed.deploy.process.ProcessFactory;
import staticContent.evaluation.testbed.deploy.process.ProcessInfo;
import staticContent.evaluation.testbed.deploy.testnode.ITestNode;
import staticContent.evaluation.testbed.deploy.utility.ConfigManager;
import staticContent.evaluation.testbed.deploy.utility.ConfigManager.Type;

import com.healthmarketscience.rmiio.GZIPRemoteInputStream;
import com.healthmarketscience.rmiio.RemoteInputStream;
import com.healthmarketscience.rmiio.RemoteInputStreamClient;
import com.healthmarketscience.rmiio.RemoteInputStreamServer;

public class Coordinator implements ICoordinator {

	/**
	 * Cached testnodes
	 */
	private Map<String, ITestNode> testnodes;
	private DiscoveryHelper discHelper;
	private ConfigManager config;
	private Logger logger = Logger.getLogger(this.getClass());
	private int vpidCounter = 0;
	
	private Map<ITestNode,Set<Integer>> startedProcesses;

	public enum DiscoveryMode {
		MULTICAST, PRECONFIGURED, REGISTRY
	}

	/**
	 * Do not use it directly. Use the getRegistry method.
	 */
	private Registry registry;

	protected static Coordinator instance = null;

	/**
	 * Constructs a new Coordinator instance.
	 *
	 */
	protected Coordinator() {
		discHelper = new DiscoveryHelper();
		testnodes = new HashMap<String, ITestNode>();
		config = ConfigManager.getInstance(Type.COORDINATOR);
		startedProcesses = new ConcurrentHashMap<ITestNode,Set<Integer>>();
	}
	
	public static Coordinator getInstance() {
		if (instance == null) {
			instance = new Coordinator();
		}
		return instance;
	}

	private String getNextVPID(){
		synchronized (this) {
			vpidCounter++;
			return vpidCounter + "";
		}
	}

	/**
	 * Returns a reference of the registry.
	 *
	 * @return a registry instance
	 *
	 * @throws RemoteException - if no connection to the registry couldn't be established
	 */
	public Registry getRegistry() throws RemoteException {
		if (registry == null) {
			registry = LocateRegistry.getRegistry(config.getString("registryAddress"), config.getInt("registryPort"), new SslRMIClientSocketFactory());
		}
		else {
			// check if there is a connection
			try {
				// TODO: Ist das nicht overkill?
				registry.list();
			} catch (Exception e) {
				registry = LocateRegistry.getRegistry(config.getString("registryAddress"), config.getInt("registryPort"), new SslRMIClientSocketFactory());
			}
		}

		return registry;
	}

	/**
	 * Returns a testnode instance with the given name.
	 *
	 * @param testNodeName - name of the wanted testnode
	 *
	 * @return a testnode instance
	 *
	 * @throws RemoteException - if remote communication with the registry failed
	 */
	public ITestNode getTestnode(String testNodeName) throws RemoteException {
		Registry registryReference = getRegistry();
		try {
			return (ITestNode)registryReference.lookup(testNodeName);
		} catch (NotBoundException e) {
			logger.debug("Testnode "+testNodeName+" is not registered. Trigger registration again.");
			
			// TODO Trigger node registration again
			logger.debug("Trigger testnode currently not implemented.");
			
			return getTestnode(testNodeName);
		}
	}

	/**
	 * Returns a set of available testnode names. The available testnodes are
	 * determined by a discovery process. With the parameter mode you can specify
	 * in whitch way the discovery process is executed.
	 *
	 * @param mode a mode for discovery of the testnodes<br><br>
	 *
	 * 	MULTICAST     - testnodes are determined by multicast discovery (works only in a local subnet)<br>
	 * 	PRECONFIGURED - testnodes are determined by a preconfigured list of node addresses in the config file<br>
	 * 	REGISTRY      - testnodes are determined by asking the registry which testnodes have registered<br>
	 *
	 * @return a set of testnode names
	 */
	public Set<String> getAvailableTestnodeNames(DiscoveryMode mode) {
		Set<String> discoveredNodes = new HashSet<String>();

		try {
			switch(mode) {
				case REGISTRY:
					discoveredNodes = discHelper.getAvailableNodesFromRegistry();
					break;
				case MULTICAST:
					discoveredNodes = discHelper.getAvailableNodesViaMulticast();
					break;
				case PRECONFIGURED:
					List<String> preconfiguredNodes = Arrays.asList(config.getStringArray("testNodeAddress"));
					discoveredNodes = discHelper.getAvailableNodesWithPreconfiguredList(preconfiguredNodes);
					break;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return discoveredNodes;
	}

	/**
	 * Returns a set of available testnode instances. The available testnodes are
	 * determined by a discovery process. With the parameter mode you can specify
	 * in which way the discovery process is executed.
	 *
	 * @param mode a mode for discovery of the testnodes<br><br>
	 *
	 * 	MULTICAST     - testnodes are determined by multicast discovery (works only in a local subnet)<br>
	 * 	PRECONFIGURED - testnodes are determined by a preconfigured list of node addresses in the config file<br>
	 * 	REGISTRY      - testnodes are determined by asking the registry which testnodes have registered<br>
	 *
	 * @return a set of testnode instances
	 * @throws RemoteException
	 */
	public Set<ITestNode> getAvailableTestnodes(DiscoveryMode mode) throws RemoteException {
		Set<ITestNode> result = new HashSet<ITestNode>();
		Registry reg = getRegistry();

		synchronized (this) {
			testnodes.clear();

			for (String testnodeName: getAvailableTestnodeNames(mode)) {
				// TODO parallelize this with threads

				try {
					ITestNode tn = (ITestNode) reg.lookup(testnodeName);

					result.add(tn);
					testnodes.put(testnodeName, tn);
					
					if (!startedProcesses.containsKey(tn) || startedProcesses.get(tn) == null) {
						startedProcesses.put(tn, new HashSet<Integer>());
					}					
				} catch (NotBoundException | RemoteException e) {
					logger.error("Could not find remote Testnode object in registry " + registry + "for name " + testnodeName + "!", e);
					// Could not find the testnode -> try to unbind
					unbindTestNode(testnodeName);
				}
			}
		}

		return result;
	}

	/**
	 * Returns a set of available testnode instances. The available testnodes are
	 * determined by asking the registry which testnodes have registered.
	 *
	 * @return a set of testnode instances
	 * @throws RemoteException
	 */
	public Set<ITestNode> getAvailableTestnodes() throws RemoteException {
		Set<ITestNode> result = new HashSet<ITestNode>();
		Registry reg = getRegistry();

		synchronized (this) {
			testnodes.clear();

			for (String testnodeName: reg.list()) {
				// only testnodes
				if(!testnodeName.startsWith("testnode_"))
					continue;
				// TODO parallelize this with threads

				try {
					ITestNode tn = (ITestNode) reg.lookup(testnodeName);

					result.add(tn);
					testnodes.put(testnodeName, tn);
					
					if (!startedProcesses.containsKey(tn) || startedProcesses.get(tn) == null) {
						startedProcesses.put(tn, new HashSet<Integer>());
					}	
				} catch (Exception e) {
					logger.error("Could not find remote Testnode object in registry " + registry + "for name " + testnodeName + "!", e);
					// Could not find the testnode -> try to unbind
					unbindTestNode(testnodeName);
				}
			}
		}

		return result;
	}

	/**
	 * Tries to contact Testnodes via the given mode and orders them to register with the registry.
	 *
	 * @param mode
	 */
	public void discoverTestnodes(DiscoveryMode mode)
	{
		try {
			switch(mode) {
				case REGISTRY:
					// nodes are already in registry -> nothing to do
					break;
				case MULTICAST:
					// TODO: Die Methode braucht eigentlich keine Rückgabe. Danach können die Testnodes aus der Registry geholt werden.
					discHelper.getAvailableNodesViaMulticast();
					break;
				case PRECONFIGURED:
					List<String> preconfiguredNodes = Arrays.asList(config.getStringArray("testNodeAddress"));
					// TODO: Die Methode braucht eigentlich keine Rückgabe. Danach können die Testnodes aus der Registry geholt werden.
					discHelper.getAvailableNodesWithPreconfiguredList(preconfiguredNodes);
					break;
			}
		} catch (Exception e) {
			logger.error("Discovery of testnodes failed in mode "+mode+"!", e);
		}
	}

	/**
	 * Copies the given file to the testnode with the given name.
	 *
	 * @param testNodeName name of the testnode
	 * @param f file to copy
	 * @throws NotBoundException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void InstallOnTestNode(String testNodeName, File f) throws NotBoundException, FileNotFoundException, IOException {
		ITestNode testNode = getTestnode(testNodeName);

		RemoteInputStreamServer istream = null;
		try {
			istream = new GZIPRemoteInputStream(new BufferedInputStream(
					new FileInputStream(f)));
			// call server (note export() call to get actual remote interface)
			testNode.installFile(f.getName(), istream.export());
		} finally {
			// since the server should have consumed the stream in the
			// sendFile()
			// call, we always want to close the stream
			if (istream != null)
				istream.close();
		}
	}
	
	/**
	 * Copies the given file to the testnode with the given name.
	 *
	 * @param testNodeName name of the testnode
	 * @param f file to copy
	 * @throws NotBoundException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void InstallZipOnTestNode(String testNodeName, File f) throws NotBoundException, FileNotFoundException, IOException {
		ITestNode testNode = getTestnode(testNodeName);

		RemoteInputStreamServer istream = null;
		try {
			istream = new GZIPRemoteInputStream(new BufferedInputStream(
					new FileInputStream(f)));
			// call server (note export() call to get actual remote interface)
			testNode.installZipFile(f.getName(), istream.export());
		} finally {
			// since the server should have consumed the stream in the
			// sendFile()
			// call, we always want to close the stream
			if (istream != null)
				istream.close();
		}
	}
	
	/**
	 * Copies the given file to the testnode with the given name.
	 *
	 * @param testNodeName name of the testnode
	 * @param f file to copy
	 * @throws NotBoundException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void InstallZipOnTestNode(ITestNode testNode, File f) throws FileNotFoundException, IOException {
		RemoteInputStreamServer istream = null;
		try {
			istream = new GZIPRemoteInputStream(new BufferedInputStream(
					new FileInputStream(f)));
			// call server (note export() call to get actual remote interface)
			testNode.installZipFile(f.getName(), istream.export());
		} finally {
			// since the server should have consumed the stream in the
			// sendFile()
			// call, we always want to close the stream
			if (istream != null)
				istream.close();
		}
	}

	/**
	 * Copies the given file to the given testnode.
	 *
	 * @param testNode reference of the testnode
	 * @param f file to copy
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void InstallOnTestNode(ITestNode testNode, File f) throws FileNotFoundException, IOException {
		InstallOnTestNode(testNode, f, f.getName());
	}
	
	/**
	 * Copies the given file to the given testnode.
	 *
	 * @param testNode reference of the testnode
	 * @param f file to copy
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void InstallOnTestNode(ITestNode testNode, File f, String filenameOnTarget) throws FileNotFoundException, IOException {
		RemoteInputStreamServer istream = null;
		try {
			istream = new GZIPRemoteInputStream(new BufferedInputStream(
					new FileInputStream(f)));
			// call server (note export() call to get actual remote interface)
			testNode.installFile(filenameOnTarget, istream.export());
		} finally {
			// since the server should have consumed the stream in the
			// sendFile()
			// call, we always want to close the stream
			if (istream != null)
				istream.close();
		}
	}

	/**
	 * Executes the class with the given className, that is located in the given classPath
	 * with the given arguments on the testnode with the given name.
	 *
	 * @param testNodeName name of the testnode
	 * @param classPath class path
	 * @param className name of the class
	 * @param args arguments
	 *
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	public void executeOnTestNode(String testNodeName, String classPath, String className, Map<String,String> args, Map<String,String> vmArgs, Map<String,String> environmentVariables) throws RemoteException, NotBoundException {
		ITestNode testNode = getTestnode(testNodeName);
		executeOnTestNode(testNode, classPath, className, args, vmArgs, environmentVariables);
	}

	/**
	 * Executes the class with the given className, that is located in the given classPath
	 * with the given arguments on the given testnode.
	 *
	 * @param testNode reference of the testnode
	 * @param classPath class path
	 * @param className name of the class
	 * @param args arguments
	 *
	 * @throws RemoteException
	 */
	public void executeOnTestNode(ITestNode testNode, String classPath, String className, Map<String,String> args, Map<String,String> vmArgs, Map<String,String> environmentVariables) throws RemoteException {
		String vpid = getNextVPID();
		
		if (startedProcesses.get(testNode) == null) {
			startedProcesses.put(testNode, new HashSet<Integer>());
		}
		
		startedProcesses.get(testNode).add(Integer.parseInt(vpid));
		testNode.execute(vpid, classPath, className, args, vmArgs, environmentVariables);
	}

	/**
	 * Executes the class with the given className, that has the same name than the given file
	 * with the given arguments on the given testnode.
	 *
	 * @param testNode reference of the testnode
	 * @param file local file pointer
	 * @param className name of the class
	 * @param args arguments
	 *
	 * @throws RemoteException
	 */
	public void executeOnTestNode(ITestNode testNode, File file, String className, Map<String,String> args, Map<String,String> vmArgs, Map<String,String> environmentVariables) throws RemoteException {
		String vpid = getNextVPID();
		
		if (startedProcesses.get(testNode) == null) {
			startedProcesses.put(testNode, new HashSet<Integer>());
		}
		
		startedProcesses.get(testNode).add(Integer.parseInt(vpid));
		testNode.execute(vpid, file.getName(), className, args, vmArgs, environmentVariables);
	}

	/**
	 * Returns a set of process instances that are running on the testnode with the given name.
	 *
	 * @param testNodeName - name of a testnode
	 *
	 * @return set of processes
	 */
	public Set<ProcessInfo> getProcesses(String testNodeName) {
		Set<ProcessInfo> result = new HashSet<ProcessInfo>();

		try {
			ITestNode testnode = getTestnode(testNodeName);

			String processJsonString = testnode.getRunningProcesses();

			result = ProcessFactory.getProcessesFromJson(processJsonString, testNodeName, testnode);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return result;
	}

	/**
	 * Returns a set of process instances that are running on the given testnode.
	 *
	 * @param testNodeName - reference of the testnode
	 *
	 * @return set of processes
	 */
	public Set<ProcessInfo> getProcesses(ITestNode testnode) {
		Set<ProcessInfo> result = new HashSet<ProcessInfo>();

		try {
			String processJsonString = testnode.getRunningProcesses();

			result = ProcessFactory.getProcessesFromJson(processJsonString, testnode.getName(), testnode);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return result;
	}

	/**
	 * Returns a set of process instances that are running on all known testnodes.
	 *
	 * @return  set of processes
	 */
	public Set<ProcessInfo> getAllProcesses() throws RemoteException {
		Set<ProcessInfo> result = new HashSet<ProcessInfo>();

		for (String testnodeName : testnodes.keySet()) {
			try {
				// TODO parallelize this with threads

				ITestNode testnode = testnodes.get(testnodeName);

				String processJsonString = testnode.getRunningProcesses();

				result.addAll(ProcessFactory.getProcessesFromJson(processJsonString, testnodeName, testnode));
			} catch (RemoteException re) {
				logger.error("Could not access testnode: " + testnodeName, re);
				unbindTestNode(testnodeName);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}

		return result;
	}

	private void unbindTestNode(String testnodeName)
	{
		try
		{
			getRegistry().unbind(testnodeName);
		} catch (Exception ex) {
			logger.error("Unbinding failed:", ex);
		}
	}

	public String getMessageLog(ITestNode node, String vpid) throws RemoteException, IOException {
		String result = null;
		InputStream istream = null;
		try{
			istream = RemoteInputStreamClient.wrap(node.getLogFileStream(vpid));
			result = convertStreamToString(istream);
		}
		catch(Exception e)
		{
			logger.error("Log File reading failed!",e);
		}
		finally
		{
			if(istream != null)
				istream.close();
		}
		return result;
	}


	private static String convertStreamToString(java.io.InputStream is) {
	    Scanner s = new java.util.Scanner(is);
	    s.useDelimiter("\\A");
	    s.close();
	    return s.hasNext() ? s.next() : "";
	}

	public ConfigManager getConfig() {
		return config;
	}
	
	public void waitForNodeRegistrations(Set<String> expectedNodeNames, Set<String> expectedNodeAddresses) throws InterruptedException, IOException {
		Set<String> availableNodeNames = this.getAvailableTestnodeNames(DiscoveryMode.REGISTRY);
		
		// TODO log which node is not registered yet
		if (!availableNodeNames.containsAll(expectedNodeNames)) {
			logger.debug("Not all expected physical nodes have registered. Wait for registrations.");
			triggerNodesToRegister(expectedNodeAddresses);
			Thread.sleep(1000);
			waitForNodeRegistrations(expectedNodeNames, expectedNodeAddresses);
		}		
	}
	
	public void triggerNodesToRegister(Collection<String> nodeAddresses) throws IOException {
		discHelper.getAvailableNodesWithPreconfiguredList(nodeAddresses);
	}
	
	/**
	 * Returns a set of vpids of started processes of the given testnode.
	 * 
	 * @param node test node
	 * 
	 * @return set of vpids of started processes
	 */
	public Set<Integer> getStartedProcessesOnNode(ITestNode node) {
		return startedProcesses.get(node);
	}
	
	/**
	 * Resets internal counters of the coordinator.
	 */
	public void reset() {
		startedProcesses = new ConcurrentHashMap<ITestNode,Set<Integer>>();
	}
	
	public boolean copySensorFileFromTestnode(ITestNode testnode, int vpid, String fileName) throws RemoteException {
		String sensorDir = System.getProperty("user.dir") +"/inputOutput/testbed/tmp";
		
		RemoteInputStream remoteInputStream = testnode.getLogFileStream(vpid+"");
		
		InputStream fileData = null;
		OutputStream os = null;
		try {
			fileData = RemoteInputStreamClient.wrap(remoteInputStream);
			os = new FileOutputStream(sensorDir + "/" + fileName,false);
	    	byte[] buffer = new byte[4096];
	        for (int n; (n = fileData.read(buffer)) != -1; )
	            os.write(buffer, 0, n);
	    } catch (Exception e) {
	    	logger.error(e.getMessage(), e);
	    	return false;
		}
		finally {
			try {
				if(fileData != null)
					fileData.close();
				if(os != null)
					os.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
				return false;
			}
		}
		
		return true;
	}
	
	public File copyFileFromTestnode(ITestNode testnode, String sourceFileName, String targetFileName) throws RemoteException {
		String tmpDir = System.getProperty("user.dir") +"/inputOutput/testbed/tmp";
		
		RemoteInputStream remoteInputStream = testnode.getStreamFromFile(sourceFileName);
		
		InputStream fileData = null;
		OutputStream os = null;
		try {
			fileData = RemoteInputStreamClient.wrap(remoteInputStream);
			os = new FileOutputStream(tmpDir + "/" + targetFileName,false);
	    	byte[] buffer = new byte[4096];
	        for (int n; (n = fileData.read(buffer)) != -1; )
	            os.write(buffer, 0, n);
	    } catch (Exception e) {
	    	logger.error(e.getMessage(), e);
	    	return null;
		}
		finally {
			try {
				if(fileData != null)
					fileData.close();
				if(os != null)
					os.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
				return null;
			}
		}
		
		return new File(tmpDir+"/"+targetFileName);
	}
}
