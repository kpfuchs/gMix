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
package framework.core.config;

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
    
    
    public String[] getPropertyAsStringArray(String key, String regex) {
		try {
			return properties.getProperty(key).split(regex);
		} catch (NullPointerException e) {
			throw new RuntimeException("key " +key +" not found in property file"); 
		}
	}
	
	
	public int getPropertyAsInt(String key) {
		
		try {
			return Integer.parseInt(getProperty(key));
		} catch (NumberFormatException e) {
			throw new RuntimeException("could not read " +key +" from property file; " +properties.getProperty(key) +" is not an Integer"); 
		}
		
	}
	
	
	public float getPropertyAsFloat(String key) {
		
		try {
			return Float.parseFloat(getProperty(key));
		} catch (NumberFormatException e) {
			throw new RuntimeException("could not read " +key +" from property file; " +properties.getProperty(key) +" is not a Float"); 
		}
		
	}
	
	
	public long getPropertyAsLong(String key) {
		
		try {
			return Long.parseLong(getProperty(key));
		} catch (NumberFormatException e) {
			throw new RuntimeException("could not read " +key +" from property file; " +properties.getProperty(key) +" is not a Long"); 
		}
		
	}
	

	public double getPropertyAsDouble(String key) {
		
		try {
			return Double.parseDouble(getProperty(key));
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
			
			String errorMessage = "could not convert the string \""
				+properties.getProperty(key) +"\" to an \"InetAddress\"" +
				"-object; specify an ip adress of the form \"x.x.x.x\" " +
				"for the key " +key +" in the property file.";
			
			throw new RuntimeException(errorMessage); 
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
	
	
	public void addProperties(String pathToPropertyFile) {
		Properties newProp = new Properties();
		try {
			newProp.load(new FileInputStream(pathToPropertyFile));	 
		} catch(IOException e) {
			e.printStackTrace();
			throw new RuntimeException("property file could not be loaded!");
		}
		for (String key:newProp.stringPropertyNames())
			this.properties.setProperty(key, newProp.getProperty(key));
	}
	
	public void addProperties(Properties properties) {
		for (String key:properties.stringPropertyNames())
			this.properties.put(key, properties.get(key));
	}
	
	public static Properties overwriteSettings(Properties properties, String args) {
		if (args != null) {
			if (!args.contains("=")) // TODO
				throw new RuntimeException("invalid parameter: " +args +". Example: java -jar gMixFramework.jar mix DUPLEX_ON=FALSE,ADDRESS=AUTO;PORT=AUTO"); 
			String[] keyValuePairs = args.split(",");
			System.out.println("args: " +args); // TODO
			System.out.println("splitting into " +keyValuePairs.length +" keyValuePairs"); // TODO
			for (String keyValuePair:keyValuePairs) {
				String[] keyValue = keyValuePair.split("=");
				System.out.println("setting " +keyValue[0] +" to " +keyValue[1]); 
				properties.setProperty(keyValue[0], keyValue[1]);
			}
		}
		return properties;
	}
	
	
	public static Properties overwriteExistingSettings(Properties properties, String args) {
		if (args != null) {
			if (!args.contains("=")) // TODO
				throw new RuntimeException("invalid parameter: " +args +". Example: java -jar gMixFramework.jar mix DUPLEX_ON=FALSE,ADDRESS=AUTO;PORT=AUTO"); 
			String[] keyValuePairs = args.split(",");
			for (String keyValuePair:keyValuePairs) {
				String[] keyValue = keyValuePair.split("=");
				if (properties.getProperty(keyValue[0]) != null)
					properties.setProperty(keyValue[0], keyValue[1]);
			}
		}
		return properties;
	}
	
	
	public static String getPropertyFromFile(String path, String key) {
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(path)); 
		} catch(IOException e) {
			throw new RuntimeException("ERROR: " +path +" not found!");
	    }
		return properties.getProperty(key);
	}
	
}
