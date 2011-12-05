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

package mix;


import java.security.KeyPair;

import inputOutputHandler.InputOutputHandlerController;

import accessControl.AccessControlController;

import keyGenerator.KeyGeneratorController;

import outputStrategy.OutputStrategyController;

import userDatabase.UserDatabaseController;

import messageProcessor.MessageProcessorController;

import externalInformationPort.ExternalInformationPortController;


/**
 * Coordinates the components of this mix. Each component (offering methods) 
 * has its own (architecture) interface (see package 
 * <code>architectureInterfaces</code>) and a controller class (named 
 * <code>"ComponentName"Controller</code>), implementing the according 
 * interface. Therefore, the implementation of each component can change, 
 * without changes to any other component (interface and implementing 
 * (controller) class remain the same).
 * <p>
 * General information: A mix is some kind of proxy that attempts to hide the 
 * correspondence between its incoming and outgoing messages, as proposed by 
 * David Chaum in 1981.
 * <p>
 * This mix implements the following functions (and others):
 * <ul>
 * <li> collecting messages (batch strategy with timeout)
 * <li> recoding messages (hybrid cryptography)
 * <li> sorting messages (recursive divide and conquer)
 * <li> detecting replays (based on timestamps and hash tables, constant time)
 * <li> validating integrity (using MACs)
 * <li> padding messages
 * <li> channels
 * <li> duplex communication
 * <li> clock synchronization (SNTP)
 * <li> dynamic reply length (depending on traffic situation)
 * <li> generating log files
 * <li> exchange of certain types of information with communication partners
 * <li> parallel message processing (e. g. recoding)
 * <li> non-blocking I/O between clients and mix
 * <li> multiplexing between mixes
 * </ul>
 * <p>
 * A test environment is found in package <code>testEnvironment</code>.
 * 
 * @author Karl-Peter Fuchs
 */
public final class Mix {

	
	/**
	 * Creates a new <code>Mix</code> which will use the bypassed <code>KeyPair
	 * </code> for asymmetric cryptography.
	 * <p>
	 * Instantiates an coordinates the components needed to mix messages.
	 * 
	 * @param keyPair	<code>KeyPair</code> that shall be used for asymmetric 
	 * 					cryptography.
	 */
	public Mix(KeyPair keyPair) {	
		
		// generate components
		UserDatabaseController userDatabase = new UserDatabaseController();
		AccessControlController accessControl = new AccessControlController();
		
		OutputStrategyController outputStrategy = 
			new OutputStrategyController();
		
		ExternalInformationPortController externalInformationPort = 
			new ExternalInformationPortController();
		
		InputOutputHandlerController inputOutputHandler = 
			new InputOutputHandlerController();
		
		MessageProcessorController messageProcessor = 
			new MessageProcessorController();
		
		MessageProcessorController recoder = 
			new MessageProcessorController();
		
		// initialize components
		userDatabase.initialize();
		recoder.initialize(keyPair);
		
		externalInformationPort.initialize(
				keyPair.getPublic(),
				recoder
				);
		
		externalInformationPort.acceptRequests();
		
		inputOutputHandler.initialize(	userDatabase,
										outputStrategy,
										externalInformationPort
										);
		
		outputStrategy.initialize(inputOutputHandler);
		
		messageProcessor.initialize(	keyPair,
										inputOutputHandler,
										accessControl,
										outputStrategy
										);
		
		
		// wait for connections
		inputOutputHandler.acceptConnections();
		
	}
	
	
	/**
	 * Creates a new <code>Mix</code> which will generate its own <code>KeyPair
	 * </code> asymmetric cryptography.
	 * <p>
	 * Instantiates an coordinates the components needed to mix messages.
	 */
	public Mix() {

		this(	(KeyPair)
				new KeyGeneratorController().generateKey(
						KeyGeneratorController.KEY_PAIR)
						);
		
	}
	
	
	/**
	 * Creates a new <code>Mix</code>.
	 * 
	 * @param argv	Not used.
	 */
	public static void main(String[] argv) {

		new Mix();

	}

}