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
package staticContent.evaluation.testbed.plan;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import staticContent.evaluation.testbed.plan.global.GlobalExecutionPlan;
import staticContent.evaluation.testbed.plan.global.SimpleExecutionPlan;
import staticContent.evaluation.testbed.plan.global.GlobalExecutionPlan.Network_Mode;
import staticContent.evaluation.testbed.plan.node.Application;
import staticContent.evaluation.testbed.plan.node.NodeExecutionPlan;
import staticContent.evaluation.testbed.plan.node.VirtualNodeApplication;
import staticContent.evaluation.testbed.statistic.Metric;

public class XMLtoObject {
	protected static Logger logger = Logger.getLogger(XMLtoObject.class);
	
	protected static int numOfRuns = 0;
	protected static List<String> nodesList;
	protected static Map<String, String> hostnameIpAssignments;
	
	public static GlobalExecutionPlan createGlobalExecutionPlan(Element globalXML) throws UnknownHostException {
		String experimentType = globalXML.getAttribute("type");
		
		Network_Mode networkMode = (experimentType.equals("real")) ? Network_Mode.REAL : Network_Mode.MODELNET;
		
		File sourceFile = new File(System.getProperty("user.dir") +"/dist/" +globalXML.getAttribute("source"));
		
		NodeList applicationsList = globalXML.getElementsByTagName("applications");
		NodeList variableParamsList = globalXML.getElementsByTagName("variableParams");
		NodeList runConfigList = globalXML.getElementsByTagName("runConfig");
		NodeList nodesXMLList = globalXML.getElementsByTagName("testnodes");
				
		Element testnodes = (Element) nodesXMLList.item(0);
		NodeList nodesElementList = testnodes.getElementsByTagName("node");
		
		nodesList = new ArrayList<String>();
		hostnameIpAssignments = new HashMap<String,String>();
		
		for (int i = 0; i < nodesElementList.getLength(); i++) {
            Element currentNode = (Element) nodesElementList.item(i);            
            String hostname     = currentNode.getAttribute("hostname");
            
            // get IP of hostname
            InetAddress address = InetAddress.getByName(hostname);
            String hostAddress = address.getHostAddress();
            
            nodesList.add(Integer.parseInt(currentNode.getAttribute("idx")), hostAddress);
            hostnameIpAssignments.put(hostname, hostAddress);
        }
		
		Element applications = (Element) applicationsList.item(0);
		Element variableParams = (Element) variableParamsList.item(0);
		Element runConfig = (Element) runConfigList.item(0);
		
		
		SimpleExecutionPlan globalPlan = new SimpleExecutionPlan(networkMode);
		
		globalPlan.setTestnodeNames(hostnameIpAssignments.keySet());
		
		if (!runConfig.getAttribute("executionTime").equals("")) {
			globalPlan.setExecutionTime(Long.parseLong(runConfig.getAttribute("executionTime")));
		}
		
		if (!runConfig.getAttribute("settlingTime").equals("")) {
			globalPlan.setSettlingTime(Long.parseLong(runConfig.getAttribute("settlingTime")));
		}
		
		if (!runConfig.getAttribute("terminationEvents").equals("")) {
			globalPlan.setTerminationEvents(readTerminationEvents(runConfig));
		}
		
		for (int i = 0; i < nodesList.size(); i++) {            
            NodeExecutionPlan nodeExecutionPlan = createNodeExecutionPlan(globalPlan, experimentType, sourceFile, runConfig, applications, variableParams, nodesList, i);
            
            globalPlan.addNodePlan(nodeExecutionPlan);
        }
		
		readEvalMetrics(globalXML, globalPlan);
		
		globalPlan.setNumOfExperiments(numOfRuns);
		
		if (networkMode == Network_Mode.MODELNET) {
			readModelnetTopology(globalXML, globalPlan);
		}
		
		return globalPlan;
	}
	
	protected static Set<String> readTerminationEvents(Element runConfig) {
		Set<String> result = new HashSet<String>();
		
		if (!runConfig.getAttribute("terminationEvents").equals("")) {
			List<String> values = Arrays.asList(runConfig.getAttribute("terminationEvents").split(","));
			
			for (String string : values) {
				result.add(string);
			}
		}
		
		return result;
	}

	public static NodeExecutionPlan createNodeExecutionPlan(GlobalExecutionPlan globalPlan, String experimentType, File sourceFile, Element runConfigXML, Element applicationsXML, Element variableParamsXML, List<String> nodesList, int nodeIndex) {
		NodeExecutionPlan nodePlan = new NodeExecutionPlan(globalPlan, nodesList.get(nodeIndex), sourceFile);
		
		NodeList runList = runConfigXML.getElementsByTagName("run");
		NodeList appList = applicationsXML.getElementsByTagName("app");		
		
		List<Element> appElementList = new ArrayList<Element>();
		
		for (int i = 0; i < appList.getLength(); i++) {            
			Element currentApp = (Element) appList.item(i);            
			appElementList.add(Integer.parseInt(currentApp.getAttribute("idx")), currentApp);
        }
		
		Map<String,List<String>> variableParams = readVariableParams(variableParamsXML);
		
		globalPlan.setVariableParameters(variableParams);
		
		for (int i = 0; i < runList.getLength(); i++) {
			Element currentRun = (Element) runList.item(i);
		    int currentNodeIndex = Integer.parseInt(currentRun.getAttribute("nodeIdx"));
		    int currentAppIndex = Integer.parseInt(currentRun.getAttribute("appIdx"));
			
		    // if run belongs to the selected node, add the application to the nodePlan
		    if (currentNodeIndex == nodeIndex) {
		    	Element appElement = appElementList.get(currentAppIndex);
		    	
		    	Application currentApplication = createApplication(experimentType, nodesList, appElement, currentRun, nodeIndex);
		    	
		    	currentApplication.setVariableParameters(variableParams);
		    	
		    	nodePlan.addApplication(currentApplication);
		    }
		}
		
		return nodePlan;
	}
	
	public static Application createApplication(String experimentType, List<String> nodesList, Element appElementXML, Element currentRunXML, int nodeIndex) {		
    	String classpath = appElementXML.getAttribute("classpath");
    	String className = appElementXML.getAttribute("className");
    	Map<String,String> environmentVariables = createArgumentMap("environmentVariables", appElementXML, currentRunXML);
    	Map<String,String> appArguments = createArgumentMap("appArgs", appElementXML, currentRunXML);
    	Map<String,String> vmArguments = createArgumentMap("vmArgs", appElementXML, currentRunXML);
    	
//    	System.out.println("classpath: " + classpath + " className: " + className);    	
//		System.out.println("env: " + environmentVariables + " args: " + appArguments +" vmArgs: "+vmArguments);
    	
    	switch(experimentType) {
	    	case "modelnet":
	    		return new VirtualNodeApplication(classpath, className, environmentVariables, appArguments, vmArguments, nodesList.get(nodeIndex));
	    	default:
	    		return new Application(classpath, className, environmentVariables, appArguments, vmArguments, nodesList.get(nodeIndex));
    	}
	}
	
	public static Map<String,String> createArgumentMap(String argumentClass, Element appXML, Element runXML) {
		NodeList appEnvVarList = appXML.getElementsByTagName(argumentClass);		
		NodeList runEnvVarList = runXML.getElementsByTagName(argumentClass);		
		
		if (runEnvVarList.getLength() > 0) {
			Element runEnvVarElement = (Element) runEnvVarList.item(0);		
			NodeList runEnvVarArgList = runEnvVarElement.getElementsByTagName("arg");
			
			if (runEnvVarArgList.getLength() > 0) {
				return createArgumentMap(runEnvVarArgList);
			}
			else if (appEnvVarList.getLength() > 0) {
				Element appEnvVarElement = (Element) appEnvVarList.item(0);
				NodeList appEnvVarArgList = appEnvVarElement.getElementsByTagName("arg");
				
				if (appEnvVarArgList.getLength() > 0) {
					return createArgumentMap(appEnvVarArgList);
				}
			}
		}
		else if (appEnvVarList.getLength() > 0) {
			Element appEnvVarElement = (Element) appEnvVarList.item(0);
			NodeList appEnvVarArgList = appEnvVarElement.getElementsByTagName("arg");
			
			if (appEnvVarArgList.getLength() > 0) {
				return createArgumentMap(appEnvVarArgList);
			}
		}
		
		return new HashMap<String,String>();
	}
	
	public static Map<String,String> createArgumentMap(NodeList elementsXML) {
		Map<String,String> result = new HashMap<String,String>();
		
		for (int i = 0; i < elementsXML.getLength(); i++) {            
			Element currentArg = (Element) elementsXML.item(i);            
			result.put(currentArg.getAttribute("name"), currentArg.getAttribute("value"));
        }
		
		return result;
	}
	
	protected static void readEvalMetrics(Element globalXML, GlobalExecutionPlan globalPlan) {
		NodeList evalMetricsList = globalXML.getElementsByTagName("evalMetrics");
		Element evalMetrics = (Element) evalMetricsList.item(0);
		
		if (evalMetrics == null) {
			logger.error("Section 'evalMetrics' in configuration is missing. No metrics are defined.");
			System.exit(1);
		}
		
		NodeList metricList = evalMetrics.getElementsByTagName("metric");
		
		if (metricList.getLength() == 0) {
			logger.error("No metrics in section 'evalMetrics' of the configuration are defined.");
			System.exit(1);
		}
		
		HashSet<Metric> wantedMetrics = new HashSet<Metric>();
		
		for (int i = 0; i < metricList.getLength(); i++) {            
			Element currentMetric = (Element) metricList.item(i);
			
			wantedMetrics.add(Metric.valueOf(currentMetric.getAttribute("name")));
        }
		
		globalPlan.setEvalMetrics(wantedMetrics);
	}
	
	protected static Map<String,List<String>> readVariableParams(Element variableParamsXML) {
		Map<String,List<String>> result = new HashMap<String,List<String>>();
		
		NodeList variableParamsList = variableParamsXML.getElementsByTagName("param");
		
		int minNumOfValues = -1;
		
		for (int i = 0; i < variableParamsList.getLength(); i++) {            
			Element currentParam = (Element) variableParamsList.item(i);
			
			String content = currentParam.getTextContent().trim();
			
			if (content.length() == 0) {
				minNumOfValues = 0;
				continue;
			}
			
			List<String> values = new ArrayList<String>(Arrays.asList(content.split(",")));
			
			result.put(currentParam.getAttribute("name"), values);
			
			if (minNumOfValues == -1) {
				minNumOfValues = values.size();
			}
			else {
				minNumOfValues = Math.min(minNumOfValues, values.size());
			}			
        }
		
		if (minNumOfValues == 0) {
			result = new HashMap<String,List<String>>();
		}
		else {
			for (String paramName: result.keySet()) {
				List<String> values = result.get(paramName);
				
				for (int i = values.size()-1; i >= minNumOfValues; i--) {
					values.remove(i);
				}
				
				result.put(paramName, values);
			}
		}
		
		numOfRuns = minNumOfValues;
		
		return result;
	}

	public static Map<String, Set<String>> createNodeVirtualIpAssignment(Element virtualModelXML) throws XMLConfigException {
		if (hostnameIpAssignments == null || hostnameIpAssignments.isEmpty()) {
			throw new XMLConfigException("No host nodes are configured. Call method 'createGlobalExecutionPlan' first.");
		}
		
		Map<String, Set<String>> result = new HashMap<String, Set<String>>();
		
		NodeList hostList = virtualModelXML.getElementsByTagName("host");
		
		for (int i = 0; i < hostList.getLength(); i++) {
			Element currentHost = (Element) hostList.item(i);
			String hostAddress = hostnameIpAssignments.get(currentHost.getAttribute("hostname"));
			
			NodeList virtualNodeList = currentHost.getElementsByTagName("virtnode");
	
			for (int j = 0; j < virtualNodeList.getLength(); j++) {
				Element currentNode = (Element) virtualNodeList.item(j);
				
				if (!result.containsKey(hostAddress)) {
					result.put(hostAddress, new HashSet<String>());
				}
				
				result.get(hostAddress).add(currentNode.getAttribute("vip"));
			}
		}

		return result;
	}
	
	protected static void readModelnetTopology(Element globalXML, GlobalExecutionPlan globalPlan) {		
		NodeList virtualTopologyList = globalXML.getElementsByTagName("virtualTopology");
		Element virtualTopologyElement = (Element) virtualTopologyList.item(0);
		
		File virtualTopologyPath = new File(System.getProperty("user.dir") +"/inputOutput/testbed/experimentDefinitions/" +virtualTopologyElement.getAttribute("path"));
		
		if (!virtualTopologyPath.exists()) {
			logger.error("Virtual topology file: "+virtualTopologyPath.getAbsolutePath()+" does not exist.");
			System.exit(1);
		}
		
		globalPlan.setVirtualTopologyFile(virtualTopologyPath);
		
		NodeList emulatorNodesXml = globalXML.getElementsByTagName("emulatornodes");
		Element emulatorNodeList = (Element) emulatorNodesXml.item(0);
		
		NodeList emulatorNodes = emulatorNodeList.getElementsByTagName("node");
		
		Set<String> emulatorNames = new HashSet<String>();
		
		for (int i = 0; i < emulatorNodes.getLength(); i++) {
			Element currentNode = (Element) emulatorNodes.item(i);
			
			emulatorNames.add(currentNode.getAttribute("hostname"));
		}
		
		globalPlan.setModelnetEmulatorNames(emulatorNames);
		
		NodeList testnodesXML = globalXML.getElementsByTagName("testnodes");
		
		Element testnodeList = (Element) testnodesXML.item(0);
		NodeList testnodes = testnodeList.getElementsByTagName("node");
		
		generateModelnetRealTopologyFile(emulatorNodes, testnodes, globalPlan);
	}
	
	public static void generateModelnetRealTopologyFile(NodeList emulatorNodesXML, NodeList testodesXML, GlobalExecutionPlan globalPlan) {		
		String topologyFileName = System.getProperty("user.dir") +"/inputOutput/testbed/tmp/real_topology.xml";
		
		Writer writer = null;

		try {
		    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(topologyFileName), "utf-8"));
		    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		    writer.write("<hardware>\n");
		    
		    for (int i = 0; i < emulatorNodesXML.getLength(); i++) {
				Element currentNode = (Element) emulatorNodesXML.item(i);				
				String netIfParam   = (currentNode.getAttribute("netdev") != null) ? " netdev=\""+currentNode.getAttribute("netdev")+"\"" : "";
				
				writer.write("<emul hostname=\""+currentNode.getAttribute("hostname")+"\""+netIfParam+" />\n");
			}
		    
		    for (int i = 0; i < testodesXML.getLength(); i++) {
				Element currentNode = (Element) testodesXML.item(i);				
				String netIfParam   = (currentNode.getAttribute("netdev") != null) ? " netdev=\""+currentNode.getAttribute("netdev")+"\"" : "";
				
				writer.write("<host hostname=\""+currentNode.getAttribute("hostname")+"\""+netIfParam+" />\n");
			}
		    
		    writer.write("</hardware>\n");
		} catch (IOException ex) {
		  // report
		} finally {
		   try {writer.close();} catch (Exception ex) {}
		}
		
		globalPlan.setRealTopologyFile(new File(topologyFileName));
	}
}
