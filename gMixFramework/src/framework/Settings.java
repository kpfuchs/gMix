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

package framework;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;


public class Settings {
	
	private Properties properties;
	
	
    public Settings(String pathToPropertyFile) {
		this.properties = new Properties();
		try {
    		properties.load(new FileInputStream(pathToPropertyFile));	 
		} catch(IOException e) {
			e.printStackTrace();
			throw new RuntimeException("property file could not be loaded!");
	    }
	}
    
  
    public Properties getPropertiesObject() {
    	return this.properties;
    }
	
    
    public String getProperty(String key) {
		try {
			return properties.getProperty(key);
		} catch (NullPointerException e) {
			throw new RuntimeException("key " +key +" not found in property file"); 
		}
	}
	
	
	public int getPropertyAsInt(String key) {
		
		try {
			return new Integer(getProperty(key));
		} catch (NumberFormatException e) {
			throw new RuntimeException("could not read " +key +" from property file; " +properties.getProperty(key) +" is not an Integer"); 
		}
		
	}
	
	
	public float getPropertyAsFloat(String key) {
		
		try {
			return new Float(getProperty(key));
		} catch (NumberFormatException e) {
			throw new RuntimeException("could not read " +key +" from property file; " +properties.getProperty(key) +" is not a Float"); 
		}
		
	}
	
	
	public long getPropertyAsLong(String key) {
		
		try {
			return new Long(getProperty(key));
		} catch (NumberFormatException e) {
			throw new RuntimeException("could not read " +key +" from property file; " +properties.getProperty(key) +" is not a Long"); 
		}
		
	}
	

	public double getPropertyAsDouble(String key) {
		
		try {
			return new Double(getProperty(key));
		} catch (NumberFormatException e) {
			throw new RuntimeException("could not read " +key +" from property file; " +properties.getProperty(key) +" is not a Double"); 
		}
		
	}
	
	
	public boolean getPropertyAsBoolean(String key) {
		
		String value = getProperty(key);

		return 	value.equals("TRUE") 
				|| value.equals("true") 
				|| value.equals("True") 
				|| value.equals("ON") 
				|| value.equals("On") 
				|| value.equals("on")
				|| value.equals("1")
				|| value.equals("YES")
				|| value.equals("yes")
				|| value.equals("Yes");
		
	}
	
	
	public InetAddress getPropertyAsInetAddress(String key) {
		try {
			
			return InetAddress.getByName(properties.getProperty(key));
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
			
			String errorMessahe = "could not convert the string \""
				+properties.getProperty(key) +"\" to an \"InetAddress\"" +
				"-object; specify an ip adress of the form \"x.x.x.x\" " +
				"for the key " +key +" in the property file.";
			
			throw new RuntimeException(errorMessahe); 
		}
	}
	
	
	public Object setProperty(String key, String value) {
		return properties.setProperty(key, value);
	}

	
	public String getConfig() {
		String config = properties.toString();
		config = config.replace('{', ' ');
		config = config.replace(',', '\n');
		config = config.replace(' ', ' ');
		config = config.replace('}', ' ');
		return config;	
	}
	
}
