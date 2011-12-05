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

package client;


import internalInformationPort.InternalInformationPortController;

import java.math.BigInteger;
import java.security.Key;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import networkClock.NetworkClockController;

import message.ChannelEstablishMessage;
import message.ChannelEstablishMessagePart;
import message.ChannelMessage;
import message.ChannelMessagePart;
import message.Message;
import message.Payload;

import exception.MessagePartHasWrongSizeException;
import exception.MessageTooLongException;


/**
 * Provides methods for generating messages (the mixes are capable of 
 * processing) from byte arrays.
 * 
 * @author Karl-Peter Fuchs
 */
final class MessageGenerator {

	/** 
	 * Reference on component <code>InternalInformationPortController</code>. 
	 * Used to display and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = internalInformationPort.getLogger();
	
	/** Reference on component <code>NetworkClock</code>. */
	private static NetworkClockController clock = new NetworkClockController();
	
	/** Number of mixes in the cascade. */
	private final int NUMBER_OF_MIXES_IN_CASCADE;
	
	/** Used to encrypt messages. */
	private Cryptography cryptography;
	
	/** Keys for message authentication codes (MACs). */
	private Key[] macKeys;
	
	
	/**
	 * Generates a new <code>MessageGenerator</code> for generating messages 
	 * (the mixes are capable of processing) from byte arrays.
	 * 
	 * @param numberOfMixesInCascade	Number of mixes in the cascade this 
	 * 									<code>Client</code> shall use.
	 * @param cryptography				Reference on <code>Cryptography</code> 
	 * 									object that shall be used to encrypt 
	 * 									messages.
	 */
	protected MessageGenerator(	int numberOfMixesInCascade, 
								Cryptography cryptography
								) {
		
		this.NUMBER_OF_MIXES_IN_CASCADE = numberOfMixesInCascade;
		this.macKeys = new Key[numberOfMixesInCascade];
		this.cryptography = cryptography;
	}
	
	
	/**
	 * Generates a message which consists of one or more (encrypted) layers,
	 * the cascade's mixes are capable of processing from the bypassed data 
	 * (Type: <code>ChannelEstablishMessage</code>). A layer is added for each 
	 * mix in the cascade.
	 * 
	 * @param bytePayload	Data to generate the mix message from.
	 * 
	 * @return				The generated <code>ChannelEstablishMessage</code> 
	 * 						as a byte array.
	 * 
	 * @exception MessageTooLongException	Thrown when the bypassed message is 
	 * 										larger than the maximum message 
	 * 										size.
	 */
	protected byte[] generateChannelEstablishMessage(byte[] bytePayload) throws 
			MessageTooLongException {
		
		int payloadLength = 
			ChannelEstablishMessagePart.PAYLOAD.getBasicLength() 
			-
			Payload.getHeaderLength();
		
		if (bytePayload.length > payloadLength) {
			
			throw new MessageTooLongException();
			
		} else { // generate mix message
			
			/*
			 * Note: For testing only (MAKES MIX INSECURE): Use first nine 
			 * bytes of this message as identifier, which is the same for 
			 * every envelope of this mix message. Used to trace messages for 
			 * debugging.
			 */
			int messageID = 
				new BigInteger(Arrays.copyOf(bytePayload, 9)).abs().intValue();
			
			LOGGER.fine(	"(Client) Sending message with ID " 
							+messageID
							);
			
			// generate payload for last mix of cascade
			Payload payload = new Payload(new byte[payloadLength]);	
			payload.setMessage(bytePayload, payloadLength);
			
			// generate envelope for last mix of cascade
			Message envelope = 
				cryptography.encryptMessage(	
						generatePlaintextChannelEstablishMessage(
							NUMBER_OF_MIXES_IN_CASCADE,
							payload,
							messageID,
							false
							), 
						NUMBER_OF_MIXES_IN_CASCADE
						);
			
			/*
			 * Generate "new" envelope for every mix in cascade (except the last
			 * one) and put "old" envelope in "new" envelope in every cycle of 
			 * the loop.
			 */
			for ( int positionOfMixInCascade = (NUMBER_OF_MIXES_IN_CASCADE - 1);
				  positionOfMixInCascade > 0;
				  positionOfMixInCascade--
				  ) {

				/*
				 * Generate "new" plaintext envelope/message (use "old" 
				 * envelope/message as payload).
				 */
				envelope = generatePlaintextChannelEstablishMessage(
							positionOfMixInCascade,
							new Payload(envelope.getByteMessage()),
							messageID,
							true
							);

				LOGGER.finer(	"(Client) Plaintext message for " 
								+positionOfMixInCascade +". mix in "
								+"cascade: \n" +envelope
								);

				// encrypt envelope/message
				envelope = 
					cryptography.encryptMessage(	envelope, 
													positionOfMixInCascade
													);
				
			}
			
			return envelope.getByteMessage();

		}
		
	}
	
	
	/**
	 * Generates a message which consists of one or more (encrypted) layers,
	 * the cascade's mixes are capable of processing from the bypassed data 
	 * (Type: <code>ChannelMessage</code>). A layer is added for each mix in 
	 * the cascade.
	 * 
	 * @param bytePayload	Data to generate the mix message from.
	 * 
	 * @return				The generated <code>ChannelMessage</code> as a byte 
	 * 						array.
	 * 
	 * @exception MessageTooLongException	Thrown when the bypassed message is 
	 * 										larger than the maximum message 
	 * 										size.
	 */
	protected byte[] generateChannelMessage(byte[] bytePayload) throws 
			MessageTooLongException {
		
		int payloadLength = 
			ChannelMessagePart.PAYLOAD.getBasicLength()
			- 
			Payload.getHeaderLength();
		
		if (bytePayload.length > payloadLength) {
			
			throw new MessageTooLongException();
			
		} else { // generate mix message
			
			/*
			 * Note: For testing only (MAKES MIX INSECURE): Use first nine 
			 * digits of this message as identifier, which is the same for 
			 * every envelope of this mix message. Used to trace messages for 
			 * debugging.
			 */
			int messageID;
			
			if (bytePayload.length == 0) { // dummy message
				
				messageID = 333333333;
				LOGGER.fine("(Client) Sending dummy message!");
				
			} else {
				
				messageID = 
					(new Integer(new String(Arrays.copyOf(bytePayload, 9))));
				
				LOGGER.fine(	"(Client) Sending message with ID " 
								+messageID
								);
				
			}
			
			// generate payload for last mix of cascade
			Payload payload = new Payload(new byte[payloadLength]);	
			payload.setMessage(bytePayload, payloadLength);
			
			// generate envelope for last mix of cascade
			Message envelope = 
				cryptography.encryptMessage(	
						generatePlaintextChannelMessage(
							NUMBER_OF_MIXES_IN_CASCADE,
							payload,
							messageID
							), 
						NUMBER_OF_MIXES_IN_CASCADE
						);
			
			/*
			 * Generate "new" envelope for every mix in cascade (except the last
			 * one) and put "old" envelope in "new" envelope in every cycle of 
			 * the loop.
			 */
			for ( int positionOfMixInCascade = (NUMBER_OF_MIXES_IN_CASCADE - 1);
				  positionOfMixInCascade > 0;
				  positionOfMixInCascade--
				  ) {
	
				/*
				 * Generate "new" plaintext envelope/message (use "old" 
				 * envelope/message as payload).
				 */
				envelope = generatePlaintextChannelMessage(
								positionOfMixInCascade,
								new Payload(envelope.getByteMessage()),
								messageID
								);
	
				LOGGER.finer(	"(Client) Plaintext message for " 
								+positionOfMixInCascade +". mix in "
								+"cascade: \n" +envelope
								);
	
				// encrypt envelope/message
				envelope = 
					cryptography.encryptMessage(	envelope, 
													positionOfMixInCascade
													);
				
			}
			
			return envelope.getByteMessage();
	
		}
		
	}
	
	
	/**
	 * Generates a new (plaintext) <code>ChannelEstablishMessage</code> for the 
	 * specified mix. Generates message header and authentication information 
	 * as well.
	 * 
	 * @param positionOfMixInCascade	The mix' position in the cascade, the
	 * 									plaintext message shall be created for.
	 * @param payload					Payload to be embedded.			
	 * @param messageID					Note: For testing only (MAKES MIX 
	 * 									INSECURE): An identifieing number, 
	 * 									which is the same for every envelope of 
	 * 									a mix message. Used to trace messages 
	 * 									for debugging.
	 * 
	 * @return							The generated (plaintext) message.
	 */
	private Message generatePlaintextChannelEstablishMessage(	
			int positionOfMixInCascade, 
			Payload payload,
			int messageID,
			boolean removePadding
			) {
		
		int numberOfFurtherHops = 
			NUMBER_OF_MIXES_IN_CASCADE - positionOfMixInCascade;
		
		int messageLength = 
			ChannelEstablishMessagePart.getMessageLength(numberOfFurtherHops);
		
		// generate empty message
		ChannelEstablishMessage plaintextMessage = 
			new ChannelEstablishMessage(	new byte[messageLength], 
											numberOfFurtherHops
											);
		
		// add content
		try {
			
			// SESSION_KEY:
			SecretKey sessionKeyForRequestChannel = 
				cryptography.getSessionKeyOfMixForRequestChannel(positionOfMixInCascade);
			SecretKey sessionKeyForReplyChannel = 
				cryptography.getSessionKeyOfMixForReplyChannel(positionOfMixInCascade);
			
			plaintextMessage.setSessionKeyForRequestChannel(sessionKeyForRequestChannel);
			plaintextMessage.setSessionKeyForReplyChannel(sessionKeyForReplyChannel);
			
			// SESSION_IV:
			IvParameterSpec sessionIVForRequestChannel = 
				cryptography.getSessionIVOfMixForRequestChannel(positionOfMixInCascade);
			IvParameterSpec sessionIVForReplyChannel = 
				cryptography.getSessionIVOfMixForReplyChannel(positionOfMixInCascade);
			
			plaintextMessage.setSessionIVForRequestChannel(sessionIVForRequestChannel.getIV());
			plaintextMessage.setSessionIVForReplyChannel(sessionIVForReplyChannel.getIV());
			
			// MAC_KEY:
			SecretKey macKey = MacGenerator.generateMacKey();
			plaintextMessage.setMACKey(macKey);
			// save key for further messages
			macKeys[positionOfMixInCascade - 1] = macKey;
			
			// TIMESTAMP:
			plaintextMessage.setTimestamp(clock.getTime());

			// MESSAGE_ID:
			plaintextMessage.setMessageID(messageID);

			// PAYLOAD:
			plaintextMessage.setPayload(payload);
			
			// MAC:
			byte[] authData = plaintextMessage.getSignedData();
			byte[] mac = MacGenerator.generateMAC(macKey, authData);
			
			plaintextMessage.setMAC(mac);
			
		} catch (MessagePartHasWrongSizeException e) {
			
			LOGGER.severe("(Client) "+e.getMessage());
			System.exit(1);
			
		}
		
		return plaintextMessage;
		
	}
	
	
	/**
	 * Generates a new (plaintext) <code>ChannelMessage</code> for the 
	 * specified mix. Generates message header and authentication information 
	 * as well.
	 * 
	 * @param positionOfMixInCascade	The mix' position in the cascade, the
	 * 									plaintext message shall be created for.
	 * @param payload					Payload to be embedded.			
	 * @param messageID					Note: For testing only (MAKES MIX 
	 * 									INSECURE): An identifieing number, 
	 * 									which is the same for every envelope of 
	 * 									a mix message. Used to trace messages 
	 * 									for debugging.
	 * 
	 * @return							The generated (plaintext) message.
	 */
	private Message generatePlaintextChannelMessage(	
			int positionOfMixInCascade, 
			Payload payload,
			int messageID
			) {
		
		int numberOfFurtherHops = 
			NUMBER_OF_MIXES_IN_CASCADE - positionOfMixInCascade;
		
		int messageLength = 
			ChannelMessagePart.getMessageLength(numberOfFurtherHops);
		
		// generate empty message
		ChannelMessage plaintextMessage = 
			new ChannelMessage(	new byte[messageLength], 
										numberOfFurtherHops
										);
		
		// add content
		try {
			
			// MESSAGE_ID:
			plaintextMessage.setMessageID(messageID);

			// PAYLOAD:
			plaintextMessage.setPayload(payload);
			
			// MAC:
			byte[] authData = plaintextMessage.getSignedData();
			
			byte[] mac = 
				MacGenerator.generateMAC(	macKeys[positionOfMixInCascade - 1], 
											authData
											);
			
			plaintextMessage.setMAC(mac);
			
		} catch (MessagePartHasWrongSizeException e) {
			
			LOGGER.severe(e.getMessage());
			System.exit(1);
			
		}
		
		return plaintextMessage;
		
	}
	
}
