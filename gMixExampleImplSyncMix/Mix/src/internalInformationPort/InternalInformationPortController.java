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

package internalInformationPort;


import java.util.logging.Logger;

import architectureInterface.InternalInformationPortInterface;


/**
 * Controller class of component <code>InternalInformationPort</code>. 
 * Implements the architecture interface 
 * <code>InternalInformationPortInterface</code>.
 * <p>
 * Used for information exchange (for example via log files (output), 
 * property files (input) or display output) with "internal" communication 
 * partners (usually the system administrator).
 * 
 * @author Karl-Peter Fuchs
 */
public class InternalInformationPortController implements 
		InternalInformationPortInterface {
	
	
	/**
	 * Generates a new <code>InternalInformationPort</code> component, used for 
	 * information exchange (e. g. generating log files, reading settings or 
	 * displaying status information) with "internal" communication partners 
	 * (usually the system administrator).
	 * 
	 * @see #initialize()
	 */
	public InternalInformationPortController() {
		
	}
	
	
	/**
	 * Empty initializer.
	 */
	public void initialize() {
		
	}
	
	
	/**
	 * Returns a <code>Logger</code> that can be used to log messages in a file 
	 * and/or display them on the standard output (depending on the log <code>
	 * Level</code>s, specified in property file). 
	 * 
	 * @return	<code>Logger</code> that can be used to log messages in a file 
	 * 			and/or display them on the standard output.
	 */
	public Logger getLogger() {
		
		return LogFileGenerator.getLogger();
		
	}
	
	
	/**
     * Returns the property with the specified key from the property file.
     * 
     * @param key 	The property key.
     * 
     * @return 		The property with the specified key in the property file.
     */
	public String getProperty(String key) {
		
		return Settings.getProperty(key);
		
	}
	
	
	/**
     * Sets the property with the specified key to the bypassed value.
     * 
     * @param key	Identifier of the property.
     * @param value	The value corresponding to the key.
     * 
     * @return		The previous value of the specified key, or null if it did 
     * 				not have one.
     */
	public Object setProperty(String key, String value) {
		
		return Settings.setProperty(key, value);
		
	}

}
