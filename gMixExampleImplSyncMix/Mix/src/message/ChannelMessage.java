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

package message;


import internalInformationPort.InternalInformationPortController;

import java.util.Arrays;
import java.util.logging.Logger;

import exception.MessagePartHasWrongSizeException;

import userDatabase.User;

import util.Util;


/**
 * <code>ExternalMessage</code> used to transmit data on an existing channel. 
 * The parts this message consists of are defined in the enumeration 
 * <code>ChannelMessagePart</code>. A pair of set and get methods 
 * is present for each defined part.
 * 
 * @author Karl-Peter Fuchs
 */
public final class ChannelMessage extends Message implements Request, 
		ExternalMessage {
	
	/** 
	 * Reference on component <code>InternalInformationPort</code>. 
	 * Used to display and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = internalInformationPort.getLogger();
	
	/** 
	 * Identifier for this type of message. Necessary since messages are 
	 * transmitted as byte streams which don't support the 
	 * <code>instanceOf</code> operator.
	 */
	public static final byte IDENTIFIER = (byte)100;
	
	/** 
	 * Number of further hops (= mixes), this message must pass to reach its 
	 * destination. Used to find a <code>MessagePart</code>'s position in the 
	 * <code>byteMessage</code> which has (a) dynamic start- and/or 
	 * end-position(s). Note: Message length isn't constant in a cascade.
	 * 
	 * @see Message#byteMessage
	 * @see ChannelEstablishMessagePart
	 */
	private final int NUMBER_OF_FURTHER_HOPS;
	
	
	/**
	 * Message constructor used by a mix.
	 * <p>
	 * Constructs a new <code>ChannelMessage</code> with the submitted 
	 * content (<code>byteMesssage</code>) for the specified user/channel.
	 * 
	 * @param byteMesssage			Byte representation of the message (as 
	 * 								received from the client/previous mix).
	 * @param channel				User/channel the message shall be created 
	 * 								for.
	 * @param numberOfFurtherHops	Number of further hops (= mixes), this 
	 * 								message must pass.
	 */
	public ChannelMessage(	byte[] byteMesssage,
							User channel,
							int numberOfFurtherHops
							) {
		
		super(	byteMesssage, 
				channel,
				ChannelMessagePart.PAYLOAD.
						getStartPosition(numberOfFurtherHops), 
				ChannelMessagePart.PAYLOAD.
						getLength(numberOfFurtherHops)
				);
		 
		this.NUMBER_OF_FURTHER_HOPS = numberOfFurtherHops;
		
	}
	
	
	/**
	 * Message constructor used by client.
	 * <p>
	 * Constructs a new <code>ChannelMessage</code> with the submitted content 
	 * (<code>byteMesssage</code>).
	 * 
	 * @param byteMesssage			Byte representation of the message (can be 
	 * 								empty).
	 * @param numberOfFurtherHops	Number of mixes in the cascade.
	 */
	public ChannelMessage(byte[] byteMesssage, int numberOfFurtherHops) {

		super(byteMesssage);
		this.NUMBER_OF_FURTHER_HOPS = numberOfFurtherHops;

	}
	
	
	/**
	 * Returns the <code>MessagePart</code> <code>MAC</code> = The Message 
	 * Authentication Code (MAC) of this message.
	 * 
	 * @return <code>MessagePart</code> <code>MAC</code>.
	 * 
	 * @see ChannelMessagePart
	 * @see ChannelMessagePart#MAC
	 */
	public byte[] getMAC() {
		
		return getPart(ChannelMessagePart.MAC);
		
	}
	
	
	/**
	 * Sets the <code>MessagePart</code> <code>MAC</code> to the 
	 * bypassed value.
	 * 
	 * @param mac The Message Authentication Code (MAC) of this message.
	 * 
	 * @see ChannelMessagePart
	 * @see ChannelMessagePart#MAC
	 */
	public void setMAC(byte[] mac) throws MessagePartHasWrongSizeException {
		
		setPart(ChannelMessagePart.MAC, mac);
		
	}
	
	
	/**
	 * Returns the <code>MessagePart</code> <code>MESSAGE_ID</code> = An 
	 * identifieing number, which is the same for every envelope of a mix 
	 * message. Used to trace messages for debugging. Can only be read 
	 * correctly, when current envelope is decrypted! Note: For testing only 
	 * (MAKES MIX INSECURE)!
	 * 
	 * @return <code>MessagePart</code> <code>MESSAGE_ID</code>.
	 * 
	 * @see ChannelMessagePart
	 * @see ChannelMessagePart#MESSAGE_ID
	 */
	@Override
	public int getMessageID() {
		
		return Util.byteArrayToInt(
					getPart(ChannelMessagePart.MESSAGE_ID)
					);
		
	}
	
	
	/**
	 * Sets the <code>MessagePart</code> <code>MESSAGE_ID</code> to the 
	 * bypassed value.
	 * 
	 * @param newID	An identifieing number, which is the same for every 
	 * 				envelope of a mix message. Used to trace messages for 
	 * 				debugging. Can only be set correctly, when current envelope 
	 * 				is decrypted! Note: For testing only (MAKES MIX INSECURE)!
	 * 
	 * @see ChannelMessagePart
	 * @see ChannelMessagePart#MESSAGE_ID
	 */
	@Override
	public void setMessageID(int newID) {
		
		try {
			
			setPart(	ChannelMessagePart.MESSAGE_ID,
						Util.intToByteArray(newID)
						);
			
		} catch (MessagePartHasWrongSizeException e) {

			LOGGER.warning(e.getMessage());
			
		}
		
	}
	
	
	/**
	 * Returns the <code>MessagePart</code> <code>PAYLOAD</code>.
	 * 
	 * @return <code>MessagePart</code> <code>PAYLOAD</code>.
	 * 
	 * @see ChannelMessagePart
	 * @see ChannelMessagePart#PAYLOAD
	 */
	public Payload getPayload() {
		
		return new Payload(getPart(ChannelMessagePart.PAYLOAD));

	}
	
	
	/**
	 * Sets the <code>MessagePart</code> <code>PAYLOAD</code> to the 
	 * bypassed value.
	 * 
	 * @param newPayload	Payload to be set.
	 * 
	 * @see ChannelMessagePart
	 * @see ChannelMessagePart#PAYLOAD
	 */
	public void setPayload(Payload newPayload) throws 
			MessagePartHasWrongSizeException {
			
		setPart(	ChannelMessagePart.PAYLOAD, 
					newPayload.getBytePayload()
					);
		
	}
	
	
	/**
	 * Returns the <code>MessagePart</code> <code>SIGNED_DATA</code>.
	 * 
	 * @return <code>MessagePart</code> <code>SIGNED_DATA</code>.
	 * 
	 * @see ChannelMessagePart
	 * @see ChannelMessagePart#SIGNED_DATA
	 */
	public byte[] getSignedData() {
		
		return getPart(ChannelMessagePart.SIGNED_DATA);

	}
	
	
	/**
	 * Sets the <code>MessagePart</code> <code>SIGNED_DATA</code> to the 
	 * bypassed value.
	 * 
	 * @param newSignedData	The signed data to be set.
	 * 
	 * @see ChannelMessagePart
	 * @see ChannelMessagePart#SIGNED_DATA
	 */
	public void setSignedData(byte[] newSignedData) throws 
			MessagePartHasWrongSizeException {
			
		setPart(ChannelMessagePart.SIGNED_DATA, newSignedData);
		
	}
	
	
	/**
	 * Returns a byte representation of a <code>MessagePart</code>. Example of 
	 * usage: <code>getPart(ChannelMessagePart.NAME_OF_ENUM_CONSTANT);</code>
	 * 
	 * @param messagePart	Enum constant who's byte representation shall be 
	 * 						retrieved (= "type" of the part).
	 * @return				Byte representation of the <code>MessagePart</code> 
	 * 						suiting the bypassed enum constant.
	 * 
	 * @see ChannelMessagePart
	 */
	private byte[] getPart(ChannelMessagePart messagePart) {
			/* 
			 * MessagePart is used to find the correct positions in 
			 * byteMessage.
			 */

		// return the corresponding part of the byte message
		return Arrays.copyOfRange(
				super.getByteMessage(), 
				messagePart.getStartPosition(NUMBER_OF_FURTHER_HOPS),
				messagePart.getEndPosition(NUMBER_OF_FURTHER_HOPS) + 1 
				);
			

	}
	

	/**
	 * Saves a byte representation of a <code>MessagePart</code>. Example of 
	 * usage: 
	 * <code>setPart(ChannelMessagePart.NAME_OF_ENUM_CONSTANT, data);
	 * </code>
	 * 
	 * @param messagePart	Enum constant who's byte representation shall be 
	 * 						saved.
	 * @param data			Byte representation of the <code>MessagePart</code> 
	 * 						to be saved.
	 * @exception MessagePartHasWrongSizeException Thrown when a <code>
	 * 						MessagePart</code> that shall be assigned is of 
	 * 						wrong size.
	 * 
	 * @see ChannelMessagePart
	 */
	private void setPart(	ChannelMessagePart messagePart,
							byte[] data
							) throws MessagePartHasWrongSizeException {
			/* 
			 * MessagePart is used to find the correct positions in 
			 * byteMessage.
			 */		

		if (data.length != messagePart.getLength(NUMBER_OF_FURTHER_HOPS)) {
			
			throw new MessagePartHasWrongSizeException();
			
		}
		
		// copy submitted data to byteMessage array
		int startPosition 
			= messagePart.getStartPosition(NUMBER_OF_FURTHER_HOPS);

		for (int i=0; i<data.length; i++) {
			
			super.getByteMessage()[startPosition+i] = data[i];
			
		}
		
	}

	
	/**
	 * Returns a simple String representation of this class featuring several 
	 * message parts. Note: Output is only correct for decrypted messages.
	 * 
	 * @return	A simple String representation of this class featuring several 
	 * 			message parts
	 */
	@Override
	public String toString() {
		
		String output = "";
		output += "MESSAGE_ID: " +getMessageID() +"\n";
		output += "MAC: " +new String(getMAC()) +"\n";
		output += "PAYLOAD: " +new String(getPayload().getBytePayload()) +"\n";

		return output;
	}
}
