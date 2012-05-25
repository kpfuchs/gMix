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

package recodingScheme;

import java.security.Key;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import message.MixMessage;
import message.Reply;
import message.Request;

import recodingScheme.RSA_OAEP_AES_OFB_v2.Config;
import userDatabase.User;
import framework.SubImplementation;
import framework.Util;


public class RSA_OAEP_AES_OFB_v2_MessageCreator extends SubImplementation implements MessageCreator {

	private Config config;
	private RSA_OAEP_AES_OFB_v2 master;
	private Cipher asymmetricCipher;
	private KeyGenerator symKeyGenerator;
	private KeyGenerator macKeyGenerator;
	private SecureRandom secureRandom;
	//private HashMap<String, String> replySeeds;
	//private int minMessageSize;
	
	
	@Override
	public void constructor() {
		if (mix != null) {
			this.master = (RSA_OAEP_AES_OFB_v2)owner;
			this.config = master.getConfig();
		} else {
			RSA_OAEP_AES_OFB_v2 c = new RSA_OAEP_AES_OFB_v2(settings, infoService, client);
			config = c.new Config();
			config.publicKeysOfMixes = getPublicKeysOfAllMixes() ;
			config.numberOfMixes = config.publicKeysOfMixes.length;
		}
	}

	
	@Override
	public void initialize() {
		// create key generators and ciphers
		try {
			this.secureRandom = SecureRandom.getInstance(config.PRNG_ALGORITHM);
			this.asymmetricCipher = Cipher.getInstance(
					config.ASYM_CRYPTOGRAPHY_ALGORITHM,
					config.CRYPTO_PROVIDER
					);
			this.asymmetricCipher.init(Cipher.ENCRYPT_MODE, config.publicKeysOfMixes[config.publicKeysOfMixes.length-1]);
			//this.minMessageSize = asymmetricCipher.getBlockSize();
			this.symKeyGenerator = KeyGenerator.getInstance(
					config.NAME_OF_SYM_KEY_GENERATOR, 
					config.CRYPTO_PROVIDER
					);
			this.symKeyGenerator.init(config.SYM_KEY_LENGTH * 8);
			this.macKeyGenerator = KeyGenerator.getInstance(config.MAC_ALGORITHM);
			this.macKeyGenerator.init(config.MAC_KEY_LENGTH * 8);		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	@Override
	public void begin() {

	}
	
	
	@Override
	public synchronized Request createMessage(byte[] payload) {
		return createMessage(payload, null);
	}
	
	
	@Override
	public synchronized Request createMessage(byte[] payload, User owner) {
		
		if (payload == null) {
			payload = new byte[0];
			System.out.println(mix +" creating dummy");
			System.out.println(mix +" config.numberOfMixes: " +config.numberOfMixes); 
		}
		
		if (payload.length > config.MAX_PAYLOAD)
			throw new RuntimeException("can't send more than " +config.MAX_PAYLOAD +" bytes in one message"); 
		
		// add padding
		int paddingLength = config.MAX_PAYLOAD - payload.length;
		byte[] lengthHeader = Util.intToByteArray(payload.length);
		payload = Util.concatArrays(lengthHeader, payload);
		if (paddingLength > 0) {
			byte[] padding  = new byte[paddingLength];
			secureRandom.nextBytes(padding);
			payload = Util.concatArrays(payload, padding);
		}
		if (RSA_OAEP_AES_OFB_v2.DUPLEX_ON)
			payload = addSingleUseReplyBlock(payload);
		
		String s = "";
		// add header and encryption layer for each mix
		for (int i=config.numberOfMixes-1; i>=0; i--) {
			
			try {
				
				// generate header (without mac; must be added later)
				byte[] mac;
				SecretKey macKey;
				SecretKey symKey;
				IvParameterSpec initVector;
				
				macKey = macKeyGenerator.generateKey();
				symKey = symKeyGenerator.generateKey();
				byte[] iv = new byte[symKey.getEncoded().length];
				secureRandom.nextBytes(iv);
				initVector = new IvParameterSpec(iv);
				
				// concat header and payload
				byte[] plaintext = Util.concatArrays(new byte[][] {
						macKey.getEncoded(),
						symKey.getEncoded(),
						initVector.getIV(),
						payload
					});
				
				// add mac to header
				Mac macGenerator = Mac.getInstance(config.MAC_ALGORITHM);
				macGenerator.init(macKey);
				mac = macGenerator.doFinal(plaintext);
				plaintext = Util.concatArrays(mac, plaintext);
				
				assert macKey.getEncoded().length == config.MAC_KEY_LENGTH;
				assert symKey.getEncoded().length == config.SYM_KEY_LENGTH;
				assert initVector.getIV().length == config.IV_LENGTH;
				assert mac.length == config.MAC_LENGTH;
				
				// encrypt message; asymmetric part
				byte[] cipherText;
				asymmetricCipher.init(Cipher.ENCRYPT_MODE, config.publicKeysOfMixes[i]);
				byte[] asymCipherText = asymmetricCipher.doFinal(plaintext, 0, asymmetricCipher.getBlockSize());
				
				// encrypt message; symmetric part
				Cipher symCipher = Cipher.getInstance(
						config.SYM_CRYPTOGRAPHY_ALGORITHM, 
						config.CRYPTO_PROVIDER
						);
				symCipher.init(Cipher.ENCRYPT_MODE, symKey, initVector);
				byte[] symCipherText = symCipher.doFinal(plaintext, asymmetricCipher.getBlockSize(), plaintext.length - asymmetricCipher.getBlockSize());
				
				cipherText = Util.concatArrays(asymCipherText, symCipherText);
				
				if (config.DEBUG_ON) {
					s +=    "[msg mix "+i +":"
							//+"\n\tlength of mac: " +mac.length
							//+"\n\tlength of macKey: " +macKey.getEncoded().length
							//+"\n\tlength of symKey: " +symKey.getEncoded().length
							//+"\n\tlength of iv: " +initVector.getIV().length
							//+"\n\tlength of payload: " +payload.length
							//+"\n\tlength of asym ciphertext: " +asymCipherText.length
							//+"\n\tlength of sym ciphertext: " +symCipherText.length
							//+"\n\ttotal length of ciphertext: " +cipherText.length
							+"(pt " +Util.md5(Arrays.copyOfRange(plaintext, config.MAC_LENGTH, plaintext.length)) +")"
							+", (ct: " +Util.md5(cipherText) +")"
							//+"\n\thash of public key: " +Util.md5(publicKeysOfMixes[i].getEncoded())
							+"] "
							; 
					//System.out.println(Util.display(Arrays.copyOfRange(plaintext, MAC_LENGTH, plaintext.length)));
					//System.out.println(Util.display(cipherText));
				}
				
				payload = cipherText;
				
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		
		//System.out.println(s); 
		
		if (owner != null) {
			s = mix.toString() +" dummy: ";
			return MixMessage.getInstanceRequest(payload, owner, settings);
		} else {
			s = client.toString() +": ";
			return MixMessage.getInstanceRequest(payload, settings);
		}
		
	}


	private byte[] addSingleUseReplyBlock(byte[] message) {
		return message; // TODO
		/*if (config.DEBUG_ON)
			System.out.println("Client " +client.getIdentifier() +": generating reply block");
		
		byte[] payload;
		
		
		byte[] seed;
		byte[] pseudonym = null;
		SecureRandom prng = null;
		
		// use (seeded) prng to create keys
		try {
			prng = SecureRandom.getInstance(config.PRNG_ALGORITHM);
			seed = new byte[config.PRNG_SEED_LENGTH];
			secureRandom.nextBytes(seed);
			prng.setSeed(seed);
			assert prng != null;
			pseudonym = new byte[config.PSEUDONYM_LENGTH]; // used to identify the correct seed when a reply is received
			prng.nextBytes(pseudonym);
			replySeeds.put(new String(pseudonym, "UTF-8"), new String(seed, "UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		// add pseudonym, address and padding (padding is used to assure that the message is long enough for the cipher to work)
		byte[] address = Util.intToByteArray(client.getIdentifier());
		byte[] padding = new byte[minMessageSize - (config.MAC_KEY_LENGTH + config.SYM_KEY_LENGTH + config.IV_LENGTH + config.MAC_LENGTH +config.PSEUDONYM_LENGTH +config.ADDRESS_LENGTH)];
		secureRandom.nextBytes(padding);
		payload = Util.concatArrays(new byte[][] {address, pseudonym, padding});

		for (int i=config.numberOfMixes-1; i>=0; i--) {
			
			try {
				
				// generate header (without mac; must be added later)
				byte[] mac;
				SecretKey macKey;
				SecretKey symKey;
				IvParameterSpec initVector;
				
				byte[] macKeyAsByteArray = new byte[config.MAC_KEY_LENGTH];
				prng.nextBytes(macKeyAsByteArray);
				macKey = new SecretKeySpec(macKeyAsByteArray, config.MAC_ALGORITHM);
				
				byte[] symKeyAsByteArray = new byte[config.SYM_KEY_LENGTH];
				prng.nextBytes(symKeyAsByteArray);
				symKey = new SecretKeySpec(symKeyAsByteArray, config.SYM_CRYPTOGRAPHY_ALGORITHM);
				
				byte[] iv = new byte[symKey.getEncoded().length];
				prng.nextBytes(iv);
				initVector = new IvParameterSpec(iv);
				
				// concat header fields and payload
				byte[] plaintext = Util.concatArrays(new byte[][] {
						macKeyAsByteArray,
						symKeyAsByteArray,
						iv,
						payload
					});
				
				// add mac to header
				Mac macGenerator = Mac.getInstance(config.MAC_ALGORITHM);
				macGenerator.init(macKey);
				mac = macGenerator.doFinal(plaintext);
				plaintext = Util.concatArrays(mac, plaintext);
				
				assert macKey.getEncoded().length == config.MAC_KEY_LENGTH;
				assert symKey.getEncoded().length == config.SYM_KEY_LENGTH;
				assert initVector.getIV().length == config.IV_LENGTH;
				assert mac.length == config.MAC_LENGTH;
				
				// encrypt message; asymmetric part
				byte[] cipherText;
				asymmetricCipher.init(Cipher.ENCRYPT_MODE, config.publicKeysOfMixes[i]);
				byte[] asymCipherText = asymmetricCipher.doFinal(plaintext, 0, asymmetricCipher.getBlockSize());
				
				// encrypt message; symmetric part
				Cipher symCipher = Cipher.getInstance(
						config.SYM_CRYPTOGRAPHY_ALGORITHM, 
						config.CRYPTO_PROVIDER
						);
				symCipher.init(Cipher.ENCRYPT_MODE, symKey, initVector);
				byte[] symCipherText = symCipher.doFinal(plaintext, asymmetricCipher.getBlockSize(), plaintext.length - asymmetricCipher.getBlockSize());
				
				cipherText = Util.concatArrays(asymCipherText, symCipherText);
				
				if (config.DEBUG_ON) {
					System.out.println("Client " +client.getIdentifier() +": message for mix "+i +":");
					System.out.println("\tlength of mac: " +mac.length); 
					System.out.println("\tlength of macKey: " +macKey.getEncoded().length); 
					System.out.println("\tlength of symKey: " +symKey.getEncoded().length); 
					System.out.println("\tlength of iv: " +initVector.getIV().length);
					System.out.println("\tlength of payload: " +payload.length);
					System.out.println("\tlength of asym ciphertext: " +asymCipherText.length);
					System.out.println("\tlength of sym ciphertext: " +symCipherText.length);
					System.out.println("\ttotal length of ciphertext: " +cipherText.length);
					System.out.println("\thash of plaintext: " +Util.md5(Arrays.copyOfRange(plaintext, config.MAC_LENGTH, plaintext.length)));
					System.out.println("\thash of ciphertext: " +Util.md5(cipherText));
					//System.out.println(Util.display(Arrays.copyOfRange(plaintext, MAC_LENGTH, plaintext.length)));
					//System.out.println(Util.display(cipherText));
				}
				
				payload = cipherText;
				
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		
		//byte[] lengthHeader = Util.shortToByteArray(payload.length);
		
		if (config.DEBUG_ON)
			System.out.println("Client " +client.getIdentifier() +": finished generating reply block");
		
		return Util.concatArrays(new byte[][] {
				//lengthHeader,
				payload, // == replyBlock
				message
			});
		*/
	}
	

	@Override
	public synchronized int getMaxPayloadForNextMessage() {
		return config.MAX_PAYLOAD;
	}


	@Override
	public int getMaxPayloadForNextReply(User user) {
		return config.MAX_PAYLOAD;
	}


	@Override
	public byte[] extractPayload(Reply reply) {
		// TODO Auto-generated method stub
		return reply.getByteMessage(); // TODO
	}


	@Override
	public Reply createReply(byte[] payload, User owner) {
		// TODO Auto-generated method stub
		return null;
	}

	
	private Key[] getPublicKeysOfAllMixes() {
		byte[][] keysAsByteArrays = infoService.getValueFromAllMixes("RSA_PUBLIC_KEY");
		Key[] keys = new Key[keysAsByteArrays.length];
		for (int i=0; i<keys.length; i++) {
			X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(keysAsByteArrays[i]);
			KeyFactory keyFactory;
			try {
				keyFactory = KeyFactory.getInstance(publicKeySpec.getFormat());
				keys[i] = keyFactory.generatePublic(publicKeySpec);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return keys;
	}
}
