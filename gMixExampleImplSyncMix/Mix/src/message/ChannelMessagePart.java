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

import java.util.logging.Logger;


/**
* Defines the message format of a mix message (<code>Message</code>) by 
* describing the parts it consists of. Common message parts are for example 
* "destination address" and "payload". The entire message format can be 
* specified within the enum fields. Any <code>MessagePart</code> can be 
* retrieved or manipulated using the methods <code>getPart(MessagePart 
* messagePart)</code> and <code>setPart(MessagePart messagePart, byte[] data)
* </code> in class <code>Message</code>. Overlapping MessageParts are supported 
* (e. g. a field "asymmetric part", which consists of several message parts 
* itself). If a <code>MessagePart</code> is of dynamic length (= its length 
* depends on the current mix' position in the cascade (Note: Message length 
* isn't constant in a cascade)), the correct length is automatically calculated 
* returned.
* <p>
* Note: A <code>MessagePart</code> is "dynamic", when its start and/or end 
* position (=index) is/are equal to, or behind the end position of the last 
* mix' payload (since the payload's size differs from mix to mix).
* <p>
* Each <code>MessagePart</code> provides the following information: It's start 
* position (= index) in the byte array of class <code>Message</code>, it's end 
* position in the same class and its length.
* <p>
* See <a href="http://java.sun.com/docs/books/tutorial/java/javaOO/enum.html">
* Enum Types Tutorial</a> for information on how to use enumerations in Java.
* 
* @author Karl-Peter Fuchs
* 
* @see Message
*/

/**
* Defines the message format/message parts of a <code>ChannelMessage</code>. 
* Common message parts are for example "message authentication code" and 
* "payload". The entire message format can be specified within the enum fields. 
* Any <code>ChannelMessagePart</code> can be retrieved or manipulated using the 
* methods <code>getPart(ChannelMessagePart messagePart)</code> and 
* <code>setPart(ChannelMessagePart messagePart, byte[] data)</code> in class 
* <code>ChannelMessage</code>.
* <p>
* Overlapping message parts are supported (e. g. a field "signed data", 
* which consists of several message parts itself). If a 
* <code>ChannelMessagePart</code> is of dynamic length (= its length depends on 
* the current mix' position in the cascade (Note: Message length isn't constant 
* in a cascade)), the correct length is calculated automatically.
* <p>
* Note: A <code>MessagePart</code> is "dynamic", when its start and/or end 
* position (=index) is/are equal to, or behind the end position of the last 
* mix' payload (since the payload's size differs from mix to mix (and 
* therefore is a dynamic part as well)).
* <p>
* Each <code>MessagePart</code> provides the following information: It's start 
* offset (= index), it's end offset and whether the part is "part of the 
* header" or not. A <code>MessagePart</code> is considered a "part of the 
* header", when it is exclusive for each mix of the cascade and therefore will 
* be dropped by the corresponding mix. For example a "message authentication 
* code" is "part of the header", since it must be different for each mix and 
* secret to all others (and therefore won't be sent forward). In contrast, the 
* "payload" is not "part of the header", since it is sent from one mix to 
* another (after recoding). Overlapping message parts can't be headers, of 
* course. 
* <p>
* See <a href="http://java.sun.com/docs/books/tutorial/java/javaOO/enum.html">
* Enum Types Tutorial</a> for information on how to use enumerations in Java.
* <p>
* Note: This enumeration contains the field "MESSAGE_ID" which makes messages 
* TRACEABLE (for debugging)!
* 
* @author Karl-Peter Fuchs
* 
* @see Message
*/
public enum ChannelMessagePart {
	
	// The fields of this enumeration:
 
	// header fields:
	/** The message authentication code (MAC) of this message. */
	MAC					(   0,   31,  true),
	
	/** 
	 * Note: For testing only (MAKES MIX INSECURE): An identifieing number, 
	 * which is the same for every envelope of a mix message. Used to trace 
	 * messages for debugging. Can (as all other header-field) only be read 
	 * correctly, when current envelope is decrypted!
	 */
	MESSAGE_ID			(  32,   35,  true),
	
	/** 
	 * Reserved space (used to fill up header's size to a multiple of the block 
	 * size).
	 */
	RESERVED			(  36,   47,  true),
	
	
	// none-header fields:
	/** 
	 * The payload.
	 * 
	 * @see Payload
	 */
	PAYLOAD				(  48, 1103, false),
	
	/** The signed data of this message. */
	SIGNED_DATA			(  32, 1103, false);
	
	
	/** 
	 * Reference on component <code>InternalInformationPort</code>. 
	 * Used to display and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = internalInformationPort.getLogger();
	
	/**
	 * The end position (=index) of the last mix' payload (Note: Message length 
	 * isn't constant in a cascade). Used to calculate dynamic message lengths. 
	 * Must be set to the same value as the highest end position of all message 
	 * parts.
	 * <b>Note: <code>basicEndPositionOfPayload + (numberOfMixesInCascade * 
	 * hesaderSize)</code> should be smaller than the Maximum Transmission Unit 
	 * of the underlying network for performance reasons!</b>
	 */
	private final static int basicEndPositionOfPayload = 1103;
	
	/**
	 * The length of a mix message, as it arrives at the last mix of a cascade 
	 * (in byte). Used as starting point to calculate mix message lengths and 
	 * start- and end positions of dynamic message parts according to the 
	 * calling mix' position in the cascade (Note: there's no fixed message 
	 * length in a cascade).
	 */
	private static final int basicMessageLength;
	
	/**
	 * The length of a <code>Message</code>'s header as defined above (in 
	 * byte). Used as incrementation factor to calculate a calling mix' message 
	 * length according to it's position in the cascade (Note: there's no fixed 
	 * message length in a cascade).
	 */
	private static final int headerLength;
	
	
	/* 
	 * Calculates basicMessageLength and headerLength. Checks if headerLength 
	 * is a multiple of the block size.
	 */
	static {
		
		// calculate basicMessageLength
		int length = 0;
		
		for (ChannelMessagePart messagePart: values()) {
			
			if ((messagePart.getEndPosition(0) + 1) > length) {
				
				length = (messagePart.getEndPosition(0) + 1);
				
			}
			
		}
		
		basicMessageLength = length;
		
		// calculate headerLength
		length = 0;
		
		for (ChannelMessagePart messagePart: values()) {
			
			if (messagePart.isPartOfHeader) {
				
				length += messagePart.getLength(0);

			}
			
		}
		
		headerLength = length;

		// check if headerLength is a multiple of the block size
		int blockSize = 
			new Integer(
				internalInformationPort.getProperty(
						"SYMMETRIC_CYPHER_BLOCK_SIZE"
					)
				);
		
		if ((headerLength % blockSize) != 0) {
				
			LOGGER.severe(	"(MIX-MP) The accumulated length of all " 
							+"header parts is not a multiple of the " 
							+"symmetric cryptography algorithm's " 
							+"block size (" +blockSize +")! \n See " 
							+"class \"MessagePart\" for further " 
							+"information."
							);
				
			System.exit(1);
				
		}
		
	}
	
	/**
	 * Basic start position (= offset) of this <code>MessagePart</code>. 
	 * "Basic" means, the start position is only valid for the last mix of a 
	 * cascade. Dynamic <code>MessagePart</code>s need additional calculation 
	 * (which is done automatically).
	 */
	private final int basicStartPosition;
	
	/**
	 * Basic end position (= offset) of this <code>MessagePart</code>. "Basic" 
	 * means, the end position is only valid for the last mix of a cascade. 
	 * Dynamic <code>MessagePart</code>s need additional calculation (which is 
	 * done automatically).
	 */
	private final int basicEndPosition;
	
	/** 
	 * Basic length of this <code>MessagePart</code> in byte. "Basic" means, 
	 * the length is only valid for the last mix of a cascade. Dynamic 
	 * <code>MessagePart</code>s need additional calculation (which is done 
	 * automatically).
	 * 
	 * @see #getLength(int)
	 * @see #hasDynamicStartPosition
	 * @see #hasDynamicEndPosition
	 */
	private final int basicLength;
	
	/** 
	 * Indicates whether this <code>MessagePart</code> is part of a mix 
	 * message's header or not (used to calculate appropriate message lengths 
	 * for a mix' cascade position).
	 * <p>
	 * A <code>MessagePart</code> is considered as "part of the header", when 
	 * it is exclusive for each mix of the cascade and therefore will be 
	 * dropped by the corresponding mix. For example a "message authentication 
	 * code" is "part of the header", since it must be different for each mix 
	 * and secret to all others (and therefore won't be sent forward). In 
	 * contrast, the "payload" is not "part of the header", since it is sent 
	 * from one mix to another (after recoding). Overlapping message parts 
	 * can't be headers, of course.
	 */
	private final boolean isPartOfHeader;
	
	/** 
	 * Indicates whether this <code>MessagePart</code>'s start position depends 
	 * on the calling mix' position in the cascade or not. Note: there's no 
	 * fixed message length in a cascade).
	 */
	private final boolean hasDynamicStartPosition;

	/** 
	 * Indicates whether this <code>MessagePart</code>'s end position depends 
	 * on the calling mix' position in the cascade or not. Note: there's no 
	 * fixed message length in a cascade).
	 */
	private final boolean hasDynamicEndPosition;
	
	/** 
	 * Indicates whether this <code>MessagePart</code>'s length or position
	 * depends on the calling mix' position in the cascade or not. Note: 
	 * there's no fixed message length in a cascade).
	 * @see #hasDynamicStartPosition
	 * @see #hasDynamicEndPosition
	 */
	private final boolean isDynamicPart;
	
	
	/**
	 * Creates a new <code>ChannelMessagePart</code>, saves the bypassed values 
	 * and calculates and saves it's basic length.
	 * 
	 * @param basicStartPosition	Basic start position (= offset) of this 
	 * 								<code>MessagePart</code>. "Basic" means, 
	 * 								the start position is only valid for the 
	 * 								last mix of a cascade. Dynamic 
	 * 								<code>MessagePart</code>s need additional 
	 * 								calculation (which is done automatically).
	 * @param basicEndPosition		Basic end position (= offset) of this 
	 * 								<code>MessagePart</code>. "Basic" means, 
	 * 								the end position is only valid for the 
	 * 								last mix of a cascade. Dynamic 
	 * 								<code>MessagePart</code>s need additional 
	 * 								calculation (which is done automatically).
	 * @param isPartOfHeader		Indicates whether this 
	 * 								<code>MessagePart</code> is part of a mix 
	 * 								message's header or not (used to calculate 
	 * 								appropriate message lengths for a mix' 
	 * 								cascade position).
	 * 								<p>
	 * 								A <code>MessagePart</code> is considered a 
	 * 								"part of the header", when it is exclusive 
	 * 								for each mix of the cascade and therefore 
	 * 								will be dropped by the corresponding mix. 
	 * 								For example a "message authentication code" 
	 * 								is "part of the header", since it must be 
	 * 								different for each mix and secret to all 
	 * 								others (and therefore won't be sent 
	 * 								forward). In contrast, the "payload" is not 
	 * 								"part of the header", since it is sent from 
	 * 								one mix to another (after recoding). 
	 * 								Overlapping message parts can't be headers, 
	 * 								of course.
	 */
	private ChannelMessagePart(	int basicStartPosition, 
										int basicEndPosition, 
										boolean isPartOfHeader
										) {
		
		this.basicStartPosition = basicStartPosition;
		this.basicEndPosition = basicEndPosition;
		this.basicLength = (basicEndPosition - basicStartPosition + 1);
		this.isPartOfHeader = isPartOfHeader;
		
		this.hasDynamicStartPosition = 
			(basicStartPosition >= basicEndPositionOfPayload)
			? true 
			: false;
		
		this.hasDynamicEndPosition = 
			(basicEndPosition >= basicEndPositionOfPayload)
			? true 
			: false;
		
		this.isDynamicPart = 
			(this.hasDynamicStartPosition || this.hasDynamicEndPosition);
		
	}
	
	
	/**
	 * Returns this <code>MessagePart</code>'s (possibly dynamic) start 
	 * position (= offset), according to the number of further hops (=number of 
	 * mixes this <code>MessagePart</code>'s <code>Message</code> still must 
	 * come to pass).
	 * <p> 
	 * Note: Every further hop needs it's own header. Therefore, the message 
	 * size is not constant.
	 * 
	 * @param numberOfFurtherHops	The number of further hops (=number of 
	 * 								mixes this <code>MessagePart</code>'s 
	 * 								<code>Message</code> still must come to 
	 * 								pass).
	 * @return						This <code>MessagePart</code>'s start 
	 * 								position (= offset).
	 */
	public int getStartPosition(int numberOfFurtherHops) {
		
		if (!this.hasDynamicStartPosition) { // "static" parts are always at the
											 // same position
			
			return this.basicStartPosition;
			
		} else { // calculate dynamic start position
			
			int result = this.basicStartPosition;
			
			result += (	numberOfFurtherHops 
						* 
						ChannelMessagePart.getHeaderLength()
						);
			
			return result;
			
		}
		
	}
		
		
	/**
	 * Returns this <code>MessagePart</code>'s (possibly dynamic) end position 
	 * (= offset), according to the number of further hops (=number of mixes 
	 * this <code>MessagePart</code>'s <code>Message</code> still must come to 
	 * pass). 
	 * <p>
	 * Note: Every further hop needs it's own header. Therefore, the message 
	 * size is not constant.
	 * 
	 * @param numberOfFurtherHops	The number of further hops (=number of 
	 * 								mixes this <code>MessagePart</code>'s 
	 * 								<code>Message</code> still must come to 
	 * 								pass).
	 * @return						This <code>MessagePart</code>'s end 
	 * 								position (= offset).
	 */
	public int getEndPosition(int numberOfFurtherHops) {
		
		if (!this.hasDynamicEndPosition) { // "static" parts are always at the 
										   // same position

			return this.basicEndPosition;

		} else { // calculate dynamic end position

			int result = this.basicEndPosition;
			
			result += (	numberOfFurtherHops 
						* 
						ChannelMessagePart.getHeaderLength()
						);

			return result;

		}
		
	}
		
		
	/**
	 * Returns the length of this (possibly dynamic) <code>MessagePart</code> 
	 * (in byte), according to the number of further hops (=number of mixes 
	 * this <code>MessagePart</code>'s <code>Message</code> still must come to 
	 * pass). Note: Every further hop needs it's own header. Therefore, the 
	 * message size is not constant.
	 * 
	 * @param numberOfFurtherHops	The number of further hops (=number of 
	 * 								mixes this <code>MessagePart</code>'s 
	 * 								<code>Message</code> still must come to 
	 * 								pass).
	 * @return 						The length of this <code>MessagePart</code> 
	 * 								in byte.
	 */
	public int getLength(int numberOfFurtherHops) {
		
		if (!this.isDynamicPart) { // "static" parts have always the same length

			return this.basicLength;

		} else { // calculate dynamic length
			
			// get start and end position
			int startPosition = getStartPosition(numberOfFurtherHops);
			int endPosition = getEndPosition(numberOfFurtherHops);
			
			return (endPosition - startPosition + 1);

		}
		
	}
	
	
	/** 
	 * Returns the "basic" length of this <code>MessagePart</code> in byte. 
	 * "Basic" means, the length is only valid for the last mix of a cascade.
	 * 
	 * @return The "basic" length of this <code>MessagePart</code> in byte.
	 */
	public int getBasicLength() {
		
		return this.basicLength;
		
	}
	
	
	/**
	 * Returns the (dynamic) length of a mix message, according to the 
	 * specified mix position in the cascade (in byte). (Note: there's no fixed 
	 * message length in a cascade).
	 * 
	 * @param numberOfFurtherHops	The number of further hops (=number of 
	 * 								mixes this <code>MessagePart</code>'s 
	 * 								<code>Message</code> still must come to 
	 * 								pass).
	 * @return						The (dynamic) length of a mix message for 
	 * 								the specified mix position in the cascade.
	 */
	public static int getMessageLength(int numberOfFurtherHops) {
		
		return (basicMessageLength + (headerLength * numberOfFurtherHops));
		
	}
	
	
	/**
	 * Returns the length of a <code>Message</code>'s header as defined above 
	 * (see static initializer) in byte. Used as incrementation factor to 
	 * calculate a calling mix' message length according to it's position in 
	 * the cascade (Note: there's no fixed message length in a cascade).
	 * 
	 * @return	The length of a <code>Message</code>'s header (in byte).
	 */
	public static int getHeaderLength() {
		
		return headerLength;
		
	}
	
}