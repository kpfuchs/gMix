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

import java.util.logging.Logger;


/**
 * Simulates several clients to test an existing cascade of mixes. The clients' 
 * behavior can be influenced using the property file 
 * <code>TestEnvironmentProperties.txt</code>. General settings are taken from 
 * there as well.
 * 
 * @author Karl-Peter Fuchs
 * 
 * @see mix.Mix
 * @see client.Client
 */
public class NetworkTest {

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
	private NetworkTest() {
		
	}
	
	
	/**
	 * Simulates several clients to test an existing cascade of mixes.
	 * 
	 * @param args Not used.
	 */
	public static void main(String[] args) {

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
