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

package framework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;

public class FrameworkStarter {

	public static String[] args = new String[0];
	
	public static void main(String[] args) {
		FrameworkStarter.args = args;
		boolean finished = false;
		String command = null;
		
		if (args.length != 0) {
			finished = execute(args[0]);
			if (!finished) {
				System.out.println("THE SPECIFIED PARAMETER \"" +args[0] +"\" IS NOT VALID"); 
				displayCommandLineOptions();
			}
		}
		
		while (!finished) {
			command = getUserInput();
			finished = execute(command);
		}
		
	} 
	
	
	// @return true == execution successful -> exit ; false == execution not successful -> user must chose what to start (-> display menu)
	private static boolean execute(String command) {
		if (command.equals("?") || command.equals("-?") || command.equalsIgnoreCase("help")  || command.equalsIgnoreCase("-help")){
			displayCommandLineOptions();
			return false;
		} else if (command.equals("1") || command.equalsIgnoreCase("-localTest") || command.equalsIgnoreCase("localTest")) {
			// TODO: display warning if xmx not set correctly
			// TODO: display warning if -ea not set
			testEnvironment.LocalTest.main(args);
		} else if (command.equals("2") || command.equalsIgnoreCase("-infoService") || command.equalsIgnoreCase("infoService")) {
			infoService.InfoServiceServer_v1.main(args);
		} else if (command.equals("3") || command.equalsIgnoreCase("-mix") || command.equalsIgnoreCase("mix")) {
			// TODO: display warning if xmx not set correctly
			framework.Mix.main(args);
		} else if (command.equals("4") || command.equalsIgnoreCase("-client") || command.equalsIgnoreCase("client")) {
			client.ClientController.main(args);
		} else if (command.equals("5") || command.equalsIgnoreCase("-mixAndClient") || command.equalsIgnoreCase("mixAndClient")) {
			// TODO: display warning if xmx not set correctly
			new Thread(new Runnable() {public void run() {framework.Mix.main(FrameworkStarter.args);}}).start(); 
			client.ClientController.main(args);
		} else if (command.equals("6") || command.equalsIgnoreCase("-loadGenerator") || command.equalsIgnoreCase("loadGenerator")) {
			// TODO: display warning if xmx not set correctly
			testEnvironment.LoadGenerator.main(args);
		} else if (command.equals("7") || command.equalsIgnoreCase("-simulator") || command.equalsIgnoreCase("simulator")) {
			// TODO: display warning if xmx not set correctly
			simulator.core.Simulator.main(args);
		} else if (command.equalsIgnoreCase("q")) {
			System.exit(0);
		} else {
			return false;
		}
		return true;
	}
	
	private static String getUserInput() {
		return getUserInput(null);
	}
	
	private static String getUserInput(String errorMessage) {
		System.out.println("gMix Framework - unstable pre alpha release v0.002 (" +Calendar.getInstance().get(Calendar.YEAR) +")");
		System.out.println("https://svs.informatik.uni-hamburg.de/gmix/"); 
		//System.out.println("This program comes with ABSOLUTELY NO WARRANTY. Type [w] for more info"); 
		System.out.println(); 
		System.out.println("note: the \"normal\" way to start the gMix framework is via \"java [-JavaOptions] -jar gMixFramework.jar [-gMixOptions]\"");
		System.out.println("      configuration is done via the txt files in the same folder as this jar file"); 
		System.out.println("      configuration via this command line tool is not supported"); 
		System.out.println();
		System.out.println("type [?] for further details or choose one of the following options:");
		System.out.println();
		System.out.println("     [1] localTest       starts mixes, clients etc in this vm"); 
		System.out.println("     [2] infoService     starts the information service"); 
		System.out.println("     [3] mix             starts a mix"); 
		System.out.println("     [4] client          starts a client");
		System.out.println("     [5] mixAndClient    starts both a mix and client");
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
		return readString();
	}
	
	
	private static void displayCopyright() {
		System.out.println();
		System.out.println("---"); 
		System.out.println("gMix framework - Copyright (C) " +Calendar.getInstance().get(Calendar.YEAR) +" SVS"); 
		System.out.println("https://svs.informatik.uni-hamburg.de/gmix/"); 
		System.out.println("This program comes with ABSOLUTELY NO WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>");
		System.out.println("---"); 
	}
	
	
	private static void displayCommandLineOptions() {
		System.out.println("Usage: \"java [-JavaOptions] -jar gMixFramework.jar [-gMixOptions]\"");
		System.out.println(); 
		System.out.println("where [-gMixOptions] include:");
		System.out.println("     -localTest              starts mixes, clients, information service and load genertor in a single java vm. all components communicate via localhost/loopback"); 
		System.out.println("     -infoService            starts the information service (used by clients and mixes for discovery and exchanging public data like ip addresses or public keys)"); 
		System.out.println("     -mix                    starts a mix"); 
		System.out.println("     -client                 starts a client");
		System.out.println("     -mixAndClient           starts both a mix and client");
		System.out.println("     -loadGenerator          starts the load generator (can be used to start and simulate the behaviour of several clients for testing or performance evaluation)");
		System.out.println("     -simulator              starts a discrete event simulator");
		System.out.println(); 
		System.out.println("suggested [-JavaOptions] include:");
		System.out.println("     -ea                     to enable assertions for debugging"); 
		System.out.println("     -Xms<size> -Xmx<size>   to associate more ram (Java heap size). example: \"-Xms1024m -Xmx1024m\" to associate 1 gigabyte."); 
		System.out.println();
		System.out.println("if you want to run a class not specified in [-gMixOptions] use: \"java -cp gMixFramework.jar packagaName.ClassName\""); 
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
