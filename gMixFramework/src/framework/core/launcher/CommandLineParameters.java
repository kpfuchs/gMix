/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2011  Karl-Peter Fuchs
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


public class CommandLineParameters {
	
	public boolean useGui = true;
	public ToolName gMixTool = null; // the "Launcher" to be started (see framework.core.launcher)
	public String globalConfigFile = null; // path to the config file to use for this run (all normal/other config files will be ignored)
	public String overwriteParameters = null; // list of parameters (key-value-pairs) to overwrite the normal or global config
	public String[] passthroughParameters = null; // tool-specific parameters
	

	public CommandLineParameters(String[] args) {
		
		Vector<String> parameters = new Vector<String>(Arrays.asList(args));
		Vector<String> unknownParameters = new Vector<String>();
		
		for(String par: parameters) {
			if (par.startsWith("-"))
				par = par.replaceFirst("-", "");
			
			if (par.equalsIgnoreCase("noGUI")) {
				useGui = false;
			} else if (par.startsWith("TOOL=")) {
				gMixTool = stringToToolName(par.split("=")[1]);
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
}

