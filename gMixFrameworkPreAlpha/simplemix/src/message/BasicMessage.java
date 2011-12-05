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
 * Interface any type of message the <code>InputOutputHandler</code> shall be 
 * capable of transmitting must implement. 
 * 
 * @author Karl-Peter Fuchs
 */
public interface BasicMessage {

	
	/**
	 * Must return an identifier for this message.
	 * 
	 * @return	Identifier for this message.
	 */
	public int getMessageID();
	
	
	/**
	 * Must set this message's identifier to the bypassed value.
	 * 
	 * @param newIdentifier	Value this message's identifier shall be set to.
	 */
	public void setMessageID(int newIdentifier);
	
	
	/**
	 * Must return an identifier for the channel/user this message belongs to.
	 * 
	 * @return	Identifier for the channel/user this message belongs to.
	 */
	public int getChannelID();
	
	
	/**
	 * Must return an identifier for the channel/user this message belongs to, 
	 * used by the next mix of the cascade.
	 * 
	 * @return	Identifier for the channel/user this message belongs to, used 
	 * 			by the next mix of the cascade.
	 */
	public int getNextMixChannelID();
	
	
	/**
	 * Must return a reference on the channel/user this message belongs to.
	 * 
	 * @return	Reference on the channel/user this message belongs to.
	 */
	public User getChannel(); // TODO: wieder einführen
	
	
	/** 
	 * Only a certain part of the message array must be transmitted to 
	 * the cascade's next mix (Some parts (e. g. the session key for the 
	 * corresponding mix) are not supposed to be sent). Which part is relevant 
	 * depends on the type of message.
	 * <p>
	 * This method must define the offset and length of the part to be 
	 * transmitted.
	 * 
	 * @param	startIndexOfPayload	Offset of the part to be transmitted.
	 * @param	payloadLength		Length of the part to be transmitted.
	 */
	public void setPayloadRange(	int startIndexOfPayload,
									int payloadLength
									);
	
	
	/** 
	 * Must return the point of time (timestamp), this message was received.
	 * 
	 * @return	Point of time (timestamp), this message was received.
	 */
	public long getTimeOfReceival();
	
	
	/** 
	 * Must set the point of time (timestamp), this message was received to the 
	 * bypassed value.
	 * 
	 * @param newTimeOfReceival	Point of time (timestamp), this message was 
	 * 							received.
	 */
	public void setTimeOfReceival(long newTimeOfReceival);
	
	
	/**
	 * Must return the byte representation of this message.
	 * 
	 * @return Byte representation of this message.
	 */
	public byte[] getByteMessage();
	
	
	/**
	 * Sets the byte representation of this message to the bypassed array.
	 * 
	 * @param byteMessage	New content for this message.
	 */
	public void setByteMessage(byte[] byteMessage);
	
}
