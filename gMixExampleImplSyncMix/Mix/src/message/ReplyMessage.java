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


import userDatabase.User;


/**
 * <code>ExternalMessage</code> used to send data to a <code>Client</code>.
 * 
 * @author Karl-Peter Fuchs
 */
public final class ReplyMessage extends Message implements Reply, 
		ExternalMessage {
		
	/** 
	 * Identifier for this type of message. Necessary since messages are 
	 * transmitted as byte streams which don't support the 
	 * <code>instanceOf</code> operator.
	 */
	public static final byte IDENTIFIER = (byte)200;
	

	/**
	 * Constructs a new <code>ReplyMessage</code> with the submitted content 
	 * (<code>byteMesssage</code>) for the bypassed user/channel.
	 * 
	 * @param byteMesssage	Byte representation of this message.
	 * @param user			User, this message belongs to.
	 */
	public ReplyMessage(	byte[] byteMesssage,
							User user
							) {
		
		super(	byteMesssage, 
				user, 
				0, 
				byteMesssage.length
				);
		
		super.setPayloadRange(0, byteMesssage.length);
		
	}
	
	
	/**
	 * Returns the message id -2222 (all <code>ReplyMessage</code> s have the 
	 * same id).
	 * 
	 * @return	-2222.
	 */
	@Override
	public int getMessageID() {
		
		return -2222;
		
	}
	
	
	/**
	 * Returns a String representation of this class.
	 * 
	 * @return	A String representation of this class.
	 */
	@Override
	public String toString() {

		return "CONTENT: " +new String(super.getByteMessage()) +"\n";
		
	}
	
}
