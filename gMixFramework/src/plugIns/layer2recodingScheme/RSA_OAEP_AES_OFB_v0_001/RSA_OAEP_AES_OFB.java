/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2012 Karl-Peter Fuchs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package plugIns.layer2recodingScheme.RSA_OAEP_AES_OFB_v0_001;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import framework.core.AnonNode;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.routing.RoutingMode;
import framework.core.util.Util;


public class RSA_OAEP_AES_OFB {

	private AnonNode owner;
	private RSA_OAEP_AES_OFB_Config config;
	private Cipher asymmetricCipher;
	private Cipher asymmetricCipherReply;
	private KeyGenerator symKeyGenerator;
	private KeyGenerator macKeyGenerator;
	private SecureRandom secureRandom;


	public RSA_OAEP_AES_OFB(AnonNode owner, RSA_OAEP_AES_OFB_Config config) {
		this.owner = owner;
		this.config = config;
	}


	public void initAsClient() {
		// create key generators and ciphers
		try {
			this.secureRandom = SecureRandom.getInstance(config.PRNG_ALGORITHM);
			this.asymmetricCipher = Cipher.getInstance(
					config.ASYM_CRYPTOGRAPHY_ALGORITHM,
					config.CRYPTO_PROVIDER
					);
			this.asymmetricCipher.init(Cipher.ENCRYPT_MODE, config.publicKeysOfMixes[config.publicKeysOfMixes.length-1]);
			//this.minMessageSize = asymmetricCipher.getBlockSize();
			this.asymmetricCipherReply = Cipher.getInstance(
					config.ASYM_CRYPTOGRAPHY_ALGORITHM,
					config.CRYPTO_PROVIDER
					);
			this.asymmetricCipherReply.init(Cipher.ENCRYPT_MODE, config.publicKeysOfMixes[config.publicKeysOfMixes.length-1]);
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


	public void initAsRecoder() {
		assert owner.ROUTING_MODE == RoutingMode.CASCADE;
		try {
			this.asymmetricCipher = Cipher.getInstance(config.ASYM_CRYPTOGRAPHY_ALGORITHM, config.CRYPTO_PROVIDER);
			this.asymmetricCipher.init(Cipher.DECRYPT_MODE, config.keyPair.getPrivate());
			this.asymmetricCipherReply = Cipher.getInstance(config.ASYM_CRYPTOGRAPHY_ALGORITHM, config.CRYPTO_PROVIDER);
			this.asymmetricCipherReply.init(Cipher.DECRYPT_MODE, config.keyPair.getPrivate());
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("could not init asym cipher at mix");
		}
	}


	public synchronized Request applyLayeredEncryption(Request request) {
		if (request.getByteMessage() == null)
			request.setByteMessage(new byte[0]);
		if (request.getByteMessage().length == 0) { // dummy
			System.out.println(owner +" creating dummy");
			System.out.println(owner +" config.numberOfMixes: " +config.numberOfMixes);
		}

		if (request.getByteMessage().length > config.MAX_PAYLOAD)
			throw new RuntimeException("can't send more than " +config.MAX_PAYLOAD +" bytes in one message");
		// add padding
		int paddingLength = config.MAX_PAYLOAD - request.getByteMessage().length;
		byte[] lengthHeader = Util.intToByteArray(request.getByteMessage().length);
		request.setByteMessage(Util.concatArrays(lengthHeader, request.getByteMessage()));
		if (paddingLength > 0) {
			byte[] padding = new byte[paddingLength];
			secureRandom.nextBytes(padding);
			request.setByteMessage(Util.concatArrays(request.getByteMessage(), padding));
		}
		if (owner.IS_DUPLEX)
			request = addSingleUseReplyBlock(request);

		//String s = "";
		// add header and encryption layer for each mix
		for (int i=config.routeLength-1; i>=0; i--) {

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
						request.getByteMessage()
				});
				if (owner.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) { 
					// add route-info
					if (i == config.routeLength-1) { // last mix
						plaintext = Util.concatArrays(Util.intToByteArray(request.route[i]), plaintext);
					} else {
						plaintext = Util.concatArrays(Util.intToByteArray(request.route[i+1]), plaintext);
					}
					
				}
				
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
				if (owner.ROUTING_MODE == RoutingMode.CASCADE)
					this.asymmetricCipher.init(Cipher.ENCRYPT_MODE, config.publicKeysOfMixes[i]);
				else {
					this.asymmetricCipher.init(Cipher.ENCRYPT_MODE, config.publicKeysOfMixes[request.route[i]]); // TODO: possible side-effect -> won't work anymore if mix-ids are chosen differently
				//	System.out.println("" +owner +" using public key " +Util.md5(config.publicKeysOfMixes[request.route[i]].getEncoded()) +"for mix " +request.route[i]);
				}
				byte[] asymCipherText = asymmetricCipher.doFinal(plaintext, 0, asymmetricCipher.getBlockSize());
				
				// encrypt message; symmetric part
				Cipher symCipher = Cipher.getInstance(
						config.SYM_CRYPTOGRAPHY_ALGORITHM,
						config.CRYPTO_PROVIDER
						);
				symCipher.init(Cipher.ENCRYPT_MODE, symKey, initVector);
				byte[] symCipherText = symCipher.doFinal(plaintext, asymmetricCipher.getBlockSize(), plaintext.length - asymmetricCipher.getBlockSize());

				cipherText = Util.concatArrays(asymCipherText, symCipherText);
				
				/*if (config.DEBUG_ON) {
s += "[msg mix "+i +":"
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
}*/

				request.setByteMessage(cipherText);

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return request;
	}

	
	


	public int getMaxPayloadForNextMessage() {
		return config.MAX_PAYLOAD;
	}


	public int getMaxPayloadForNextReply() {
		return config.MAX_PAYLOAD;
	}


	public synchronized Reply extractPayload(Reply reply) {
		byte[] pseudonym = Arrays.copyOfRange(reply.getByteMessage(), 0, config.PSEUDONYM_LENGTH);
		byte[] payload = Arrays.copyOfRange(reply.getByteMessage(), config.PSEUDONYM_LENGTH, reply.getByteMessage().length );
		byte[] seed;
		SecureRandom prng = null;

		SecretKey symKey;
		IvParameterSpec initVector;
		Cipher decryptCipher = null;
		
		try {
			seed = config.replySeeds.get(new String(pseudonym,"UTF-8"));
			prng = SecureRandom.getInstance(config.PRNG_ALGORITHM);
			prng.setSeed(seed);
			decryptCipher = Cipher.getInstance(config.SYM_CRYPTOGRAPHY_ALGORITHM, config.CRYPTO_PROVIDER);
		
			byte[][] symKeys = new byte[config.numberOfMixes][config.SYM_KEY_LENGTH];
			byte[][] IVs= new byte[config.numberOfMixes][config.IV_LENGTH];
			
			for(int i=0; i<config.numberOfMixes;i++)
			{
				prng.nextBytes(symKeys[i]);
				prng.nextBytes(IVs[i]);
			}
		
		for (int i=config.numberOfMixes-1; i>=0; i--) 
		{ 
			
			symKey = new SecretKeySpec(symKeys[i], config.SYM_CRYPTOGRAPHY_ALGORITHM);
			initVector = new IvParameterSpec(IVs[i]);
			
			decryptCipher.init(Cipher.DECRYPT_MODE, symKey, initVector);
			payload = decryptCipher.doFinal(payload, 0, payload.length);
		}
		
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		// remove Padding
		byte[] lengthHeader = Arrays.copyOfRange(payload, 0, config.LENGTH_HEADER_LENGTH);
		int payloadLength = Util.byteArrayToInt(lengthHeader);
		if (payloadLength == 0) // dummy
			payload = new byte[0];
		else
			payload = Arrays.copyOfRange(payload, config.LENGTH_HEADER_LENGTH, config.LENGTH_HEADER_LENGTH + payloadLength); 
		reply.setByteMessage(payload);
		return reply;
	}

	private Request addSingleUseReplyBlock(Request request) {
		/*if (config.DEBUG_ON)
		System.out.println("Client " +client.getIdentifier() +": generating reply block");
	*/
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
		secureRandom.nextBytes(pseudonym); 
		config.replySeeds.put(new String(pseudonym, "UTF-8"), seed);
	} catch (Exception e) {
		e.printStackTrace();
	}
	
	
	
	byte[] padding = new byte[asymmetricCipher.getBlockSize() - (config.MAC_KEY_LENGTH  + config.SYM_KEY_LENGTH + config.IV_LENGTH + config.MAC_LENGTH +config.PSEUDONYM_LENGTH)];
	secureRandom.nextBytes(padding);
	payload = Util.concatArrays(new byte[][] {pseudonym,padding}); 
	for (int i=0; i<=config.routeLength-1; i++) { 
		
		try {
			
			// generate header (without mac; must be added later)
			byte[] mac;
			SecretKey macKey;
			SecretKey symKey;
			IvParameterSpec initVector;
			
			byte[] macKeyAsByteArray = new byte[config.MAC_KEY_LENGTH];
			secureRandom.nextBytes(macKeyAsByteArray);
			macKey = new SecretKeySpec(macKeyAsByteArray, config.MAC_ALGORITHM);
			
			byte[] symKeyAsByteArray =  new byte[config.SYM_KEY_LENGTH];
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
			
			if (owner.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) { // add route-info
				if (i == 0) { // last mix
					plaintext = Util.concatArrays(Util.intToByteArray(request.route[i]), plaintext);
				} else {
					plaintext = Util.concatArrays(Util.intToByteArray(request.route[i-1]), plaintext);
				}
			}
			
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
			if (owner.ROUTING_MODE == RoutingMode.CASCADE)
				this.asymmetricCipher.init(Cipher.ENCRYPT_MODE, config.publicKeysOfMixes[i]);
			else {
				this.asymmetricCipher.init(Cipher.ENCRYPT_MODE, config.publicKeysOfMixes[request.route[i]]); // TODO: possible side-effect -> won't work anymore if mix-ids are chosen differently
			//	System.out.println("" +owner +" using public key " +Util.md5(config.publicKeysOfMixes[request.route[i]].getEncoded()) +"for mix " +request.route[i]);
			}
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
			//	System.out.println("Client " +client.getIdentifier() +": message for mix "+i +":");
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
	
	byte[] lengthHeader = Util.intToByteArray(payload.length);
//	if (config.DEBUG_ON)
//		System.out.println("Client " +owner.getIdentifier() +": finished generating reply block");
	

		request.setByteMessage( Util.concatArrays(new byte[][] {
			lengthHeader,
				payload			,	// == replyBlock
				request.getByteMessage()//message
		
			
		}));
	return request;
}


	public synchronized Request recodeMessage(Request message) {
		synchronized (asymmetricCipher) 
		{
			boolean isLastMix =false;
			if(owner.ROUTING_MODE == RoutingMode.CASCADE)
				isLastMix = owner.IS_LAST_MIX;
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
			if (owner.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) { // determine next hop if source routing is enabled
				byte[] nextHopAddress = Arrays.copyOfRange(asymPlaintext, pointer, pointer += 4);
				int address = Util.byteArrayToInt(nextHopAddress);
				if (address == owner.PUBLIC_PSEUDONYM) { // this mix is the last hop
					if (owner.DISPLAY_ROUTE_INFO)
						System.out.println(""+owner +" setting nextHopAddress to \"LAST HOP\""); 
					message.nextHopAddress = MixMessage.NONE;
					isLastMix= true;
				} else { // this mix is not the last hop
					if (owner.DISPLAY_ROUTE_INFO)
						System.out.println(""+owner +" setting nextHopAddress to " +address); 
					message.nextHopAddress = address;
				}
			}
			
			
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
				System.out.println(owner +" " +cipherTextHash +" -> " +Util.md5(signedData));
			byte[] locallyGeneratedMac = macGenerator.doFinal(signedData);
			
			if (!Arrays.equals(locallyGeneratedMac, mac)) {
				System.out.println("wrong MAC!"); // TODO
				return null;
			} 
			
			if (config.PERFORM_REPLY_DETECTION)
				if (config.replayDetection.isReplay(macKeyAsByteArray))
					return null;
			
			if (isLastMix ) {
				if(!owner.IS_DUPLEX)
				{
				// remove Padding
				byte[] lengthHeader = Arrays.copyOfRange(plaintext, pointer, pointer += config.LENGTH_HEADER_LENGTH);
				int payloadLength = Util.byteArrayToInt(lengthHeader);
				if (payloadLength == 0) // dummy
					plaintext = new byte[0];
				else
					plaintext = Arrays.copyOfRange(plaintext, pointer, pointer + payloadLength); 
				message.setByteMessage(plaintext);
				}
				if (owner.IS_DUPLEX) { 
					//	RBlength|replyblock|PTlength|plaintext|padding
					byte[] RBlengthHeader = Arrays.copyOfRange(plaintext, pointer, pointer += config.LENGTH_HEADER_LENGTH);
					int replyblockLength = Util.byteArrayToInt(RBlengthHeader);
					byte[] replyblock = Arrays.copyOfRange(plaintext, pointer, pointer += replyblockLength); 
					ArrayList<byte[]> rpList;
					if(config.replyblocks.containsKey(message.getOwner()))
					{
						rpList = config.replyblocks.get(message.getOwner());
					}
					else
					{
						rpList = new ArrayList<byte[]>();
					}
					rpList.add(replyblock);
					config.replyblocks.put(message.getOwner(), rpList);
					
					byte[] lengthHeader = Arrays.copyOfRange(plaintext, pointer, pointer += config.LENGTH_HEADER_LENGTH);
					int payloadLength = Util.byteArrayToInt(lengthHeader);
					if (payloadLength == 0) // dummy
						plaintext = new byte[0];
					else
						plaintext = Arrays.copyOfRange(plaintext, pointer, pointer + payloadLength); 
					message.setByteMessage(plaintext);
			
					return message;
				} else
					return message;
				
			} else {
				
				message.setByteMessage(Arrays.copyOfRange(plaintext, pointer, plaintext.length));
				
				return message;
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(owner +" Exception-message (ciphertext): " +Util.md5(message.getByteMessage()));
			return null;
		}
		}
	}

	
	


	public Reply recodeReply(Reply message) {
		synchronized(asymmetricCipherReply)
		{
			byte [] replyblock;
			byte[] payload;
		
			if(message.getByteMessage().length<=config.MAX_PAYLOAD)
		{
				int paddingLength = config.MAX_PAYLOAD - message.getByteMessage().length;
				byte[] lengthHeader = Util.intToByteArray(message.getByteMessage().length);
				message.setByteMessage(Util.concatArrays(lengthHeader, message.getByteMessage()));
				if (paddingLength > 0) {
					byte[] padding = new byte[paddingLength];
					 new SecureRandom().nextBytes(padding);
			message.setByteMessage(Util.concatArrays(message.getByteMessage(),padding));
				}
			replyblock = config.replyblocks.get(message.getOwner()).remove(0);
			payload = message.getByteMessage();
			}
		
		else
		{
			replyblock =  Arrays.copyOfRange(message.getByteMessage(), 0, message.getByteMessage().length - (config.MAX_PAYLOAD+config.LENGTH_HEADER_LENGTH));
			payload =  Arrays.copyOfRange(message.getByteMessage(), replyblock.length, message.getByteMessage().length);
		}
 		try {
			// decrypt asymmetrically encrypted part
			byte[] asymPlaintext = asymmetricCipherReply.doFinal(replyblock, 0, asymmetricCipherReply.getBlockSize());
			// extract data from derypted header (= first part of the "asymPlaintext")
			byte[] mac;
			SecretKey macKey;
			SecretKey symKey;
			IvParameterSpec initVector;
			int pointer = 0;
			mac = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.MAC_LENGTH);
//			
//			if (owner.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) { // determine next hop if source routing is enabled
//				byte[] nextHopAddress = Arrays.copyOfRange(asymPlaintext, pointer, pointer += 4);
//				int address = Util.byteArrayToInt(nextHopAddress);
//				if (address == owner.PUBLIC_PSEUDONYM) { // this mix is the last hop
//					if (owner.DISPLAY_ROUTE_INFO)
//						System.out.println(""+owner +" setting nextHopAddress to \"LAST HOP\""); 
//					message.nextHopAddress = MixMessage.NONE;
//					isLastMix = true;
//				} else { // this mix is not the last hop
//					if (owner.DISPLAY_ROUTE_INFO)
//						System.out.println(""+owner +" setting nextHopAddress to " +address); 
//					message.nextHopAddress = address;
//				}
//			}
			

			byte[] macKeyAsByteArray = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.MAC_KEY_LENGTH);
			macKey = new SecretKeySpec(macKeyAsByteArray, config.MAC_ALGORITHM);
			
			byte[] symKeyAsByteArray = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.SYM_KEY_LENGTH);
			symKey = new SecretKeySpec(symKeyAsByteArray, config.SYM_CRYPTOGRAPHY_ALGORITHM);
			
			byte[] ivAsByteArray = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.IV_LENGTH);
			initVector = new IvParameterSpec(ivAsByteArray);
			// decrypt symmetrically encrypted part
			Cipher decryptCipher = Cipher.getInstance(config.SYM_CRYPTOGRAPHY_ALGORITHM, config.CRYPTO_PROVIDER);
					
			decryptCipher.init(Cipher.DECRYPT_MODE, symKey, initVector);
			byte[] symPlaintext = decryptCipher.doFinal(replyblock, asymmetricCipherReply.getBlockSize(), replyblock.length - asymmetricCipherReply.getBlockSize());
			byte[] plaintext = Util.concatArrays(asymPlaintext, symPlaintext);
			// validate mac
			Mac macGenerator = Mac.getInstance(config.MAC_ALGORITHM);
			macGenerator.init(macKey);
			byte[] signedData = Arrays.copyOfRange(plaintext, config.MAC_LENGTH, plaintext.length);
			if (config.DEBUG_ON)
				System.out.println(owner +" plaintext: " +Util.md5(signedData) +" (of " +Util.md5(message.getByteMessage()) +")");
			byte[] locallyGeneratedMac = macGenerator.doFinal(signedData);
			
			if (!Arrays.equals(locallyGeneratedMac, mac)) {
				System.out.println("wrong MAC!"); // TODO
				return null;
			} 
			
			if (config.PERFORM_REPLY_DETECTION)
				if (config.replayDetection.isReplay(macKeyAsByteArray))
					return null;
			replyblock = Arrays.copyOfRange(plaintext, pointer, plaintext.length);
			// encrypt payload with seeded keys
			Cipher symCipher = Cipher.getInstance(
					config.SYM_CRYPTOGRAPHY_ALGORITHM, 
					config.CRYPTO_PROVIDER
					);
			symCipher.init(Cipher.ENCRYPT_MODE, symKey, initVector);
			byte[] payloadCipherText = symCipher.doFinal(payload, 0, payload.length);
			
			byte[] cipherText = Util.concatArrays(replyblock, payloadCipherText);
			if (owner.IS_FIRST_MIX) {
				// remove Padding
				byte[] pseudonym = Arrays.copyOfRange(replyblock, 0, config.PSEUDONYM_LENGTH);
				
				cipherText = Util.concatArrays(pseudonym, payloadCipherText);
			}
				message.setByteMessage(cipherText);
				return message;
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		}
		
		
		
	}

}