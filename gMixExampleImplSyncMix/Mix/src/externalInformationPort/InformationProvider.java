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

package externalInformationPort;


import internalInformationPort.InternalInformationPortController;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import util.Util;


/**
 * Provides <code>Information</code> for "external" communication partners (e. 
 * g. other mixes or clients) vie network (UDP).
 * 
 * @author Karl-Peter Fuchs
 */
final class InformationProvider extends Thread {

	/** 
	 * Reference on component <code>InternalInformationPort</code>. 
	 * Used to display and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = internalInformationPort.getLogger();
	
	/** Maximum size a request may have. */
	private final static int MAX_PACKET_SIZE = 1024;
	
	/** 
	 * Reference on component <code>ExternalInformationPort</code>. 
	 * Used to receive data from <code>ExternalInformationPort</code> 
	 * components on different hosts.
	 */
	private ExternalInformationPortController externalInformationPort;
	
	/** DatagramSocket this <code>InformationProvider</code> runs on. */
	private DatagramSocket datagramSocket;
	
	/** 
	 * Enumeration used to identify incoming requests.
	 * 
	 * @see Information
	 */
	EnumMap<Information, Integer> enumMap;
	
	/** This mix' public key. */
	private PublicKey publicKey;
	
	/** Key used to encrypt data between this mix and its predecessor. */
	private SecretKey interMixKey;
	
	/** 
	 * Key used to encrypt data between this mix and its predecessor, 
	 * encrypted with the predecessor's public key.
	 */
	private byte[] encryptedInterMixKey;
	
	/** 
	 * Initialization vector used to encrypt data between this mix and its 
	 * predecessor.
	 */
	private IvParameterSpec interMixIV;
	
	/**
	 * Indicates whether the key used to encrypt data between this mix and its 
	 * predecessor has already been encrypted (= prepared for sending via 
	 * an insecure channel).
	 * 
	 * @see #interMixKey
	 * @see #encryptedInterMixKey
	 */
	private boolean isSymmetricKeyEncrypted = false;
	
	/** 
	 * The mix' position in the cascade this object belongs to. "1" means 
	 * "first mix", "2" means "a middle mix" and "3" means "last mix" of 
	 * cascade (or single mix).
	 */
	private int positionOfMixInCascade;
	
	/** Host name of the next mix in the cascade. */
	private String nextMixHostName;
	
	/** Port number of the next mix in the cascade. */
	private int nextMixInfoPort;
	
	/** Address of the previous mix in the cascade. */
	private InetAddress previousMixAddress; 
	
	/** 
	 * Port number of the previous mix' <code>InformationProvider</code> 
	 * component.
	 */
	private int previousMixInfoPort;
	
	
	/**
	 * Creates a new <code>InformationProvider</code> that provides 
	 * <code>Information</code> for "external" communication partners (e. g. 
	 * other mixes or clients) vie network (UDP).
	 * 
	 * @param publicKey	This mix' public key.
	 * @param eip		Reference on component <code>
	 * 					ExternalInformationPort</code>. Used to reveive  
	 * 					<code>Information</code> from communication partners.
	 */
	protected InformationProvider(	PublicKey publicKey,
									ExternalInformationPortController eip) {
		
		this.publicKey = publicKey;
		this.externalInformationPort = eip;
		
		this.positionOfMixInCascade = 
			new Integer(internalInformationPort.
					getProperty("POSITION_OF_MIX_IN_CASCADE")
				);
		
		// set enumMap up
		enumMap = new EnumMap<Information, Integer>(Information.class);
		
		for (Information info : Information.values()) {
			
			enumMap.put(info, info.getIdentifier());
			
		}

		// read values from property file
		
		this.nextMixHostName = 
			internalInformationPort.getProperty("NEXT_MIX_ADDRESS");
		
		this.nextMixInfoPort = 
			new Integer(
					internalInformationPort.getProperty("NEXT_MIX_INFO_PORT")
				);
		
		this.previousMixInfoPort = 
			new Integer(internalInformationPort.
					getProperty("PREVIOUS_MIX_INFO_PORT")
				);

	}
	
	
	/**
	 * Sets the key used to encrypt data between this mix and its predecessor.
	 * 
	 * @param interMixKey	Key used to encrypt data between this mix and its 
	 * 						predecessor.
	 */
	protected void setInterMixKeyWithPreviousMix(SecretKey interMixKey) {
		
		this.interMixKey = interMixKey;
		
		
	}


	/**
	 * Sets the initialization vector used to encrypt data between this mix and 
	 * its predecessor.
	 * 
	 * @param interMixIV	Initialization vector used to encrypt data between 
	 * 						this mix and its predecessor.
	 */
	protected void setInterMixIVWithPreviousMix(IvParameterSpec interMixIV) {
		
		this.interMixIV = interMixIV;
		
	}
	
	
	/**
	 * Make this <code>InformationProvider</code> start listening for 
	 * requests (on communication channel).
	 */
	protected void acceptRequests() {
		
		int port = 
			new Integer(internalInformationPort.getProperty("INFO_PORT"));
		
		InetAddress bindAddress = null;
		
		try {
			
			bindAddress = 
				InetAddress.getByName(
						internalInformationPort.getProperty("INFO_BIND_ADDRESS")
					);
			
			previousMixAddress = 
				InetAddress.getByName(
						internalInformationPort.
							getProperty("PREVIOUS_MIX_ADDRESS")
					);
			
		} catch (UnknownHostException e) {
			
			LOGGER.severe(	"(MIX IP) Invalid bind address specified in "
							+"property file: "
							+internalInformationPort.getProperty(
									"INFO_BIND_ADDRESS") +"!"
							+e.getMessage()
							);
			
			System.exit(1);
			
		}
		
		try {
			
			// instantiate a ServerSocket
			datagramSocket = new DatagramSocket(port, bindAddress);
			
		} catch (IOException e) {
			
			LOGGER.severe(	"(MIX IP) Couldn't bind socket to "
							+bindAddress +":" +port +"!" 
							+e.getMessage()
							);
			
			System.exit(1);
			
		}
		
		start();
		
	}
	
	
	/**
	 * Generates a byte array containing the information specified by the 
	 * bypassed value (= information, the caller is interested in).
	 * 
	 * @param informationOfInterest	Information, the caller is interested in.
	 * @param data					Data submitted by caller.
	 * 
	 * @return 	Byte array containing the information specified by the bypassed 
	 * 			value (= information, the caller is interested in) or 
	 * 			<code>null</code> if the requested information is not available.
	 * 
	 */
	private byte[] generateResponse(	Information informationOfInterest, 
										byte[] data
										) {
			
		switch (informationOfInterest) {
		
			case PUBLIC_KEY:
				return providePublicKey();			
				
			case INTER_MIX_KEY:
				return provideInterMixKey();
				
			case INTER_MIX_IV:
				return provideInterMixIV();
				
			case NEXT_MIX_ADDRESS:
				return provideNextMixAddress();
				
			case NEXT_MIX_INFO_PORT:
				return provideNextMixInfoPort();
				
			default:
				return null;
			
		}
		
	}
	
	
	/**
	 * Provides this mix' <code>PUBLIC_KEY</code>.
	 * <p>
	 * Security note: Key is not signed!
	 * 
	 * @return	This mix' <code>PUBLIC_KEY</code>.
	 */
	private byte[] providePublicKey() {
		
		// Security note: Key is not signed!
		X509EncodedKeySpec publicKeySpec = 
			new X509EncodedKeySpec(publicKey.getEncoded());
		
		return publicKeySpec.getEncoded();
		
	}
	
	
	/**
	 * Provides this mix' <code>INTER_MIX_KEY</code>.
	 * <p>
	 * Security note: Key is not signed!
	 * 
	 * @return	This mix' <code>INTER_MIX_KEY</code>.
	 */
	private byte[] provideInterMixKey() {
		
		if (!isSymmetricKeyEncrypted) {
				
			return encryptInterMixKey();
				
		} else {
				
			return encryptedInterMixKey;
				
		}
		
	}
	
	
	/**
	 * Provides this mix' <code>INTER_MIX_IV</code>.
	 * <p>
	 * Security note: Not signed!
	 * 
	 * @return	This mix' <code>INTER_MIX_IV</code>.
	 */
	private byte[] provideInterMixIV() {
		
		return interMixIV.getIV();
		
	}


	/**
	 * Provides this mix' <code>NEXT_MIX_ADDRESS</code>.
	 * <p>
	 * Security note: Not signed!
	 * 
	 * @return	This mix' <code>NEXT_MIX_ADDRESS</code>.
	 */
	private byte[] provideNextMixAddress() {
	
		if (positionOfMixInCascade != 3) { // first or middle mix
				
			InetAddress nextMixAddress = null;
				
			try {
					
				nextMixAddress = InetAddress.getByName(nextMixHostName);
					
			} catch (UnknownHostException e) {

				return null;
					
			}

			return nextMixAddress.getAddress();
				
		} else { // last mix
				
			return null;
				
		}
	
	}
	
	
	/**
	 * Provides this mix' <code>NEXT_MIX_INFO_PORT</code>.
	 * <p>
	 * Security note: Not signed!
	 * 
	 * @return	This mix' <code>NEXT_MIX_INFO_PORT</code>.
	 */
	private byte[] provideNextMixInfoPort() {
	
		if (positionOfMixInCascade != 3) { // first or middle mix
			
			return Util.intToByteArray(nextMixInfoPort);
			
		} else { // last mix
			
			return null;
			
		}
	
	}

	
	/**
	 * Encrypts and returns the <code>interMixKey>/code>.
	 * 
	 * @return	The encrypted <code>interMixKey>/code>.
	 * 
	 * @see #interMixKey
	 * @see #isSymmetricKeyEncrypted
	 */
	private byte[] encryptInterMixKey() {
		
		try {
			
			// get public key of previous mix
			PublicKey publicKey = 
				(PublicKey) externalInformationPort.getInformation(
									previousMixAddress, 
									previousMixInfoPort, 
									Information.PUBLIC_KEY
									);
			
			// get Cipher
			Cipher asymmetricCipher = 
				Cipher.getInstance(
					"RSA/None/PKCS1Padding",
					internalInformationPort.getProperty("CRYPTO_PROVIDER")
					);
				
			// init Cipher
			asymmetricCipher.init(
					Cipher.ENCRYPT_MODE, 
					publicKey
					);
			
			// encrypt and return symmetric key
			encryptedInterMixKey = 
				asymmetricCipher.doFinal(interMixKey.getEncoded());
			
			isSymmetricKeyEncrypted = true;
			
			return encryptedInterMixKey;
				
		} catch (Exception e) {

			LOGGER.fine(e.getMessage());
			return null;
				
		}
		
	}


	/**
	 * Answers incoming requests.
	 */
	@Override
	public void run() {

		while (true) { // answer requests
        	
            try {
            	
                // receive request
            	byte[] buf = new byte[MAX_PACKET_SIZE];
            	DatagramPacket packet = new DatagramPacket(buf, buf.length);
                datagramSocket.receive(packet);

                // generate response
                Information informationOfInterest = null;
                
                int requestIdentifier = 
                	Util.byteArrayToInt(Arrays.copyOf(buf, 4));
                
                // try to find information, the sender is interested in (using 
                // the submitted identifier)
                for (Information info : Information.values()) {
        			
                	if (info.getIdentifier() == requestIdentifier) {
                		
                		informationOfInterest = info;
                		break;
                		
                	}
        			
        		}
                
                if (informationOfInterest != null) { // information available
                	
                	// read data included in request (if present)
                	byte[] data = null;
                	
                	int lengthOfBypassedData = 
                		Util.byteArrayToInt(Arrays.copyOfRange(buf, 4, 8));
                		
                	if (lengthOfBypassedData != 0) { // data included
                		
                		data = Arrays.copyOfRange(	buf, 
                									8, 
                									(8 + lengthOfBypassedData)
                									);
                		
                	}
                	
                	byte[] payload = 
                		generateResponse(informationOfInterest, data);
                	
                	if (payload != null) {
                		
                		// header indicating type of message
                    	byte[] header1 =
                    		Util.intToByteArray(
                    			informationOfInterest.getIdentifier()
                    			);
                    	
                		// header indicating length of message
                    	byte[] header2 = Util.intToByteArray(payload.length);
                    	buf = Util.mergeArrays(header1, header2);
                    	buf = Util.mergeArrays(buf, payload);
                    	
                	} else {
                		
                		buf = 
                			Util.intToByteArray(
                				Information.NOT_AVAILABLE.getIdentifier()
                    			);
                		
                	}
                			
                } else {
                	
                	buf = 
                		Util.intToByteArray(
                			Information.NOT_AVAILABLE.getIdentifier()
                			);
                	
                }
                
                // send response
                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                packet = new DatagramPacket(buf, buf.length, address, port);
                datagramSocket.send(packet);
                
            } catch (IOException e) {
            	
            	LOGGER.fine("(MIX IP) Couldnt't read request!");
            	continue;
                
            }
            
        }
        
    }

}
