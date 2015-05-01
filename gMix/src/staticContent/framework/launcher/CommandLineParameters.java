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
package staticContent.framework.launcher;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import staticContent.framework.config.MatchingMechanism;
import staticContent.framework.config.Paths;
import staticContent.framework.config.Settings;


public class CommandLineParameters {
	
	public boolean useGui = true;
	public boolean useSimGui = false;
	public String configFile = null;
	public boolean userHasProvidedACustomConfigFile = false;
	public ToolName gMixTool = null; // the "Launcher" to be started (see framework.core.launcher)
	public String overwriteParameters = null; // list of parameters (key-value-pairs) to overwrite the normal or global config
	public String[] passthroughParameters = null; // tool-specific parameters
	
	private boolean settingsObjectCreated = false;
	private Settings settings;
	public String[] origArgs;
	
	
	public CommandLineParameters(String[] args) {
		init(args, null, null);
	}
	
	
	public CommandLineParameters(String[] args, ToolName gMixTool) {
		init(args, gMixTool, null);
	}

	
	public CommandLineParameters(String[] args, String config) {
		init(args, null, config);
	}
	
	
	public CommandLineParameters(String[] args, ToolName gMixTool, String config) {
		init(args, gMixTool, config);
	}
	
	
	private void init(String[] args, ToolName gMixTool, String config) {
		this.origArgs = args;
		if (gMixTool != null)
			this.gMixTool = gMixTool;
		Vector<String> parameters = new Vector<String>(Arrays.asList(args));
		Vector<String> unknownParameters = new Vector<String>();
		
		for(String par: parameters) {
			String senPar;
			if (par.startsWith("-"))
				senPar = (""+par).replaceFirst("-", "");
			else
				senPar = par;
			if (senPar.equalsIgnoreCase("nogui") || senPar.startsWith("noGUI")  || senPar.startsWith("NOGUI")  || senPar.startsWith("NO_GUI")) {
				useGui = false;
			} else if (senPar.startsWith("tool=") || senPar.startsWith("Tool=") || senPar.startsWith("TOOL=")) {
				if (gMixTool == null)
					this.gMixTool = ToolName.getToolByIdentifier(senPar.split("=")[1]);
			} else if (senPar.startsWith("config=") || senPar.startsWith("Config=")  || senPar.startsWith("CONFIG=") || senPar.startsWith("configfile=") || senPar.startsWith("ConfigFile=")  || senPar.startsWith("CONFIGFILE=") || senPar.startsWith("configFile=") || senPar.startsWith("CONFIG_FILE=")) {
				configFile = senPar.split("=")[1];
				userHasProvidedACustomConfigFile = true;
			} else if (senPar.startsWith("overwrite=") || senPar.startsWith("Overwrite=")  || senPar.startsWith("OVERWRITE=")) {
				senPar = senPar.replaceAll("'", "");
				senPar = senPar.replaceFirst("overwrite=", "");
				senPar = senPar.replaceFirst("Overwrite=", "");
				senPar = senPar.replaceFirst("OVERWRITE=", "");
				overwriteParameters = senPar;
			} else {
				unknownParameters.add(par);
			}
		}
		
		if (config != null)
			configFile = config;
		
		if (configFile == null) 
			configFile = new Settings(Paths.ANON_NODE_CONFIG_FILE_PATH +"default.txt").getProperty("DEFAULT_CONFIG");
		
		if (unknownParameters.size() > 0) {
			passthroughParameters = unknownParameters.toArray(new String[0]);
		} else {
			passthroughParameters = new String[0];
		}
	}
	
	
	@Override
	public String toString() {
		String s = "CommandLineParameters: \n";
		s += "NOGUI=" +!useGui +"\n";
		s += "TOOL=" +gMixTool +"\n";
		s += "CONFIGFILE=" +configFile +"\n";
		s += "OVERWRITE=" +overwriteParameters +"\n";
		List<String> c = Arrays.asList(passthroughParameters);
		s += "PASSTHROUGH_PARAMETERS=" +c +"\n";
		return s;
	}
	
	
	public Settings generateSettingsObject() {
		if (settingsObjectCreated)
			return settings;
		settingsObjectCreated = true;
		settings = new Settings(Paths.PATH_TO_PATH_CONFIG_FILE);
		settings.addProperties(Paths.ANON_NODE_CONFIG_FILE_PATH + configFile);
		settings.addProperties(Paths.LG_PROPERTY_FILE_PATH); // load generator properties
		
		if (gMixTool != ToolName.SIMULATOR) // load plug-in settings (unless we want to start the simulator which does not use those plug-ins)
			loadPluginSettings(settings, overwriteParameters);
		
		// settigns in config file have higher priority than normal plug-in settings -> overwrite (again)
		settings.addProperties(Paths.ANON_NODE_CONFIG_FILE_PATH + configFile);
		
		if (overwriteParameters != null) // overwriteParameters have higher priority than all other settings -> overwrite (again)
			Settings.overwriteSettings(settings.getPropertiesObject(), overwriteParameters, false);
		
		// validate composition
		boolean validateConfig = settings.getPropertyAsBoolean("VALIDATE_CONFIG");
		if (validateConfig && !MatchingMechanism.isConfigValid(settings))
			throw new RuntimeException("invalid plug-in composition"); 
		
		return settings;
	}
	
	
	public static Settings loadPluginSettings(String pathToPluginComposition, String overwriteParameters) {
		Settings settings = new Settings(pathToPluginComposition);
		loadPluginSettings(settings, overwriteParameters);
		return settings;
	}
	
	
	public static Settings loadPluginSettings(String pathToPluginComposition) {
		Settings settings = new Settings(pathToPluginComposition);
		loadPluginSettings(settings, null);
		return settings;
	}
	
	
	public static void loadPluginSettings(Settings settings, String overwriteParameters) {
		if (overwriteParameters != null) // overwrite so that loadPluginSettings() will load the right configurations
			Settings.overwriteSettings(settings.getPropertiesObject(), overwriteParameters, true);
		for (int i=1; i<=5; i++) {
			loadSettingsForPlugin(settings, "LAYER_" +i +"_PLUG-IN_CLIENT");
			loadSettingsForPlugin(settings, "LAYER_" +i +"_PLUG-IN_MIX");
		}
		if (settings.getProperty("GLOBAL_ROUTING_PLUG-IN") != null)
			settings.addProperties(Paths.getProperty("GLOBAL_ROUTING_PLUG-IN_PATH") +settings.getProperty("GLOBAL_ROUTING_PLUG-IN") +"/PlugInSettings.txt");
		if (settings.getProperty("DYNAMIC_ROUTING_PLUG-IN") != null)
			settings.addProperties(Paths.getProperty("DYNAMIC_ROUTING_PLUG-IN_PATH") +settings.getProperty("DYNAMIC_ROUTING_PLUG-IN") +"/PlugInSettings.txt");
		if (settings.getProperty("SOURCE_ROUTING_PLUG-IN") != null)
			settings.addProperties(Paths.getProperty("SOURCE_ROUTING_PLUG-IN_PATH") +settings.getProperty("SOURCE_ROUTING_PLUG-IN") +"/PlugInSettings.txt");
	}

	
	public static void loadSettingsForPlugin(Settings settings, String key) {
		Settings plugInSettings = new Settings(Paths.getProperty(key +"_PATH") +settings.getProperty(key) +"/PlugInSettings.txt");
		
		String requirementsStr = plugInSettings.getProperty("SAME_LAYER_REQUIREMENTS");
		if (requirementsStr != null && !requirementsStr.equals(""))
			for (String staticFunction: requirementsStr.split(","))
				plugInSettings.addProperties((Paths.getProperty(key +"_PATH")).replace("/layerPlugIns/", "/staticFunctionPlugIns/") +staticFunction +"/StaticFunctionSettings.txt");
		
		requirementsStr = plugInSettings.getProperty("LAYER_1_CLIENT_REQUIREMENTS");
		if (requirementsStr != null && !requirementsStr.equals(""))
			for (String staticFunction: requirementsStr.split(","))
				plugInSettings.addProperties((Paths.getProperty("LAYER_1_PLUG-IN_CLIENT_PATH")).replace("/layerPlugIns/", "/staticFunctionPlugIns/") +staticFunction +"/StaticFunctionSettings.txt");
		
		requirementsStr = plugInSettings.getProperty("LAYER_1_MIX_REQUIREMENTS");
		if (requirementsStr != null && !requirementsStr.equals(""))
			for (String staticFunction: requirementsStr.split(","))
				plugInSettings.addProperties((Paths.getProperty("LAYER_1_PLUG-IN_MIX_PATH")).replace("/layerPlugIns/", "/staticFunctionPlugIns/") +staticFunction +"/StaticFunctionSettings.txt");
		
		requirementsStr = plugInSettings.getProperty("LAYER_2_CLIENT_REQUIREMENTS");
		if (requirementsStr != null && !requirementsStr.equals(""))
			for (String staticFunction: requirementsStr.split(","))
				plugInSettings.addProperties((Paths.getProperty("LAYER_2_PLUG-IN_CLIENT_PATH")).replace("/layerPlugIns/", "/staticFunctionPlugIns/") +staticFunction +"/StaticFunctionSettings.txt");
		
		requirementsStr = plugInSettings.getProperty("LAYER_2_MIX_REQUIREMENTS");
		if (requirementsStr != null && !requirementsStr.equals(""))
			for (String staticFunction: requirementsStr.split(","))
				plugInSettings.addProperties((Paths.getProperty("LAYER_2_PLUG-IN_MIX_PATH")).replace("/layerPlugIns/", "/staticFunctionPlugIns/") +staticFunction +"/StaticFunctionSettings.txt");
		
		requirementsStr = plugInSettings.getProperty("LAYER_3_MIX_REQUIREMENTS");
		if (requirementsStr != null && !requirementsStr.equals(""))
			for (String staticFunction: requirementsStr.split(","))
				plugInSettings.addProperties((Paths.getProperty("LAYER_3_PLUG-IN_MIX_PATH")).replace("/layerPlugIns/", "/staticFunctionPlugIns/") +staticFunction +"/StaticFunctionSettings.txt");
		
		requirementsStr = plugInSettings.getProperty("LAYER_3_CLIENT_REQUIREMENTS");
		if (requirementsStr != null && !requirementsStr.equals(""))
			for (String staticFunction: requirementsStr.split(","))
				plugInSettings.addProperties((Paths.getProperty("LAYER_4_PLUG-IN_CLIENT_PATH")).replace("/layerPlugIns/", "/staticFunctionPlugIns/") +staticFunction +"/StaticFunctionSettings.txt");
		
		requirementsStr = plugInSettings.getProperty("LAYER_4_MIX_REQUIREMENTS");
		if (requirementsStr != null && !requirementsStr.equals(""))
			for (String staticFunction: requirementsStr.split(","))
				plugInSettings.addProperties((Paths.getProperty("LAYER_4_PLUG-IN_MIX_PATH")).replace("/layerPlugIns/", "/staticFunctionPlugIns/") +staticFunction +"/StaticFunctionSettings.txt");
		
		requirementsStr = plugInSettings.getProperty("LAYER_4_CLIENT_REQUIREMENTS");
		if (requirementsStr != null && !requirementsStr.equals(""))
			for (String staticFunction: requirementsStr.split(","))
				plugInSettings.addProperties((Paths.getProperty("LAYER_4_PLUG-IN_CLIENT_PATH")).replace("/layerPlugIns/", "/staticFunctionPlugIns/") +staticFunction +"/StaticFunctionSettings.txt");
		
		requirementsStr = plugInSettings.getProperty("LAYER_5_MIX_REQUIREMENTS");
		if (requirementsStr != null && !requirementsStr.equals(""))
			for (String staticFunction: requirementsStr.split(","))
				plugInSettings.addProperties((Paths.getProperty("LAYER_5_PLUG-IN_MIX_PATH")).replace("/layerPlugIns/", "/staticFunctionPlugIns/") +staticFunction +"/StaticFunctionSettings.txt");
		
		requirementsStr = plugInSettings.getProperty("LAYER_5_CLIENT_REQUIREMENTS");
		if (requirementsStr != null && !requirementsStr.equals(""))
			for (String staticFunction: requirementsStr.split(","))
				plugInSettings.addProperties((Paths.getProperty("LAYER_5_PLUG-IN_CLIENT_PATH")).replace("/layerPlugIns/", "/staticFunctionPlugIns/") +staticFunction +"/StaticFunctionSettings.txt");
		
		settings.addProperties(plugInSettings.getPropertiesObject());
	}
	
}

