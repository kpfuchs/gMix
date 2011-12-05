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

package messageProcessor;

import message.BasicMessage;


/**
 * Detects whether a message has already been processed, or not. Detection is 
 * done using timestamps and hash tables.
 * <p>
 * This class is thread-safe (but parallel execution won't increase 
 * performance).
 * 
 * @author Karl-Peter Fuchs
 */
class ReplayDetection {


	/**
	 * Loads values from property file and initializes hash tables.
	 */
	protected ReplayDetection() {
			
	}
	
	
	/**
	 * Detects whether a message has already been processed, or not. Detection 
	 * is done using timestamps and hash tables.
	 * 
	 * @param basicMessage	The message to be checked.
	 * 
	 * @return				Indicates whether the bypasses message is a replay  
	 * 						or not.
	 */
	protected boolean isReplay(BasicMessage basicMessage) {

		return false; // TODO: implement
		
	}

}