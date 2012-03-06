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

import infoService.InfoServiceClient_v1;
import infoService.MixList;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import client.ClientController;

import message.Request;

import recodingScheme.staticFunctions.Curve25519;
import recodingScheme.staticFunctions.ReplayDetectionBasic_v1;

import userDatabase.User;
import framework.Implementation;
import framework.LocalClassLoader;
import framework.Settings;
import framework.Util;


public class Sphinx_v1 extends Implementation implements RecodingScheme {

	private Config config;
	private RequestThread[] requestThreads;
	@SuppressWarnings("unused")
	private Sphinx_v1_MessageCreator messageCreator;
	private static SecureRandom secureRandom;
	
	public static final byte[] ZERO16 = {
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	public static final byte[] ZERO32 = {
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	
	
	public Sphinx_v1() {
		
	}
	
	// used by Sphinx_v1_MessageCreator
	public Sphinx_v1(Settings settings, InfoServiceClient_v1 infoService, ClientController client){
		this.settings = settings;
		this.infoService = infoService;
		this.clientController = client;
	}
	

	@Override
	public void constructor() {
		this.config = new Config();
		try {
			secureRandom = SecureRandom.getInstance(config.PRNG_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException("could not init random generator " +config.PRNG_ALGORITHM); 
		}
		String packageName = this.getClass().getPackage().toString().replace("package ", "");
		requestThreads = new RequestThread[config.NUMBER_OF_THREADS];
		
		for (int i=0; i<config.NUMBER_OF_THREADS; i++) {
			Sphinx_v1_Recoder recodingScheme = (Sphinx_v1_Recoder)LocalClassLoader.instantiateSubImplementation(packageName, "Sphinx_v1_Recoder.java", this);
			requestThreads[i] = new RequestThread(recodingScheme);
		}
	}

	
	@Override
	public void initialize() {
		String packageName = this.getClass().getPackage().toString().replace("package ", "");
		if (!mix.isLastMix()) {
			config.mixList = infoService.getMixList();
			config.mixIdsSphinx = infoService.getValueFromAllMixes("SPHINX_MIX_ID");
			config.publicKeysOfMixes = infoService.getValueFromAllMixes("SPHINX_PUBLIC_KEY");
			config.NUMBER_OF_MIXES = config.publicKeysOfMixes.length;
			config.publicKeysOfMixes = Arrays.copyOfRange(config.publicKeysOfMixes, (mix.getIdentifier() +1), config.publicKeysOfMixes.length);
			this.messageCreator = (Sphinx_v1_MessageCreator) LocalClassLoader.instantiateSubImplementation(packageName, "Sphinx_v1_MessageCreator.java", this); 
		}
	}

	@Override
	public void begin() {
		for (int i=0; i<requestThreads.length; i++)
			requestThreads[i].start();
	}

	
	@Override
	public boolean supportsDummyTraffic() {
		return false;	// TODO: implement
	}

	
	@Override
	public int getMaxPayloadForNextReply(User user) {
		return config.MAX_PAYLOAD_SIZE;
	}

	
	class Config {
		
		int MAX_PAYLOAD_SIZE = settings.getPropertyAsInt("MAX_PAYLOAD_SIZE");
		int SECURITY_PARAMETER_SIZE = 16;  // k must be 16 bytes to work with ECC (curve25519)
		int NUMBER_OF_MIXES;
		int ROUTE_LENGTH; // number of mixes to chose by clients
		String PRNG_ALGORITHM = settings.getProperty("PRNG_ALGORITHM");
		String CRYPTO_PROVIDER = settings.getProperty("CRYPTO_PROVIDER");
		int ALPHA_SIZE = 32;
		int BETA_SIZE;
		int GAMMA_SIZE = 16;
		int DELTA_SIZE = MAX_PAYLOAD_SIZE + 1 + 16; // 1 = overhead for padding info in delta (delta=payload); 16 overhead for mac in delta (delta=payload)
		boolean DEBUG_ON = settings.getPropertyAsBoolean("DEBUG_OUTPUT");
		boolean PERFORM_REPLY_DETECTION = settings.getPropertyAsBoolean("PERFORM_REPLAY_DETECTION");
		int NUMBER_OF_THREADS;
		
		ReplayDetectionBasic_v1 replayDetection;
		HashMap<String, Sphinx_v1_ReplyData> replyDataTable;
		
		byte[] publicKey;
		byte[] privateKey;
		byte[] id;
		
		byte[][] mixIdsSphinx;
		byte[][] publicKeysOfMixes;
		
		MixList mixList;
		
		
		public Config() {
			if (mix != null) { // mix
				NUMBER_OF_MIXES = mix.getNumberOfMixes();
				ROUTE_LENGTH = mix.getNumberOfMixes(); // number of mixes to chose by clients
				BETA_SIZE = 16 + (ROUTE_LENGTH * 32);
				byte[][] keyPair = generateKeyPair(this);
				publicKey = keyPair[0];
				privateKey = keyPair[1];
				id = generateMixId(this);
				infoService.postValueAsMix(mix.getIdentifier(), "SPHINX_MIX_ID", id);
				infoService.postValueAsMix(mix.getIdentifier(), "SPHINX_PUBLIC_KEY", publicKey);
				if (PERFORM_REPLY_DETECTION)
					this.replayDetection = ReplayDetectionBasic_v1.getInstance(mix);
				this.NUMBER_OF_THREADS = settings.getPropertyAsInt("NUMBER_OF_THREADS");
				// -1 means "automatic detection"
				this.NUMBER_OF_THREADS = (this.NUMBER_OF_THREADS == -1) ?  Runtime.getRuntime().availableProcessors(): this.NUMBER_OF_THREADS;
			} else { // client
				NUMBER_OF_MIXES = infoService.getNumberOfMixes();
				ROUTE_LENGTH = NUMBER_OF_MIXES; // TODO
				BETA_SIZE = 16 + (ROUTE_LENGTH * 32);
				id = generateClientId(this);
				//infoService = clientController.getInfoServiceClient();
				try {
					infoService.postValue(new String(id, "UTF-8"), Util.intToByteArray(clientController.getIdentifier()));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					throw new RuntimeException("could not post id"); 
				}
				mixList = infoService.getMixList();
				config = this;
				config.mixIdsSphinx = infoService.getValueFromAllMixes("SPHINX_MIX_ID");
				config.publicKeysOfMixes = infoService.getValueFromAllMixes("SPHINX_PUBLIC_KEY");
				replyDataTable = new HashMap<String, Sphinx_v1_ReplyData>();
			}
		}
		
		
		// in byte
		public int getDeltaOverhead() {
			return DELTA_SIZE - MAX_PAYLOAD_SIZE;
		}

		// in byte
		public int getTotalOverhead() {
			return getDeltaOverhead() + getTotalHeaderSize();
		}
		
		// in byte
		public int getTotalHeaderSize() {
			return ALPHA_SIZE + BETA_SIZE + GAMMA_SIZE;
		}
		
		// in byte
		public int getTotalMessageSize() {
			return getTotalHeaderSize() + DELTA_SIZE;
		}


		public byte[] getGlobalMixIdFor(byte[] nextMixId) {
			for(int i=0; i<mixIdsSphinx.length; i++)
				if (Arrays.equals(nextMixId, mixIdsSphinx[i]))
					return Util.intToByteArray(mixList.mixIDs[i]);
			return null;
		}

	} // end "Config"

	
	class RequestThread extends Thread {
		
		Sphinx_v1_Recoder recodingScheme;
		
		public RequestThread(Sphinx_v1_Recoder recodingScheme) {
			this.recodingScheme = recodingScheme;
		}

		
		@Override
		public void run() {
			while (true) { // process messages
				Request request = inputOutputHandler.getRequest();
				request = recodingScheme.recodeMessage(request);
				if (request != null)
					outputStrategy.addRequest(request);
			}
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
	public static byte[] padBody(byte[] body, Config config) {
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

	
	public static byte[] unpadBody(byte[] body, Config config) {
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
	public static byte[][] generateKeyPair(Config config) {
		byte[] publicKey = new byte[config.ALPHA_SIZE];
		byte[] privateKey = new byte[config.ALPHA_SIZE];
		try {
			SecureRandom secureRandom = SecureRandom.getInstance(config.PRNG_ALGORITHM);
			secureRandom.nextBytes(privateKey);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("could not generate keyPair"); 
		}
		Curve25519.keygen(publicKey, null, privateKey);
		return new byte[][] {publicKey, privateKey};
	}
	
	
	public static byte[] generateMixId(Config config) {
		byte[] pseudonym = new byte[config.SECURITY_PARAMETER_SIZE];
		SecureRandom secureRandom;
		try {
			secureRandom = SecureRandom.getInstance(config.PRNG_ALGORITHM);
			secureRandom.nextBytes(pseudonym);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException("could not generate mix id"); 
		}
		pseudonym[0] = Sphinx_v1_MixHeader.MIX_PREFIX;
		return pseudonym;
	}
	
	
	public static byte[] generateClientId(Config config) {
		byte[] id = new byte[config.SECURITY_PARAMETER_SIZE / 2];
		SecureRandom secureRandom;
		try {
			secureRandom = SecureRandom.getInstance(config.PRNG_ALGORITHM);
			secureRandom.nextBytes(id);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException("could not generate mix id"); 
		}
		id[0] = Sphinx_v1_MixHeader.CLIENT_PREFIX;
		return id;
	}
	
	
	public static byte[] generateReplyAddress(byte[] clientID, byte[] replyID, Config config) {
		assert clientID.length == config.SECURITY_PARAMETER_SIZE / 2;
		assert replyID.length == config.SECURITY_PARAMETER_SIZE / 2;
		assert clientID[0] == Sphinx_v1_MixHeader.CLIENT_PREFIX;;
		return Util.concatArrays(clientID, replyID);
	}
	
	
	// id used to identify the keys needed to derypt a reply
	public static byte[] generateReplyID(Config config) {
		byte[] id = new byte[config.SECURITY_PARAMETER_SIZE / 2];
		secureRandom.nextBytes(id);
		return id;
	}
	
	
	public static byte[] extractClientId(byte[] replyAddress, Config config) {
		return Arrays.copyOfRange(replyAddress, 0, config.SECURITY_PARAMETER_SIZE / 2);
	}
	
	
	public static byte[] extractReplyId(byte[] replyAddress, Config config) {
		return Arrays.copyOfRange(replyAddress, config.SECURITY_PARAMETER_SIZE / 2, replyAddress.length);
	}


	public Config getConfig() {
		return config;
	}
	
}
