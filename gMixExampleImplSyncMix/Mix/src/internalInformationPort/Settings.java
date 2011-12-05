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

package internalInformationPort;


import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Properties;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


/**
 * Provides properties from the file <code>Properties.txt</code>.
 * 
 * Checks if Bouncy Castle and the Java Cryptography Extension (JCE) Unlimited 
 * Strength Jurisdiction Policy Files are available.
 * 
 * @author Karl-Peter Fuchs
 */
final class Settings {
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = LogFileGenerator.getLogger();
	
	/** <code>Properties</code> object to load values from. */
	private static Properties properties = new Properties();
 
	
	/* Loads property file from local file system. */
    static {
    	
    	try {
    		
			properties.load(new FileInputStream("Properties.txt"));
			
		} catch(IOException e) {
			
			LOGGER.severe(	"Property file could not be loaded!"
							+e.getMessage()
							);
			
			e.printStackTrace();
	    	System.exit(1);
	    	
	    }	
		
		
		// check if bouncy castle is installed
		try {
			
			Security.addProvider(new BouncyCastleProvider());
			KeyGenerator keyGen = KeyGenerator.getInstance("AES", "BC");
		    keyGen.init(new SecureRandom());
		    Key key = keyGen.generateKey();
		    Cipher encrypt = Cipher.getInstance("AES/OFB/NOPADDING", "BC");
		    encrypt.init(Cipher.ENCRYPT_MODE, key);
		    ByteArrayOutputStream baos = new ByteArrayOutputStream();
			CipherOutputStream cos = new CipherOutputStream(baos, encrypt);
			cos.write("plaintext".getBytes());
			cos.close();
			
		} catch (Exception e) {
			
			LOGGER.severe(	"The Bouncy Castle crypto provider (http://www.bou"+
							"ncycastle.org/) seems to be not installed or " +
							"working! \n" +
							"Please add the Bouncy Castle jar-file to your " +
							"classpath.\n"
							+e.getMessage()
						);
			
		    e.printStackTrace();
		    System.exit(1);
		    
		}
		
		// check if the Java Cryptography Extension (JCE) Unlimited Strength 
		// Jurisdiction Policy Files are installed
		try {
	    	KeyPairGenerator kpg =  KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048);
		    kpg.generateKeyPair();
	    } catch (NoSuchAlgorithmException e) {
	    	
	    	LOGGER.severe(	"The Java Cryptography Extension (JCE) Unlimited" +
	    					" Strength Jurisdiction Policy Files (http://www." +
	    					"oracle.com/technetwork/java/javase/downloads/" +
	    					"index.html) seem to be not installed or " +
							"working! \n"
	    					+e.getMessage()
							);
	
	    	e.printStackTrace();
	    	System.exit(1);
	    	
	    }
	    
		addFurterProperties();
		
    }
    
    
    /**
	 * Empty constructor. Never used since all methods are static.
	 */
    private Settings() {
		
	}
	
	
	/**
     * Searches and returns the property with the specified key in the 
     * <code>property</code> object.
     * 
     * @param key 	The property key.
     * @return 		Property with the specified key in the <code>property
     * 				</code> object.
     */
	protected static String getProperty(String key) {
		
		return properties.getProperty(key);
		
	}
	
	
	/**
     * Sets the property with the specified key in the <code>property
     * </code> object to the bypassed value.
     * 
     * @param key	The key to be placed into the property list.
     * @param value	The value corresponding to the key.
     * @return		The previous value of the specified key, or null if it did 
     * 				not have one.
     */
	protected static Object setProperty(String key, String value) {
		
		return properties.setProperty(key, value);
		
	}
	
	
	/**
     * Returns this <code>Settings</code>' <code>Properties</code> object.
     * 
     * @return This <code>Settings</code>' <code>Properties</code> object.
     */
	protected static Properties getProperties() {
		
		return properties;
		
	}
	
	
	/**
	 * Adds further properties to the <code>Properties</code> object, that can 
	 * be calculated from existing properties and therefore shouldn't occur 
	 * in the property file itself.
	 */
	private static void addFurterProperties() {
		
		// INTER_MIX_BLOCK_SIZE:
		try {
			
			Cipher tempCipher = Cipher.getInstance(
					properties.getProperty("INTER_MIX_CRYPTOGRAPHY_ALGORITHM"), 
					properties.getProperty("CRYPTO_PROVIDER")
					);
			
			int interMixBlockSize = tempCipher.getBlockSize();
			tempCipher = null;
			
			properties.setProperty(	"INTER_MIX_BLOCK_SIZE", 
									""+interMixBlockSize
									);
			
		} catch (Exception e) {
			
			LOGGER.severe(	"(Settings) Couldn't detect block size for inter-" 
							+"mix cryptography"
							);
			
			System.exit(1);
			
		}
		
		// SYMMETRIC_CYPHER_BLOCK_SIZE:
		try {
			
			Cipher tempCipher = Cipher.getInstance(
					properties.getProperty("SYM_CRYPTOGRAPHY_ALGORITHM"), 
					properties.getProperty("CRYPTO_PROVIDER")
					);
			
			int symmetricCipherBlockSize = tempCipher.getBlockSize();
			tempCipher = null;
			
			properties.setProperty(	"SYMMETRIC_CYPHER_BLOCK_SIZE", 
									""+symmetricCipherBlockSize
									);
			
		} catch (Exception e) {

			LOGGER.severe(	"(Settings) Couldn't detect block size for " 
							+"symmetric cipher!"
							);
			
			System.exit(1);
			
		}
		
		
	}

}
