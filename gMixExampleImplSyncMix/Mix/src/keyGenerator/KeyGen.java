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

package keyGenerator;


import internalInformationPort.InternalInformationPortController;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.logging.Logger;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;


/**
 * Generates cryptographic keys.
 * 
 * @author Karl-Peter Fuchs
 */
final class KeyGen {


	/** 
	 * Reference on component <code>InternalInformationPort</code>. 
	 * Used to display and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = internalInformationPort.getLogger();
	
	
	/**
	 * Empty, private constructor. Never used, since all methods are static.
	 */
	private KeyGen() {
		
		
	}
	
	
	/**
	 * Generates and returns a <code>KeyPair</code>.
	 * 
	 * @return The generated <code>KeyPair</code>.
	 */
	protected static KeyPair generateKeyPair() {

		// load values from property file
		String asymKeyGeneratorName = 
			internalInformationPort.getProperty("NAME_OF_ASYM_KEY_GENERATOR");
		
		int asymKeyLength = 
			new Integer(internalInformationPort.getProperty("ASYM_KEY_LENGTH"));

		KeyPairGenerator keyPairGenerator = null;
		
		try {
	    	
			keyPairGenerator = 
				KeyPairGenerator.getInstance(asymKeyGeneratorName);
			
			keyPairGenerator.initialize(asymKeyLength);
		    
	    } catch (NoSuchAlgorithmException e) {
	    	
	    	LOGGER.severe(	"(MIX) Couldn't generate keypair!"
	    					+e.getMessage()
	    					);
	    	
	    	System.exit(1);
	    	
	    }

		return keyPairGenerator.generateKeyPair();
		
	}
	
	
	/**
	 * Generates and returns a <code>SecretKey</code>.
	 * 
	 * @return The generated <code>SecretKey</code>.
	 */
	protected static SecretKey generateInterMixKey() {
		
		try {
			
			KeyGenerator keyGenerator = 
				KeyGenerator.getInstance(
					internalInformationPort.getProperty(
						"NAME_OF_INTER_MIX_KEY_GENERATOR"
						), 
					internalInformationPort.getProperty("CRYPTO_PROVIDER")
				);
			
			keyGenerator.init(
				new Integer(internalInformationPort.getProperty(
						"INTER_MIX_KEY_LENGTH")
					)
				);
			
			return keyGenerator.generateKey();
			
		} catch (NoSuchAlgorithmException e) {
			
			LOGGER.severe(	"(MIX) Couldn't generate InterMixKey!"
							+e.getMessage()
							);
	
			System.exit(1);
			return null;
			
		} catch (NoSuchProviderException e) {
			
			LOGGER.severe(	"(MIX) Couldn't generate InterMixKey!"
							+e.getMessage()
							);
	
			System.exit(1);
			return null;
			
		}
		
	} 
	
	
	/**
	 * Generates and returns an initialization vector 
	 * (<code>IvParameterSpec</code>).
	 * 
	 * @return The generated initialization vector.
	 */
	protected static IvParameterSpec generateInterMixIV() {
		
		int blockLength = 
			new Integer(internalInformationPort.getProperty(
					"INTER_MIX_BLOCK_SIZE")
				);
		
		byte[] interMixIVAsByteArray = new byte[blockLength];
		new SecureRandom().nextBytes(interMixIVAsByteArray);
		
		return new IvParameterSpec(interMixIVAsByteArray);
		
	}
	
}
