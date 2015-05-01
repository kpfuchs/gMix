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
package staticContent.evaluation.testbed.deploy.testnode;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

import com.healthmarketscience.rmiio.RemoteInputStream;

public interface ITestNode extends Remote {

	/**
	 * Loads the content that is sent with the given input stream into a file with the given name.
	 *
	 * @param fileName name of the file
	 * @param remoteInputStream input stream
	 *
	 * @return true on success, otherwise false.
	 *
	 * @throws RemoteException
	 */
	public boolean installFile(String fileName, RemoteInputStream remoteInputStream) throws RemoteException;
	
	/**
	 * Loads the zipped content that is sent with the given input stream into a file with the given name.
	 * After the file is successfully transmitted, the file is unzipped.
	 *
	 * @param fileName name of the file
	 * @param remoteInputStream input stream
	 *
	 * @return true on success, otherwise false.
	 *
	 * @throws RemoteException
	 */
	public boolean installZipFile(String fileName, RemoteInputStream remoteInputStream) throws RemoteException;

	/**
	 * Executes the main method of the given class in the given classpath with the given arguments.
	 * The classpath must a path to a java jar file. The classname must consist of the package name
	 * and the classname separated by a dot. The virtual process id should be an unique identifier
	 * for the executed process over all instances of test nodes.
	 *
	 *
	 * @param vpid      virtual process id
	 * @param classpath path of the executable jar file
	 * @param classname name of the executable class
	 * @param args      array of arguments
	 * @param vmArgs    array of arguments for the java vm
	 *
	 * @return true if the process was created successfully, otherwise false.
	 *
	 * @throws RemoteException
	 */
	public boolean execute(String vpid, String classpath, String classname, Map<String,String> args, Map<String,String> vmArgs, Map<String,String> environmentVariables) throws RemoteException;
	
	public boolean executeCommand(String[] command, boolean waitFor) throws RemoteException;

	/**
	 * Returns an array of installed files.
	 *
	 * @return array of files
	 *
	 * @throws RemoteException
	 */
	public File[] getInstalledFiles() throws RemoteException;

	/**
	 * Returns the running processes a json string.
	 *
	 * @return a json string of running processes.
	 *
	 * @throws RemoteException
	 */
	public String getRunningProcesses() throws RemoteException;

	/**
	 * Terminates the process and all child processes of the given process id.
	 *
	 * @param pid process id
	 *
	 * @return true if the process termination scripts are successfully executed, otherwise false.
	 *
	 * @throws RemoteException
	 */
	public boolean killProcess(String pid) throws RemoteException;
	
	/**
	 * Terminates all started process instances and all child instances..
	 *
	 * @return true if the process termination scripts are successfully executed, otherwise false.
	 *
	 * @throws RemoteException
	 */
	public boolean killAllProcesses() throws RemoteException;

	/**
	 * Returns the unique name to identify the testnode at the RMI registry.
	 *
	 * @return a unique identifier
	 * 
	 * @throws RemoteException
	 */
	public String getName() throws RemoteException;

	/**
	 * Returns the host name of the node, as set via config.
	 *
	 * @return an IP or fully qualified hostname on which the testnode is running
	 * @throws RemoteException
	 */
	public String getHostName() throws RemoteException;
	
	public RemoteInputStream getLogFileStream(String vpid) throws RemoteException;
	
	public boolean deleteAllLogFiles() throws RemoteException;
	
	public Set<String> getAssignedVirtualAddresses() throws RemoteException;
	
	public RemoteInputStream getStreamFromFile(String filename) throws RemoteException;
	
	
}