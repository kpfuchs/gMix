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
 * <code>ExternalMessage</code> used to release an existing channel.
 * 
 * @author Karl-Peter Fuchs
 */
public final class ChannelReleaseMessage extends Message implements Request, 
		ExternalMessage {
		
	/** 
	 * Identifier for this type of message. Necessary since messages are 
	 * transmitted as byte streams which don't support the 
	 * <code>instanceOf</code> operator.
	 */
	public static final byte IDENTIFIER = (byte)150;
	
	
	/**
	 * Constructs a new <code>ChannelReleaseMessage</code> for the specified 
	 * user/channel.
	 * 
	 * @param channel				User/channel the message shall be created 
	 * 								for.
	 */
	public ChannelReleaseMessage(User channel) {
		
		super(new byte[0], channel, 0, 0);
		
	}


	/**
	 * Returns a simple String representation of this object.
	 * 
	 * @return	A simple String representation of this object.
	 */
	@Override
	public String toString() {

		return "ChannelReleaseMessage (no data).\n";
		
	}
	
	
	/**
	 * Returns the message id -1111 (all <code>ChannelReleaseMessages</code> 
	 * have the same id).
	 * 
	 * @return	-1111.
	 */
	@Override
	public int getMessageID() {
		
		return -1111;
		
	}
	
}
