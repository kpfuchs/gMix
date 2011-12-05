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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import architectureInterface.KeyGeneratorInterface;
import framework.Implementation;
import framework.Settings;


/**
 * Generates cryptographic keys.
 * 
 * @author Karl-Peter Fuchs
 */
public class BasicKeyGenerator extends Implementation implements KeyGeneratorInterface {

	/** 
	 * <code>SecretKey</code> used to encrypt (header) data between two mixes. 
	 * Key size and type are specified in the property file.
	 */
	public static final int INTER_MIX_KEY = 1;
	
	/** 
	 * Initialization vector (<code>IvParameterSpec</code>) used to encrypt 
	 * (header) data between two mixes.
	 */
	public static final int INTER_MIX_IV = 2;
	
	/** 
	 * <code>KeyPair</code> (public key and a private key) used for asymmetric 
	 * cryptography. Key size and type are specified in the property file.
	 */
	public static final int KEY_PAIR = 3;
	
	
	@Override
	public void constructor() {

	}
	

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void begin() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public String[] getCompatibleImplementations() {
		return (new String[] {	"outputStrategy.BasicBatch",
								"outputStrategy.BasicPool",
								"inputOutputHandler.BasicInputOutputHandler",
								"keyGenerator.BasicKeyGenerator",
								"messageProcessor.BasicMessageProcessor",
								"externalInformationPort.BasicExternalInformationPort",
								"networkClock.BasicSystemTimeClock",
								"userDatabase.BasicUserDatabase",
								"message.BasicMessage"	
			});
	}


	/**
	 * Generates a cryptographic key according to the bypassed identifier.
	 * 
	 * @param identifier	Type of key to be generated (see class constants).
	 * 
	 * @return				The generated cryptographic key.
	 */
	@Override
	public Object generateKey(int identifier) {
		
		switch (identifier) {
		
			case INTER_MIX_KEY:
				return generateInterMixKey();
			
			case INTER_MIX_IV:
				return generateInterMixIV();
				
			case KEY_PAIR:
				return generateKeyPair();
				
			default:
				return null;
			
		}	
		
	}
	
	
	@Override
	public boolean usesPropertyFile() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Generates and returns a <code>KeyPair</code>.
	 * 
	 * @return The generated <code>KeyPair</code>.
	 */
	protected static KeyPair generateKeyPair() {

		// load values from property file
		String asymKeyGeneratorName = Settings.getProperty("NAME_OF_ASYM_KEY_GENERATOR");
		
		int asymKeyLength = Settings.getPropertyAsInt("ASYM_KEY_LENGTH");

		KeyPairGenerator keyPairGenerator = null;
		
		try {
	    	
			keyPairGenerator = 
				KeyPairGenerator.getInstance(asymKeyGeneratorName);
			
			keyPairGenerator.initialize(asymKeyLength);
		    
	    } catch (NoSuchAlgorithmException e) {
	    	
	    	e.printStackTrace();
	    	throw new RuntimeException("(MIX) Couldn't generate keypair!");   	
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
					Settings.getProperty("NAME_OF_INTER_MIX_KEY_GENERATOR"), 
					Settings.getProperty("CRYPTO_PROVIDER")
				);
			
			keyGenerator.init(Settings.getPropertyAsInt("INTER_MIX_KEY_LENGTH"));
			
			return keyGenerator.generateKey();
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException("ERROR: (MIX) Couldn't generate InterMixKey!"); 
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
			throw new RuntimeException("ERROR: (MIX) Couldn't generate InterMixKey!"); 
		}
		
	} 
	
	
	/**
	 * Generates and returns an initialization vector 
	 * (<code>IvParameterSpec</code>).
	 * 
	 * @return The generated initialization vector.
	 */
	protected static IvParameterSpec generateInterMixIV() {
		
		int blockLength = Settings.getPropertyAsInt("INTER_MIX_BLOCK_SIZE");
		byte[] interMixIVAsByteArray = new byte[blockLength];
		new SecureRandom().nextBytes(interMixIVAsByteArray);
		
		return new IvParameterSpec(interMixIVAsByteArray);
		
	}
	
}
