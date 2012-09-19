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
package plugIns.layer2recodingScheme.sphinx_v0_001;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import staticFunctions.layer2recodingScheme.curve25519.Curve25519;

import framework.core.AnonNode;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.routing.RoutingMode;
import framework.core.util.Util;


public class Sphinx {

	private AnonNode owner;
	private Sphinx_Config config;
	
	private static SecureRandom secureRandom;
	
	public static final byte[] ZERO16 = {
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	public static final byte[] ZERO32 = {
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	
	
	public Sphinx(AnonNode owner, Sphinx_Config config) {
		this.owner = owner;
		this.config = config;
		if (owner.ROUTING_MODE == RoutingMode.FREE_ROUTE_DYNAMIC_ROUTING)
			throw new RuntimeException("unsupported routing mode: FREE_ROUTE_DYNAMIC_ROUTING"); 
		try {
			Sphinx.secureRandom = SecureRandom.getInstance(config.PRNG_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException("could not init random generator " +config.PRNG_ALGORITHM); 
		}
	}
	

	public void initAsClient() {

	}
	
	
	public void initAsRecoder() {
		
	}
	
	
	public synchronized Request applyLayeredEncryption(Request request) {
		byte[] payload = request.getByteMessage();
		if (payload == null) {
			payload = new byte[0];
			System.out.println(owner +" creating dummy");
			System.out.println(owner +" config.ROUTE_LENGTH: " +config.ROUTE_LENGTH); 
		}
		if (payload.length > config.MAX_PAYLOAD)
			throw new RuntimeException("can't send more than " +config.MAX_PAYLOAD +" bytes in one message"); 
		
		EncryptedMessage em = new EncryptedMessage();
		try {
			int[] route;
			if (owner.ROUTING_MODE == RoutingMode.CASCADE)
				route = owner.mixList.mixIDs;
			else
				route = request.route;
			MixHeader header = new MixHeader(config, secureRandom, new Route(route, config));
			byte[] addressForLastMix = new byte[config.SECURITY_PARAMETER_SIZE];
			secureRandom.nextBytes(addressForLastMix);
			addressForLastMix[0] = MixHeader.SPECIAL_DEST_PREFIX;
			em.alpBetaGam = header.createHeader(addressForLastMix);

			byte[] body = Util.concatArrays(Sphinx.ZERO16, payload);
			body = Sphinx.padBody(body, config);
			assert body.length == config.DELTA_SIZE;
			
			byte[][] deltas = new byte[config.ROUTE_LENGTH][];
			
			// delta v-1
			byte[] delta = Sphinx.pi(Sphinx.hashPi(header.getSecret(config.ROUTE_LENGTH-1), config.SECURITY_PARAMETER_SIZE), body, config.SECURITY_PARAMETER_SIZE);
			deltas[config.ROUTE_LENGTH-1] = delta;
			
			// deltas for 0<=i<v-1
			for (int i=config.ROUTE_LENGTH-2; i>=0; i--) {
				delta = Sphinx.pi(Sphinx.hashPi(header.getSecret(i), config.SECURITY_PARAMETER_SIZE), delta, config.SECURITY_PARAMETER_SIZE);
				deltas[i] = delta;
			}
			
			em.delta = deltas[0];
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		request.setByteMessage(em.toByteArray(config));
		return request;
	}

	
	public int getMaxPayloadForNextMessage() {
		return config.MAX_PAYLOAD;
	}

	
	public int getMaxPayloadForNextReply() {
		return config.MAX_PAYLOAD;
	}

	
	private ReplyData createReplyData(byte[] replyId, int[] route) throws Exception {
		ReplyData replyData = new ReplyData();
		replyData.replyId = replyId;
		Route routeObj = new Route(route, config);
		MixHeader header = new MixHeader(config, secureRandom, routeObj);
		replyData.alpBetaGam = header.createHeader(Sphinx.generateReplyAddress(config.id, replyId, config));
		replyData.ktilde =  new byte[config.SECURITY_PARAMETER_SIZE];
		secureRandom.nextBytes(replyData.ktilde);
		replyData.keytuples = new byte[config.ROUTE_LENGTH][];
		for (int i=0; i<config.ROUTE_LENGTH; i++) {
			try {
				replyData.keytuples[i] = Sphinx.hashPi(header.getSecret(i), config.SECURITY_PARAMETER_SIZE);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		replyData.idMix0 = routeObj.mixIdsSphinx[0];
		return replyData;
	}

	
	// "nym" is the pseudonym, the reply data is accessible under on the nymserver
	public void publishNym(byte[] nym, int[] route) {
		try {
			ReplyData replyData = createReplyData(Sphinx.generateReplyID(config), route);
			config.replyDataTable.put(new String(replyData.replyId, "UTF-8"), replyData);
			byte[] idMix0 = replyData.idMix0;
			byte[][] alpBetaGam = replyData.alpBetaGam;
			byte[] ktilde = replyData.ktilde;
			owner.getInfoService().postValue(new String(nym, "UTF-8"), Util.concatArrays(new byte[][] {idMix0, alpBetaGam[0], alpBetaGam[1], alpBetaGam[2], ktilde}));  // for testing; this should be done via an anonymous channel of course...
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	
	public Reply extractPayload(Reply reply) {
		byte[] replyId = Arrays.copyOfRange(reply.getByteMessage(), 0, 8);
		byte[] message = Arrays.copyOfRange(reply.getByteMessage(), 8, reply.getByteMessage().length);
		ReplyData replyData = config.replyDataTable.get(replyId);
		if (replyData == null) {
			System.out.println("Unreadable reply message received");
			return null;
		} else {
			try {
				for (int i=replyData.keytuples.length-1; i>=0; i--) {
					message = Sphinx.pi(replyData.keytuples[i], message, config.SECURITY_PARAMETER_SIZE);
				}
				message = Sphinx.pii(replyData.ktilde, message, config.SECURITY_PARAMETER_SIZE);
				message = Sphinx.unpadBody(message, config);
				byte[] payload = Arrays.copyOfRange(message, config.SECURITY_PARAMETER_SIZE, message.length);
				if (Arrays.equals(Arrays.copyOf(message, config.SECURITY_PARAMETER_SIZE), Sphinx.ZERO16)) {
					//System.out.println(new String(payload) + " received by " + Util.md5(clientId));
					reply.setByteMessage(payload);
					return reply;
				} else {
					System.err.println("Corrupted message received by");
					return null;
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("could not decrypt reply"); 
			}
		}
	}


	public synchronized Request recodeMessage(Request message) {
		try {
			EncryptedMessage msg = new EncryptedMessage(message, config);
			// System.out.println("processing at " + new String(Hex.encode(_name)));
			if (msg.alpBetaGam[0].length != 32) {
				System.out.println("alpha is not an element of ECC group curve25519");
				return null;
			}
			byte[] sharedSecret = Sphinx.genSharedSecret(msg.alpBetaGam[0], config.privateKey);
			byte[] tag = Sphinx.hashTau(sharedSecret);

			if (config.PERFORM_REPLY_DETECTION) {
				if (config.replayDetection.isReplay(tag)) {
					System.out.println("the shared secret was already seen");
					return null;
				}
			}

			if (!Arrays.equals(msg.alpBetaGam[2], Sphinx.mu(Sphinx.hashMu(sharedSecret, config.SECURITY_PARAMETER_SIZE), msg.alpBetaGam[1], config.SECURITY_PARAMETER_SIZE))) {
				System.err.println("MAC mismatch " +message +", " +Util.md5(message.getByteMessage()) +", " +message +", " +owner); // TODO: remove 
				return null;
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
			byte[] B = Sphinx.xor(Util.concatArrays(msg.alpBetaGam[1], Sphinx.ZERO32), Sphinx.rho(Sphinx.hashRho(sharedSecret, config.SECURITY_PARAMETER_SIZE), config.ROUTE_LENGTH, config.SECURITY_PARAMETER_SIZE));
			
			if (B[0] == MixHeader.MIX_PREFIX) { // this mix is not the final hop -> forward message to next hop
				
				byte[] nextMixId = Arrays.copyOf(B, config.SECURITY_PARAMETER_SIZE);
				//byte[] remaining = Arrays.copyOfRange(B, Config._k, B.length);
				byte[] blindingFactor = Sphinx.hashB(msg.alpBetaGam[0], sharedSecret);
				msg.alpBetaGam[0] = Sphinx.genSharedSecret(msg.alpBetaGam[0], blindingFactor);
				msg.alpBetaGam[2] = Arrays.copyOfRange(B, config.SECURITY_PARAMETER_SIZE, 2 * config.SECURITY_PARAMETER_SIZE);
				msg.alpBetaGam[1] = Arrays.copyOfRange(B, config.SECURITY_PARAMETER_SIZE * 2, B.length);
				msg.delta = Sphinx.pii(Sphinx.hashPi(sharedSecret, config.SECURITY_PARAMETER_SIZE), msg.delta, config.SECURITY_PARAMETER_SIZE);
				//System.out.println("this mix is not the final hop -> forward message to next hop"); 
				if (owner.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) {
					message.nextHopAddress = config.getGlobalMixIdFor(nextMixId);
					assert message.nextHopAddress != -1;
					if (owner.DISPLAY_ROUTE_INFO)
						System.out.println(""+owner +" setting nextHopAddress to " +message.nextHopAddress); 
				}
				message.setByteMessage(msg.toByteArray(config));
				return message;
			
			} else if (B[0] == MixHeader.SPECIAL_DEST_PREFIX) { // this mix is the final hop; message is a request
				
				//System.out.println("this mix is the final hop; message is a request"); 
				msg.delta = Sphinx.pii(Sphinx.hashPi(sharedSecret, config.SECURITY_PARAMETER_SIZE), msg.delta, config.SECURITY_PARAMETER_SIZE);
				msg.delta = Sphinx.unpadBody(msg.delta, config);
				byte[] payload = Arrays.copyOfRange(msg.delta, config.SECURITY_PARAMETER_SIZE, msg.delta.length);
				//System.out.println("payload received by last mix: " +Util.md5(payload)); 
				message.setByteMessage(payload);
				if (owner.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) {
					message.nextHopAddress = MixMessage.NONE;
					assert message.nextHopAddress != -1;
					if (owner.DISPLAY_ROUTE_INFO)
						System.out.println(""+owner +" setting nextHopAddress to \"LAST HOP\"");
				}
				return message;
			
			} else if (B[0] == MixHeader.CLIENT_PREFIX) { // this mix is the final hop; message is a reply
				
				//System.out.println("this mix is the final hop; message is a reply"); 
				byte[] addressData = Arrays.copyOf(B, config.SECURITY_PARAMETER_SIZE);
				byte[] clientId = Sphinx.extractClientId(addressData, config);
				byte[] replyId = Sphinx.extractReplyId(addressData, config);
				msg.delta = Sphinx.pii(Sphinx.hashPi(sharedSecret, config.SECURITY_PARAMETER_SIZE), msg.delta, config.SECURITY_PARAMETER_SIZE);
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

	
	
	
	/**
	 * Compute a pseudo-random generator
	 * 
	 * @param key
	 * @return the PRG
	 * @throws NoSuchPaddingException
	 * @throws NoSuchProviderException
	 * @throws NoSuchAlgorithmException
	 */
	public static byte[] rho(byte[] key, int _r, int _k) throws Exception {
		byte[] zeros = new byte[(2 * _r + 3) * _k];
		// Arrays.fill(zeros, (byte)0);
		// byte[] prg = new byte[(2 * _r + 3) * _k];
		byte[] prg = null;
		SecretKey secret = new SecretKeySpec(key, "AES");

		Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding", "BC");
		cipher.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(ZERO16));

		prg = cipher.doFinal(zeros);
		return prg;
	}

	
	public static byte[] hashRho(byte[] s, int _k) throws Exception {
		return Arrays.copyOf(hash(Util.concatArrays("hashRho".getBytes("utf-8"), s)), _k);
	}
	

	/**
	 * The HMAC
	 * 
	 * @param key
	 *            The hashed shared secret of length _k
	 * @param data
	 *            Data of arbitrary length
	 * @return The truncated HMAC of length _k
	 */
	public static byte[] mu(byte[] key, byte[] data, int _k) throws Exception {
		byte[] out = null;
		SecretKey secret = new SecretKeySpec(key, "Hmac-SHA256");
		Mac mac = Mac.getInstance("Hmac-SHA256", "BC");
		mac.init(secret);
		out = mac.doFinal(data);
		return Arrays.copyOf(out, _k);

	}

	
	public static byte[] hashMu(byte[] s, int _k) throws Exception {
		return Arrays.copyOf(hash(Util.concatArrays("hashMu:".getBytes("utf-8"), s)), _k);
	}

	
	public static byte[] pi(byte[] key, byte[] data, int _k) throws Exception {
		return lionessEnc(key, data, _k);
	}

	
	public static byte[] pii(byte[] key, byte[] data, int _k) throws Exception {
		return lionessDec(key, data, _k);
	}

	
	public static byte[] hashPi(byte[] s, int _k) throws Exception {
		return Arrays.copyOf(hash(Util.concatArrays("hashPi:".getBytes("utf-8"), s)), _k);
	}

	
	public static byte[] hashTau(byte[] s) throws Exception {
		return hash(Util.concatArrays("hashTau:".getBytes("utf-8"), s));
	}

	
	/**
	 * Compute a hash to use as a blinding factor
	 */
	public static byte[] hashB(byte[] alpha, byte[] s) throws Exception {
		return hash(Util.concatArrays(new byte[][] {"hashB".getBytes("utf-8"), alpha, s}));

	}

	
	/**
	 * Compute a SHA-256 hash of the input
	 */
	public static byte[] hash(byte[] data) throws Exception {
		byte[] hash = null;
		MessageDigest md = MessageDigest.getInstance("SHA-256", "BC");
		hash = md.digest(data);
		return hash;
	}


	public static byte[] lionessEnc(byte[] key, byte[] msg, int _k) throws Exception {
		assert key.length == _k;
		assert msg.length >= _k * 2;

		// Round 1
		byte[] msgTrunc1 = Arrays.copyOfRange(msg, _k, msg.length);
		byte[] msgTrunc2 = Arrays.copyOf(msg, _k);
		byte[] hashR1 = Arrays.copyOf(
				hash(Util.concatArrays(new byte[][] {msgTrunc1, key, "1".getBytes("utf-8")})), _k);

		byte[] r1 = Util.concatArrays(xor(hashR1, msgTrunc2), msgTrunc1);

		// Round 2
		byte[] k2 = xor(Arrays.copyOf(r1, _k), key);
		byte[] r2 = null;
		{
			Cipher c = Cipher.getInstance("AES/CTR/NoPadding", "BC");
			c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(k2, "AES"),
					new IvParameterSpec(ZERO16));
			r2 = Util.concatArrays(Arrays.copyOf(r1, _k),
					c.doFinal(Arrays.copyOfRange(r1, _k, r1.length)));
		}
		// Round 3
		byte[] r2Trunc1 = Arrays.copyOfRange(r2, _k, r2.length);
		byte[] r2Trunc2 = Arrays.copyOf(r2, _k);
		byte[] hashR3 = Arrays.copyOf(
				hash(Util.concatArrays(new byte[][] {r2Trunc1, key, "3".getBytes("utf-8")})), _k);

		byte[] r3 = Util.concatArrays(xor(hashR3, r2Trunc2), r2Trunc1);
		// Round 4
		byte[] k4 = xor(Arrays.copyOf(r3, _k), key);
		byte[] r4 = null;
		{
			Cipher c = Cipher.getInstance("AES/CTR/NoPadding", "BC");
			c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(k4, "AES"),
					new IvParameterSpec(ZERO16));
			r4 = Util.concatArrays(Arrays.copyOf(r3, _k),
					c.doFinal(Arrays.copyOfRange(r3, _k, r3.length)));
		}
		return r4;
	}

	public static byte[] lionessDec(byte[] key, byte[] msg, int _k) throws Exception {
		byte[] r4 = msg;

		// Round 4
		byte[] k4 = xor(Arrays.copyOf(r4, _k), key);
		byte[] r3 = null;
		{
			Cipher c = Cipher.getInstance("AES/CTR/NoPadding", "BC");
			c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(k4, "AES"),
					new IvParameterSpec(ZERO16));
			r3 = Util.concatArrays(Arrays.copyOf(r4, _k),
					c.doFinal(Arrays.copyOfRange(r4, _k, r4.length)));
		}

		// Round 3
		byte[] r3Trunc1 = Arrays.copyOfRange(r3, _k, r3.length);
		byte[] r3Trunc2 = Arrays.copyOf(r3, _k);
		byte[] hashR2 = Arrays.copyOf(
				hash(Util.concatArrays(new byte[][] {r3Trunc1, key, "3".getBytes("utf-8")})), _k);
		byte[] r2 = Util.concatArrays(xor(hashR2, r3Trunc2), r3Trunc1);

		// Round 2
		byte[] k2 = xor(Arrays.copyOf(r2, _k), key);
		byte[] r1 = null;
		{
			Cipher c = Cipher.getInstance("AES/CTR/NoPadding", "BC");
			c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(k2, "AES"),
					new IvParameterSpec(ZERO16));
			r1 = Util.concatArrays(Arrays.copyOf(r2, _k),
					c.doFinal(Arrays.copyOfRange(r2, _k, r2.length)));
		}

		// Round 1
		byte[] r1Trunc1 = Arrays.copyOfRange(r1, _k, r1.length);
		byte[] r1Trunc2 = Arrays.copyOf(r1, _k);
		byte[] hashR1 = Arrays.copyOf(
				hash(Util.concatArrays(new byte[][] {r1Trunc1, key, "1".getBytes("utf-8")})), _k);
		byte[] r0 = Util.concatArrays(xor(hashR1, r1Trunc2), r1Trunc1);

		return r0;
	}

	
	/**
	 * Generate the shared secret pubKey^privKey (element of ECC curve22519)
	 * 
	 * @param pubKey
	 *            public key of participant 1 (Base)
	 * @param privKey
	 *            private key of participant 2 (Exponent)
	 * @return the shared secret for both participants
	 */
	public static byte[] genSharedSecret(byte[] pubKey, byte[] privKey) {
		byte[] sharedSecret = new byte[32];
		Curve25519.curve(sharedSecret, privKey, pubKey);
		return sharedSecret;

	}
	

	public static byte[] genSharedSecret(byte[] base, byte[][] exps) {
		byte[] sharedSecret = new byte[32];
		sharedSecret = genSharedSecret(base, exps[0]);
		for (int exp = 1; exp<exps.length; exp++)
			if (exps[exp] != null)
				sharedSecret = genSharedSecret(sharedSecret, exps[exp]);
		return sharedSecret;
	}

	
	// overhead: 1 byte
	public static byte[] padBody(byte[] body, Sphinx_Config config) {
		byte[] paddedBody = Util.concatArrays(body, new byte[] {(byte) 127});
		int l = config.DELTA_SIZE - paddedBody.length;
		if (l > 0) {
			byte[] padding = new byte[l];
			Arrays.fill(padding, (byte) 255);
			paddedBody = Util.concatArrays(paddedBody, padding);
		}
		assert paddedBody.length == config.DELTA_SIZE;
		return paddedBody;
	}

	
	public static byte[] unpadBody(byte[] body, Sphinx_Config config) {
		assert body.length == config.DELTA_SIZE; 
		int paddingLength = 0;
		for (int i = body.length - 1; i >= 0; i--) {
			assert (body[i] == (byte) 255 | body[i] == (byte) 127);
			paddingLength++; // number of padded x7f and xff bytes (= Number of xff's +1)
			if (body[i] == (byte) 127)
				break;
		}
		return Arrays.copyOf(body, body.length - paddingLength);
	}
	
	
	/**
	 * Xor two byte-arrays
	 */
	public static byte[] xor(byte[] x, byte[] y) {
		assert (x.length == y.length): "x.length != y.length (" +x.length +" != " +y.length +")";
		byte[] xored = new byte[x.length];
		for (int i = 0; i < xored.length; i++)
			xored[i] = (byte) (x[i] ^ y[i]);
		return xored;
	}
	
	
	// 0: public
	// 1: private
	public static byte[][] generateKeyPair(Sphinx_Config sphinx_Config) {
		byte[] publicKey = new byte[sphinx_Config.ALPHA_SIZE];
		byte[] privateKey = new byte[sphinx_Config.ALPHA_SIZE];
		try {
			SecureRandom secureRandom = SecureRandom.getInstance(sphinx_Config.PRNG_ALGORITHM);
			secureRandom.nextBytes(privateKey);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("could not generate keyPair"); 
		}
		Curve25519.keygen(publicKey, null, privateKey);
		return new byte[][] {publicKey, privateKey};
	}
	
	
	public static byte[] generateMixId(Sphinx_Config sphinx_Config) {
		byte[] pseudonym = new byte[sphinx_Config.SECURITY_PARAMETER_SIZE];
		SecureRandom secureRandom;
		try {
			secureRandom = SecureRandom.getInstance(sphinx_Config.PRNG_ALGORITHM);
			secureRandom.nextBytes(pseudonym);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException("could not generate mix id"); 
		}
		pseudonym[0] = MixHeader.MIX_PREFIX;
		return pseudonym;
	}
	
	
	public static byte[] generateClientId(Sphinx_Config sphinx_Config) {
		byte[] id = new byte[sphinx_Config.SECURITY_PARAMETER_SIZE / 2];
		SecureRandom secureRandom;
		try {
			secureRandom = SecureRandom.getInstance(sphinx_Config.PRNG_ALGORITHM);
			secureRandom.nextBytes(id);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException("could not generate mix id"); 
		}
		id[0] = MixHeader.CLIENT_PREFIX;
		return id;
	}
	
	
	public static byte[] generateReplyAddress(byte[] clientID, byte[] replyID, Sphinx_Config config) {
		assert clientID.length == config.SECURITY_PARAMETER_SIZE / 2;
		assert replyID.length == config.SECURITY_PARAMETER_SIZE / 2;
		assert clientID[0] == MixHeader.CLIENT_PREFIX;;
		return Util.concatArrays(clientID, replyID);
	}
	
	
	// id used to identify the keys needed to derypt a reply
	public static byte[] generateReplyID(Sphinx_Config config) {
		byte[] id = new byte[config.SECURITY_PARAMETER_SIZE / 2];
		secureRandom.nextBytes(id);
		return id;
	}
	
	
	public static byte[] extractClientId(byte[] replyAddress, Sphinx_Config config) {
		return Arrays.copyOfRange(replyAddress, 0, config.SECURITY_PARAMETER_SIZE / 2);
	}
	
	
	public static byte[] extractReplyId(byte[] replyAddress, Sphinx_Config config) {
		return Arrays.copyOfRange(replyAddress, config.SECURITY_PARAMETER_SIZE / 2, replyAddress.length);
	}

}
