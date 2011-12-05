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

package messageProcessor;


import internalInformationPort.InternalInformationPortController;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import userDatabase.User;

import message.BasicMessage;
import message.ChannelEstablishMessage;
import message.ChannelMessage;
import message.Message;
import message.ReplyMessage;


/**
 * Recodes (decrypts/encrypts) messages. Prevents linkability of (incoming and 
 * outgoing) messages due to their appearance.
 * <p>
 * This class is thread-safe (but parallel execution of a single instance's 
 * methods won't increase performance. For parallel processing, several 
 * <code>Recoder</code>s are needed).
 * 
 * @author Karl-Peter Fuchs
 */
final class Recoder {
	
	/** 
	 * Reference on <code>InternalInformationPort</code>. Used to display 
	 * and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = internalInformationPort.getLogger();
	
	/** Public and private key of the asymmetric crypto system. */
	private final KeyPair KEY_PAIR;
	
	/** Cipher for asymmetric cryptography. */
	private Cipher asymmetricCipher;
	
	
	
	/**
	 * Constructor used to generate a <code>Recoder</code> used to decrypt 
	 * requests.
	 * <p>
	 * Creates a new <code>Recoder</code> which will use the bypassed <code>
	 * KeyPair</code> to decrypt messages.
	 * <p> 
	 * Instantiates <code>Cipher</code> objects for later use as specified in 
	 * the property file.
	 * 
	 * @param keyPair	Public and private key of the asymmetric crypto system.
	 */
	protected Recoder(KeyPair keyPair) {

		this.KEY_PAIR = keyPair;		
		
		// Instantiate Cipher objects for later use
		try {
			
			if (keyPair != null) {
				
				asymmetricCipher = Cipher.getInstance(
						internalInformationPort.getProperty(
								"ASYM_CRYPTOGRAPHY_ALGORITHM"
								),
						internalInformationPort.getProperty("CRYPTO_PROVIDER")
						);
					
				asymmetricCipher.init(
						Cipher.DECRYPT_MODE, 
						KEY_PAIR.getPrivate()
						);
				
			}
			
		} catch (NoSuchAlgorithmException e) {
			
			LOGGER.severe(e.getMessage());
			System.exit(1);
			
		} catch (NoSuchPaddingException e) {
			
			LOGGER.severe(e.getMessage());
			System.exit(1);
			
		} catch (NoSuchProviderException e) {
			
			LOGGER.severe(e.getMessage());
			System.exit(1);
			
		} catch (InvalidKeyException e) {
			
			LOGGER.severe(e.getMessage());
			System.exit(1);
			
		}

	}
	
	
	/**
	 * Constructor used to generate a <code>Recoder</code> used to encrypt 
	 * replies.
	 */
	protected Recoder() {
		
		this(null);
		
	}
	
	
	/**
	 * Returns the public key of this <code>Recoder</code>.
	 * 
	 * @return	The public key of this <code>Recoder</code>.
	 */
	protected Key getPublicKey() {
		
		return this.KEY_PAIR.getPublic();
		
	}
	
	
	/**
	 * Recodes (decrypts/encrypts) the bypassed message according to its type.
	 * 
	 * @param message	The message to be recoded.
	 * @return 			The recoded message (same reference as previously 
	 * 					bypassed). "Null", if recoding failed.
	 */
	protected Message recode(BasicMessage message) {
		
		if (message instanceof ChannelEstablishMessage) {
			 
			 return decrypt((ChannelEstablishMessage)message);
			 
		 } else if (message instanceof ChannelMessage) {
			 
			 return decrypt((ChannelMessage)message);
			 
		 } else { // reply
			 
			 return encrypt((ReplyMessage)message);
			 
		 }
		
	}

	
	/**
	 * Decrypts the hybridly encrypted, bypassed 
	 * <code>ChannelEstablishMessage</code>. 
	 * 
	 * @param message	The message to be decrypted.
	 * 
	 * @return 			The decrypted message (same reference as previously 
	 * 					bypassed). <code>null</code>, if recoding failed.
	 */
	private ChannelEstablishMessage decrypt(ChannelEstablishMessage message) {
		
		// decrypt asymmetrically encrypted part
		try {

			byte[] asymmetricCiphertext = message.getAsymmetricPartEncrypted();

			// TODO: remove
			//System.out.println("\n\nMIX: received this cyphertext as asym messagepart " + "  length(" +asymmetricCiphertext.length +") : ["+ new String(Util.generateMD5Hash(asymmetricCiphertext)) +"]"); 
			//System.out.println("content: " +((new String(asymmetricCiphertext)).replaceAll("\n", " ")).replaceAll("\r", " ")); 

			
			byte[] asymmetricPlaintext = 
				asymmetricCipher.doFinal(asymmetricCiphertext);

			// TODO: remove
			//System.out.println("and now decrypted: " + "  length(" +asymmetricPlaintext.length +") : ["+ ((new String(Util.generateMD5Hash(asymmetricPlaintext))).replaceAll("\n", " ")).replaceAll("\r", " ") +"]"); 
			//System.out.println("content: " +((new String(asymmetricPlaintext)).replaceAll("\n", " ")).replaceAll("\r", " ")); 
			
			message.setAsymmetricPartPlain(asymmetricPlaintext);
			
		} catch (Exception e) {
			/* 
			 * Note: ANY Exception must be caught, not just the explicit 
			 * ones. Otherwise, the following Denial-of-service attack is 
			 * possible: An attacker sends a manipulated message which 
			 * causes an uncaught Exception (e. g. 
			 * org.bouncycastle.crypto.DataLengthException). After the 
			 * Exception is thrown, this method will never return and 
			 * therefore block its corresponding mix-thread.
			 */
				e.printStackTrace();
			LOGGER.warning(	"Message could not be decrypted! " 
							+e.getMessage()
							);
			
			return null;
			
		}
			
		// decrypt symmetrically encrypted part
		try {

			User channel = message.getChannel();
			initializeCiphersForUser(channel, message);
			
			byte[] symmetricCiphertext = 
				message.getSymmetricPart();
			
			byte[] symmetricPlaintext = 
				channel.getDecryptCipher().update(symmetricCiphertext);

			message.setSymmetricPart(symmetricPlaintext);
			channel.setIsChannelEstablished(true);
			channel.setMacKey(message.getMACKey());
			
			// TODO: remove
			//System.out.println("sym decrypted: " + "  length(" +symmetricPlaintext.length +") : ["+ ((new String(Util.generateMD5Hash(symmetricPlaintext))).replaceAll("\n", " ")).replaceAll("\r", " ") +"]"); 
			//System.out.println("content: " +((new String(symmetricPlaintext)).replaceAll("\n", " ")).replaceAll("\r", " ")); 
			
			
		} catch (Exception e) {
			/* 
			 * Note: ANY Exception must be caught, not just the explicit 
			 * ones.  Otherwise, the following Denial-of-service attack is 
			 * possible: An attacker sends a manipulated message which 
			 * causes an uncaught Exception. After the Exception is thrown, 
			 * this method will never return and therefore block its 
			 * corresponding mix-thread.
			 */
			e.printStackTrace();
			LOGGER.warning(	"Message could not be decrypted! " 
							+e.getMessage()
							);
	
			return null;
			
		}
		
		return message;
		
	}
	
	
	/**
	 * Decrypts the symmetrically encrypted, bypassed 
	 * <code>ChannelMessage</code>. 
	 * 
	 * @param message	The message to be decrypted.
	 * 
	 * @return 			The decrypted message (same reference as previously 
	 * 					bypassed). <code>null</code>, if recoding failed.
	 */
	private ChannelMessage decrypt(ChannelMessage message) {
		
		try {

			User channel = message.getChannel();
			byte[] symmetricCiphertext = message.getByteMessage();
			
			byte[] symmetricPlaintext = 
				channel.getDecryptCipher().update(symmetricCiphertext);

			message.setByteMessage(symmetricPlaintext);
			
		} catch (Exception e) {
			/* 
			 * Note: ANY Exception must be caught, not just the explicit 
			 * ones.  Otherwise, the following Denial-of-service attack is 
			 * possible: An attacker sends a manipulated message which 
			 * causes an uncaught Exception. After the Exception is thrown, 
			 * this method will never return and therefore block its 
			 * corresponding mix-thread.
			 */
			
			LOGGER.warning(	"Message could not be decrypted! " 
							+e.getMessage()
							);
	
			return null;
			
		}
		
		return message;
		
	}
	
	
	/**
	 * Encrypts the bypassed <code>ReplyMessage</code>. 
	 * 
	 * @param message	The message to be encrypted.
	 * 
	 * @return 			The encrypted message (same reference as previously 
	 * 					bypassed). <code>null</code>, if recoding failed.
	 */
	private ReplyMessage encrypt(ReplyMessage message) {
		
		try {

			User channel = message.getChannel();
			byte[] symmetricPlaintext = message.getByteMessage();
			
			byte[] symmetricCiphertext = 
				channel.getEncryptCipher().update(symmetricPlaintext);
			
			message.setByteMessage(symmetricCiphertext);
			
		} catch (Exception e) {
			/* 
			 * Note: ANY Exception must be caught, not just the explicit 
			 * ones.  Otherwise, the following Denial-of-service attack is 
			 * possible: An attacker sends a manipulated message which 
			 * causes an uncaught Exception. After the Exception is thrown, 
			 * this method will never return and therefore block its 
			 * corresponding mix-thread.
			 */
			
			LOGGER.warning(	"Message could not be decrypted! " 
							+e.getMessage()
							);
	
			return null;
			
		}
		
		return message;
		
	}
	

	/**
	 * Initializes the ciphers used to recode messages for the bypassed 
	 * user/channel.
	 * 
	 * @param channel	User/channel the ciphers shall be initialized for.
	 * @param message	Message containing the data needed to initialize the 
	 * 					ciphers (session key and initialization vector).
	 * 
	 * @throws Exception	If the initialization process fails.
	 */
	private void initializeCiphersForUser(	User channel, 
											ChannelEstablishMessage message
											) throws Exception {
		
		Cipher decryptCipher;
		Cipher encryptCipher;
		
		decryptCipher = Cipher.getInstance(
			internalInformationPort.getProperty("SYM_CRYPTOGRAPHY_ALGORITHM"),
			internalInformationPort.getProperty("CRYPTO_PROVIDER")
			);
			
		decryptCipher.init(	Cipher.DECRYPT_MODE,
							message.getSessionKeyForRequestChannel(),
							message.getSessionIVForRequestChannel()
							);
		
		encryptCipher = 
			Cipher.getInstance(
				internalInformationPort.getProperty(
						"SYM_CRYPTOGRAPHY_ALGORITHM"
						),
				internalInformationPort.getProperty("CRYPTO_PROVIDER")
				);
		
		
		encryptCipher.init(	Cipher.ENCRYPT_MODE,
							message.getSessionKeyForReplyChannel(),
							message.getSessionIVForReplyChannel()
							);
		
		channel.setEncryptCipher(encryptCipher);
		channel.setDecryptCipher(decryptCipher);
	
	}
	
	
	/**
	 * Decrypts the bypassed data using the internal asymmetric cipher (and 
	 * private key) and the specified transformation. 
	 * 
	 * @param data				Data to be decrypted.
	 * @param transformation	Transformation that shall be used for 
	 * 							decryption.
	 * @return					Decrypted data.
	 * 
	 * @throws Exception	Any type of error preventing the data from being 
	 * 						decrypted.
	 */
	protected byte[] decrypt(byte[] data, String transformation) throws 
		Exception {
		
		Cipher asymmetricCipher = 
			Cipher.getInstance(
					transformation,
					internalInformationPort.getProperty("CRYPTO_PROVIDER")
					);
			
		asymmetricCipher.init(Cipher.DECRYPT_MODE, KEY_PAIR.getPrivate());
			
		return asymmetricCipher.doFinal(data);
		
	}	
	
}