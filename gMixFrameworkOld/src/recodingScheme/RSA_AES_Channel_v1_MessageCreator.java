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
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import recodingScheme.RSA_AES_Channel_v1.Config;

import message.ChannelEstablishMessage;
import message.ChannelMessage;
import message.Reply;
import message.Request;
import userDatabase.User;
import framework.LocalClassLoader;
import framework.SubImplementation;
import framework.Util;


public class RSA_AES_Channel_v1_MessageCreator extends SubImplementation implements MessageCreator {

	private Config config;
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
	private boolean DUPLEX;
	
	@Override
	public void constructor() {
		if (mix != null) {
			throw new RuntimeException("not supported"); 
		} else {
			DUPLEX = infoService.getIsDuplexModeOn();
			RSA_AES_Channel_v1 c = new RSA_AES_Channel_v1(settings);
			config = c.new Config();
			config.publicKeysOfMixes = getPublicKEysOfAllMixes();
			config.numberOfMixes = config.publicKeysOfMixes.length;
			this.macKeys = new SecretKey[config.numberOfMixes]; 
			this.sessionKeysForRequestChannel = new SecretKey[config.numberOfMixes];
			this.sessionKeysForReplyChannel = new SecretKey[config.numberOfMixes];
			this.sessionIVsForRequestChannel = new IvParameterSpec[config.numberOfMixes];
			this.sessionIVsForReplyChannel = new IvParameterSpec[config.numberOfMixes];
			this.symmetricEncryptCiphers = new Cipher[config.numberOfMixes];
			this.symmetricDecryptCiphers = new Cipher[config.numberOfMixes];
			this.asymmetricCiphers = new Cipher[config.numberOfMixes];
		}
	}

	
	@Override
	public void initialize() {
		// create key generators and ciphers
		try {
			this.DUPLEX = infoService.getIsDuplexModeOn();
			this.secureRandom = SecureRandom.getInstance(config.PRNG_ALGORITHM);
			this.symKeyGenerator = KeyGenerator.getInstance(config.NAME_OF_SYM_KEY_GENERATOR);
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
		if (!channelEstablished) {
			channelEstablished = true;
			return createChannelEstablishMessage(payload);
		} else {
			return createChannelMessage(payload);
		}
		
	}
	
	
	private ChannelEstablishMessage createChannelEstablishMessage(byte[] payload) {
		
		if (payload == null) // dummy
			payload = new byte[0]; 
		
		if (payload.length > config.MAX_PAYLOAD)
			throw new RuntimeException("can't send more than " +config.MAX_PAYLOAD +" bytes in one message"); 
		
		// generate keys, init ciphers etc.
		for (int i=config.numberOfMixes-1; i>=0; i--) {
			try {
				this.asymmetricCiphers[i] = Cipher.getInstance(settings.getProperty("ASYM_CRYPTOGRAPHY_ALGORITHM"), settings.getProperty("CRYPTO_PROVIDER"));
				this.asymmetricCiphers[i].init(Cipher.ENCRYPT_MODE, config.publicKeysOfMixes[i]);
				this.sessionKeysForRequestChannel[i] = symKeyGenerator.generateKey();
				this.macKeys[i] = macKeyGenerator.generateKey();
				byte[] ivReq = new byte[sessionKeysForRequestChannel[i].getEncoded().length];
				secureRandom.nextBytes(ivReq);
				this.sessionIVsForRequestChannel[i] = new IvParameterSpec(ivReq);
				this.symmetricEncryptCiphers[i] = Cipher.getInstance(settings.getProperty("SYM_CRYPTOGRAPHY_ALGORITHM"), settings.getProperty("CRYPTO_PROVIDER"));
				this.symmetricEncryptCiphers[i].init(Cipher.ENCRYPT_MODE, sessionKeysForRequestChannel[i], sessionIVsForRequestChannel[i]);             				
				if (DUPLEX) {
					this.sessionKeysForReplyChannel[i] = symKeyGenerator.generateKey();
					byte[] ivRep = new byte[sessionKeysForReplyChannel[i].getEncoded().length];
					secureRandom.nextBytes(ivRep);
					this.sessionIVsForReplyChannel[i] = new IvParameterSpec(ivRep);
					this.symmetricDecryptCiphers[i] = Cipher.getInstance(settings.getProperty("SYM_CRYPTOGRAPHY_ALGORITHM"), settings.getProperty("CRYPTO_PROVIDER"));
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
				if (DUPLEX) {
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
				plaintext = Util.concatArrays(mac, plaintext); 
				
				assert macKeys[i].getEncoded().length == config.MAC_KEY_LENGTH;
				assert sessionKeysForRequestChannel[i].getEncoded().length == config.SYM_KEY_LENGTH;
				assert sessionIVsForRequestChannel[i].getIV().length == config.IV_LENGTH;
				assert mac.length == config.MAC_LENGTH;
				if (DUPLEX) {
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
				byte[] symCipherText = symmetricEncryptCiphers[i].update(symPt);
				assert symCipherText.length == symPt.length;
				
				cipherText = Util.concatArrays(asymCipherText, symCipherText);
				assert cipherText.length % symmetricEncryptCiphers[i].getBlockSize() == 0;
				
				if (config.DEBUG_ON) {
					//System.out.println("-[-[Ð[: asymPlaintext (" +Util.md5(asymPt) +") + symPlaintext ("+Util.md5(symPt) +") -> (" +Util.md5(asymCipherText) +"), (" +Util.md5(symCipherText) +") using " +Util.md5(sessionKeysForRequestChannel[i].getEncoded()) +", " +Util.md5(symmetricEncryptCiphers[i].getIV())); 
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
		
		System.out.println(s); 
		s = client.toString() +": ";
		ChannelEstablishMessage result = (ChannelEstablishMessage)LocalClassLoader.instantiateMixRequest("ChannelEstablishMessage.java", "message");
		result.setByteMessage(payload);
		
		return result;
	}
	
	
	private ChannelMessage createChannelMessage(byte[] payload) {
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
				
				byte[] ciphertext = symmetricEncryptCiphers[i].update(payload);
				assert ciphertext.length == payload.length;
				payload = ciphertext;
				
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				throw new RuntimeException(""); 
			} catch (InvalidKeyException e) {
				e.printStackTrace();
				throw new RuntimeException("");
			}
			
		}
		
		ChannelMessage result = (ChannelMessage)LocalClassLoader.instantiateMixRequest("ChannelMessage.java", "message");
		result.setByteMessage(payload);
		return result;
		
	}


	@Override
	public synchronized Request createMessage(byte[] payload, User owner) {
		throw new RuntimeException("not supported"); 
	}
	

	@Override
	public synchronized int getMaxPayloadForNextMessage() {
		return config.MAX_PAYLOAD;
	}


	@Override
	public int getMaxPayloadForNextReply(User user) {
		return config.MAX_PAYLOAD;
	}

/*
 * ChannelData channelData = message.getOwner().getAttachment(this, ChannelData.class);
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
		assert result.length == message.getByteMessage().length;
		message.setByteMessage(result);
		return message;
		*/
	@Override
	public byte[] extractPayload(Reply reply) {
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
		return reply.getByteMessage();
	}


	@Override
	public Reply createReply(byte[] payload, User owner) {
		throw new RuntimeException("not supported"); 
	}

	
	private Key[] getPublicKEysOfAllMixes() {
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
