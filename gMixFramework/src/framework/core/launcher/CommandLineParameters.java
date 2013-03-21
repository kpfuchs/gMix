/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2012  Karl-Peter Fuchs
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
 */
package framework.core.launcher;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import framework.core.config.MatchingMechanism;
import framework.core.config.Paths;
import framework.core.config.Settings;


public class CommandLineParameters {
	
	public boolean useGui = true;
	public ToolName gMixTool = null; // the "Launcher" to be started (see framework.core.launcher)
	public String globalConfigFile = null; // path to the config file to use for this run (all normal/other config files will be ignored)
	public String overwriteParameters = null; // list of parameters (key-value-pairs) to overwrite the normal or global config
	public String[] passthroughParameters = null; // tool-specific parameters
	
	private boolean settingsObjectCreated = false;
	private Settings settings;
	

	public CommandLineParameters(String[] args, ToolName gMixTool) {
		init(args, gMixTool);
	}
	
	
	public CommandLineParameters(String[] args) {
		init(args, null);
	}
	
	
	private void init(String[] args, ToolName gMixTool) {
		if (gMixTool != null)
			this.gMixTool = gMixTool;
		Vector<String> parameters = new Vector<String>(Arrays.asList(args));
		Vector<String> unknownParameters = new Vector<String>();
		
		for(String par: parameters) {
			if (par.startsWith("-"))
				par = par.replaceFirst("-", "");
			
			if (par.equalsIgnoreCase("noGUI")) {
				useGui = false;
			} else if (par.startsWith("TOOL=")) {
				if (gMixTool == null)
					this.gMixTool = stringToToolName(par.split("=")[1]);
			} else if (par.startsWith("GLOBAL_CONFIG=")) {
				globalConfigFile = par.split("=")[1];
			} else if (par.startsWith("OVERWRITE=")) {
				par = par.replaceAll("'", "");
				par = par.replaceFirst("OVERWRITE=", "");
				overwriteParameters = par;
			} else {
				unknownParameters.add(par);
			}
		}
		
		if (unknownParameters.size() > 0) {
			passthroughParameters = unknownParameters.toArray(new String[0]);
		} else {
			passthroughParameters = new String[0];
		}
	}
	
	
	@Override
	public String toString() {
		String s = "CommandLineParameters: \n";
		s += "NO_GUI=" +!useGui +"\n";
		s += "TOOL=" +gMixTool +"\n";
		s += "GLOBAL_CONFIG=" +globalConfigFile +"\n";
		s += "OVERWRITE=" +overwriteParameters +"\n";
		List<String> c = Arrays.asList(passthroughParameters);
		s += "PASSTHROUGH_PARAMETERS=" +c +"\n";
		return s;
	}

	
	public static ToolName stringToToolName(String toolNameAsString) {
		
		if (toolNameAsString.equals("1") || toolNameAsString.equalsIgnoreCase("-localTest") || toolNameAsString.equalsIgnoreCase("localTest")) {
			return ToolName.LOCAL_TEST;
		} else if (toolNameAsString.equals("2") || toolNameAsString.equalsIgnoreCase("-infoService") || toolNameAsString.equalsIgnoreCase("infoService")) {
			return ToolName.INFO_SERVICE;
		} else if (toolNameAsString.equals("3") || toolNameAsString.equalsIgnoreCase("-mix") || toolNameAsString.equalsIgnoreCase("mix")) {
			return ToolName.MIX;
		} else if (toolNameAsString.equals("4") || toolNameAsString.equalsIgnoreCase("-client") || toolNameAsString.equalsIgnoreCase("client")) {
			return ToolName.CLIENT;
		} else if (toolNameAsString.equals("5") || toolNameAsString.equalsIgnoreCase("-mixAndClient") || toolNameAsString.equalsIgnoreCase("mixAndClient") || toolNameAsString.equalsIgnoreCase("-p2p") || toolNameAsString.equalsIgnoreCase("p2p")) {
			return ToolName.P2P;
		} else if (toolNameAsString.equals("6") || toolNameAsString.equalsIgnoreCase("-loadGenerator") || toolNameAsString.equalsIgnoreCase("loadGenerator")) {
			return ToolName.LOAD_GENERATOR;
		} else if (toolNameAsString.equals("7") || toolNameAsString.equalsIgnoreCase("-simulator") || toolNameAsString.equalsIgnoreCase("simulator")) {
			return ToolName.SIMULATOR;
		} else {
			return ToolName.NOT_SET;
		}
	}
	
	
	public Settings generateSettingsObject() {
		if (settingsObjectCreated)
			return settings;
		settingsObjectCreated = true;
		
		if (globalConfigFile != null) {
			settings = new Settings(globalConfigFile);
			settings.setProperty("GLOBAL_CONFIG_MODE_ON", "TRUE");
			return settings;
		}
		
		settings = new Settings(Paths.PATH_TO_PATH_CONFIG_FILE);
		settings.addProperties(Paths.GENERAL_CONFIG_PROPERTY_FILE_PATH);
		settings.setProperty("GLOBAL_CONFIG_MODE_ON", "FALSE");
		
		if (	gMixTool == ToolName.CLIENT 
				|| gMixTool == ToolName.LOAD_GENERATOR
				|| gMixTool == ToolName.LOCAL_TEST
				|| gMixTool == ToolName.MIX
				|| gMixTool == ToolName.P2P
				|| gMixTool == null) {
			loadPluginSettings();
		}
		
		if (gMixTool == ToolName.LOAD_GENERATOR)
			settings.addProperties(Paths.LG_PROPERTY_FILE_PATH);
		
		if (overwriteParameters != null)
			Settings.overwriteSettings(settings.getPropertiesObject(), overwriteParameters);
		
		// validate composition
		boolean validateConfig = settings.getPropertyAsBoolean("VALIDATE_CONFIG");
		if (validateConfig && !MatchingMechanism.isConfigValid(settings))
			throw new RuntimeException("invalid plug-in composition"); 
		
		return settings;
	}
	
	

	private void loadPluginSettings() {
		// load plugin-composition-file:
		Settings pluginComposition = new Settings("./inputOutput/anonNode/pluginComposition/" + settings.getProperty("PLUG_IN_COMPOSITION"));
		if (overwriteParameters != null)
			Settings.overwriteExistingSettings(pluginComposition.getPropertiesObject(), overwriteParameters);
		settings.addProperties(pluginComposition.getPropertiesObject());
		for (String key: pluginComposition.getPropertiesObject().stringPropertyNames()) {
			String value = pluginComposition.getProperty(key);
			if (value != null && !value.equals("")) {
				for (String plugInName: value.split(",")) {
					if (plugInName.equalsIgnoreCase("AUTO"))
						continue;
					Settings plugInSettings = new Settings(Paths.getProperty(key +"_PATH") +plugInName +"/PlugInSettings.txt");
					String staticFunctions = plugInSettings.getProperty("SAME_LAYER_REQUIREMENTS");
					if (staticFunctions != null && !staticFunctions.equals(""))
						for (String staticFunction: staticFunctions.split(","))
							plugInSettings.addProperties((Paths.getProperty(key +"_PATH")).replace("/plugIns/", "/staticFunctions/") +staticFunction +"/StaticFunctionSettings.txt");
					
					staticFunctions = plugInSettings.getProperty("LAYER_1_CLIENT_REQUIREMENTS");
					if (staticFunctions != null && !staticFunctions.equals(""))
						for (String staticFunction: staticFunctions.split(","))
							plugInSettings.addProperties((Paths.getProperty("LAYER_1_PLUG-IN_CLIENT_PATH")).replace("/plugIns/", "/staticFunctions/") +staticFunction +"/StaticFunctionSettings.txt");
					staticFunctions = plugInSettings.getProperty("LAYER_1_MIX_REQUIREMENTS");
					if (staticFunctions != null && !staticFunctions.equals(""))
						for (String staticFunction: staticFunctions.split(","))
							plugInSettings.addProperties((Paths.getProperty("LAYER_1_PLUG-IN_MIX_PATH")).replace("/plugIns/", "/staticFunctions/") +staticFunction +"/StaticFunctionSettings.txt");
					
					staticFunctions = plugInSettings.getProperty("LAYER_2_CLIENT_REQUIREMENTS");
					if (staticFunctions != null && !staticFunctions.equals(""))
						for (String staticFunction: staticFunctions.split(","))
							plugInSettings.addProperties((Paths.getProperty("LAYER_2_PLUG-IN_CLIENT_PATH")).replace("/plugIns/", "/staticFunctions/") +staticFunction +"/StaticFunctionSettings.txt");
					staticFunctions = plugInSettings.getProperty("LAYER_2_MIX_REQUIREMENTS");
					if (staticFunctions != null && !staticFunctions.equals(""))
						for (String staticFunction: staticFunctions.split(","))
							plugInSettings.addProperties((Paths.getProperty("LAYER_2_PLUG-IN_MIX_PATH")).replace("/plugIns/", "/staticFunctions/") +staticFunction +"/StaticFunctionSettings.txt");
					
					staticFunctions = plugInSettings.getProperty("LAYER_3_MIX_REQUIREMENTS");
					if (staticFunctions != null && !staticFunctions.equals(""))
						for (String staticFunction: staticFunctions.split(","))
							plugInSettings.addProperties((Paths.getProperty("LAYER_3_PLUG-IN_MIX_PATH")).replace("/plugIns/", "/staticFunctions/") +staticFunction +"/StaticFunctionSettings.txt");
					staticFunctions = plugInSettings.getProperty("LAYER_3_CLIENT_REQUIREMENTS");
					if (staticFunctions != null && !staticFunctions.equals(""))
						for (String staticFunction: staticFunctions.split(","))
							plugInSettings.addProperties((Paths.getProperty("LAYER_4_PLUG-IN_CLIENT_PATH")).replace("/plugIns/", "/staticFunctions/") +staticFunction +"/StaticFunctionSettings.txt");
					
					staticFunctions = plugInSettings.getProperty("LAYER_4_MIX_REQUIREMENTS");
					if (staticFunctions != null && !staticFunctions.equals(""))
						for (String staticFunction: staticFunctions.split(","))
							plugInSettings.addProperties((Paths.getProperty("LAYER_4_PLUG-IN_MIX_PATH")).replace("/plugIns/", "/staticFunctions/") +staticFunction +"/StaticFunctionSettings.txt");
					staticFunctions = plugInSettings.getProperty("LAYER_4_CLIENT_REQUIREMENTS");
					if (staticFunctions != null && !staticFunctions.equals(""))
						for (String staticFunction: staticFunctions.split(","))
							plugInSettings.addProperties((Paths.getProperty("LAYER_4_PLUG-IN_CLIENT_PATH")).replace("/plugIns/", "/staticFunctions/") +staticFunction +"/StaticFunctionSettings.txt");
					
					staticFunctions = plugInSettings.getProperty("LAYER_5_MIX_REQUIREMENTS");
					if (staticFunctions != null && !staticFunctions.equals(""))
						for (String staticFunction: staticFunctions.split(","))
							plugInSettings.addProperties((Paths.getProperty("LAYER_5_PLUG-IN_MIX_PATH")).replace("/plugIns/", "/staticFunctions/") +staticFunction +"/StaticFunctionSettings.txt");
					staticFunctions = plugInSettings.getProperty("LAYER_5_CLIENT_REQUIREMENTS");
					if (staticFunctions != null && !staticFunctions.equals(""))
						for (String staticFunction: staticFunctions.split(","))
							plugInSettings.addProperties((Paths.getProperty("LAYER_5_PLUG-IN_CLIENT_PATH")).replace("/plugIns/", "/staticFunctions/") +staticFunction +"/StaticFunctionSettings.txt");
					
					settings.addProperties(plugInSettings.getPropertiesObject());
				}
			}
		}
	}


	/*public void addPlugInSettings(String plugInPath, String plugInName, Settings settings) {
		Settings plugInSettings = new Settings(plugInPath +  plugInName + "/PlugInSettings.txt");
		settings.addProperties(plugInSettings.getPropertiesObject());
		String requirements = plugInSettings.getProperty("SAME_LAYER_REQUIREMENTS");
		if (requirements != null)
			for (String requiredStaticFunction:requirements.split(","))
				settings.addProperties(
						plugInPath.replace("/plugIns/", "/staticFunctions/")
						+requiredStaticFunction
						+"/StaticFinctionSettings.txt"
						);
			
			
	
	}*/

}

