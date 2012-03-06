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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import recodingScheme.RSA_AES_Channel_v1.ChannelData;
import recodingScheme.RSA_AES_Channel_v1.Config;

import message.Reply;
import message.Request;
import framework.SubImplementation;
import framework.Util;


public class RSA_AES_Channel_v1_Recoder extends SubImplementation implements Recoder {

	private Config config;
	private RSA_AES_Channel_v1 master;
	private Cipher asymmetricCipher;
	private SecureRandom secureRandom;
	
	
	@Override
	public void constructor() {
		this.master = (RSA_AES_Channel_v1)owner;
		this.config = master.getConfig();
		try {
			this.asymmetricCipher = Cipher.getInstance(config.ASYM_CRYPTOGRAPHY_ALGORITHM, config.CRYPTO_PROVIDER);
			this.asymmetricCipher.init(Cipher.DECRYPT_MODE, config.keyPair.getPrivate());
			this.secureRandom = SecureRandom.getInstance(config.PRNG_ALGORITHM);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("could not init asym cipher at mix"); 
		} 

		// publish public key so clients can use it:
		infoService.postValueAsMix(mix.getIdentifier(), "RSA_PUBLIC_KEY", config.keyPair.getPublic().getEncoded()); // TODO: do only once in Config-object...
	}

	
	@Override
	public void initialize() {

	}

	
	@Override
	public void begin() {

	}

	
	// returns the message's payload
	public synchronized Request recodeMessage(Request message) {
		if (message.getOwner().getAttachment(master, ChannelData.class).macKey == null)
			return recodeChannelEstablishMessage(message);
		else
			return recodeChannelMessage(message);
	}


	private Request recodeChannelEstablishMessage(Request message) {
		String cipherTextHash = null;
		try {
			ChannelData channelData = message.getOwner().getAttachment(master, ChannelData.class);
			
			if (config.DEBUG_ON) {
				cipherTextHash = Util.md5(message.getByteMessage());
			}
			// decrypt asymmetrically encrypted part
			byte[] asymCipherText = Arrays.copyOfRange(message.getByteMessage(), 0 ,asymmetricCipher.getBlockSize());
			byte[] asymPlaintext = asymmetricCipher.doFinal(asymCipherText);
			
			// extract data from derypted header (= first part of the "asymPlaintext")
			byte[] mac;
			SecretKey symKey;
			IvParameterSpec initVector;
			int payloadLength;
			
			int pointer = 0;
			mac = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.MAC_LENGTH);
			
			byte[] macKeyAsByteArray = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.MAC_KEY_LENGTH);
			channelData.macKey = new SecretKeySpec(macKeyAsByteArray, config.MAC_ALGORITHM);
			
			byte[] symKeyAsByteArray = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.SYM_KEY_LENGTH);
			symKey = new SecretKeySpec(symKeyAsByteArray, config.SYM_CRYPTOGRAPHY_ALGORITHM);
			
			byte[] ivAsByteArray = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.IV_LENGTH);
			initVector = new IvParameterSpec(ivAsByteArray);
			
			channelData.decryptCipher = Cipher.getInstance(config.SYM_CRYPTOGRAPHY_ALGORITHM, config.CRYPTO_PROVIDER);
			channelData.decryptCipher.init(Cipher.DECRYPT_MODE, symKey, initVector);
			
			if (mix.isDuplex()) {
				byte[] symRepKeyAsByteArray = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.SYM_KEY_LENGTH);
				SecretKey symRepKey = new SecretKeySpec(symRepKeyAsByteArray, config.SYM_CRYPTOGRAPHY_ALGORITHM);
				ivAsByteArray = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.IV_LENGTH);
				initVector = new IvParameterSpec(ivAsByteArray);
				channelData.encryptCipher = Cipher.getInstance(config.SYM_CRYPTOGRAPHY_ALGORITHM, config.CRYPTO_PROVIDER);
				channelData.encryptCipher.init(Cipher.ENCRYPT_MODE, symRepKey, initVector);
			}
			
			byte[] payloadLengthAsArray = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.LENGTH_HEADER_LENGTH);
			payloadLength = Util.byteArrayToInt(payloadLengthAsArray);
				
			// decrypt symmetrically encrypted part
			byte[] symCipherText = Arrays.copyOfRange(message.getByteMessage(), asymmetricCipher.getBlockSize(), message.getByteMessage().length);
			byte[] symPlaintext = channelData.decryptCipher.update(symCipherText);
			
			assert symCipherText.length == symPlaintext.length: mix.toString() +": symCipherText.length != symPlaintext.length (" +symCipherText.length +"!=" +symPlaintext.length +")";
			byte[] plaintext = Util.concatArrays(asymPlaintext, symPlaintext);
			
			//System.out.println("-[-[Ð[-mix: asymPlaintext (" +Util.md5(asymPlaintext) +") + symPlaintext ("+Util.md5(symPlaintext) +") -> (" +Util.md5(asymCipherText) +"), (" +Util.md5(symCipherText) +") using " +Util.md5(symKeyAsByteArray) +", " +Util.md5(channelData.decryptCipher.getIV())); 
			
			// validate mac
			Mac macGenerator = Mac.getInstance(config.MAC_ALGORITHM);
			macGenerator.init(channelData.macKey);
			byte[] signedData = Arrays.copyOfRange(plaintext, config.MAC_LENGTH, plaintext.length);
			if (config.DEBUG_ON)
				System.out.println(mix +" " +cipherTextHash +" -> " +Util.md5(signedData));
			byte[] locallyGeneratedMac = macGenerator.doFinal(signedData);
			
			if (!Arrays.equals(locallyGeneratedMac, mac)) {
				System.out.println("wrong MAC!");
				//System.out.println("%&/%&/ wrong MAC: " +Util.md5(mac) + ", signedData: " +Util.md5(signedData));  // TODO
				return null;
			}
			
			if (config.PERFORM_REPLY_DETECTION)
				if (config.replayDetection.isReplay(macKeyAsByteArray))
					return null;
			
			// extract payload (= receive data without headers and padding)
			if (payloadLength == 0) {// dummy
				message.setByteMessage(new byte[0]);
			} else {
				message.setByteMessage(Arrays.copyOfRange(plaintext, pointer, (pointer + payloadLength)));
			}
			
			return message;
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(mix +"Exception-message (ciphertext): " +Util.md5(message.getByteMessage()));
			return null;
		}
		
	}
	
	
	private Request recodeChannelMessage(Request message) {
		try {
			String ct = Util.md5(message.getByteMessage());
			int ctlength = message.getByteMessage().length;
			ChannelData channelData = message.getOwner().getAttachment(master, ChannelData.class);
			byte[] plaintext = channelData.decryptCipher.update(message.getByteMessage());
			assert ctlength == message.getByteMessage().length;
			
			// validate mac
			int pointer = 0;
			byte[] mac = Arrays.copyOfRange(plaintext, pointer, pointer += config.MAC_LENGTH);
			Mac macGenerator = Mac.getInstance(config.MAC_ALGORITHM);
			macGenerator.init(channelData.macKey);
			byte[] signedData = Arrays.copyOfRange(plaintext, config.MAC_LENGTH, plaintext.length);
			byte[] locallyGeneratedMac = macGenerator.doFinal(signedData);
			
			if (!Arrays.equals(locallyGeneratedMac, mac)) {
				System.err.println(mix.toString() +"wrong MAC cm! " +ct +" mac of " +Util.md5(signedData) +" is " +Util.md5(mac)); // TODO
				return null;
			}
			
			// extract payload (= receive data without headers and padding)
			byte[] payloadLengthAsArray = Arrays.copyOfRange(plaintext, pointer, pointer += config.LENGTH_HEADER_LENGTH);
			int payloadLength = Util.byteArrayToInt(payloadLengthAsArray);
			if (payloadLength == 0) { // dummy
				message.setByteMessage(new byte[0]);
			} else {
				message.setByteMessage(Arrays.copyOfRange(plaintext, pointer, pointer += payloadLength));
			}
			//System.out.println("0=0=0=0=0: cm@" +mix.toString() +": symPlaintext:"+Util.md5(plaintext) +" <- " +ct +", received mac:" +Util.md5(mac) +", locallyGeneratedMac: " +Util.md5(locallyGeneratedMac) +", signedData: " +Util.md5(signedData) +"; key: " +Util.md5(channelData.reqKey.getEncoded()) +"; iv: "+Util.md5(channelData.decryptCipher.getIV()) ); 
			
			return message;
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			return null;
		}
		
	}

	
	@Override
	public synchronized Reply recodeReply(Reply message) {
		ChannelData channelData = message.getOwner().getAttachment(master, ChannelData.class);
		if (mix.isLastMix()) {
			if (message.getByteMessage() == null) // dummy
				message.setByteMessage(new byte[0]);
			if (message.getByteMessage().length > config.MAX_PAYLOAD)
				throw new RuntimeException("can't send more than " +config.MAX_PAYLOAD +" bytes in one message"); 
			
			// add length header
			byte[] lengthHeader = Util.intToByteArray(message.getByteMessage().length);
			message.setByteMessage(Util.concatArrays(lengthHeader, message.getByteMessage()));
			// add padding
			int desiredLength = config.MAX_PAYLOAD + config.LENGTH_HEADER_LENGTH;
			int diffToBlockSize = desiredLength % channelData.encryptCipher.getBlockSize();
			if (diffToBlockSize != 0)
				desiredLength += channelData.encryptCipher.getBlockSize() - diffToBlockSize;
			byte[] padding = new byte[desiredLength - message.getByteMessage().length];
			secureRandom.nextBytes(padding);
			message.setByteMessage(Util.concatArrays(message.getByteMessage(), padding));
			assert (message.getByteMessage().length % channelData.encryptCipher.getBlockSize()) == 0;
		}
		
		byte[] result = channelData.encryptCipher.update(message.getByteMessage());
		//System.out.println(" oOoOo " +mix.toString() +": " +Util.md5(message.getByteMessage()) + " -> " +Util.md5(result)); 
		assert result.length == message.getByteMessage().length;
		message.setByteMessage(result);
		return message;
	}

}
