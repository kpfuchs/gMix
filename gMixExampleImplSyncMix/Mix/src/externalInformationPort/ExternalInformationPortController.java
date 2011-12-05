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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Logger;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import util.Util;

import messageProcessor.MessageProcessorController;

import exception.InformationRetrieveException;

import architectureInterface.ExternalInformationPortInterface;


/**
 * Controller class of component <code>ExternalInformationPort</code>. 
 * Implements the architecture interface 
 * <code>ExternalInformationPortInterface</code>. 
 * <p>
 * Used for <code>Information</code> (for example a public key) exchange with 
 * "external" communication partners (for example other mixes or clients). This 
 * component doesn't affect the sending of mix messages directly (That's done 
 * by the <code>InputOutputHandler</code>: see 
 * <code>architectureInterface.InputOutputHandlerInterface</code>).
 * <p>
 * Each exchangeable type of information is specified in the enumeration 
 * <code>Information</code>.
 * 
 * @author Karl-Peter Fuchs
 */
public class ExternalInformationPortController implements 
		ExternalInformationPortInterface {

	/** 
	 * Reference on component <code>InternalInformationPort</code>. 
	 * Used to display and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = internalInformationPort.getLogger();
	
	/** 
	 * Reference on <code>MessageProcessor</code> component. Used to encrypt 
	 * certain kinds of <code>Information</code>.
	 */
	private MessageProcessorController messageProcessor;
	
	/** Provides information about this mix (cascade). */
	private InformationProvider informationProvider;
	
	
	/**
	 * Generates a new <code>ExternalInformationPort</code> component.
	 * <p>
	 * Used for <code>Information</code> (for example a public key) exchange 
	 * with "external" communication partners (for example other mixes or 
	 * clients). This component doesn't affect the sending of mix messages 
	 * directly (That's done by the <code>InputOutputHandler</code>: see 
	 * <code>architectureInterface.InputOutputHandlerInterface</code>).
	 * <p>
	 * Each exchangeable type of information is specified in the enumeration 
	 * <code>Information</code>.
	 * <p>
	 * Component can't be used before calling <code>initialize()</code>.
	 * 
	 * @see #initialize()
	 * @see #initialize(PublicKey, MessageProcessorController)
	 */
	public ExternalInformationPortController() {
		
		
	}
	
	
	/**
	 * Initialization method for this component. Makes this component capable 
	 * of receiving AND providing <code>Information</code> (see 
	 * <code>initialize()</code> for receival only). Must be called before 
	 * using an instance of this class for anything but dispensing references 
	 * on the instance itself.
	 * 
	 * @param publicKey			The mix' public key, this object belongs to 
	 * 							(will be made available via 
	 * 							<code>InformationProvider</code>).
	 * @param messageProcessor	Used to encrypt certain kinds of 
	 * 							<code>Information</code>.
	 * 
	 * @see #initialize()
	 */
	public void initialize(	PublicKey publicKey,
							MessageProcessorController messageProcessor) {
		
		this.messageProcessor = messageProcessor;
		
		this.informationProvider = new InformationProvider(publicKey, this);
		
	}
	
	
	/**
	 * Initialization method for this component. Makes this component capable 
	 * of receiving BUT NOT providing <code>Information</code> from a 
	 * communication partner's <code>ExternalInformationPort</code> (see 
	 * <code>initialize(PublicKey, MessageProcessorController)</code> for 
	 * providing <code>Information</code>, too). Must be called before 
	 * using an instance of this class for anything but dispensing references 
	 * on the instance itself.
	 * 
	 * @see #initialize(PublicKey, MessageProcessorController)
	 */
	public void initialize() {

	}
	
	
	/**
	 * Makes the bypassed key available via <code>InformationProvider</code>. 
	 * This key is used to encrypt data between mixes.
	 * 
	 * @param interMixKey	Key used to encrypt data between mixes.
	 */
	public void setInterMixKeyWithPreviousMix(SecretKey interMixKey) {
		
		informationProvider.setInterMixKeyWithPreviousMix(interMixKey);
		
	}
	
	
	/**
	 * Makes the bypassed initialization vector available via 
	 * <code>InformationProvider</code>. This initialization vector is used to 
	 * encrypt data between mixes.
	 * 
	 * @param interMixIV	Initialization vector used to encrypt data 
	 * 						between mixes.
	 */
	public void setInterMixIVWithPreviousMix(
			IvParameterSpec interMixIV
			) {
		
		informationProvider.setInterMixIVWithPreviousMix(interMixIV);
		
	}
	

	/**
	 * Generic method to receive some <code>Information</code> from a 
	 * specified <code>ExternalInformationPort</code> component.
	 * <p>
	 * If communication channel isn't safe, but transmitted data is sensitive, 
	 * cryptographic measures (like authentication and encryption) should be 
	 * applied.
	 * 
	 * @param informationProviderAddress	Address of the <code>
	 * 										ExternalInformationPort</code> 
	 * 										component to receive data from.
	 * @param informationProviderPort		Port the <code>
	 * 										ExternalInformationPort</code> 
	 * 										component to receive data from  
	 * 										runs on.
	 * @param informationOfInterest			Type of <code>Information</code>,
	 * 										that shall be received.
	 * 
	 * @return								The requested <code>Information
	 * 										</code>.
	 * 
	 * @throws	InformationRetrieveException 	Thrown, when requested <code>
	 * 											Information</code> not 
	 * 											available.
	 */
	public Object getInformation(
			InetAddress informationProviderAddress,
			int informationProviderPort, 
			Information informationOfInterest)
			throws InformationRetrieveException {
		
		switch (informationOfInterest) {
		
			case PUBLIC_KEY:
				return getPublicKey(	informationProviderAddress,
										informationProviderPort
										);
				
			case INTER_MIX_KEY:
				return getInterMixKey(	informationProviderAddress,
										informationProviderPort
										);
				
			case INTER_MIX_IV:
				return getInterMixIV(	informationProviderAddress,
										informationProviderPort
										);
				
			case NEXT_MIX_ADDRESS:
				return getNextMixAddress(	informationProviderAddress,
											informationProviderPort
											);
				
			case NEXT_MIX_INFO_PORT:
				return getNextMixInfoPort(	informationProviderAddress,
											informationProviderPort
											);
				
			default:
				return InformationGrabber.getInformation(
						informationProviderAddress,
						informationProviderPort,
						informationOfInterest,
						null
						);
			
		}

	}

	
	/**
	 * Generic method to receive some <code>Information</code> from a 
	 * specified <code>ExternalInformationPort</code> component, sending the 
	 * bypassed data.
	 * <p>
	 * If communication channel isn't safe, but transmitted data is sensitive, 
	 * cryptographic measures (like authentication and encryption) should be 
	 * applied.
	 * 
	 * @param informationProviderAddress	Address of the <code>
	 * 										ExternalInformationPort</code> 
	 * 										component to receive data from.
	 * @param informationProviderPort		Port the <code>
	 * 										ExternalInformationPort</code> 
	 * 										component to receive data from  
	 * 										runs on.
	 * @param informationOfInterest			Type of <code>Information</code>,
	 * 										that shall be received.
	 * 
	 * @return								The requested <code>Information
	 * 										</code>.
	 * 
	 * @throws	InformationRetrieveException 	Thrown, when requested <code>
	 * 											Information</code> not 
	 * 											available.
	 */
	public Object getInformation(
			InetAddress informationProviderAddress,
			int informationProviderPort, 
			Information informationOfInterest,
			byte[] data) throws InformationRetrieveException {
		
		switch (informationOfInterest) {
		
			default:
				return InformationGrabber.getInformation(
						informationProviderAddress,
						informationProviderPort,
						informationOfInterest,
						data
						);
		
		}
		
	}
	
	
	/**
	 * Generic method to receive some <code>Information</code> from several 
	 * <code>ExternalInformationPort</code> components. The first component's 
	 * address and port number must be specified. The other components 
	 * addresses are received by their predecessor  (= telescope 
	 * implementation).
	 * <p>
	 * If communication channel isn't safe, but transmitted data is sensitive, 
	 * cryptographic measures (like authentication and encryption) should be 
	 * applied.
	 * 
	 * @param informationProviderAddress	Address of the <code>
	 * 										InformationProvider</code>
	 * 										-component to receive data from.
	 * @param informationProviderPort		Port the <code>InformationProvider
	 * 										</code>-component to receive data 
	 * 										from runs on.
	 * @param informationOfInterest			Type of <code>Information</code>,
	 * 										that shall be received.
	 * 
	 * @return								The requested <code>Information
	 * 										</code>s.
	 * 
	 * @throws	InformationRetrieveException 	Thrown, when requested <code>
	 * 											Information</code> not 
	 * 											available.
	 */
	public Object[] getInformationFromAll(
			InetAddress informationProviderAddress,
			int informationProviderPort, 
			Information informationOfInterest
			) throws InformationRetrieveException {
		
		switch (informationOfInterest) {
		
			case PUBLIC_KEY:
				return getPublicKeys(	informationProviderAddress,
										informationProviderPort
										);
		
			default:
				return InformationGrabber.getInformationFromAll(
						informationProviderAddress,
						informationProviderPort,
						informationOfInterest,
						null
						);
		
		}
		
	}


	/**
	 * Generic method to receive some <code>Information</code> from several 
	 * <code>ExternalInformationPort</code> components, sending the bypassed 
	 * data. The first component's address and port number must be specified. 
	 * The other components' addresses are received by their predecessor  (= 
	 * telescope implementation).
	 * <p>
	 * If communication channel isn't safe, but transmitted data is sensitive, 
	 * cryptographic measures (like authentication and encryption) should be 
	 * applied.
	 * 
	 * @param informationProviderAddress	Address of the <code>
	 * 										InformationProvider</code> 
	 * 										component to receive data from.
	 * @param informationProviderPort		Port the <code>InformationProvider
	 * 										</code> component to receive data 
	 * 										from runs on.
	 * @param informationOfInterest			Type of <code>Information</code>,
	 * 										that shall be received.
	 * 
	 * @return								The requested <code>Information
	 * 										</code>s.
	 * 
	 * @throws	InformationRetrieveException 	Thrown, when requested <code>
	 * 											Information</code> not 
	 * 											available.
	 */
	public Object[] getInformationFromAll(
			InetAddress informationProviderAddress,
			int informationProviderPort, 
			Information informationOfInterest,
			byte[] data
			) throws InformationRetrieveException {
		
		switch (informationOfInterest) {
		
			default:
				return InformationGrabber.getInformationFromAll(
						informationProviderAddress,
						informationProviderPort,
						informationOfInterest,
						data
						);
	
		}
		
	}
	
	
	/**
	 * Make component start listening for requests (on communication channel).
	 */
	public void acceptRequests() {
		
		informationProvider.acceptRequests();
		
	}
	
	
	/**
	 * Retrieves the specified's mix <code>SYMMETRIC_KEY</code>.
	 * 
	 * @param nexMixInformationProviderAddress	Address of the next mix' <code>
	 * 											ExternalInformationPort</code>
	 * 											component.
	 * @param nexMixInformationProviderPort		Port the <code>
	 * 											ExternalInformationPort</code>
	 * 											component to receive data 
	 * 											from runs on.
	 * 
	 * @return									The specified mix' 
	 * 											<code>SYMMETRIC_KEY</code>.
	 * 
	 * @throws	InformationRetrieveException 	Thrown, when requested <code>
	 * 											Information</code> not 
	 * 											available.
	 */
	private SecretKey getInterMixKey(
			InetAddress nexMixInformationProviderAddress,
			int nexMixInformationProviderPort
		) throws InformationRetrieveException {

		byte[] encryptedKey = 
			InformationGrabber.getInformation(
					nexMixInformationProviderAddress, 
					nexMixInformationProviderPort, 
					Information.INTER_MIX_KEY,
					null
					);
		
		byte[] decryptedKey;
		
		try {
			
			decryptedKey = 
				messageProcessor.decrypt(encryptedKey, "RSA/None/PKCS1Padding");
			
		} catch (Exception e) {
	
			e.printStackTrace();
			throw new InformationRetrieveException();
	
		}
		
		String algorithm = 
			internalInformationPort.getProperty(
					"NAME_OF_INTER_MIX_KEY_GENERATOR"
					);
		
		return new SecretKeySpec(decryptedKey, algorithm);
	
	}

	
	/**
	 * Retrieves the specified mix' <code>IV</code>.
	 * 
	 * @param nexMixInformationProviderAddress	Address of the next mix' <code>
	 * 											ExternalInformationPort</code> 
	 * 											component.
	 * @param nexMixInformationProviderPort		Port the <code>
	 * 											ExternalInformationPort</code> 
	 * 											component to receive data 
	 * 											from runs on.
	 * 
	 * @return									The specified mix' 
	 * 											<code>IV</code>.
	 * 
	 * @throws	InformationRetrieveException 	Thrown, when requested <code>
	 * 											Information</code> not 
	 * 											available.
	 */
	private IvParameterSpec getInterMixIV(
			InetAddress nexMixInformationProviderAddress,
			int nexMixInformationProviderPort
			) throws InformationRetrieveException {
		
		byte[] ivAsByteArray = 
			InformationGrabber.getInformation(
					nexMixInformationProviderAddress, 
					nexMixInformationProviderPort, 
					Information.INTER_MIX_IV,
					null
					);
		
		return new IvParameterSpec(ivAsByteArray);
		
	}

	
	/**
	 * Retrieves the specified mix' <code>NEXT_MIX_ADDRESS</code>.
	 * 
	 * @param informationProviderAddress	Address of the next mix' <code>
	 * 										ExternalInformationPort</code> 
	 * 										component.
	 * @param informationProviderPort		Port the <code>
	 * 										ExternalInformationPort</code> 
	 * 										component to receive data from runs 
	 * 										on.
	 * 
	 * @return								The specified mix' 
	 * 										<code>NEXT_MIX_ADDRESS</code>.
	 * 
	 * @throws	InformationRetrieveException 	Thrown, when requested <code>
	 * 											Information</code> not 
	 * 											available.
	 */
	private InetAddress getNextMixAddress(
			InetAddress informationProviderAddress,
			int informationProviderPort
			) throws InformationRetrieveException {
		
		byte[] addressAsByteArray = 
			InformationGrabber.getInformation(
				informationProviderAddress,
				informationProviderPort,
				Information.NEXT_MIX_ADDRESS,
				null
				);
			
		try {
			
			return InetAddress.getByAddress(addressAsByteArray);
			
		} catch (UnknownHostException e) {
			
			LOGGER.severe(e.getMessage());
			return null;
			
		}
		
	}

	
	/**
	 * Retrieves the specified mix' <code>NEXT_MIX_INFO_PORT</code>.
	 * 
	 * @param informationProviderAddress	Address of the next mix' <code>
	 * 										ExternalInformationProvider</code> 
	 * 										component (to receive data from).
	 * @param informationProviderPort		Port the <code>
	 * 										ExternalInformationProvider</code> 
	 * 										component (to receive data from)  
	 * 										runs on.
	 * 
	 * @return								The specified mix' 
	 * 										<code>NEXT_MIX_INFO_PORT</code>.
	 * 
	 * @throws	InformationRetrieveException 	Thrown, when requested <code>
	 * 											Information</code> not 
	 * 											available.
	 */
	private int getNextMixInfoPort(
			InetAddress informationProviderAddress,
			int informationProviderPort
			) throws InformationRetrieveException {
		
		byte[] portAsByteArray = 
			InformationGrabber.getInformation(	
					informationProviderAddress,
					informationProviderPort,
					Information.NEXT_MIX_INFO_PORT,
					null
					);
		
		return Util.byteArrayToInt(portAsByteArray);
		
	}
	
	
	/**
	 * Retrieves the specified mix' <code>PUBLIC_KEY</code>.
	 * 
	 * @param informationProviderAddress	Address of the next mix' <code>
	 * 										ExternalInformationProvider</code> 
	 * 										component (to receive data from).
	 * @param informationProviderPort		Port the <code>
	 * 										ExternalInformationProvider</code> 
	 * 										component (to receive data from)  
	 * 										runs on.
	 * 
	 * @return								The specified mix' 
	 * 										<code>PUBLIC_KEY</code>.
	 * 
	 * @throws	InformationRetrieveException 	Thrown, when requested <code>
	 * 											Information</code> not 
	 * 											available.
	 */
	private PublicKey getPublicKey(
			InetAddress informationProviderAddress,
			int informationProviderPort
			) throws InformationRetrieveException {
		
		byte[] keyAsByteArray = 
			InformationGrabber.getInformation(
					informationProviderAddress,
					informationProviderPort,
					Information.PUBLIC_KEY,
					null
					);
		
		X509EncodedKeySpec publicKeySpec = 
			new X509EncodedKeySpec(keyAsByteArray);
			
		KeyFactory keyFactory;
		
		try {
			
			keyFactory = KeyFactory.getInstance(publicKeySpec.getFormat());
			
			return keyFactory.generatePublic(publicKeySpec);
			
		} catch (NoSuchAlgorithmException e) {
			
			LOGGER.severe(e.getMessage());
			return null;
			
		} catch (InvalidKeySpecException e) {
			
			LOGGER.severe(e.getMessage());
			return null;
			
		}
		
	}

	
	/**
	 * Retrieves the <code>PUBLIC_KEY</code>s of all mixes in the specified 
	 * cascade.
	 * 
	 * @param informationProviderAddress	Address of the next mix' <code>
	 * 										ExternalInformationProvider</code> 
	 * 										component (to receive data from).
	 * @param informationProviderPort		Port the <code>
	 * 										ExternalInformationProvider</code> 
	 * 										component (to receive data from)  
	 * 										runs on.
	 * 
	 * @return								The <code>PUBLIC_KEY</code>s of 
	 * 										all mixes in the specified cascade.
	 * 
	 * @throws	InformationRetrieveException 	Thrown, when requested <code>
	 * 											Information</code> not 
	 * 											available.
	 */
	private PublicKey[] getPublicKeys(
			InetAddress informationProviderAddress,
			int informationProviderPort
			) throws InformationRetrieveException {
		
		try {
			
			PublicKey[] result;
			
			byte[][] keysAsByteArrays = 
				InformationGrabber.getInformationFromAll(
						informationProviderAddress,
						informationProviderPort,
						Information.PUBLIC_KEY,
						null
						);
			
			result = new PublicKey[keysAsByteArrays.length];
			
			// convert received public keys (byte[]) to objects
			for(int i=0; i<keysAsByteArrays.length; i++) {
				
				X509EncodedKeySpec publicKeySpec = 
						new X509EncodedKeySpec(keysAsByteArrays[i]);
						
				KeyFactory keyFactory = 
					KeyFactory.getInstance(publicKeySpec.getFormat());
				
					
				result[i] = keyFactory.generatePublic(publicKeySpec);
				
			}
			
			return result;
			
		} catch (NoSuchAlgorithmException e) {
			
			LOGGER.severe(e.getMessage());
			return null;
			
		} catch (InvalidKeySpecException e) {
			
			LOGGER.severe(e.getMessage());
			return null;
			
		}
		
	}
	
}
