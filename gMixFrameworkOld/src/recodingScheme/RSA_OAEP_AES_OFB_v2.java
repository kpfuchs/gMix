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

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import client.ClientController;

import recodingScheme.staticFunctions.ReplayDetectionBasic_v1;

import message.Reply;
import message.Request;

import userDatabase.User;
import framework.Implementation;
import framework.LocalClassLoader;
import framework.Settings;


public class RSA_OAEP_AES_OFB_v2 extends Implementation implements RecodingScheme, DummyGenerator {

	private Config config;
	private RequestThread[] requestThreads;
	private ReplyThread[] replyThreads;
	private RSA_OAEP_AES_OFB_v2_MessageCreator messageCreator;
	public static boolean DUPLEX_ON;
	
	
	public RSA_OAEP_AES_OFB_v2(){
		
	}
	
	
	// used by Sphinx_v1_MessageCreator
	public RSA_OAEP_AES_OFB_v2(Settings settings, InfoServiceClient_v1 infoService, ClientController client){
		this.settings = settings;
		this.infoService = infoService;
		this.clientController = client;
	}
	
	
	@Override
	public void constructor() {
		this.config = new Config();
		String packageName = this.getClass().getPackage().toString().replace("package ", "");
		requestThreads = new RequestThread[config.NUMBER_OF_THREADS];
		if (mix.isDuplex()) {
			DUPLEX_ON = true;
			replyThreads = new ReplyThread[config.NUMBER_OF_THREADS];
		}
		for (int i=0; i<config.NUMBER_OF_THREADS; i++) {
			RSA_OAEP_AES_OFB_v2_Recoder recodingScheme = (RSA_OAEP_AES_OFB_v2_Recoder)LocalClassLoader.instantiateSubImplementation(packageName, "RSA_OAEP_AES_OFB_v2_Recoder.java", this);
			requestThreads[i] = new RequestThread(recodingScheme);
			if (mix.isDuplex()) {
				replyThreads[i] = new ReplyThread(recodingScheme);
			}
		}
	}

	
	@Override
	public void initialize() {
		String packageName = this.getClass().getPackage().toString().replace("package ", "");
		if (!mix.isLastMix()) {
			Key[] publicKeys = getPublicKeysOfAllMixes();
			config.publicKeysOfMixes = Arrays.copyOfRange(publicKeys, (mix.getIdentifier() +1), publicKeys.length);
			config.numberOfMixes = config.publicKeysOfMixes.length;
			this.messageCreator = (RSA_OAEP_AES_OFB_v2_MessageCreator) LocalClassLoader.instantiateSubImplementation(packageName, "RSA_OAEP_AES_OFB_v2_MessageCreator.java", this); 

		}	
	}

	
	@Override
	public void begin() {
		for (int i=0; i<requestThreads.length; i++) {
			requestThreads[i].start();
			if (mix.isDuplex())
				replyThreads[i].start();
		}
	}

	
	@Override
	public Request generateDummy(User user) {
		if (!mix.isLastMix())
			return messageCreator.createMessage(null, user);
		else
			return null;
	}

	
	@Override
	public Request generateDummy() {
		return messageCreator.createMessage(null);
	}

	
	@Override
	public Reply generateDummyReply(User user) {
		// TODO Auto-generated method stub
		return null;
	}
	

	@Override
	public Reply generateDummyReply() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean supportsDummyTraffic() {
		return true;
	}

	@Override
	public int getMaxPayloadForNextReply(User user) {
		// TODO Auto-generated method stub
		return 0;
	}

	
	public Config getConfig() {
		return this.config;
	}

	
	public KeyPair generateKeyPair(String keyGen, int keyLength) {
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(keyGen);
			keyPairGenerator.initialize(keyLength);
			return keyPairGenerator.generateKeyPair();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	class Config {
		String CRYPTO_PROVIDER = settings.getProperty("CRYPTO_PROVIDER");
		String ASYM_CRYPTOGRAPHY_ALGORITHM = settings.getProperty("ASYM_CRYPTOGRAPHY_ALGORITHM");
		String SYM_CRYPTOGRAPHY_ALGORITHM = settings.getProperty("SYM_CRYPTOGRAPHY_ALGORITHM");
		String MAC_ALGORITHM = settings.getProperty("MAC_ALGORITHM");
		String NAME_OF_SYM_KEY_GENERATOR = settings.getProperty("NAME_OF_SYM_KEY_GENERATOR");
		String NAME_OF_ASYM_KEY_GENERATOR = settings.getProperty("NAME_OF_ASYM_KEY_GENERATOR");
		int ASYM_KEY_LENGTH = settings.getPropertyAsInt("ASYM_KEY_LENGTH"); // in byte
		int SYM_KEY_LENGTH = settings.getPropertyAsInt("SYM_KEY_LENGTH"); // in byte
		int IV_LENGTH = settings.getPropertyAsInt("IV_LENGTH"); // in byte
		int MAC_KEY_LENGTH = settings.getPropertyAsInt("MAC_KEY_LENGTH"); // in byte
		int MAC_LENGTH = settings.getPropertyAsInt("MAC_LENGTH"); // in byte
		int LENGTH_HEADER_LENGTH = settings.getPropertyAsInt("LENGTH_HEADER_LENGTH"); // in byte
		int MAX_PAYLOAD = settings.getPropertyAsInt("MAX_PAYLOAD"); // in byte
		boolean DEBUG_ON = settings.getPropertyAsBoolean("DEBUG_OUTPUT");
		boolean PERFORM_REPLY_DETECTION = settings.getPropertyAsBoolean("PERFORM_REPLAY_DETECTION");
		
		// settings for reply block
		String PRNG_ALGORITHM = settings.getProperty("PRNG_ALGORITHM"); // used to derive keys for reply packages
		int PRNG_SEED_LENGTH = settings.getPropertyAsInt("PRNG_SEED_LENGTH"); // in byte
		int PSEUDONYM_LENGTH = settings.getPropertyAsInt("PSEUDONYM_LENGTH"); // in byte
		int ADDRESS_LENGTH = settings.getPropertyAsInt("ADDRESS_LENGTH"); // in byte (address of the client)
		
		// additional data
		int NUMBER_OF_THREADS;
		Key[] publicKeysOfMixes; // will be set later (see "initialize()")
		int numberOfMixes;
		KeyPair keyPair;
		ReplayDetectionBasic_v1 replayDetection;
		ConcurrentHashMap<String, String> replySeeds;
		
		public Config() {
			if (mix != null) { // mix
				this.keyPair = generateKeyPair(NAME_OF_ASYM_KEY_GENERATOR, ASYM_KEY_LENGTH * 8);
				infoService.postValueAsMix(mix.getIdentifier(), "RSA_PUBLIC_KEY", keyPair.getPublic().getEncoded());
				if (PERFORM_REPLY_DETECTION)
					this.replayDetection = ReplayDetectionBasic_v1.getInstance(mix);
				this.NUMBER_OF_THREADS = settings.getPropertyAsInt("NUMBER_OF_THREADS");
				// -1 means "automatic detection"
				this.NUMBER_OF_THREADS = (this.NUMBER_OF_THREADS == -1) ?  Runtime.getRuntime().availableProcessors(): this.NUMBER_OF_THREADS;
			} else { // client
				DUPLEX_ON = infoService.getIsDuplexModeOn();
				if (DUPLEX_ON)
					replySeeds = new ConcurrentHashMap<String, String>();
			}
		}
	} // end "Config"
	
	
	class RequestThread extends Thread {
		
		RSA_OAEP_AES_OFB_v2_Recoder recodingScheme;
		
		public RequestThread(RSA_OAEP_AES_OFB_v2_Recoder recodingScheme) {
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
	
	
	class ReplyThread extends Thread {
		
		RSA_OAEP_AES_OFB_v2_Recoder recodingScheme;
		
		public ReplyThread(RSA_OAEP_AES_OFB_v2_Recoder recodingScheme) {
			this.recodingScheme = recodingScheme;
		}
		
		
		@Override
		public void run() {
			while (true) { // process messages
				Reply reply = inputOutputHandler.getReply();
				reply = recodingScheme.recodeReply(reply);
				if (reply != null)
					outputStrategy.addReply(reply);
			}
		}
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
