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

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import staticFunctions.layer2recodingScheme.basicReplayDetection_v0_001.ReplayDetectionBasic;

import framework.core.AnonNode;
import framework.core.config.Settings;
import framework.core.routing.RoutingMode;
import framework.core.userDatabase.User;
import framework.infoService.InfoServiceClient;


public class RSA_OAEP_AES_OFB_Config {

public String CRYPTO_PROVIDER;
public String ASYM_CRYPTOGRAPHY_ALGORITHM;
public String SYM_CRYPTOGRAPHY_ALGORITHM;
public String MAC_ALGORITHM;
public String NAME_OF_SYM_KEY_GENERATOR;
public String NAME_OF_ASYM_KEY_GENERATOR;
public int ASYM_KEY_LENGTH;
public int SYM_KEY_LENGTH;
public int IV_LENGTH;
public int MAC_KEY_LENGTH;
public int MAC_LENGTH;
public int LENGTH_HEADER_LENGTH;
public int MAX_PAYLOAD;
public boolean DEBUG_ON;
public boolean PERFORM_REPLY_DETECTION;


// settings for reply block
public String PRNG_ALGORITHM; // used to derive keys for reply packages
public int PRNG_SEED_LENGTH; // in byte
public int PSEUDONYM_LENGTH; // in byte
public int ADDRESS_LENGTH; // in byte (address of the client)

// additional data
public int NUMBER_OF_THREADS;
public Key[] publicKeysOfMixes;
public int numberOfMixes;
public int routeLength;
public KeyPair keyPair;
public ReplayDetectionBasic replayDetection;
public ConcurrentHashMap<String, byte[]> replySeeds;
public ConcurrentHashMap<User, ArrayList<byte[]>> replyblocks;

private InfoServiceClient infoService;
private Settings settings;


public RSA_OAEP_AES_OFB_Config(AnonNode owner, boolean isClientConfigObject) {
this.settings = owner.getSettings();
this.infoService = owner.getInfoService();

this.MAX_PAYLOAD = owner.MAX_PAYLOAD;
this.DEBUG_ON = owner.RS_DEBUG_OUTPUT_ON;
this.PERFORM_REPLY_DETECTION = owner.REPLY_DETECTION_ON;
this.CRYPTO_PROVIDER = owner.CRYPTO_PROVIDER;

this.ASYM_CRYPTOGRAPHY_ALGORITHM = settings.getProperty("ASYM_CRYPTOGRAPHY_ALGORITHM");
this.SYM_CRYPTOGRAPHY_ALGORITHM = settings.getProperty("SYM_CRYPTOGRAPHY_ALGORITHM");
this.MAC_ALGORITHM = settings.getProperty("MAC_ALGORITHM");
this.NAME_OF_SYM_KEY_GENERATOR = settings.getProperty("NAME_OF_SYM_KEY_GENERATOR");
this.NAME_OF_ASYM_KEY_GENERATOR = settings.getProperty("NAME_OF_ASYM_KEY_GENERATOR");
this.ASYM_KEY_LENGTH = settings.getPropertyAsInt("ASYM_KEY_LENGTH"); // in byte
this.SYM_KEY_LENGTH = settings.getPropertyAsInt("SYM_KEY_LENGTH"); // in byte
this.IV_LENGTH = settings.getPropertyAsInt("IV_LENGTH"); // in byte
this.MAC_KEY_LENGTH = settings.getPropertyAsInt("MAC_KEY_LENGTH"); // in byte
this.MAC_LENGTH = settings.getPropertyAsInt("MAC_LENGTH"); // in byte
this.LENGTH_HEADER_LENGTH = settings.getPropertyAsInt("LENGTH_HEADER_LENGTH"); // in byte

// settings for reply block
this.PRNG_ALGORITHM = settings.getProperty("PRNG_ALGORITHM"); // used to derive keys for reply packages
this.PRNG_SEED_LENGTH = settings.getPropertyAsInt("PRNG_SEED_LENGTH"); // in byte
this.PSEUDONYM_LENGTH = settings.getPropertyAsInt("PSEUDONYM_LENGTH"); // in byte
this.ADDRESS_LENGTH = settings.getPropertyAsInt("ADDRESS_LENGTH"); // in byte (address of the client)

if (isClientConfigObject) {
this.publicKeysOfMixes = getPublicKeysOfAllMixes();
this.numberOfMixes = publicKeysOfMixes.length;
if (owner.ROUTING_MODE == RoutingMode.CASCADE)
	this.routeLength = numberOfMixes;
else
	this.routeLength = owner.FREE_ROUTE_LENGTH;
if (owner.IS_DUPLEX)
replySeeds = new ConcurrentHashMap<String, byte[]>();
} else { // mix
// publish public key:
this.keyPair = generateKeyPair(NAME_OF_ASYM_KEY_GENERATOR, ASYM_KEY_LENGTH * 8);
infoService.postValueAsMix(owner.PUBLIC_PSEUDONYM, "RSA_PUBLIC_KEY", keyPair.getPublic().getEncoded());
if (PERFORM_REPLY_DETECTION)
this.replayDetection = ReplayDetectionBasic.getInstance(owner);
this.NUMBER_OF_THREADS = owner.NUMBER_OF_THREADS;
replyblocks = new ConcurrentHashMap<User, ArrayList<byte[]>>();
}
}


// used in casade setups by mixes that generate dummies
public void loadPlubicKeysOfOtherMixes() {
this.publicKeysOfMixes = getPublicKeysOfAllMixes();
}


private KeyPair generateKeyPair(String keyGen, int keyLength) {
try {
KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(keyGen);
keyPairGenerator.initialize(keyLength);
return keyPairGenerator.generateKeyPair();
} catch (Exception e) {
e.printStackTrace();
}
return null;
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


