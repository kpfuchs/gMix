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

import java.security.Key;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

import util.Util;

import client.Client;

import exception.InformationRetrieveException;

import externalInformationPort.ExternalInformationPortController;
import externalInformationPort.Information;


/**
 * Simulates a <code>Client</code>s behavior using its 
 * <code>InputStream</code> and <code>OutputStream</code> according to the 
 * following logic: "Send <code>K</code> bytes of data with a delay of 
 * <code>L</code> ms between sending, <code>M</code> times."
 * <p>
 * <code>K</code>, <code>L</code> and <code>M</code> are taken from the 
 * property file (<code>TestEnvironmentProperties.txt</code>).
 * 
 * @author Karl-Peter Fuchs
 */
final class ClientSimulator extends Thread {
	
	/** 
	 * Reference on component <code>InternalInformationPort</code>. 
	 * Used to display and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = internalInformationPort.getLogger();
	
	/** Random number generator. */
	private static Random random = new Random();

	/** Public keys of the mix used to encrypt messages. */
	private Key[] publicKeysOfMixes;
	
	
	/**
	 * Generates a new <code>ClientSimulator</code> that can be used to 
	 * simulates a <code>Client</code>s behavior using its 
	 * <code>InputStream</code> and <code>OutputStream</code> according to the 
	 * following logic: "Send <code>K</code> bytes of data with a delay of 
	 * <code>L</code> ms between sending, <code>M</code> times."
	 * <p>
	 * <code>K</code>, <code>L</code> and <code>M</code> are taken from the 
	 * property file (<code>TestEnvironmentProperties.txt</code>).
	 */
	protected ClientSimulator() {

		this.publicKeysOfMixes = getPublicKeys();
		
		// start ClientSimulator
		start();
	}

	
	/**
	 * Simulates a <code>Client</code>'s behavior.
	 */
	@Override
	public void run() {
		
		try {
			
			// generate Client
			Client client = new Client(publicKeysOfMixes);
			client.connect();
			InputStream cascadeInputStream = client.getInputStream();
			OutputStream cascadeOutputStream = client.getOutputStream();
			
			// "Send K bytes of data with a delay of L ms between sending, M 
			// times."
			
			int m = getVariable("M");
			for (int i=1; i<=m; i++) {
				
				// generate random message
				int size = getVariable("K");
				byte[] message = generateRandomMessage(size);
				
				LOGGER.fine(	"(Client" +Thread.currentThread()
								+") Starting to write " +i +". message ("
								+size +" bytes): \n" +new String(message)
								);
				
				
				// send message
				cascadeOutputStream.write(message);
				cascadeOutputStream.flush();
				
				
				// submit message id to ReceivalCheck
				int messageID = 
					new BigInteger(Arrays.copyOf(message, 9)).abs().intValue();
				
				ReceivalCheck.addSentMessage(messageID);			
				
				
				// receive reply
				byte[] reply = new byte[size];
				reply = Util.forceRead(cascadeInputStream, reply);
				
				LOGGER.fine(	"(Client" +Thread.currentThread()
								+") Received " +i +". reply ("
								+reply.length +" bytes): \n" +new String(reply)
								);
				
				
				// submit message id to ReceivalCheck
				messageID = 
					new BigInteger(Arrays.copyOf(reply, 9)).abs().intValue();
				
				ReceivalCheck.addReceivedMessage(messageID);
				
				
				// add delay
				if (i != m) { // not last round
					
					try {
						
						Thread.sleep((long)ClientSimulator.getVariable("L"));
						
					} catch (InterruptedException e) {
						
						LOGGER.severe(e.getMessage());
						continue;
						
					}
					
				}
				
			}
			
			// disconnect
			
			LOGGER.fine(	"(Client" +Thread.currentThread()
							+") Disconnecting since all data is written!"
							);
			
			client.disconnect();
			
				
		} catch (IOException e) {
			
			LOGGER.warning(	"(ClientSimulator) Message could not be sent! "
							+e.getMessage()
							);
		}
		
	}
	
	
	/**
	 * Generates a random byte message (each byte represents a number between 
	 * 0 and 9 (both inclusive; ANSI)).
	 * 
	 * @param size	Length of the random byte message to be created.
	 * 
	 * @return	A random byte message.
	 */
	private byte[] generateRandomMessage(int size) {
		
		if (size < 10) { // minimum length is 10, since first 9 digits 
						 // are used as message id
			size = 10;
			
		}
		
		String message = "";
		
		for(int j=0; j<size; j++) {
			
			message += (char)(random.nextInt(10)+'0');
			
		}
		
		return message.getBytes();
		
	}
	
	
	/**
	 * Returns the value of one of the variables <code>X, Y, Z, K, L,</code> or 
	 * <code>M</code> from property file (as specified by the bypassed value).
	 * 
	 * @param variableKey	Name (=key) of the variable (=property) to be 
	 * 						returned.
	 * 
	 * @return 				Value of the specified variable.
	 */
	protected static int getVariable(String variableKey) {
		
		String valueFromPropertyFile = Settings.getProperty(variableKey);
		
		if (valueFromPropertyFile.charAt(0) == 'R') { // random value requested
			
			return random.nextInt(getUpperBound(valueFromPropertyFile) + 1);
			
		} else { // absolute value specified
			
			return new Integer(valueFromPropertyFile);
		}
		
	}
	
	
	/**
	 * Returns <code>x</code> from a bypassed String of form 
	 * <code>RANDOM-x</code>, where <code>x</code> is a number (e. g. 
	 * "RANDOM-1000"). Used to get the upper bound for a randomly chosen value 
	 * for one of the variables <code>X, Y, Z, K, L,</code> or <code>M</code> 
	 * as specified by the user in property file.
	 * 
	 * @param expression	String containing the upper bound.
	 * 
	 * @return				Upper bound extracted from the bypassed String.
	 */
	private static int getUpperBound(String expression) {
		
		expression = expression.substring(7, expression.length());
		
		return new Integer(expression);
		
	}
	
	
	/**
	 * Retrieves the public keys of all mixes in the cascade (address of first 
	 * mix must be specified in property file) for later encryption.
	 * 
	 * @return	The public keys of all mixes in the cascade.
	 */
	private static PublicKey[] getPublicKeys() {
		
		PublicKey[] publicKeys = null;
		
		try {
			
			ExternalInformationPortController externalInformationPort = 
				new ExternalInformationPortController();
			
			externalInformationPort.initialize();
			
			InetAddress informationProviderAddress = 
				InetAddress.getByName(
						internalInformationPort.getProperty("CASCADE_ADDRESS")
						);
			
			int informationProviderPort = 
				new Integer(
						internalInformationPort.getProperty("CASCADE_INFO_PORT")
						);
			
			
			publicKeys = 
				(PublicKey[]) externalInformationPort.getInformationFromAll(
									informationProviderAddress,
									informationProviderPort,
									Information.PUBLIC_KEY
									);
			
		} catch (InformationRetrieveException e) {
			
			LOGGER.severe(e.getMessage());
			System.exit(1);
			
		} catch (UnknownHostException e) {
			
			LOGGER.severe(e.getMessage());
			System.exit(1);
			
		}
		
		return publicKeys;
		
	}
	
}