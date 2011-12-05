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

package testEnvironment;


import internalInformationPort.InternalInformationPortController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import mix.Mix;


/**
 * Sets up a test environment on <code>localhost</code>, consisting of a 
 * mix cascade and several clients. The clients' behavior can be influenced 
 * using the property file <code>TestEnvironmentProperties.txt</code>. General 
 * settings are taken from there as well (like the number of mixes/clients to 
 * be simulated).
 * 
 * @author Karl-Peter Fuchs
 * 
 * @see mix.Mix
 * @see client.Client
 */
public class LocalTest {

	/** 
	 * Reference on component <code>InternalInformationPort</code>. 
	 * Used to display and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = internalInformationPort.getLogger();
	
	
	/** 
	 * Empty constructor.
	 */
	private LocalTest() {
		
	}

	
	/**
	 * Sets up a test environment on <code>localhost</code>, consisting of a 
	 * mix cascade and several clients. The clients' behavior can be influenced 
	 * using the property file <code>TestEnvironmentProperties.txt</code>. 
	 * General settings are taken from there as well (like the number of 
	 * mixes/clients to be simulated).
	 * 
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		
		// load values from property file
		int numberOfMixesInCascade = 
			new Integer(Settings.getProperty("NUMBER_OF_MIXES_IN_CASCADE"));
		
		int startPortOfPortRange = 
			new Integer(Settings.getProperty("START_PORT_OF_PORT_RANGE"));
		
		int infoStartPortOfPortRange = 
			new Integer(Settings.getProperty("INFO_START_PORT_OF_PORT_RANGE"));
		
		// set up mix cascade
	    InetAddress localHostAddress = null;
	    
	    try {
	    	
		    localHostAddress = InetAddress.getLocalHost();
 
	    }	catch (UnknownHostException e) {
	    	e.printStackTrace();
	    	LOGGER.severe(e.getMessage());
			System.exit(1);
			
		}
	    
	    startPortOfPortRange += numberOfMixesInCascade;
	    infoStartPortOfPortRange += numberOfMixesInCascade;
	    
		for (	int positionInCascade = numberOfMixesInCascade;
				positionInCascade > 0; 
				positionInCascade--) {

			int numberOfFurtherHops = 
				numberOfMixesInCascade - positionInCascade;
			
			int numberOfFurtherReplyHops = 
				numberOfMixesInCascade + 
				(positionInCascade - numberOfMixesInCascade - 1);

			if (positionInCascade == numberOfMixesInCascade) {
				
				internalInformationPort.setProperty(
						"POSITION_OF_MIX_IN_CASCADE", (""+3)
						);
				
			} else if (positionInCascade == 1) {
				
				internalInformationPort.setProperty(
						"POSITION_OF_MIX_IN_CASCADE", (""+1)
						);
				
			} else {
				
				internalInformationPort.setProperty(
						"POSITION_OF_MIX_IN_CASCADE", (""+2)
						);
				
			}
			
			internalInformationPort.setProperty(
					"PORT", (""+(--startPortOfPortRange))
					);
			
			internalInformationPort.setProperty(	"BIND_ADDRESS",
											localHostAddress.getHostName()
											);
			
			internalInformationPort.setProperty(	"CASCADE_ADDRESS",
											localHostAddress.getHostName()
											);
			
			internalInformationPort.setProperty(
					"NEXT_MIX_PORT", (""+(startPortOfPortRange + 1))
					);
			
			internalInformationPort.setProperty(
					"NEXT_MIX_ADDRESS", localHostAddress.getHostName()
					);
			
			internalInformationPort.setProperty(
					"PREVIOUS_MIX_ADDRESS", localHostAddress.getHostName()
					);
			
			internalInformationPort.setProperty(
					"PREVIOUS_MIX_PORT", (""+(startPortOfPortRange - 1))
					);
			
			internalInformationPort.setProperty(
					"NUMBER_OF_FURTHER_MIXES", (""+numberOfFurtherHops)
					);
			
			internalInformationPort.setProperty(
					"NUMBER_OF_PREVIOUS_MIXES", (""+numberOfFurtherReplyHops)
					);
			
			internalInformationPort.setProperty(
					"INFO_PORT", (""+(--infoStartPortOfPortRange))
					);
			
			internalInformationPort.setProperty(
					"INFO_BIND_ADDRESS", localHostAddress.getHostName()
					);
			
			internalInformationPort.setProperty(
					"NEXT_MIX_INFO_PORT", (""+(infoStartPortOfPortRange + 1))
					);
			
			internalInformationPort.setProperty(
					"PREVIOUS_MIX_INFO_PORT", 
					(""+(infoStartPortOfPortRange - 1))
					);
	
			new Mix();
				
		}
		
		// set up ClientSimulator(s)
		int targetNumberOfClients = ClientSimulator.getVariable("Z");
		int numberOfStartedClients = 0;
		
		while (numberOfStartedClients < targetNumberOfClients) {
			// "Start X clients every Y ms until Z clients are started!"	
			
			int x = ClientSimulator.getVariable("X");
			
			for (int i=0; i<x; i++) {
				
				new ClientSimulator();
				
			}
			
			numberOfStartedClients += x;
			
			try {
				
				Thread.sleep((long)ClientSimulator.getVariable("Y"));
				
			} catch (InterruptedException e) {
				
				LOGGER.severe(e.getMessage());
				continue;
				
			}
			
		}

	}

}
