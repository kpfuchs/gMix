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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import staticContent.evaluation.testbed.deploy.coordinator.Coordinator;
import staticContent.evaluation.testbed.deploy.testnode.ITestNode;

public class Application {	
	protected String classpath;
	protected String className;
	protected Map<String,String> environmentVariables;
	protected Map<String,String> appArguments;
	protected Map<String,String> vmArguments;
	protected Map<String,List<String>> variableParameters = new HashMap<String,List<String>>();
	
	protected Coordinator coordinator;
	protected String realNodeIpAddress;
	protected ITestNode testnode;
	
	public Application(String classpath, String className, Map<String,String> environmentVariables, Map<String,String> appArguments, Map<String,String> vmArguments, String realNodeIpAddress) {
		this.classpath = classpath;
		this.className = className;
		this.environmentVariables = environmentVariables;
		this.appArguments = appArguments;
		this.vmArguments = vmArguments;
		this.realNodeIpAddress = realNodeIpAddress;
		this.coordinator = Coordinator.getInstance();
		
		this.appArguments = addKeyValueToMap("-OVERWRITE", "GLOBAL_LOCAL_MODE_ON=FALSE", this.appArguments, false);
		
		setRealIp(realNodeIpAddress);
	}
	
	protected void setRealIp(String realNodeIpAddress) {
		this.realNodeIpAddress = realNodeIpAddress;
		appArguments = addKeyValueToMap("-OVERWRITE", "GLOBAL_MIX_BIND_ADDRESS="+realNodeIpAddress, appArguments, false);
	}
	
	public void setVariableParameters(Map<String,List<String>> variableParameters) {
		this.variableParameters = variableParameters;
	}
	
	public void execute(int runIndex) throws IOException {
		if (testnode == null) {
			testnode = coordinator.getTestnode("testnode_"+realNodeIpAddress);
		}
		
		String variableParametersString = "";
		String spacer = "";
		
		for (String parameterName: variableParameters.keySet()) {
			variableParametersString += spacer+parameterName+"="+variableParameters.get(parameterName).get(runIndex);
			spacer = ",";
		}
		
		Map<String,String> newAppArguments = new HashMap<>(appArguments);
		
		newAppArguments = addKeyValueToMap("-OVERWRITE", variableParametersString, newAppArguments, false);
		newAppArguments.put("-OVERWRITE", "'"+newAppArguments.get("-OVERWRITE")+"'");
		
		coordinator.executeOnTestNode(testnode, classpath, className, newAppArguments, vmArguments, environmentVariables);
	}

	public void setTestnode(ITestNode testnode) {
		this.testnode = testnode;
	}
	
	public void setInfoServiceAddress(String infoServiceAddress) {		
		appArguments = addKeyValueToMap("-OVERWRITE", "GLOBAL_INFO_SERVICE_ADDRESS="+infoServiceAddress, appArguments, false);
	}
	
	public boolean isInfoService() {		
		return appArguments.containsKey("-TOOL") && appArguments.get("-TOOL").equals("infoService");
	}
	
	public String getRealIp() {
		return realNodeIpAddress;
	}
	
	protected static Map<String,String> addKeyValueToMap(String key, String value, Map<String,String> map, boolean overwrite) {
		if (!overwrite) {
			if (map.containsKey(key)) {
				map.put(key, map.get(key)+","+value);
			}
			else {
				map.put(key, value);
			}
		}
		else {
			map.put(key, value);
		}
		
		return map;
	}
	
//	public static void main(String[] args) {
//		Map<String,String> appArguments = new HashMap<String,String>();
//		appArguments.put("-OVERWRITE", "GLOBAL_INFO_SERVICE_ADDRESS=192.168.4.51,GLOBAL_INFO_SERVICE_PORT=22002,GLOBAL_MIX_BIND_ADDRESS=192.168.4.52");
//		
//		Application app = new Application("", "", null, appArguments, null, "");
//		
//		Map<String,List<String>> variableParameters = new HashMap<String,List<String>>();
//		List<String> batchSizes = new ArrayList<String>();
//		batchSizes.add("20");
//		batchSizes.add("50");
//		batchSizes.add("100");
//		
//		variableParameters.put("BATCH_SIZE", batchSizes);
//		
//		app.setVariableParameters(variableParameters);
//				
//		try {
//			app.execute(1);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
}
