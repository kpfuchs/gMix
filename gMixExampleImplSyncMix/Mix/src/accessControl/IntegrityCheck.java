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

package accessControl;


import internalInformationPort.InternalInformationPortController;

import message.ChannelEstablishMessage;
import message.ChannelMessage;
import message.Message;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.SecretKey;


/**
 * Checks integrity of a message by generating a local message authentication 
 * code and comparing it to the one included in the message.
 * <p>
 * This class is thread-safe.
 * 
 * @author Karl-Peter Fuchs
 */
final class IntegrityCheck {
	
	/** 
	 * Reference on component <code>InternalInformationPort</code>. 
	 * Used to display and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = internalInformationPort.getLogger();
	
	
	/**
	 * Empty constructor. Never used since all methods are static.
	 */
	private IntegrityCheck() {
			
	}
	
	
	/**
	 * Generates and returns a message authentication code for the bypassed 
	 * message. Method is thread-safe.
	 * 
	 * @param key		Key to generate message authentication code with.
	 * @param message	Message to be authenticated.
	 * 
	 * @return			Message authentication code for the bypassed message.	
	 */
	private static byte[] generateMAC(Key key, byte[] message) {
		
		
		String MAC_ALGORITHM = 
			internalInformationPort.getProperty("MAC_ALGORITHM");
		
		Mac macGenerator = null;
		
		try {
			
			macGenerator = Mac.getInstance(MAC_ALGORITHM);
			
		} catch (NoSuchAlgorithmException e) {
			
			LOGGER.severe(	"(IntegrityCheck) Invalid \"MAC_ALGORITHM\" "
							+"specified in property file!" 
							+e.getMessage()
							);
			
			System.exit(1);
			
		}
		
		try {
			
			macGenerator.init(key);
			
		} catch (InvalidKeyException e) {
			
			LOGGER.fine(	"(IntegrityCheck) Invalid key!"
							+e.getMessage()
							);
			
		}

		return macGenerator.doFinal(message);
	
	}
	
	
	/**
	 * Checks if the bypassed message has integrity. Method is thread-safe.
	 * 
	 * @param message	Message to check.
	 * 
	 * @return			Whether message authentication code is correct or not.
	 */
	protected static boolean isMACCorrect(Message message) {
		
		if (message instanceof ChannelEstablishMessage) {
			
			return isMACCorrect((ChannelEstablishMessage)message);
		
		} else {
			
			return isMACCorrect((ChannelMessage)message);
		
		}
		 
	 }
	
	 
	/**
	 * Checks if the bypassed <code>ChannelEstablishMessage</code> has 
	 * integrity. Method is thread-safe.
	 * 
	 * @param message	Message to check.
	 * 
	 * @return			Whether message authentication code is correct or not.
	 */
	private static boolean isMACCorrect(ChannelEstablishMessage message) {
		
		boolean isProperlyAuthenticated;		
		SecretKey macKey = message.getChannel().getMacKey();
		byte[] signedData = message.getSignedData();	
		byte[] locallyGeneratedMAC = generateMAC(macKey, signedData);
		byte[] receivedMac = message.getMAC();
		
		isProperlyAuthenticated = Arrays.equals(locallyGeneratedMAC,
												receivedMac
								  				);
		
		if (!isProperlyAuthenticated) {
			
			LOGGER.fine("(IntegrityCheck) Wrong MAC!");
			LOGGER.finer(message.toString());
			
		}

		return isProperlyAuthenticated;
		
	}
	
	
	/**
	 * Checks if the bypassed <code>ChannelMessage</code> has integrity. 
	 * Method is thread-safe.
	 * 
	 * @param message	Message to check.
	 * 
	 * @return			Whether message authentication code is correct or not.
	 */
	private static boolean isMACCorrect(ChannelMessage message) {
		
		boolean isProperlyAuthenticated;		
		SecretKey macKey = message.getChannel().getMacKey();
		byte[] signedData = message.getSignedData();
		byte[] locallyGeneratedMAC = generateMAC(macKey, signedData);
		byte[] receivedMac = message.getMAC();
		
		isProperlyAuthenticated = Arrays.equals(locallyGeneratedMAC,
												receivedMac
								  				);
		
		if (!isProperlyAuthenticated) {
			
			LOGGER.fine("(IntegrityCheck) Wrong MAC!");
			LOGGER.finer(message.toString());
			
		}

		return isProperlyAuthenticated;
		
	}
	
}
