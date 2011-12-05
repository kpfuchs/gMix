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


import java.util.Random;


/**
 * Enumeration used to identify different types of information, that can be 
 * received and/or provided using an <code>ExternalInformationPort</code> 
 * component. 
 * 
 * @author Karl-Peter Fuchs
 * 
 * @see Information
 * 
 */
public enum Information {
	
	/** This mix' public key. */
	PUBLIC_KEY,
	
	/** Key used to encrypt data between this mix and its predecessor. */
	INTER_MIX_KEY,
	
	/** 
	 * Initialization vector used to encrypt data between this mix and its 
	 * predecessor.
	 */
	INTER_MIX_IV,
	
	/** The next mix' address. */
	NEXT_MIX_ADDRESS,
	
	/** Port number the next mix' <code>InformationProvider</code> runs on. */
	NEXT_MIX_INFO_PORT,
	
	/** 
	 * Indicates that the requested <code>Information</code> is not available.
	 */
	NOT_AVAILABLE;
	
	/** 
	 * Identifieing number for this <code>Information</code> (automatically 
	 * generated).
	 */
	private int identifier;
	
	
	/* Generate identifieing number for each information. */
	static {
		
		Random random = new Random(123); // use fixed seed
		
		for (Information info : Information.values()) {
			
			info.identifier = random.nextInt();;
			
		}
		
	}
	
	
	/** 
	 * Returns this <code>Information</code>'s identifieing number.
	 * 
	 * @return	This <code>Information</code>'s identifieing number.
	 */
	public int getIdentifier() {
		
		return this.identifier;
		
	}

}
