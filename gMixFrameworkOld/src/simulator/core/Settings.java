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

package simulator.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


public class Settings {
	
	
	public static boolean DEBUG_ON;
	
	/** <code>Properties</code> object to load values from. */
	private static Properties properties = new Properties();
 
    
    public static Properties initialize(String pathToPropertyFile) {
    	
    	try {
    		
    		properties.load(new FileInputStream(pathToPropertyFile));
    		DEBUG_ON = Settings.getProperty("DEBUG_OUTPUT").equals("ON");
			 
		} catch(IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Property file could not be loaded!");
	    }
		
		return properties;
		
    }
    
    
    /**
	 * Empty constructor. Never used since all methods are static.
	 */
    private Settings() {
		
	}
	
	
	/**
     * Searches and returns the property with the specified key in the 
     * <code>property</code> object.
     * 
     * @param key 	The property key.
     * @return 		Property with the specified key in the <code>property
     * 				</code> object.
     */
	public static String getProperty(String key) {
		
		return properties.getProperty(key);
		
	}
	
	
	public static int getPropertyAsInt(String key) {
		
		return new Integer(properties.getProperty(key));
		
	}
	

	public static double getPropertyAsDouble(String key) {
		
		return new Double(properties.getProperty(key));
		
	}
	
	
	public static boolean getPropertyAsBoolean(String key) {
		
		return properties.getProperty(key).equals("TRUE");
		
	}
	
	
	/**
     * Sets the property with the specified key in the <code>property
     * </code> object to the bypassed value.
     * 
     * @param key	The key to be placed into the property list.
     * @param value	The value corresponding to the key.
     * @return		The previous value of the specified key, or null if it did 
     * 				not have one.
     */
	public static Object setProperty(String key, String value) {
		
		return properties.setProperty(key, value);
		
	}
	
	
	/**
     * Returns this <code>Settings</code>' <code>Properties</code> object.
     * 
     * @return This <code>Settings</code>' <code>Properties</code> object.
     */
	public static Properties getProperties() {
		
		return properties;
		
	}

	
	public static String getConfig() {
		
		String config = Settings.getProperties().toString();
		config = config.replace('{', ' ');
		config = config.replace(',', '\n');
		config = config.replace(' ', ' ');
		config = config.replace('}', ' ');
		
		return config;
		
	}
	
}
