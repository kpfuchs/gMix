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

package accessControl;


import message.Message;

import architectureInterface.AccessControlInterface;


/**
 * Controller class of component <code>AccessControl</code>. Implements  
 * the architecture interface <code>AccessControlInterface</code>.
 * <p>
 * Used for an integrity check, based on message authentication codes (MAC).
 * 
 * @author Karl-Peter Fuchs
 */
public class AccessControlController implements AccessControlInterface {

	
	/**
	 * Generates a new <code>AccessControl</code> component which can be used 
	 * for an integrity check, based on message authentication codes (MAC).
	 * <p>
	 * Empty constructor.
	 */
	public AccessControlController() {
		
		
	}
	
	
	/**
	 * Initializes the this component.
	 * <p>
	 * Empty initializer.
	 */
	public void initialize() {
		
		
	}
	
	
	/**
	 * Performs an integrity check on the bypassed message.
	 * @param message The message to be checked.
	 * @return Result of integrity check.
	 */
	public boolean isMACCorrect(Message message) {

		return IntegrityCheck.isMACCorrect(message);
		
	}

}
