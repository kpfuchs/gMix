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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.ProcessBuilder.Redirect;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import staticContent.evaluation.testbed.deploy.process.ProcessInfo;
import staticContent.evaluation.testbed.deploy.process.ProcessUtility;
import staticContent.evaluation.testbed.deploy.utility.NetworkUtility;

import com.healthmarketscience.rmiio.GZIPRemoteInputStream;
import com.healthmarketscience.rmiio.RemoteInputStream;
import com.healthmarketscience.rmiio.RemoteInputStreamClient;
import com.healthmarketscience.rmiio.RemoteInputStreamServer;

public class TestNode extends UnicastRemoteObject implements Serializable, ITestNode {

	private static final long serialVersionUID = 4572244666093684182L;
	private static final Logger logger = Logger.getLogger("TestNode");
	private final String name;
	private final String hostname;
	private static final String projectRootDir = System.getProperty("user.dir");
	private static final String testbedRootDir = projectRootDir +"/inputOutput/testbed";
	private static final String testbedInstallDir = testbedRootDir +"/install";
	private static final String testbedSensorLogDir = testbedRootDir +"/log";

	//TODO: zusammenlegen?
	private final Map<String, Process> managedProcesses = new HashMap<String, Process>();
	private final Map<String, ProcessInfo> managedProcessInfos = new HashMap<String, ProcessInfo>();

	
	public TestNode(String name, String hostname) throws RemoteException {
		super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory(null, null, true));

		this.name = name;
		this.hostname = hostname;
	}

	
	@Override
	public boolean installFile(String fileName, RemoteInputStream remoteInputStream) {
		String installDir = "";
		InputStream fileData = null;
		OutputStream os = null;
		try {
			installDir = testbedInstallDir;
			fileData = RemoteInputStreamClient.wrap(remoteInputStream);
			os = new FileOutputStream(installDir + "/" + fileName, false);
	    	byte[] buffer = new byte[4096];
	        for (int n; (n = fileData.read(buffer)) != -1; )
	            os.write(buffer, 0, n);
	    } catch (Exception e) {
	    	logger.error(e.getMessage(), e);
			return false;
		} finally {
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
		logger.info("installed: " + installDir + "/" + fileName);
		return true;
	}

	
	@Override
	public File[] getInstalledFiles() {
		try {
			File installDir = new File(testbedInstallDir);
			return installDir.listFiles();
		} catch(Exception e) {
			logger.error("Could not load installed file list!", e);
		}
		return new File[0];
	};

	
	@Override
	public boolean execute(String vpid, String classpath, String classname, Map<String,String> args, Map<String,String> vmArgs, Map<String,String> environmentVariables) throws RemoteException {
		String installDir = "";
		try {
			if(managedProcesses.containsKey(vpid))
				throw new InvalidParameterException("VPID already taken on this Testnode!");
			
			args = replacePlaceholders(args);
			vmArgs = replacePlaceholders(vmArgs);
			
			Comparator<String> comparator = new Comparator<String>() {

				@Override
				public int compare(String arg0, String arg1) {
					/*
					 * order is: -noGUI, -TOOL, -CONFIGFILE, -OVERWRITE and the rest
					 */
					
					switch(arg0) {
						case "-noGUI":
							return -1;
						case "-TOOL":
							if (arg1.equals("-noGUI")) 
								return 1;						
							else 
								return -1;
						case "-CONFIGFILE":
							if (arg1.equals("-OVERWRITE")) 
								return -1;						
							else 
								return 1;
						default:
							return 1;
					}
				}
				
			};
			
			Map<String,String> orderedArgs = new TreeMap<String,String>(comparator);
			orderedArgs.putAll(args);

			installDir = testbedInstallDir;
			String logfilePath = testbedSensorLogDir +"/process_"+vpid+".log";			
			
			String argString = generateArgString(orderedArgs);
			String vmArgString = generateArgString(vmArgs);			

			// just for display in the coordinator
			String programCall = "java "+vmArgString+" -cp \""+installDir+"/"+classpath+"\" \""+classname + "\" '"+argString+"'";
			
			List<String> processCommand = new ArrayList<String>();
			
			if (environmentVariables == null) {
				environmentVariables = new HashMap<String,String>();
			}			
			
			processCommand.add("java");
			if (!vmArgString.isEmpty()) processCommand.add(vmArgString);
			processCommand.add(((classname.isEmpty())?"-jar":"-cp"));
			processCommand.add(classpath);
			if (!classname.isEmpty()) processCommand.add(classname);
			
			// add args to command
			for (Map.Entry<String, String> entry : orderedArgs.entrySet()) {			    
			    String value = entry.getValue();				
			    processCommand.add(entry.getKey()+((value.isEmpty()) ? "" : "="+value));
			}
			

			// initialize the ProcessBuilder
			ProcessBuilder builder = new ProcessBuilder(processCommand);
			builder.directory(new File(installDir));			
			builder.environment().putAll(environmentVariables);

			// create log file if none exists yet
			File logFile = new File(logfilePath);
			synchronized (this) {
				if(!logFile.exists())
					logFile.createNewFile();
			}

			// Redirect the output to the log file
			builder.redirectErrorStream(true);
			builder.redirectOutput(Redirect.appendTo(logFile));

		    logger.info("Create process with the following command: "+ builder.command());

			// create and store the Process for later termination
			Process process = builder.start();
			if(process == null)
				throw new Exception("Process start failed!");
			
			managedProcesses.put(vpid, process);
			managedProcessInfos.put(vpid, new ProcessInfo(vpid, programCall));
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}

		if(ProcessUtility.isRunning(managedProcesses.get(vpid)))
			logger.info("Process successfully started.");
		else
			logger.info("Process start failed or process already exited!");

		return true;
	}


	public static String generateArgString(Map<String, String> args) {
		String result = "";

		for (Map.Entry<String, String> entry : args.entrySet()) {		    
		    String value = entry.getValue();		    
		    String arg   = entry.getKey()+((value.isEmpty()) ? "" : "="+value);			
			result += ((result.isEmpty()) ? "" : " ")+arg;
		}
		
		return result;
	}

	@Override
	public String getRunningProcesses() throws RemoteException {
		logger.debug("Find running processes.");

		try {
			JSONObject resultObj = new JSONObject();
			JSONObject jProcessesObj = new JSONObject();

			for(Map.Entry<String,Process> processEntry : managedProcesses.entrySet())
			{
				ProcessInfo info = managedProcessInfos.get(processEntry.getKey());
				jProcessesObj.put(info.getVpid(), info.toJson());
			}

			resultObj.putOpt("hostName", getHostName());
			resultObj.putOpt("processes", jProcessesObj);

			return resultObj.toString();

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage());
		}
	}

	@Override
	public boolean killProcess(String vpid) throws RemoteException {
		Process process = managedProcesses.get(vpid);
		try{
			managedProcesses.remove(vpid);
			managedProcessInfos.remove(vpid);
			process.destroy();

			logger.debug("Killed process with vpid "+vpid);
			
			return true;	
		} catch(Exception e) {
			logger.error("Could not kill the process!", e);
		}
		
		logger.debug("Killing process with vpid "+vpid+" failed.");
		
		return false;
	}
	
	

	/**
	 * Replaces each defined placeholder with the placeholder value on the testnode.
	 * Defined placeholders are:
	 *
	 * <projectRoot> - path to the root directory of the project
	 *
	 * @param input
	 *
	 * @return string with replaced placeholders
	 */
	protected static String replacePlaceholders(String input) {
		String result = input;		
		
		result = result.replaceAll("<projectRoot>", projectRootDir);
		result = result.replaceAll("<testbedRoot>", testbedRootDir);

		return result;
	}
	
	protected static Map<String,String> replacePlaceholders(Map<String,String> map) {
		for(String key: map.keySet()) {
			map.put(key, replacePlaceholders(map.get(key)));
		}

		return map;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getHostName() {
		return hostname;
	}

	@Override
	public RemoteInputStream getLogFileStream(String vpid) {
		String logfilePath = null;
		RemoteInputStreamServer istream = null;
		try {
			logfilePath = testbedSensorLogDir +"/process_"+vpid+".log";

			istream = new GZIPRemoteInputStream(new BufferedInputStream(
					new FileInputStream(logfilePath)));
			return istream.export();
		} catch (Exception e) {
			if(istream != null)
				istream.close();
			logger.error("Could not open Log file: "+logfilePath, e);
			return null;
		}
	}

	@Override
	public boolean killAllProcesses() throws RemoteException {
		boolean success = true;
		
		// copy to avoid removing elements while iterating over it
		Set<String> managedProcessesCopy = new HashSet<>(managedProcesses.keySet());
		
		for (String vpid : managedProcessesCopy) {
			success = killProcess(vpid) && success;
		}		
		
		return true;
	}

	@Override
	public boolean deleteAllLogFiles() throws RemoteException {
		try {
			String logDir = testbedSensorLogDir;
			
			FilenameFilter filter = new FilenameFilter() {
		        @Override
		        public boolean accept(File dir, String name) {
		            return name.toLowerCase().endsWith(".log");
		        }
		    };
			
			File[] sensorFiles = new File(logDir).listFiles(filter);
			
			for (int i = 0; i < sensorFiles.length; i++) {
				File file = sensorFiles[i];				
				file.delete();
				
				logger.debug("Deleted log file: "+file.getAbsolutePath());
			}
		} catch (Exception e) {
			logger.error("Could not delete all log files: ", e);
			return false;
		}
		return true;
	}

	@Override
	public boolean executeCommand(String[] command, boolean waitFor) throws RemoteException {		
		for (int i = 0; i < command.length; i++) {
			command[i] = replacePlaceholders(command[i]);			
		}
		
		ProcessBuilder builder = new ProcessBuilder(command);
		
		logger.debug("Execute command: "+builder.command());

		Process process;
		try {
			process = builder.start();
			
			if (process == null) return false;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		
		if (waitFor) {
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
		}		
		
		logger.debug("Executed command: "+builder.command());
		
		return true;
	}

	@Override
	public Set<String> getAssignedVirtualAddresses() throws RemoteException {		
		return NetworkUtility.getVirtualIpAddresses();
	}

	@Override
	public boolean installZipFile(String fileName, RemoteInputStream remoteInputStream) throws RemoteException {
		// fetch file
		boolean success = installFile(fileName, remoteInputStream);
		
		if (!success) {
			return false;
		}
		
		String installDir = testbedInstallDir;
		String absoluteFilename = installDir+"/"+fileName;
		
		// unzip
	    try {
	         ZipFile zipFile = new ZipFile(absoluteFilename);
	         zipFile.extractAll(installDir);
	    } catch (ZipException e) {
	    	logger.error("Unable to unzip file: "+absoluteFilename,e);
	    	return false;
	    }
	    
	    logger.info("unzipped installed file: " + absoluteFilename + " to dir: " + installDir);
		
		return true;
	}

	@Override
	public RemoteInputStream getStreamFromFile(String filename) throws RemoteException {
		String filePath = replacePlaceholders(filename);
		RemoteInputStreamServer istream = null;
		try {
			istream = new GZIPRemoteInputStream(new BufferedInputStream(new FileInputStream(filePath)));
			return istream.export();
		} catch (Exception e) {
			if(istream != null)
				istream.close();
			logger.error("Could not open file: "+filePath, e);
			return null;
		}
	}
}
