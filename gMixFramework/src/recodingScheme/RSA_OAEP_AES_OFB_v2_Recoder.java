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

import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import message.Reply;
import message.Request;

import recodingScheme.RSA_OAEP_AES_OFB_v2.Config;
import framework.SubImplementation;
import framework.Util;


public class RSA_OAEP_AES_OFB_v2_Recoder extends SubImplementation implements Recoder {

	private Config config;
	private RSA_OAEP_AES_OFB_v2 master;
	private Cipher asymmetricCipher;
	
	
	@Override
	public void constructor() {
		this.master = (RSA_OAEP_AES_OFB_v2)owner;
		this.config = master.getConfig();
		try {
			this.asymmetricCipher = Cipher.getInstance(config.ASYM_CRYPTOGRAPHY_ALGORITHM, config.CRYPTO_PROVIDER);
			this.asymmetricCipher.init(Cipher.DECRYPT_MODE, config.keyPair.getPrivate());
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("could not init asym cipher at mix"); 
		} 

		// publish public key so clients can use it:
		infoService.postValueAsMix(mix.getIdentifier(), "RSA_PUBLIC_KEY", config.keyPair.getPublic().getEncoded());
	}
	

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}
	

	@Override
	public void begin() {
		// TODO Auto-generated method stub
		
	}

	
	// returns the message's payload
	public synchronized Request recodeMessage(Request message) {
		
		String cipherTextHash = null;
		
		try {
			
			if (config.DEBUG_ON) {
				cipherTextHash = Util.md5(message.getByteMessage());
			//	System.out.println("Mix " +owner.getIdentifier() +": received this message (ciphertext): " +Util.md5(message.getByteMessage()));
				//System.out.println("my public key: " +Util.md5(keyPair.getPublic().getEncoded()));
			}
			// decrypt asymmetrically encrypted part
			byte[] asymPlaintext = asymmetricCipher.doFinal(message.getByteMessage(), 0, asymmetricCipher.getBlockSize());
			
			// extract data from derypted header (= first part of the "asymPlaintext")
			byte[] mac;
			SecretKey macKey;
			SecretKey symKey;
			IvParameterSpec initVector;
			
			int pointer = 0;
			mac = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.MAC_LENGTH);

			byte[] macKeyAsByteArray = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.MAC_KEY_LENGTH);
			macKey = new SecretKeySpec(macKeyAsByteArray, config.MAC_ALGORITHM);
			
			byte[] symKeyAsByteArray = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.SYM_KEY_LENGTH);
			symKey = new SecretKeySpec(symKeyAsByteArray, config.SYM_CRYPTOGRAPHY_ALGORITHM);
			
			byte[] ivAsByteArray = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.IV_LENGTH);
			initVector = new IvParameterSpec(ivAsByteArray);
			
			// decrypt symmetrically encrypted part
			Cipher decryptCipher = Cipher.getInstance(config.SYM_CRYPTOGRAPHY_ALGORITHM, config.CRYPTO_PROVIDER);
					
			decryptCipher.init(Cipher.DECRYPT_MODE, symKey, initVector);
			byte[] symPlaintext = decryptCipher.doFinal(message.getByteMessage(), asymmetricCipher.getBlockSize(), message.getByteMessage().length - asymmetricCipher.getBlockSize());
			byte[] plaintext = Util.concatArrays(asymPlaintext, symPlaintext);
			
			// validate mac
			Mac macGenerator = Mac.getInstance(config.MAC_ALGORITHM);
			macGenerator.init(macKey);
			byte[] signedData = Arrays.copyOfRange(plaintext, config.MAC_LENGTH, plaintext.length);
			if (config.DEBUG_ON)
				System.out.println(mix +" " +cipherTextHash +" -> " +Util.md5(signedData));
			byte[] locallyGeneratedMac = macGenerator.doFinal(signedData);
			
			if (!Arrays.equals(locallyGeneratedMac, mac)) {
				System.out.println("wrong MAC!"); // TODO
				return null;
			} 
			
			if (config.PERFORM_REPLY_DETECTION)
				if (config.replayDetection.isReplay(macKeyAsByteArray))
					return null;
				
			if (mix.isLastMix()) {
				// remove Padding
				byte[] lengthHeader = Arrays.copyOfRange(plaintext, pointer, pointer += config.LENGTH_HEADER_LENGTH);
				int payloadLength = Util.byteArrayToInt(lengthHeader);
				if (payloadLength == 0) // dummy
					plaintext = new byte[0];
				else
					plaintext = Arrays.copyOfRange(plaintext, pointer, pointer + payloadLength);
				message.setByteMessage(plaintext);
				
				if (mix.isDuplex()) { // send reply (note: last mix == receiver in this case)
					// owner.prevMix.addReply(plaintext); // TODO
					return message; // TODO: remove replyblock from returned plaintext?
				} else
					return message;
				
			} else {
				message.setByteMessage(Arrays.copyOfRange(plaintext, pointer, plaintext.length));
				return message;
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(mix +"Exception-message (ciphertext): " +Util.md5(message.getByteMessage()));
			return null;
		}
		
	}


	public synchronized Reply recodeReply(Reply message) {
		return message; // TODO
 		/*try {
			
			// decrypt asymmetrically encrypted part
			byte[] asymPlaintext = asymmetricCipher.doFinal(message.getByteMessage(), 0, asymmetricCipher.getBlockSize());
			
			// extract data from derypted header (= first part of the "asymPlaintext")
			byte[] mac;
			SecretKey macKey;
			SecretKey symKey;
			IvParameterSpec initVector;
			
			int pointer = 0;
			mac = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.MAC_LENGTH);

			byte[] macKeyAsByteArray = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.MAC_KEY_LENGTH);
			macKey = new SecretKeySpec(macKeyAsByteArray, config.MAC_ALGORITHM);
			
			byte[] symKeyAsByteArray = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.SYM_KEY_LENGTH);
			symKey = new SecretKeySpec(symKeyAsByteArray, config.SYM_CRYPTOGRAPHY_ALGORITHM);
			
			byte[] ivAsByteArray = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.IV_LENGTH);
			initVector = new IvParameterSpec(ivAsByteArray);
			
			// decrypt symmetrically encrypted part
			Cipher decryptCipher = Cipher.getInstance(config.SYM_CRYPTOGRAPHY_ALGORITHM, config.CRYPTO_PROVIDER);
					
			decryptCipher.init(Cipher.DECRYPT_MODE, symKey, initVector);
			byte[] symPlaintext = decryptCipher.doFinal(message.getByteMessage(), asymmetricCipher.getBlockSize(), message.getByteMessage().length - asymmetricCipher.getBlockSize());
			byte[] plaintext = Util.concatArrays(asymPlaintext, symPlaintext);
			
			// validate mac
			Mac macGenerator = Mac.getInstance(config.MAC_ALGORITHM);
			macGenerator.init(macKey);
			byte[] signedData = Arrays.copyOfRange(plaintext, config.MAC_LENGTH, plaintext.length);
			if (config.DEBUG_ON)
				System.out.println(mix +" plaintext: " +Util.md5(signedData) +" (of " +Util.md5(message.getByteMessage()) +")");
			byte[] locallyGeneratedMac = macGenerator.doFinal(signedData);
			
			if (!Arrays.equals(locallyGeneratedMac, mac)) {
				System.out.println("wrong MAC!"); // TODO
				return null;
			} 
			
			if (config.PERFORM_REPLY_DETECTION)
				if (config.replayDetection.isReplay(macKeyAsByteArray))
					return null;
			
			
			// todo: use derived keys to encrypt payload
			
			
			if (mix.isLastMix()) {
				// remove Padding
				byte[] lengthHeader = Arrays.copyOfRange(plaintext, pointer, pointer += config.LENGTH_HEADER_LENGTH);
				int payloadLength = Util.byteArrayToInt(lengthHeader);
				plaintext = Arrays.copyOfRange(plaintext, pointer, pointer + payloadLength);
				message.setByteMessage(plaintext);
				
				if (mix.isDuplex()) { // send reply (note: last mix == receiver in this case)
					//owner.prevMix.addReply(plaintext); // TODO
					return message; // TODO: remove replyblock from returned plaintext?
				} else
					return message;
				
			} else {
				message.setByteMessage(Arrays.copyOfRange(plaintext, pointer, plaintext.length));
				return message;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		// TODO: hier weiter
		// if owner.isFirstMix()
			//Client reveiver = NetworkNode.getNode(address);
			//receiver.addReply(msg);
		
		
		
		
		// TODO Auto-generated method stub
		//return null;*/
		
		
	}
	
}
