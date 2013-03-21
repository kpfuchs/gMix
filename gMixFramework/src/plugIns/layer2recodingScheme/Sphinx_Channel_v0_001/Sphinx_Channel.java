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
package plugIns.layer2recodingScheme.Sphinx_Channel_v0_001;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import plugIns.layer2recodingScheme.Sphinx_Channel_v0_001.MixPlugIn.ChannelData;

import framework.core.AnonNode;
import framework.core.config.Settings;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.routing.RoutingMode;
import framework.core.util.Util;


public class Sphinx_Channel {

	private AnonNode owner;
	private Sphinx_Channel_Config config;
	private Sphinx_Config configSphinx;
	private Settings settings;
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
	
	
	public Sphinx_Channel(AnonNode owner, Sphinx_Channel_Config config, Sphinx_Config configSphinx) {
		this.owner = owner;
		this.config = config;
		this.configSphinx = configSphinx;
		this.settings = owner.getSettings();
		
	}
	
	
	public void initAsClient() {
		this.macKeys = new SecretKey[config.routeLength]; 
		this.sessionKeysForRequestChannel = new SecretKey[config.routeLength];
		this.sessionKeysForReplyChannel = new SecretKey[config.routeLength];
		this.sessionIVsForRequestChannel = new IvParameterSpec[config.routeLength];
		this.sessionIVsForReplyChannel = new IvParameterSpec[config.routeLength];
		this.symmetricEncryptCiphers = new Cipher[config.routeLength];
		this.symmetricDecryptCiphers = new Cipher[config.routeLength];
		this.asymmetricCiphers = new Cipher[config.routeLength];
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
	
	
	// returns the message's payload
	public synchronized Request recodeMessage(Request message, ChannelData channelData) {
		if (channelData.macKey == null)
			return recodeChannelEstablishMessage(message, channelData);
		else
			return recodeChannelMessage(message, channelData);
	}


	private Request recodeChannelEstablishMessage(Request message, ChannelData channelData) {
		try {
			EncryptedMessage msg = new EncryptedMessage(message, configSphinx);
			// System.out.println("processing at " + new String(Hex.encode(_name)));
			if (msg.alpBetaGam[0].length != 32) {
				System.out.println("alpha is not an element of ECC group curve25519");
				return null;
			}
			byte[] sharedSecret = Sphinx.genSharedSecret(msg.alpBetaGam[0], configSphinx.privateKey);
			byte[] tag = Sphinx.hashTau(sharedSecret);

			if (config.PERFORM_REPLY_DETECTION) {
				if (config.replayDetection.isReplay(tag)) {
					System.out.println("the shared secret was already seen");
					return null;
				}
			}

			if (!Arrays.equals(msg.alpBetaGam[2], Sphinx.mu(Sphinx.hashMu(sharedSecret, configSphinx.SECURITY_PARAMETER_SIZE), msg.alpBetaGam[1], configSphinx.SECURITY_PARAMETER_SIZE))) {
				System.err.println("MAC mismatch " +message +", " +Util.md5(message.getByteMessage()) +", " +message +", " +owner); // TODO: remove 
				return null;
			}
			
			SecureRandom prng = SecureRandom.getInstance(config.PRNG_ALGORITHM);
			prng.setSeed(sharedSecret);
			
			byte[] symKeyReqAsByte= new byte[config.SYM_KEY_LENGTH];
			prng.nextBytes(symKeyReqAsByte);
			SecretKey symKey = new SecretKeySpec(symKeyReqAsByte, config.SYM_CRYPTOGRAPHY_ALGORITHM);
			
			byte[] ivReq = new byte[config.IV_LENGTH];
			prng.nextBytes(ivReq);
			IvParameterSpec initVector = new IvParameterSpec(ivReq);
			
			byte[] macKeyAsByteArray = new byte[config.MAC_KEY_LENGTH];
			prng.nextBytes(macKeyAsByteArray);
			
			channelData.decryptCipher = Cipher.getInstance(config.SYM_CRYPTOGRAPHY_ALGORITHM, config.CRYPTO_PROVIDER);
			channelData.decryptCipher.init(Cipher.DECRYPT_MODE, symKey, initVector);
			channelData.macKey = new SecretKeySpec(macKeyAsByteArray, config.MAC_ALGORITHM);
			if(owner.IS_DUPLEX)
			{
				byte[] symKeyRepAsByte = new byte[config.SYM_KEY_LENGTH];
				prng.nextBytes(symKeyRepAsByte);
				SecretKey symKeyRep = new SecretKeySpec(symKeyRepAsByte, config.SYM_CRYPTOGRAPHY_ALGORITHM);
				
				byte[] ivRep = new byte[config.IV_LENGTH];
				prng.nextBytes(ivRep);
				IvParameterSpec initVectorRep = new IvParameterSpec(ivRep);
				channelData.encryptCipher = Cipher.getInstance(config.SYM_CRYPTOGRAPHY_ALGORITHM, config.CRYPTO_PROVIDER);
				channelData.encryptCipher.init(Cipher.ENCRYPT_MODE, symKeyRep, initVectorRep);
			}
			
			/*
			 * if (anonNode.ROUTING_MODE == RoutingMode.CASCADE) {
						outputStrategyLayerMix.addRequest(request);
					} else if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) {
						byte[][] splitted = Util.split(getRouteHeaderSize(anonNode), request.getByteMessage());
						UnpackedIdArray routeInfo = MixList.unpackIdArrayWithPos(splitted[0]);
						if (routeInfo.pos >= routeInfo.route.length) {
							if (anonNode.DISPLAY_ROUTE_INFO)
								System.out.println(""+anonNode +" setting nextHopAddress to \"LAST HOP\" (pos: " +routeInfo.pos +")"); 
							request.nextHopAddress = MixMessage.NONE;
							request.setByteMessage(splitted[1]);
						} else {
							if (anonNode.DISPLAY_ROUTE_INFO)
								System.out.println(""+anonNode +" setting nextHopAddress to " +routeInfo.route[routeInfo.pos] +", pos: " +routeInfo.pos); 
							request.nextHopAddress = routeInfo.route[routeInfo.pos];
							routeInfo.pos++;
							System.arraycopy(MixList.packIdArray(routeInfo), 0, request.getByteMessage(), 0, getRouteHeaderSize(anonNode));
						}
						outputStrategyLayerMix.addRequest(request);
					} else if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_DYNAMIC_ROUTING) {
						request.nextHopAddress = anonNode.mixList.getRandomMixId();
						outputStrategyLayerMix.addRequest(request);
					} else {
						throw new RuntimeException("not supported routing mode: " +anonNode.ROUTING_MODE); 
					}
			 */
			byte[] B = Sphinx.xor(Util.concatArrays(msg.alpBetaGam[1], Sphinx.ZERO32), Sphinx.rho(Sphinx.hashRho(sharedSecret, configSphinx.SECURITY_PARAMETER_SIZE), configSphinx.ROUTE_LENGTH, configSphinx.SECURITY_PARAMETER_SIZE));
			
			if (B[0] == MixHeader.MIX_PREFIX) { // this mix is not the final hop -> forward message to next hop
				
				byte[] nextMixId = Arrays.copyOf(B, configSphinx.SECURITY_PARAMETER_SIZE);
				//byte[] remaining = Arrays.copyOfRange(B, Config._k, B.length);
				byte[] blindingFactor = Sphinx.hashB(msg.alpBetaGam[0], sharedSecret);
				msg.alpBetaGam[0] = Sphinx.genSharedSecret(msg.alpBetaGam[0], blindingFactor);
				msg.alpBetaGam[2] = Arrays.copyOfRange(B, configSphinx.SECURITY_PARAMETER_SIZE, 2 * configSphinx.SECURITY_PARAMETER_SIZE);
				msg.alpBetaGam[1] = Arrays.copyOfRange(B, configSphinx.SECURITY_PARAMETER_SIZE * 2, B.length);
				msg.delta = Sphinx.pii(Sphinx.hashPi(sharedSecret, configSphinx.SECURITY_PARAMETER_SIZE), msg.delta, configSphinx.SECURITY_PARAMETER_SIZE);
				//System.out.println("this mix is not the final hop -> forward message to next hop"); 
				if (owner.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) {
					message.nextHopAddress = configSphinx.getGlobalMixIdFor(nextMixId);
					channelData.nextHopAddress = configSphinx.getGlobalMixIdFor(nextMixId);
					assert message.nextHopAddress != -1;
					if (owner.DISPLAY_ROUTE_INFO)
						System.out.println(""+owner +" setting nextHopAddress to " +message.nextHopAddress); 
				}
				message.setByteMessage(msg.toByteArray(configSphinx));
				return message;
			
			} else if (B[0] == MixHeader.SPECIAL_DEST_PREFIX) { // this mix is the final hop; message is a request
				
				//System.out.println("this mix is the final hop; message is a request"); 
				msg.delta = Sphinx.pii(Sphinx.hashPi(sharedSecret, configSphinx.SECURITY_PARAMETER_SIZE), msg.delta, configSphinx.SECURITY_PARAMETER_SIZE);
				msg.delta = Sphinx.unpadBody(msg.delta, configSphinx);
				byte[] payload = Arrays.copyOfRange(msg.delta, configSphinx.SECURITY_PARAMETER_SIZE, msg.delta.length);
				//System.out.println("payload received by last mix: " +Util.md5(payload)); 
				message.setByteMessage(payload);
				if (owner.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) {
					message.nextHopAddress = MixMessage.NONE;
					channelData.nextHopAddress = MixMessage.NONE; 
					assert message.nextHopAddress != -1;
					if (owner.DISPLAY_ROUTE_INFO)
						System.out.println(""+owner +" setting nextHopAddress to \"LAST HOP\"");
				}
				return message;
			
			} else if (B[0] == MixHeader.CLIENT_PREFIX) { // this mix is the final hop; message is a reply
				
				//System.out.println("this mix is the final hop; message is a reply"); 
				byte[] addressData = Arrays.copyOf(B, configSphinx.SECURITY_PARAMETER_SIZE);
				byte[] clientId = Sphinx.extractClientId(addressData, configSphinx);
				byte[] replyId = Sphinx.extractReplyId(addressData, configSphinx);
				msg.delta = Sphinx.pii(Sphinx.hashPi(sharedSecret, configSphinx.SECURITY_PARAMETER_SIZE), msg.delta, configSphinx.SECURITY_PARAMETER_SIZE);
				message.nextHopAddress = Util.byteArrayToInt(owner.getInfoService().getValue(new String(clientId, "UTF-8"))); // TODO -> mechanism to forward replies...
				message.setByteMessage(Util.concatArrays(replyId, msg.delta));
				return message;
			
			} else {
				System.err.println("received unknown id"); 
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
	
	private Request recodeChannelMessage(Request message, ChannelData channelData) {
		try {
			String ct = Util.md5(message.getByteMessage());
			int ctlength = message.getByteMessage().length;
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
				System.err.println(owner.toString() +"wrong MAC cm! " +message +": " +ct +" mac of " +Util.md5(signedData) +" is " +Util.md5(mac)); // TODO
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
			
			if (owner.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) { // set next hop address if source routing is enabled
				if (channelData.nextHopAddress == owner.PUBLIC_PSEUDONYM) { // this mix is the last hop
					if (owner.DISPLAY_ROUTE_INFO)
						System.out.println(""+owner +" setting nextHopAddress to \"LAST HOP\""); 
					message.nextHopAddress = MixMessage.NONE;
				} else { // this mix is not the last hop
					if (owner.DISPLAY_ROUTE_INFO)
						System.out.println(""+owner +" setting nextHopAddress to " +channelData.nextHopAddress); 
					message.nextHopAddress = channelData.nextHopAddress;
				}
			}
			
			return message;
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			return null;
		}
		
	}

	
	public synchronized Reply recodeReply(Reply message, ChannelData channelData) {
		boolean isLastMix = false;
		if (owner.ROUTING_MODE == RoutingMode.CASCADE && owner.IS_LAST_MIX)
			isLastMix = true;
		else if (channelData.nextHopAddress == MixMessage.NONE)
			isLastMix = true;
		if (isLastMix) {
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
		// System.out.println(" oOoOo " +owner.toString() +" sende " +Util.md5(message.getByteMessage()) + " -> " +Util.md5(result) +"( for " +message.getOwner() +", " +channelData +")"); 
		assert result.length == message.getByteMessage().length: "" +result.length +" != " +message.getByteMessage().length;
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
		//System.out.println("START CHANNEL ESTABLISH MESSAGE!!!");
		EncryptedMessage em = new EncryptedMessage();
		try {
			int[] route;
			if (owner.ROUTING_MODE == RoutingMode.CASCADE)
				route = owner.mixList.mixIDs;
			else
				route = request.route;
			MixHeader header = new MixHeader(configSphinx, secureRandom, new Route(route, configSphinx));
			byte[] addressForLastMix = new byte[configSphinx.SECURITY_PARAMETER_SIZE];
			secureRandom.nextBytes(addressForLastMix);
			addressForLastMix[0] = MixHeader.SPECIAL_DEST_PREFIX;
			em.alpBetaGam = header.createHeader(addressForLastMix);

			byte[] body = Util.concatArrays(Sphinx.ZERO16, payload);
			body = Sphinx.padBody(body, configSphinx);
			assert body.length == configSphinx.DELTA_SIZE;
			
			byte[][] deltas = new byte[configSphinx.ROUTE_LENGTH][];
			
			// delta v-1
			byte[] delta = Sphinx.pi(Sphinx.hashPi(header.getSecret(configSphinx.ROUTE_LENGTH-1), configSphinx.SECURITY_PARAMETER_SIZE), body, configSphinx.SECURITY_PARAMETER_SIZE);
			deltas[configSphinx.ROUTE_LENGTH-1] = delta;
			
			// deltas for 0<=i<v-1
			for (int i=configSphinx.ROUTE_LENGTH-2; i>=0; i--) {
				delta = Sphinx.pi(Sphinx.hashPi(header.getSecret(i), configSphinx.SECURITY_PARAMETER_SIZE), delta, configSphinx.SECURITY_PARAMETER_SIZE);
				deltas[i] = delta;
			}
			
			em.delta = deltas[0];
			
	
		// generate keys, init ciphers etc.
		for (int i=config.routeLength-1; i>=0; i--) {
			
				this.asymmetricCiphers[i] = Cipher.getInstance(settings.getProperty("ASYM_CRYPTOGRAPHY_ALGORITHM"), owner.CRYPTO_PROVIDER);
				if (owner.ROUTING_MODE == RoutingMode.CASCADE)
					this.asymmetricCiphers[i].init(Cipher.ENCRYPT_MODE, config.publicKeysOfMixes[i]);
				else {
					this.asymmetricCiphers[i].init(Cipher.ENCRYPT_MODE, config.publicKeysOfMixes[request.route[i]]); // TODO: possible side-effect -> won't work anymore if mix-ids are chosen differently
					System.out.println("" +owner +" using public key " +Util.md5(config.publicKeysOfMixes[request.route[i]].getEncoded()) +"for mix " +request.route[i]);
				}
				SecureRandom prng = SecureRandom.getInstance(config.PRNG_ALGORITHM);
				prng.setSeed(header.getSecret(i));
				byte[] sessionKeyRequest= new byte[config.SYM_KEY_LENGTH];
				prng.nextBytes(sessionKeyRequest);
				this.sessionKeysForRequestChannel[i] = new SecretKeySpec(sessionKeyRequest, config.SYM_CRYPTOGRAPHY_ALGORITHM);
				byte[] ivReq = new byte[sessionKeysForRequestChannel[i].getEncoded().length];
				prng.nextBytes(ivReq);
				this.sessionIVsForRequestChannel[i] = new IvParameterSpec(ivReq);
				byte[] macKey = new byte[config.MAC_KEY_LENGTH];
				prng.nextBytes(macKey);
				this.macKeys[i] = new SecretKeySpec(macKey, config.MAC_ALGORITHM);
				this.symmetricEncryptCiphers[i] = Cipher.getInstance(settings.getProperty("SYM_CRYPTOGRAPHY_ALGORITHM"), owner.CRYPTO_PROVIDER);
				this.symmetricEncryptCiphers[i].init(Cipher.ENCRYPT_MODE, sessionKeysForRequestChannel[i], sessionIVsForRequestChannel[i]);             				
				if (owner.IS_DUPLEX) {
					byte[] sessionKeyReply= new byte[config.SYM_KEY_LENGTH];
					prng.nextBytes(sessionKeyReply);
					this.sessionKeysForReplyChannel[i] = new SecretKeySpec(sessionKeyReply, config.SYM_CRYPTOGRAPHY_ALGORITHM);
					byte[] ivRep = new byte[sessionKeysForReplyChannel[i].getEncoded().length];
					prng.nextBytes(ivRep);
					this.sessionIVsForReplyChannel[i] = new IvParameterSpec(ivRep);
					this.symmetricDecryptCiphers[i] = Cipher.getInstance(settings.getProperty("SYM_CRYPTOGRAPHY_ALGORITHM"), owner.CRYPTO_PROVIDER);
					this.symmetricDecryptCiphers[i].init(Cipher.DECRYPT_MODE, sessionKeysForReplyChannel[i], sessionIVsForReplyChannel[i]);          
				}
		
		}} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		request.setByteMessage(em.toByteArray(configSphinx));
		return request;
	}
	
	
	private Request createChannelMessage(Request request) {
		byte[] payload = request.getByteMessage();
		if (payload == null) // dummy
			payload = new byte[0];
		if (payload.length > config.MAX_PAYLOAD)
			throw new RuntimeException("can't send more than " +config.MAX_PAYLOAD +" bytes in one message"); 
		
		for (int i=config.routeLength-1; i>=0; i--) {
			// add length-header
			try {
				byte[] payloadLength = Util.intToByteArray(payload.length);
				payload = Util.concatArrays(payloadLength, payload);
				// add padding for last mix' payload
				if (i == config.routeLength-1 && payload.length != config.MAX_PAYLOAD) {
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
		//byte[] forLater = reply.getByteMessage().clone();
		for (int i=0; i<config.routeLength; i++) {
			Cipher cipher = symmetricDecryptCiphers[i];
			byte[] plaintext = cipher.update(result);
			assert plaintext.length == result.length;
			//System.out.println(" oOoOo " +owner.toString() +" haber empfangen: " +Util.md5(plaintext) + " <- " +Util.md5(result)); 
			result = plaintext;
		}
		int lengthOfPayload = Util.byteArrayToInt(Arrays.copyOf(result, 4));
		try {
			result = Arrays.copyOfRange(result, config.LENGTH_HEADER_LENGTH, config.LENGTH_HEADER_LENGTH + lengthOfPayload);
			
		} catch (Exception e) {
			//System.err.println("oOoOo " +owner.toString() +" haber empfangen: " +Util.md5(result) + " <- " +Util.md5(forLater)); 
			e.printStackTrace();
		}
		reply.setByteMessage(result);
		return reply;
	}

}
