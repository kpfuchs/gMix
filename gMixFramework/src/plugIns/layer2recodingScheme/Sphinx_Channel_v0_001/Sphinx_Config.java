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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;

import staticFunctions.layer2recodingScheme.basicReplayDetection_v0_001.ReplayDetectionBasic;

import framework.core.AnonNode;
import framework.core.config.Settings;
import framework.core.routing.MixList;
import framework.core.routing.RoutingMode;
import framework.core.util.Util;
import framework.infoService.InfoServiceClient;


public class Sphinx_Config {
	
	public int MAX_PAYLOAD;
	public boolean DEBUG_ON;
	public boolean PERFORM_REPLY_DETECTION;
	public String CRYPTO_PROVIDER;
	
	public int SECURITY_PARAMETER_SIZE; // k must be 16 bytes to work with ECC (curve25519)
	public int NUMBER_OF_MIXES;
	public int ROUTE_LENGTH; // number of mixes to chose by clients
	public int ALPHA_SIZE;
	public int BETA_SIZE;
	public int GAMMA_SIZE;
	public int DELTA_SIZE; // 1 = overhead for padding info in delta (delta=payload); 16 overhead for mac in delta (delta=payload)
	
	public String PRNG_ALGORITHM;
	public int NUMBER_OF_THREADS;
	
	public ReplayDetectionBasic replayDetection;
	public HashMap<String, ReplyData> replyDataTable;
	
	public byte[] publicKey;
	public byte[] privateKey;
	public byte[] id;
	
	public byte[][] mixIdsSphinx;
	public byte[][] publicKeysOfMixes;
	
	public MixList mixList;

	private InfoServiceClient infoService;
	private Settings settings;

	
	public Sphinx_Config(AnonNode owner, boolean isClientConfigObject) {
		this.settings = owner.getSettings();
		this.infoService = owner.getInfoService();
		
		this.MAX_PAYLOAD = owner.MAX_PAYLOAD;
		this.DEBUG_ON = owner.RS_DEBUG_OUTPUT_ON;
		this.PERFORM_REPLY_DETECTION = owner.REPLY_DETECTION_ON;
		this.CRYPTO_PROVIDER = owner.CRYPTO_PROVIDER;
		
		this.SECURITY_PARAMETER_SIZE = 16;  // k must be 16 bytes to work with ECC (curve25519)
		this.NUMBER_OF_MIXES = owner.NUMBER_OF_MIXES;
		if (owner.ROUTING_MODE == RoutingMode.CASCADE)
			this.ROUTE_LENGTH = this.NUMBER_OF_MIXES;
		else
			this.ROUTE_LENGTH = owner.FREE_ROUTE_LENGTH;
		this.ALPHA_SIZE = 32;
		this.BETA_SIZE = 16 + (ROUTE_LENGTH * 32);
		this.GAMMA_SIZE = 16;
		this.DELTA_SIZE = MAX_PAYLOAD + 1 + 16; // 1 = overhead for padding info in delta (delta=payload); 16 overhead for mac in delta (delta=payload)
		
		this.PRNG_ALGORITHM = settings.getProperty("PRNG_ALGORITHM");
		
		if (isClientConfigObject) {
			this.id = Sphinx.generateClientId(this);
			try {
				infoService.postValue(new String(id, "UTF-8"), Util.intToByteArray(owner.PUBLIC_PSEUDONYM));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				throw new RuntimeException("could not post id"); 
			}
			loadPublicKeysOfMixes(owner);
		} else { // mix
			byte[][] keyPair = Sphinx.generateKeyPair(this);
			publicKey = keyPair[0];
			privateKey = keyPair[1];
			id = Sphinx.generateMixId(this);
			infoService.postValueAsMix(owner.PUBLIC_PSEUDONYM, "SPHINX_MIX_ID", id);
			infoService.postValueAsMix(owner.PUBLIC_PSEUDONYM, "SPHINX_PUBLIC_KEY", publicKey);
			if (PERFORM_REPLY_DETECTION)
				this.replayDetection = ReplayDetectionBasic.getInstance(owner);
			this.NUMBER_OF_THREADS = owner.NUMBER_OF_THREADS;
		}
	}


	public void loadPublicKeysOfMixes(AnonNode owner) {
		this.mixList = owner.mixList;
		this.mixIdsSphinx = infoService.getValueFromAllMixes("SPHINX_MIX_ID");
		this.publicKeysOfMixes = infoService.getValueFromAllMixes("SPHINX_PUBLIC_KEY");
		this.replyDataTable = new HashMap<String, ReplyData>();
	}

	
	// in byte
	public int getDeltaOverhead() {
		return DELTA_SIZE - MAX_PAYLOAD;
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


	public int getGlobalMixIdFor(byte[] nextMixId) {
		for(int i=0; i<mixIdsSphinx.length; i++)
		{
			if (Arrays.equals(nextMixId, mixIdsSphinx[i]))
				return mixList.mixIDs[i];
		}
		System.err.println("unknown id"); 
		return -1;
	}
	
}
