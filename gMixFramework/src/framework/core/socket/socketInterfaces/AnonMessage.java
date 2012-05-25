/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2012  Karl-Peter Fuchs
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
package framework.core.socket.socketInterfaces;

import framework.core.userDatabase.User;

public class AnonMessage {

	public static final int NOT_SET = -1;
	
	//private int endToEndPseudonym = NOT_SET;
	private int sourcePseudonym = NOT_SET;
	private int sourcePort = NOT_SET;
	private int destinationPseudonym = NOT_SET;
	private int destinationPort = NOT_SET;
	private int maxReplySize = NOT_SET;
	
	private byte[] message;
	private User user;
	
	
	public AnonMessage(byte[] message) {
		this.message = message;
	}


	/**
	 * @return the endToEndPseudonym
	 */
	//public int getEndToEndPseudonym() {
	//	return endToEndPseudonym;
	//}


	/**
	 * @param endToEndPseudonym the endToEndPseudonym to set
	 */
	//public void setEndToEndPseudonym(int endToEndPseudonym) {
	//	this.endToEndPseudonym = endToEndPseudonym;
	//}


	/**
	 * @return the sourcePseudonym
	 */
	public int getSourcePseudonym() {
		return sourcePseudonym;
	}


	/**
	 * @param sourcePseudonym the sourcePseudonym to set
	 */
	public void setSourcePseudonym(int sourcePseudonym) {
		this.sourcePseudonym = sourcePseudonym;
	}


	/**
	 * @return the sourcePort
	 */
	public int getSourcePort() {
		return sourcePort;
	}


	/**
	 * @param sourcePort the sourcePort to set
	 */
	public void setSourcePort(int sourcePort) {
		this.sourcePort = sourcePort;
	}


	/**
	 * @return the destinationPseudonym
	 */
	public int getDestinationPseudonym() {
		return destinationPseudonym;
	}


	/**
	 * @param destinationPseudonym the destinationPseudonym to set
	 */
	public void setDestinationPseudonym(int destinationPseudonym) {
		this.destinationPseudonym = destinationPseudonym;
	}


	/**
	 * @return the destinationPort
	 */
	public int getDestinationPort() {
		return destinationPort;
	}


	/**
	 * @param destinationPort the destinationPort to set
	 */
	public void setDestinationPort(int destinationPort) {
		this.destinationPort = destinationPort;
	}


	/**
	 * @return the message
	 */
	public byte[] getByteMessage() {
		return message;
	}


	/**
	 * @param message the message to set
	 */
	public void setByteMessage(byte[] message) {
		this.message = message;
	}


	public User getUser() {
		return user;
	}


	public void setUser(User user) {
		this.user = user;
	}


	public int getMaxReplySize() {
		return maxReplySize;
	}


	public void setMaxReplySize(int maxReplySize) {
		this.maxReplySize = maxReplySize;
	}



	
}
