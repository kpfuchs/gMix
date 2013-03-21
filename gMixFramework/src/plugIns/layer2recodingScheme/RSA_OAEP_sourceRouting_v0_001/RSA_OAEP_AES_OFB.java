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
package plugIns.layer2recodingScheme.RSA_OAEP_sourceRouting_v0_001;

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
		assert owner.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING;
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


	
	
	public synchronized Request applyLayeredEncryption(Request request)
	{
		try{
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
	
	SecretKey[] symKeys = new SecretKey[config.routeLength];
	IvParameterSpec[] initVectors = new IvParameterSpec[config.routeLength];
	
	byte[][] randomNumbers = new byte[config.routeLength-1][config.ASYM_KEY_LENGTH];
	for(int i=0; i<config.routeLength ;i++)
	{
		symKeys[i] =  symKeyGenerator.generateKey();
		byte[] iv = new byte[config.SYM_KEY_LENGTH];
		secureRandom.nextBytes(iv);
		initVectors[i] = new IvParameterSpec(iv);
		
	}
	
	
		Cipher symCipher = Cipher.getInstance(
				config.SYM_CRYPTOGRAPHY_ALGORITHM,
				config.CRYPTO_PROVIDER
				);
	
	for(int i=0; i<config.routeLength-1 ;i++)
	{
		SecureRandom prng = SecureRandom.getInstance(config.PRNG_ALGORITHM);
		prng.setSeed(symKeys[i].getEncoded());
		prng.nextBytes(randomNumbers[i]);
		for(int k=i+1; k<config.routeLength-1;k++)
		{
			//precompute the decrypt-operation at the mixes (necessary for the MAC)
			symCipher.init(Cipher.DECRYPT_MODE, symKeys[k], initVectors[k]);
			randomNumbers[i] = symCipher.doFinal(randomNumbers[i]);
		}
		
	
	}
	
	
	// add header and encryption layer for each mix
	
	byte[] header =null ;
	byte[] payload = request.getByteMessage() ;
	for (int i=config.routeLength-1; i>=0; i--) {
			// generate header (without mac; must be added later)
			byte[] mac;
			SecretKey macKey;
			macKey = macKeyGenerator.generateKey();
			// concat header and payload
			 byte[] firstBlock = Util.concatArrays(new byte[][] {
					macKey.getEncoded(),
					symKeys[i].getEncoded(),
					initVectors[i].getIV(),
			});
			if (owner.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) { 
				// add route-info
				if (i == config.routeLength-1) { // last mix
					firstBlock = Util.concatArrays(Util.intToByteArray(request.route[i]), firstBlock);
				} else {
					firstBlock = Util.concatArrays(Util.intToByteArray(request.route[i+1]), firstBlock);
				}
				
			}
			// add padding to assure that the message is long enough for the cipher to work
			byte[] padding = new byte[asymmetricCipher.getBlockSize()-(firstBlock.length + config.MAC_LENGTH)];
			secureRandom.nextBytes(padding);
			firstBlock = Util.concatArrays(firstBlock, padding); 
			if(i==config.routeLength-1)
			{
				header = firstBlock;
				//add Pseudorandom-numbers
				for(int k=0; k<randomNumbers.length;k++)
					header = Util.concatArrays(header,randomNumbers[k]);
			
			}
			else
			{
				//cut the last block (random number)
				header = Arrays.copyOfRange(header, 0, header.length-config.ASYM_KEY_LENGTH);
				
				header = Util.concatArrays(firstBlock,header);
				
						
			}
			byte[] plaintext =  Util.concatArrays(header, payload);
			// add mac to header
			Mac macGenerator = Mac.getInstance(config.MAC_ALGORITHM);
			macGenerator.init(macKey);
			mac = macGenerator.doFinal(plaintext);
			firstBlock = Util.concatArrays(mac, firstBlock);
			assert macKey.getEncoded().length == config.MAC_KEY_LENGTH;
			assert symKeys[i].getEncoded().length == config.SYM_KEY_LENGTH;
			assert initVectors[i].getIV().length == config.IV_LENGTH;
			assert mac.length == config.MAC_LENGTH;
			// encrypt message; asymmetric part
			if (owner.ROUTING_MODE == RoutingMode.CASCADE)
				this.asymmetricCipher.init(Cipher.ENCRYPT_MODE, config.publicKeysOfMixes[i]);
			else {
				this.asymmetricCipher.init(Cipher.ENCRYPT_MODE, config.publicKeysOfMixes[request.route[i]]); // TODO: possible side-effect -> won't work anymore if mix-ids are chosen differently
			//	System.out.println("" +owner +" using public key " +Util.md5(config.publicKeysOfMixes[request.route[i]].getEncoded()) +"for mix " +request.route[i]);
			}
		
			int pointer = firstBlock.length-config.MAC_LENGTH;
			firstBlock= asymmetricCipher.doFinal(firstBlock, 0, firstBlock.length);
			//replace firstBlock plaintext with firstBlock cipherText
			header = Util.concatArrays(firstBlock, Arrays.copyOfRange(header, pointer, header.length));
		
		
			
			// encrypt message; symmetric part
			symCipher.init(Cipher.ENCRYPT_MODE, symKeys[i], initVectors[i]);
			payload = symCipher.doFinal(payload, 0, payload.length);
			
			if(i!=config.routeLength-1) 
			{
				
				pointer = firstBlock.length;
				
				byte[] headerCiphertext = firstBlock;
				for(int k=0; k <config.routeLength-1; k++)
				{
				 byte[] SymBlock= symCipher.doFinal(header, pointer, config.ASYM_KEY_LENGTH);
				headerCiphertext =  Util.concatArrays(headerCiphertext, SymBlock);
				pointer+=config.ASYM_KEY_LENGTH;
				
				}
				header = headerCiphertext;
				
			}

			

	}
	
	request.setByteMessage(Util.concatArrays(header,payload));
	return request;
		}
		 catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		
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
			// TODO Auto-generated catch block
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

	try{
		
	
	byte[] seed;
	byte[] pseudonym = null;
	SecureRandom prng = null;
	
	// use (seeded) prng to create keys
	
		//prng for the sym keys
		prng = SecureRandom.getInstance(config.PRNG_ALGORITHM);
		seed = new byte[config.PRNG_SEED_LENGTH];
		secureRandom.nextBytes(seed);
		prng.setSeed(seed);
		assert prng != null;
		pseudonym = new byte[config.PSEUDONYM_LENGTH]; // used to identify the correct seed when a reply is received
		secureRandom.nextBytes(pseudonym); 
		config.replySeeds.put(new String(pseudonym, "UTF-8"), seed);
	SecretKey[] symKeys = new SecretKey[config.routeLength];
	IvParameterSpec[] initVectors = new IvParameterSpec[config.routeLength];
	
	byte[][] randomNumbers = new byte[config.routeLength-1][config.ASYM_KEY_LENGTH];

	//generate sym. keys + IV's
	for(int i=0; i<config.routeLength ;i++)
	{
		byte[] symKeyAsByteArray =  new byte[config.SYM_KEY_LENGTH];
		prng.nextBytes(symKeyAsByteArray);
		symKeys[i] = new SecretKeySpec(symKeyAsByteArray, config.SYM_CRYPTOGRAPHY_ALGORITHM);
		byte[] iv = new byte[config.SYM_KEY_LENGTH];
		prng.nextBytes(iv);
		initVectors[i] = new IvParameterSpec(iv);
		
	}
	
	Cipher symCipher = Cipher.getInstance(
			config.SYM_CRYPTOGRAPHY_ALGORITHM,
			config.CRYPTO_PROVIDER
			);

	//generate the pseudo random numbers
for(int i=0; i<config.routeLength-1 ;i++)
{
	SecureRandom prng2 = SecureRandom.getInstance(config.PRNG_ALGORITHM);
	prng2.setSeed(symKeys[i].getEncoded());
	prng2.nextBytes(randomNumbers[i]);
	for(int k=i+1; k<config.routeLength-1;k++)
	{
		//precompute the decrypt-operation at the mixes (necessary for the MAC)
		symCipher.init(Cipher.DECRYPT_MODE, symKeys[k], initVectors[k]);
		randomNumbers[i] = symCipher.doFinal(randomNumbers[i]);
	}
	
}
	
		
byte[] header =null ;

for (int i=0; i<=config.routeLength-1; i++){
		// generate header (without mac; must be added later)
		byte[] mac;
		SecretKey macKey;
		macKey = macKeyGenerator.generateKey();
		// concat header and payload
		 byte[] firstBlock = Util.concatArrays(new byte[][] {
				macKey.getEncoded(),
				symKeys[(config.routeLength-1)-i].getEncoded(),
				initVectors[(config.routeLength-1)-i].getIV(),
		});
		 if(i==0)
			 firstBlock = Util.concatArrays(firstBlock, pseudonym);

		if (owner.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) { 
			// add route-info
			if (i == 0) { // last mix
				firstBlock = Util.concatArrays(Util.intToByteArray(request.route[i]), firstBlock);
			} else {
				firstBlock = Util.concatArrays(Util.intToByteArray(request.route[i-1]), firstBlock);
			}
			
		}
		// add padding to assure that the message is long enough for the cipher to work
		byte[] padding = new byte[asymmetricCipher.getBlockSize()-(firstBlock.length + config.MAC_LENGTH)];
		secureRandom.nextBytes(padding);
		firstBlock = Util.concatArrays(firstBlock, padding); 
		if(i==0)
		{
			header = firstBlock;
			//add Pseudorandom-numbers
			for(int k=0; k<randomNumbers.length;k++)
				header = Util.concatArrays(header,randomNumbers[k]);
		
		}
		else
		{
			//cut the last block (random number)
			header = Arrays.copyOfRange(header, 0, header.length-config.ASYM_KEY_LENGTH);
			
			header = Util.concatArrays(firstBlock,header);
			
					
		}
		// add mac to header
		Mac macGenerator = Mac.getInstance(config.MAC_ALGORITHM);
		macGenerator.init(macKey);
		
		mac = macGenerator.doFinal(header);
		firstBlock = Util.concatArrays(mac, firstBlock);

		// encrypt message; asymmetric part
		if (owner.ROUTING_MODE == RoutingMode.CASCADE)
			this.asymmetricCipher.init(Cipher.ENCRYPT_MODE, config.publicKeysOfMixes[i]);
		else {
			this.asymmetricCipher.init(Cipher.ENCRYPT_MODE, config.publicKeysOfMixes[request.route[i]]); // TODO: possible side-effect -> won't work anymore if mix-ids are chosen differently
		//	System.out.println("" +owner +" using public key " +Util.md5(config.publicKeysOfMixes[request.route[i]].getEncoded()) +"for mix " +request.route[i]);
		}
	
		int pointer = firstBlock.length-config.MAC_LENGTH;
		firstBlock= asymmetricCipher.doFinal(firstBlock, 0, firstBlock.length);
		//replace firstBlock plaintext with firstBlock cipherText
		header = Util.concatArrays(firstBlock, Arrays.copyOfRange(header, pointer, header.length));
	
	
		
		// encrypt message; symmetric part
		symCipher.init(Cipher.ENCRYPT_MODE, symKeys[(config.routeLength-1)-i], initVectors[(config.routeLength-1)-i]);
		
		if(i!=0) 
		{
			
			pointer = firstBlock.length;
			
			byte[] headerCiphertext = firstBlock;
			for(int k=0; k <config.routeLength-1; k++)
			{
			 byte[] SymBlock= symCipher.doFinal(header, pointer, config.ASYM_KEY_LENGTH);
			headerCiphertext =  Util.concatArrays(headerCiphertext, SymBlock);
			pointer+=config.ASYM_KEY_LENGTH;
			
			}
			header = headerCiphertext;
			
		}

		

	}
byte[] lengthHeader = Util.intToByteArray(header.length);
request.setByteMessage( Util.concatArrays(new byte[][] {
		lengthHeader,
			header			,	// == replyBlock
			request.getByteMessage()//message
	
	}));
	
		return request;
	
	}
	catch (Exception e) {
		e.printStackTrace();
		return null;
	}
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
			pointer = asymPlaintext.length; //skip padding
		
			// decrypt symmetrically encrypted part
			Cipher decryptCipher = Cipher.getInstance(config.SYM_CRYPTOGRAPHY_ALGORITHM, config.CRYPTO_PROVIDER);
			decryptCipher.init(Cipher.DECRYPT_MODE, symKey, initVector);
			
			byte[] plaintext;
			byte[] payloadPlaintext;
			int payloadPointer = owner.FREE_ROUTE_LENGTH * asymmetricCipher.getBlockSize();
			if(!isLastMix)
			{
				
				byte[] headerPlaintext = asymPlaintext ;
				int blockPointer = asymmetricCipher.getBlockSize(); 
				
				for(int k=0; k<owner.FREE_ROUTE_LENGTH-1;k++)
				{
					 
					byte[] symBlock = decryptCipher.doFinal(message.getByteMessage(), blockPointer, asymmetricCipher.getBlockSize());
					 headerPlaintext = Util.concatArrays(headerPlaintext, symBlock);
					 blockPointer+= asymmetricCipher.getBlockSize();
					
					
				}
			
				payloadPlaintext = decryptCipher.doFinal(message.getByteMessage(), payloadPointer, message.getByteMessage().length -payloadPointer);
				plaintext = Util.concatArrays(headerPlaintext, payloadPlaintext);
			}
			else
			{
				//decrypt payload only 
				payloadPlaintext = decryptCipher.doFinal(message.getByteMessage(), payloadPointer, message.getByteMessage().length-payloadPointer);
				plaintext = Util.concatArrays(asymPlaintext, Arrays.copyOfRange(message.getByteMessage(), asymmetricCipher.getBlockSize(), payloadPointer));
				plaintext = Util.concatArrays(plaintext, payloadPlaintext);
				
			}

			payloadPointer = pointer + (owner.FREE_ROUTE_LENGTH-1)*asymmetricCipher.getBlockSize();
			
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
				byte[] lengthHeader = Arrays.copyOfRange(plaintext, payloadPointer, payloadPointer += config.LENGTH_HEADER_LENGTH);
				int payloadLength = Util.byteArrayToInt(lengthHeader);
				if (payloadLength == 0) // dummy
					plaintext = new byte[0];
				else
					plaintext = Arrays.copyOfRange(plaintext, payloadPointer, payloadPointer + payloadLength); 
				message.setByteMessage(plaintext);
				}
				if (owner.IS_DUPLEX) { 
					//	RBlength|replyblock|PTlength|plaintext|padding
					pointer = payloadPointer;
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
				} 
				
				else
					return message;
				
			}
			//not last Mix
			else {
				byte[] header = Arrays.copyOfRange(plaintext, pointer, payloadPointer); 
				byte[] payload = Arrays.copyOfRange(plaintext,payloadPointer, plaintext.length);
				SecureRandom prng = SecureRandom.getInstance(config.PRNG_ALGORITHM);
				prng.setSeed(symKeyAsByteArray);
				byte[] padding = new byte[config.ASYM_KEY_LENGTH];
				prng.nextBytes(padding);
				header = Util.concatArrays(header,padding);
				message.setByteMessage(Util.concatArrays(header, payload));
				
				
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
			boolean isLastMix = false;	
			byte[] replyblock;
			byte[] payload;
			
			
			if(message.getByteMessage().length<=config.MAX_PAYLOAD )
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
			byte[] pseudonym = null;
			IvParameterSpec initVector;
			int pointer = 0;
			mac = Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.MAC_LENGTH);
			
			if (owner.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) { // determine next hop if source routing is enabled
				byte[] nextHopAddress = Arrays.copyOfRange(asymPlaintext, pointer, pointer += 4);
				int address = Util.byteArrayToInt(nextHopAddress);
				if (address == owner.PUBLIC_PSEUDONYM) { // this mix is the last hop
					if (owner.DISPLAY_ROUTE_INFO)
						System.out.println(""+owner +" setting nextHopAddress to \"LAST HOP\""); 
					message.nextHopAddress = MixMessage.CLIENT;
					isLastMix = true;
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
			if(isLastMix)
				pseudonym =  Arrays.copyOfRange(asymPlaintext, pointer, pointer += config.PSEUDONYM_LENGTH);
			pointer = asymPlaintext.length;
			// decrypt symmetrically encrypted part
			Cipher decryptCipher = Cipher.getInstance(config.SYM_CRYPTOGRAPHY_ALGORITHM, config.CRYPTO_PROVIDER);
					
			decryptCipher.init(Cipher.DECRYPT_MODE, symKey, initVector);
			
			byte[] plaintext;
			if(!isLastMix)
			{
				
				byte[] headerPlaintext = asymPlaintext ;
				int blockPointer = asymmetricCipher.getBlockSize();
				
				for(int k=0; k<owner.FREE_ROUTE_LENGTH-1;k++)
				{
					 
					byte[] symBlock = decryptCipher.doFinal(replyblock, blockPointer, asymmetricCipher.getBlockSize());
					 headerPlaintext = Util.concatArrays(headerPlaintext, symBlock);
					 blockPointer+= asymmetricCipher.getBlockSize();
					
				}
				plaintext=headerPlaintext;
			}
			
			else //not last mix
			{ 
				plaintext = Util.concatArrays(asymPlaintext, Arrays.copyOfRange(replyblock, asymmetricCipher.getBlockSize(), replyblock.length));
			}
				// validate mac
				Mac macGenerator = Mac.getInstance(config.MAC_ALGORITHM);
				macGenerator.init(macKey);
				byte[] signedData = Arrays.copyOfRange(plaintext, config.MAC_LENGTH, plaintext.length);
//				
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
			
			
			if(!isLastMix)
			{
				SecureRandom prng = SecureRandom.getInstance(config.PRNG_ALGORITHM);
				prng.setSeed(symKeyAsByteArray);
				byte[] padding = new byte[config.ASYM_KEY_LENGTH];
				prng.nextBytes(padding);
				replyblock= Util.concatArrays(replyblock,padding);
				
			}
			
			byte[] cipherText = Util.concatArrays(replyblock, payloadCipherText);
			
			if (isLastMix) {
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