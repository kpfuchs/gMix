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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.concurrent.LinkedBlockingQueue;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import recodingScheme.staticFunctions.ReplayDetectionBasic_v1;

import message.MixMessage;
import message.Reply;
import message.Request;

import userDatabase.User;
import userDatabase.UserAttachment;
import framework.Implementation;
import framework.LocalClassLoader;
import framework.Settings;

public class RSA_AES_Channel_v1 extends Implementation implements RecodingScheme {

	private Config config;
	private WorkerThread[] workerThreads;
	private RequestDistributorThread requestDistributorThread;
	private ReplyDistributorThread replyDistributorThread;
	
	public RSA_AES_Channel_v1(){

	}
	
	
	// used by RSA_AES_Channel_v1_MessageCreator
	public RSA_AES_Channel_v1(Settings settings) {
		this.settings = settings;
	}
	
	
	@Override
	public void constructor() {
		this.config = new Config();
		String packageName = this.getClass().getPackage().toString().replace("package ", "");
		this.requestDistributorThread = new RequestDistributorThread();
		this.replyDistributorThread = new ReplyDistributorThread();
		this.workerThreads = new WorkerThread[config.NUMBER_OF_THREADS];
		for (int i=0; i<config.NUMBER_OF_THREADS; i++) {
			RSA_AES_Channel_v1_Recoder recodingScheme = (RSA_AES_Channel_v1_Recoder)LocalClassLoader.instantiateSubImplementation(packageName, "RSA_AES_Channel_v1_Recoder.java", this);
			workerThreads[i] = new WorkerThread(recodingScheme);;

		}
	}

	
	@Override
	public void initialize() {

	}

	
	@Override
	public void begin() {
		for (int i=0; i<workerThreads.length; i++)
			this.workerThreads[i].start();
		this.requestDistributorThread.start();
		this.replyDistributorThread.start();
	}


	@Override
	public boolean supportsDummyTraffic() {
		return false;
	}

	
	@Override
	public int getMaxPayloadForNextReply(User user) {
		// TODO Auto-generated method stub
		return config.MAX_PAYLOAD;
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
				
			}
		}
	} // end "Config"
	
	
	class ChannelData extends UserAttachment {
		SecretKey macKey;
		Cipher decryptCipher;
		Cipher encryptCipher;
		int threadId;
		
		public ChannelData(User owner, int threadId) {
			super(owner, getThis());
			this.threadId = threadId;
		}
	}
	
	
	class RequestDistributorThread extends Thread {
		
		private int next = -1;
		
		@Override
		public void run() {
			while (true) { // process messages
				Request request = inputOutputHandler.getRequest();
				if (request.getOwner().getAttachment(getThis(), ChannelData.class) == null) {
					int threadId = getNextThreadId();
					new ChannelData(request.getOwner(), threadId);
					try {
						workerThreads[threadId].queue.put(request);
					} catch (InterruptedException e) {
						e.printStackTrace();
						continue;
					}
				} else {
					ChannelData cd = request.getOwner().getAttachment(getThis(), ChannelData.class);
					try {
						workerThreads[cd.threadId].queue.put(request);
					} catch (InterruptedException e) {
						e.printStackTrace();
						continue;
					}
				}
			}	
		}
		
		private int getNextThreadId() {
			if (++next == workerThreads.length)
				next = 0;
			return next;
		}
	}
	
	
	class ReplyDistributorThread extends Thread {
		
		@Override
		public void run() {
			while (true) { // process messages
				Reply reply = inputOutputHandler.getReply();
				ChannelData cd = reply.getOwner().getAttachment(getThis(), ChannelData.class);
				workerThreads[cd.threadId].queue.add(reply);
			}	
		}
		
	}
	
	
	class WorkerThread extends Thread {
		
		LinkedBlockingQueue<MixMessage> queue;
		RSA_AES_Channel_v1_Recoder recodingScheme;
		
		public WorkerThread(RSA_AES_Channel_v1_Recoder recodingScheme) {
			this.recodingScheme = recodingScheme;
			this.queue = new LinkedBlockingQueue<MixMessage>(10);
		}

		
		@Override
		public void run() {
			while (true) { // process messages
				try {
					MixMessage message = queue.take();
					if (message instanceof Request) {
						Request recodedMsg = recodingScheme.recodeMessage((Request)message);
						if (recodedMsg != null)
							outputStrategy.addRequest(recodedMsg);
					} else {
						Reply recodedMsg = recodingScheme.recodeReply((Reply)message);
						if (recodedMsg != null)
							outputStrategy.addReply(recodedMsg);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				}
			}	
		}
		
	}
	
	
	private RSA_AES_Channel_v1 getThis() {
		return this;
	}
	
}
