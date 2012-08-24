/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2012  Karl-Peter Fuchs
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
package plugIns.layer2recodingScheme.RSA_AES_LossTolerantChannel_v0_001;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


import plugIns.layer2recodingScheme.RSA_AES_LossTolerantChannel_v0_001.MixPlugIn.ChannelData;

import framework.core.AnonNode;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.routing.RoutingMode;
import framework.core.util.Util;


public class RSA_AES_LossTolerantChannel {

	private AnonNode owner;
	private RSA_AES_LossTolerantChannel_Config config;
	private Cipher asymmetricCipher;
	private Cipher asymmetricCiphers[];
	private KeyGenerator symKeyGenerator;
	private KeyGenerator macKeyGenerator;
	private SecretKey[] macKeys;
	private SecretKey[] sessionKeysForRequestChannel;
	private SecretKey[] sessionKeysForReplyChannel;
	private IvParameterSpec[] sessionIVsForRequestChannel;
	private IvParameterSpec[] sessionIVsForReplyChannel;
	private Cipher[] symmetricEncryptCiphers;
	private Cipher[] symmetricDecryptCiphers;
	private SecureRandom secureRandom;
	private boolean channelEstablished = false;
	
	
	public RSA_AES_LossTolerantChannel(AnonNode owner, RSA_AES_LossTolerantChannel_Config config) {
		this.owner = owner;
		this.config = config;
	}
	
	
	public void initAsClient() {
		if (owner.ROUTING_MODE != RoutingMode.CASCADE)
			throw new RuntimeException("not supported"); // TODO: support it...
		this.macKeys = new SecretKey[config.numberOfMixes]; 
		this.sessionKeysForRequestChannel = new SecretKey[config.numberOfMixes];
		this.sessionKeysForReplyChannel = new SecretKey[config.numberOfMixes];
		this.sessionIVsForRequestChannel = new IvParameterSpec[config.numberOfMixes];
		this.sessionIVsForReplyChannel = new IvParameterSpec[config.numberOfMixes];
		this.symmetricEncryptCiphers = new Cipher[config.numberOfMixes];
		this.symmetricDecryptCiphers = new Cipher[config.numberOfMixes];
		this.asymmetricCiphers = new Cipher[config.numberOfMixes];
		// create key generators and ciphers
		try {
			this.secureRandom = SecureRandom.getInstance(config.PRNG_ALGORITHM);
			this.symKeyGenerator = KeyGenerator.getInstance(config.NAME_OF_SYM_KEY_GENERATOR);
			this.symKeyGenerator.init(config.SYM_KEY_LENGTH * 8);
			this.macKeyGenerator = KeyGenerator.getInstance(config.MAC_ALGORITHM);
			this.macKeyGenerator.init(config.MAC_KEY_LENGTH * 8);		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	// TODO: aufrufen!
	public void initAsRecoder() {
		try {
			this.secureRandom = SecureRandom.getInstance(config.PRNG_ALGORITHM);
			this.asymmetricCipher = Cipher.getInstance(config.ASYM_CRYPTOGRAPHY_ALGORITHM, config.CRYPTO_PROVIDER);
			this.asymmetricCipher.init(Cipher.DECRYPT_MODE, config.keyPair.getPrivate());
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("could not init asym cipher at mix"); 
		}
	}
	
	
	public synchronized Request recodeMessage(Request message, ChannelData channelData) {
		if (channelData.established) { // weak check if channel already established
			return recodeChannelMessage(message, channelData);
		} else {
			synchronized (config) { // secure check
				if (!channelData.established) {
					return recodeChannelEstablishMessage(message, channelData);
				} else {
					return recodeChannelMessage(message, channelData);
				}
			}
		}
	}


	private Request recodeChannelEstablishMessage(Request message, ChannelData channelData) {
		String cipherTextHash = null;
		try {
			if (config.DEBUG_ON) {
				cipherTextHash = Util.md5(message.getByteMessage());
			}
			
			// decrypt asymmetrically encrypted part
			byte[] asymCipherText = Arrays.copyOfRange(message.getByteMessage(), 0 ,asymmetricCipher.getBlockSize());
			byte[] asymPlaintext = asymmetricCipher.doFinal(asymCipherText);
			
			// extract data from derypted header (= first part of the "asymPlaintext")
			byte[] mac;
			//SecretKey symKey;
			IvParameterSpec initVector;
			int payloadLength;
			
			int pointer = 0;
			mac = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.MAC_LENGTH);
			
			byte[] macKeyAsByteArray = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.MAC_KEY_LENGTH);
			channelData.macKey = new SecretKeySpec(macKeyAsByteArray, config.MAC_ALGORITHM);
			
			byte[] symKeyAsByteArray = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.SYM_KEY_LENGTH);
			channelData.symSessionKey = new SecretKeySpec(symKeyAsByteArray, config.SYM_CRYPTOGRAPHY_ALGORITHM);
			
			byte[] ivAsByteArray = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.IV_LENGTH);
			initVector = new IvParameterSpec(ivAsByteArray);
			
			channelData.decryptCipher = Cipher.getInstance(config.SYM_CRYPTOGRAPHY_ALGORITHM, config.CRYPTO_PROVIDER);
			channelData.decryptCipher.init(Cipher.DECRYPT_MODE, channelData.symSessionKey, initVector);
			
			if (owner.IS_DUPLEX) {
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
			
			assert symCipherText.length == symPlaintext.length: owner.toString() +": symCipherText.length != symPlaintext.length (" +symCipherText.length +"!=" +symPlaintext.length +")";
			symPlaintext = Arrays.copyOfRange(symPlaintext, ivAsByteArray.length, symPlaintext.length); // explicit IV mode; first block must be removed (cf. S. Vaudenay: "Security Flaws Induced by CBC Padding-Applications to SSL, IPSEC, WTLS")
			byte[] plaintext = Util.concatArrays(asymPlaintext, symPlaintext);
			
			//System.out.println("-[-[[-mix: asymPlaintext (" +Util.md5(asymPlaintext) +") + symPlaintext ("+Util.md5(symPlaintext) +") -> (" +Util.md5(asymCipherText) +"), (" +Util.md5(symCipherText) +") using " +Util.md5(symKeyAsByteArray) +", " +Util.md5(channelData.decryptCipher.getIV())); 
			
			// validate mac
			Mac macGenerator = Mac.getInstance(config.MAC_ALGORITHM);
			macGenerator.init(channelData.macKey);
			byte[] signedData = Arrays.copyOfRange(plaintext, config.MAC_LENGTH, plaintext.length);
			if (config.DEBUG_ON)
				System.out.println(owner +" " +cipherTextHash +" -> " +Util.md5(signedData) +" key: " +Util.md5(channelData.macKey.getEncoded()));
			byte[] locallyGeneratedMac = macGenerator.doFinal(signedData);
			System.out.println("mix: mac-data: " +Util.md5(signedData) +", mac: " +Util.md5(locallyGeneratedMac) +", received mac: " +Util.md5(mac)); // TODO
			
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
			
			channelData.established = true;
			return message;
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(owner +" Exception-message (ciphertext): " +Util.md5(message.getByteMessage()));
			return null;
		}
		
	}
	
	
	private Request recodeChannelMessage(Request message, ChannelData channelData) {
		try {
			String ct = Util.md5(message.getByteMessage());
			byte[] ivAsArray = Arrays.copyOf(message.getByteMessage(), config.IV_LENGTH);
			IvParameterSpec iv = new IvParameterSpec(ivAsArray);
			byte[] cipherText = Arrays.copyOfRange(message.getByteMessage(), config.IV_LENGTH, message.getByteMessage().length);
			byte[] plaintext;
			synchronized (channelData.decryptCipher) {
				channelData.decryptCipher.init(Cipher.DECRYPT_MODE, channelData.symSessionKey, iv);
				plaintext = channelData.decryptCipher.doFinal(cipherText);
			}
			assert cipherText.length == plaintext.length;
			plaintext = Arrays.copyOfRange(plaintext, ivAsArray.length, plaintext.length); // explicit IV mode; first block must be removed (cf. S. Vaudenay: "Security Flaws Induced by CBC Padding-Applications to SSL, IPSEC, WTLS")
			
			// validate mac
			int pointer = 0;
			byte[] mac = Arrays.copyOfRange(plaintext, pointer, pointer += config.MAC_LENGTH);
			Mac macGenerator = Mac.getInstance(config.MAC_ALGORITHM);
			macGenerator.init(channelData.macKey);
			byte[] signedData = Arrays.copyOfRange(plaintext, config.MAC_LENGTH, plaintext.length);
			byte[] locallyGeneratedMac = macGenerator.doFinal(signedData);
			//System.out.println("mix: mac-data: " +Util.md5(signedData) +", mac: " +Util.md5(locallyGeneratedMac) +", received mac: " +Util.md5(mac) +"\n" +new String(signedData) +"\n\n\n"); // TODO
			//System.out.println("mix: plaintext: " +Util.md5(plaintext) +", ciphertext: " +Util.md5(cipherText) +", symiv: " +Util.md5(ivAsArray) +", symKey: " +Util.md5(channelData.symSessionKey.getEncoded()) +", macKey: " +Util.md5(channelData.macKey.getEncoded()) +", mac-data: " +Util.md5(signedData) +", mac (generated): " +Util.md5(locallyGeneratedMac) +", received mac: " +Util.md5(mac)); 
			
			if (!Arrays.equals(locallyGeneratedMac, mac)) {
				System.err.println(owner.toString() +" wrong MAC cm! " +ct +" mac of " +Util.md5(signedData) +" is " +Util.md5(mac) +" key: " +Util.md5(channelData.macKey.getEncoded())); // TODO
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
			
			return message;
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			return null;
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
			return null;
		} catch (BadPaddingException e) {
			e.printStackTrace();
			return null;
		}
		
	}

	
	public synchronized Reply recodeReply(Reply message, ChannelData channelData) {
		if (owner.IS_LAST_MIX) {
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


	public synchronized Request applyLayeredEncryption(Request request) {
		if (!channelEstablished) {
			channelEstablished = true;
			return createChannelEstablishMessage(request);
		} else {
			return createChannelMessage(request);
		}
	}

	
	private Request createChannelEstablishMessage(Request request) {
		byte[] payload = request.getByteMessage();
		if (payload == null) // dummy
			payload = new byte[0]; 
		
		if (payload.length > config.MAX_PAYLOAD)
			throw new RuntimeException("can't send more than " +config.MAX_PAYLOAD +" bytes in one message"); 
		
		// generate keys, init ciphers etc.
		for (int i=config.numberOfMixes-1; i>=0; i--) {
			try {
				this.asymmetricCiphers[i] = Cipher.getInstance(config.ASYM_CRYPTOGRAPHY_ALGORITHM, config.CRYPTO_PROVIDER);
				this.asymmetricCiphers[i].init(Cipher.ENCRYPT_MODE, config.publicKeysOfMixes[i]);
				this.sessionKeysForRequestChannel[i] = symKeyGenerator.generateKey();
				this.macKeys[i] = macKeyGenerator.generateKey();
				byte[] ivReq = new byte[sessionKeysForRequestChannel[i].getEncoded().length];
				secureRandom.nextBytes(ivReq);
				this.sessionIVsForRequestChannel[i] = new IvParameterSpec(ivReq);
				this.symmetricEncryptCiphers[i] = Cipher.getInstance(config.SYM_CRYPTOGRAPHY_ALGORITHM, config.CRYPTO_PROVIDER);
				this.symmetricEncryptCiphers[i].init(Cipher.ENCRYPT_MODE, sessionKeysForRequestChannel[i], sessionIVsForRequestChannel[i]);             				
				if (owner.IS_DUPLEX) {
					this.sessionKeysForReplyChannel[i] = symKeyGenerator.generateKey();
					byte[] ivRep = new byte[sessionKeysForReplyChannel[i].getEncoded().length];
					secureRandom.nextBytes(ivRep);
					this.sessionIVsForReplyChannel[i] = new IvParameterSpec(ivRep);
					this.symmetricDecryptCiphers[i] = Cipher.getInstance(config.SYM_CRYPTOGRAPHY_ALGORITHM, config.CRYPTO_PROVIDER);
					this.symmetricDecryptCiphers[i].init(Cipher.DECRYPT_MODE, sessionKeysForReplyChannel[i], sessionIVsForReplyChannel[i]);             				
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		String s = "";
		// add header and encryption layer for each mix
		for (int i=config.numberOfMixes-1; i>=0; i--) {
			try {
				// generate header (without mac; must be added later)
				byte[] mac;
				byte[] plaintext;
				byte[] payloadLength = Util.intToByteArray(payload.length);
				
				// concat header and payload
				if (owner.IS_DUPLEX) {
					plaintext = Util.concatArrays(new byte[][] {
							macKeys[i].getEncoded(),
							sessionKeysForRequestChannel[i].getEncoded(),
							sessionIVsForRequestChannel[i].getIV(),
							sessionKeysForReplyChannel[i].getEncoded(),
							sessionIVsForReplyChannel[i].getIV(),
							payloadLength,
							payload
						});
				} else {
					plaintext = Util.concatArrays(new byte[][] {
							macKeys[i].getEncoded(),
							sessionKeysForRequestChannel[i].getEncoded(),
							sessionIVsForRequestChannel[i].getIV(),
							payloadLength,
							payload
						});
				}
				
				// add padding for last mix' payload
				if (i == config.numberOfMixes-1 && payload.length != config.MAX_PAYLOAD) {
					byte[] padding = new byte[config.MAX_PAYLOAD - payload.length];
					secureRandom.nextBytes(padding);
					plaintext = Util.concatArrays(plaintext, padding);
				}
				// make sure message lengths are a multiple of the sym crypto algorithm's block size	
				int symPlaintextLength = (plaintext.length + config.MAC_LENGTH) - asymmetricCiphers[i].getBlockSize(); // note that "getBlockSize()" returns the max length of a plaintext encryptable with the asym. cipher (= aysm. keylength - overhead)
				int diffToBlockSize = symPlaintextLength % symmetricEncryptCiphers[i].getBlockSize();
				if (diffToBlockSize != 0) {
					byte[] padding = new byte[symmetricEncryptCiphers[i].getBlockSize() - diffToBlockSize];
					secureRandom.nextBytes(padding);
					plaintext = Util.concatArrays(plaintext, padding);
					symPlaintextLength += padding.length;
				}
				
				// add mac to header
				Mac macGenerator = Mac.getInstance(config.MAC_ALGORITHM);
				macGenerator.init(macKeys[i]);
				mac = macGenerator.doFinal(plaintext);
				System.out.println("client: mac-data: " +Util.md5(plaintext) +", mac: " +Util.md5(mac)); // TODO
				plaintext = Util.concatArrays(mac, plaintext); 
				
				assert macKeys[i].getEncoded().length == config.MAC_KEY_LENGTH;
				assert sessionKeysForRequestChannel[i].getEncoded().length == config.SYM_KEY_LENGTH;
				assert sessionIVsForRequestChannel[i].getIV().length == config.IV_LENGTH;
				assert mac.length == config.MAC_LENGTH;
				if (owner.IS_DUPLEX) {
					assert sessionKeysForReplyChannel[i].getEncoded().length == config.SYM_KEY_LENGTH;
					assert sessionIVsForReplyChannel[i].getIV().length == config.IV_LENGTH;
				}
				
				// encrypt message; asymmetric part
				byte[] cipherText;
				byte[] asymPt = Arrays.copyOfRange(plaintext, 0, asymmetricCiphers[i].getBlockSize());
				byte[] asymCipherText = asymmetricCiphers[i].doFinal(asymPt);
				
				// encrypt message; symmetric part
				byte[] symPt = Arrays.copyOfRange(plaintext, asymmetricCiphers[i].getBlockSize(), plaintext.length);
				assert symPt.length == symPlaintextLength;
				assert symPt.length % symmetricEncryptCiphers[i].getBlockSize() == 0;
				
				// explicit IV mode; first block must be random (cf. S. Vaudenay: "Security Flaws Induced by CBC Padding-Applications to SSL, IPSEC, WTLS")
				byte[] rand = new byte[config.IV_LENGTH]; 
				secureRandom.nextBytes(rand);
				symPt = Util.concatArrays(rand, symPt);
				
				byte[] symCipherText = symmetricEncryptCiphers[i].doFinal(symPt);
				assert symCipherText.length == symPt.length;
				
				cipherText = Util.concatArrays(asymCipherText, symCipherText);
				assert cipherText.length % symmetricEncryptCiphers[i].getBlockSize() == 0;
				
				if (config.DEBUG_ON) {
					//System.out.println("-[-[[: asymPlaintext (" +Util.md5(asymPt) +") + symPlaintext ("+Util.md5(symPt) +") -> (" +Util.md5(asymCipherText) +"), (" +Util.md5(symCipherText) +") using " +Util.md5(sessionKeysForRequestChannel[i].getEncoded()) +", " +Util.md5(symmetricEncryptCiphers[i].getIV())); 
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
							+", (ct: " +Util.md5(cipherText) +") key: " +Util.md5(macKeys[i].getEncoded())
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
		
		System.out.println(s); 
		s = owner.toString() +": ";
		
		request.setByteMessage(payload);
		return request;
	}
	
	
	private Request createChannelMessage(Request request) {
		byte[] payload = request.getByteMessage();
		if (payload == null) // dummy
			payload = new byte[0];
		if (payload.length > config.MAX_PAYLOAD)
			throw new RuntimeException("can't send more than " +config.MAX_PAYLOAD +" bytes in one message"); 
		
		for (int i=config.numberOfMixes-1; i>=0; i--) {
			// add length-header
			try {
				byte[] payloadLength = Util.intToByteArray(payload.length);
				payload = Util.concatArrays(payloadLength, payload);
				// add padding for last mix' payload
				if (i == config.numberOfMixes-1 && payload.length != config.MAX_PAYLOAD) {
					byte[] padding = new byte[(config.MAX_PAYLOAD + config.MAC_LENGTH) - payload.length];
					secureRandom.nextBytes(padding);
					payload = Util.concatArrays(payload, padding);
				}
				// make sure message lengths are a multiple of the sym crypto algorithm's block size	
				int symPlaintextLength = payload.length + config.MAC_LENGTH;
				int diffToBlockSize = symPlaintextLength % symmetricEncryptCiphers[i].getBlockSize();
				if (diffToBlockSize != 0) {
					byte[] padding = new byte[symmetricEncryptCiphers[i].getBlockSize() - diffToBlockSize];
					secureRandom.nextBytes(padding);
					payload = Util.concatArrays(payload, padding);
					symPlaintextLength += padding.length;
				}
				
				// add mac to header
				Mac macGenerator = Mac.getInstance(config.MAC_ALGORITHM);
				macGenerator.init(macKeys[i]);
				byte[] mac = macGenerator.doFinal(payload);
				payload = Util.concatArrays(mac, payload);
				
				// explicit IV mode; first block must be random (cf. S. Vaudenay: "Security Flaws Induced by CBC Padding-Applications to SSL, IPSEC, WTLS")
				byte[] rand = new byte[config.IV_LENGTH]; 
				secureRandom.nextBytes(rand);
				payload = Util.concatArrays(rand, payload);
				
				// include iv with packet
				byte[] ivAsArray = new byte[config.IV_LENGTH];
				secureRandom.nextBytes(ivAsArray);
				IvParameterSpec iv = new IvParameterSpec(ivAsArray);
				symmetricEncryptCiphers[i].init(Cipher.ENCRYPT_MODE, sessionKeysForRequestChannel[i], iv);
				
				byte[] ciphertext = symmetricEncryptCiphers[i].update(payload);
				assert ciphertext.length == payload.length;
				ciphertext = Util.concatArrays(ivAsArray, ciphertext);
				
				payload = ciphertext;
				
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				throw new RuntimeException(""); 
			} catch (InvalidKeyException e) {
				e.printStackTrace();
				throw new RuntimeException("");
			} catch (InvalidAlgorithmParameterException e) {
				e.printStackTrace();
			}
			
		}
		
		request.setByteMessage(payload);
		return request;
	}
	
	
	public int getMaxPayloadForNextMessage() {
		return config.MAX_PAYLOAD;
	}


	public int getMaxPayloadForNextReply() {
		return config.MAX_PAYLOAD;
	}


	public Reply extractPayload(Reply reply) {
		byte[] result = reply.getByteMessage();
		for (int i=0; i<config.numberOfMixes; i++) {
			Cipher cipher = symmetricDecryptCiphers[i];
			byte[] plaintext = cipher.update(result);
			assert plaintext.length == result.length;
			//System.out.println(" oOoOo " +client.toString() +": " +Util.md5(plaintext) + " <- " +Util.md5(result)); 
			result = plaintext;
		}
		int lengthOfPayload = Util.byteArrayToInt(Arrays.copyOf(result, 4));
		byte[] payload = Arrays.copyOfRange(result, config.LENGTH_HEADER_LENGTH, config.LENGTH_HEADER_LENGTH + lengthOfPayload);
		reply.setByteMessage(payload);
		return reply;
	}

}
