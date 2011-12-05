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

import java.security.Key;
import java.security.KeyPair;
import message.BasicMessage;
import message.Message;
import message.Reply;
import message.Request;


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
	
	/** Public and private key of the asymmetric crypto system. */
	private KeyPair KEY_PAIR;
	
	/** Cipher for asymmetric cryptography. */
	//private Cipher asymmetricCipher;
	
	
	
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
		// TODO
		/*this.KEY_PAIR = keyPair;		
		
		// Instantiate Cipher objects for later use
		try {
			
			if (keyPair != null) {
				
				asymmetricCipher = Cipher.getInstance(
						Settings.getProperty("ASYM_CRYPTOGRAPHY_ALGORITHM"),
						Settings.getProperty("CRYPTO_PROVIDER")
						);
					
				asymmetricCipher.init(
						Cipher.DECRYPT_MODE, 
						KEY_PAIR.getPrivate()
						);
				
			}
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException("found no implementation for ASYM_CRYPTOGRAPHY_ALGORITHM " +Settings.getProperty("ASYM_CRYPTOGRAPHY_ALGORITHM")); 
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
			throw new RuntimeException("found no implementation for ASYM_CRYPTOGRAPHY_ALGORITHM " +Settings.getProperty("ASYM_CRYPTOGRAPHY_ALGORITHM")); 
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
			throw new RuntimeException("found no suiting crypto provider " +Settings.getProperty("CRYPTO_PROVIDER")); 
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			throw new RuntimeException("invalid key"); 
		}
*/
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
		
		if (message instanceof Request) {
			 
			 return decrypt((Request)message);
			 
		 } else { // reply
			 
			 return encrypt((Reply)message);
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
	private Message decrypt(Request message) {
		
		// TODO: implement
		
		
		return (Message)message;
		
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
	private Message encrypt(Reply message) {
		
		// TODO: implement
		
		
		return (Message)message;
		
	}

	
}