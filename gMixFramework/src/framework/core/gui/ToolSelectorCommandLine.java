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
package framework.core.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;

import framework.core.launcher.CommandLineParameters;
import framework.core.launcher.ToolName;


public class ToolSelectorCommandLine {

	
	public ToolSelectorCommandLine(CommandLineParameters params) {
		if (params.gMixTool != null && params.gMixTool != ToolName.NOT_SET) // start desired tool directly
			ToolName.execute(params);
		else
			displayCommandLineOptions();
		params.gMixTool = getUserInput();
		ToolName.execute(params);
	}
	
	
	private static ToolName getUserInput() {
		ToolName t = ToolName.NOT_SET;
		do {
			displayUserInputOptions(null);
			String userInput = readString();
			if (userInput.equals("?") || userInput.equalsIgnoreCase("help"))
				displayCommandLineOptions();
			else if (userInput.equalsIgnoreCase("q"))
				System.exit(0);
			t = ToolName.getToolByIdentifier(userInput);
		} while (t == ToolName.NOT_SET);
		return t;
	}
	
	
	private static void displayUserInputOptions(String errorMessage) {
		System.out.println("gMix Framework - unstable pre alpha release v0.003 (" +Calendar.getInstance().get(Calendar.YEAR) +")");
		System.out.println("https://svs.informatik.uni-hamburg.de/gmix/"); 
		//System.out.println("This program comes with ABSOLUTELY NO WARRANTY. Type [w] for more info"); 
		System.out.println(); 
		System.out.println("note: the \"normal\" way to start the gMix framework is via \"java [-JavaOptions] -jar gMixFramework.jar [noGUI] [gMixTool] [toolParameters]\"");
		System.out.println("      configuration is done via the config files in the \"inputOutput\" folder"); 
		System.out.println("      configuration via this command line tool is not supported"); 
		System.out.println();
		System.out.println("type [?] for further details or choose one of the following options:");
		System.out.println();
		System.out.println("     [1] localTest       starts mixes, clients etc in this vm"); 
		System.out.println("     [2] infoService     starts the information service"); 
		System.out.println("     [3] mix             starts a mix"); 
		System.out.println("     [4] client          starts a client");
		System.out.println("     [5] p2p             starts both a mix and client");
		System.out.println("     [6] loadGenerator   starts the load generator"); 
		System.out.println("     [7] simulator       starts a discrete event simulator");
		System.out.println(); 
		System.out.println("     [q] quit            exits the program"); 
		displayCopyright();
		System.out.println();
		if (errorMessage == null) {
			System.out.print("your choice: ");
		} else {
			System.out.println(errorMessage); 
			System.out.print("new choice: ");
		}
	}
	
	
	private static void displayCopyright() {
		System.out.println();
		System.out.println("---"); 
		System.out.println("gMix framework - Copyright (C) " +Calendar.getInstance().get(Calendar.YEAR) +" SVS"); 
		System.out.println("https://svs.informatik.uni-hamburg.de/gmix/"); 
		System.out.println("This program comes with ABSOLUTELY NO WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>");
		System.out.println("---"); 
	}
	
	
	public static void displayCommandLineOptions() {
		System.out.println("Usage: \"java [JavaOptions] -jar gMixFramework.jar [noGUI] [gMixTool] [toolParameters]\"");
		System.out.println(); 
		System.out.println("   starting with [noGUI] (e.g. \"java -jar gMixFramework.jar -noGUI -TOOL=mix\") will start the framework in command line mode");
		System.out.println(); 
		System.out.println("   available [gMixTool]s:");
		System.out.println("     -TOOL=localTest                     starts mixes, clients, information service and load genertor in a single java vm. all components communicate via localhost/loopback"); 
		System.out.println("     -TOOL=infoService                   starts the information service (used by clients and mixes for discovery and exchanging public data like ip addresses or public keys)"); 
		System.out.println("     -TOOL=mix                           starts a mix"); 
		System.out.println("     -TOOL=client                        starts a client");
		System.out.println("     -TOOL=p2p                           starts both a mix and client");
		System.out.println("     -TOOL=loadGenerator                 starts the load generator (can be used to start and simulate the behaviour of several clients for testing or performance evaluation)");
		System.out.println("     -TOOL=simulator                     starts a discrete event simulator");
		System.out.println();
		System.out.println("   possible [toolParameters]:");
		System.out.println("     -GLOBAL_CONFIG=filename.txt    will ignore all current configuration files and execute the [gMixTool] with the configuration provided in the file \"filename.txt\". Example: \"java -jar gMixFramework.jar -noGUI -TOOL=mix -GLOBAL_CONFIG=filename.txt\""); 
		System.out.println("     -OVERWRITE='[KEY_1=VALUE_1],[KEY_2=VALUE_2],...[KEY_n=VALUE_n]'    will overwrite all keys ([KEY]) in the current configuration with the values ([VALUE]) provided. Example: \"java -jar gMixFramework.jar -noGUI -TOOL=mix -OVERWRITE='INFO_SERVICE_ADDRESS=10.1.1.11,INFOSERVICE_PORT=22002'\"");
		System.out.println(); 
		System.out.println("   suggested [-JavaOptions] include:");
		System.out.println("     -ea                     to enable assertions for debugging"); 
		System.out.println("     -Xms<size> -Xmx<size>   to associate more ram (Java heap size). example: \"-Xms1024m -Xmx1024m\" to associate 1 gigabyte."); 
		System.out.println();
		
		System.out.println();
		System.out.println("if you want to run a class not specified in [-gMixTool]s use: \"java -cp gMixFramework.jar packagaName.ClassName\""); 
		// TODO: include description for property files
		waitForReturn();
	}
	
	
	private static String readString() {
		try {
    		BufferedReader input =  new BufferedReader(new InputStreamReader(System.in));
    		return input.readLine();
    	} catch (IOException e) {
    		e.printStackTrace();
    		return readString();
    	}
    }
	
	
	private static void waitForReturn() {
		System.out.println("press [RETURN] to continue"); 
		readString();
	}
	
}
