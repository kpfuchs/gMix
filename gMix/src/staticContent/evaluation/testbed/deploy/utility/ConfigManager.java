/*******************************************************************************
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2014  SVS
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
 *******************************************************************************/
package staticContent.evaluation.testbed.deploy.utility;

import java.io.File;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

public class ConfigManager extends PropertiesConfiguration {

	private static ConfigManager instance = null;
	public enum Type {
	    COORDINATOR, REGISTRY, TESTNODE
	}

	public ConfigManager(String string) throws ConfigurationException {
		super(string);
	}

	/**
	 * Returns an instance of the ConfigManager.
	 *
	 * @return instance of the ConfigManager
	 */
	public static ConfigManager getInstance(Type t) {
		if (instance == null) {
			String filename = "";

			switch (t) {
				case COORDINATOR:
					filename = "config/coordinator.properties";
					break;
				case REGISTRY:
					filename = "config/registry.properties";
					break;
				case TESTNODE:
					filename = "config/testnode.properties";
					break;
			}

			String configFilePath = System.getProperty("user.dir") +"/inputOutput/testbed/"+filename;			
			
			try {
				instance = new ConfigManager(configFilePath);
				instance.setThrowExceptionOnMissing(true);
			}
			catch (ConfigurationException e) {
				System.err.println("Configuration file: " + configFilePath + " cannot be loaded");
				System.exit(1);
			}
		}
		return instance;
	}

	/**
	 * Returns the path of the directory of the executable jar file from which this method was started.
	 *
	 * @return absolute path of the executable file
	 */
	public static String getDirOfExecutable() {
		String name = ConfigManager.class.getName().replace('.', '/');
	    String s    = ConfigManager.class.getResource("/" + name + ".class").toString();

	    if (s.startsWith("file:")) {
	    	s = s.substring(s.indexOf("file:")+5);
	    	s = s.replace('/', File.separatorChar);
	    	name = name.replace('/', File.separatorChar);

	    	if (s.indexOf(":") != -1) {
	    		s = s.substring(1);
	    	}

	    	return s.substring(0, s.indexOf(name)-4);
	    }
	    else if (s.startsWith("jar:")) {
	    	s = s.substring(s.indexOf("jar:file:")+9);
	    	s = s.replace('/', File.separatorChar);
		    s = s.substring(0, s.indexOf(".jar")+4);

		    if (s.indexOf(":") != -1) {
		    	s = s.substring(s.lastIndexOf(':')-1);
	    	}

		    return s.substring(0, s.lastIndexOf(File.separatorChar) + 1);
	    }

	    return "-1";
	}

	/**
	 * Returns the absolute path of the given filePath.
	 * If filePath is already an absolute path, filePath is returned.
	 * Otherwise filePath is interpreted as relative path in the directory
	 * of the executable jar file from which this method was started. The
	 * absolute path of then the absolute path name of the directory
	 * of the executable jar file from which this method was started concatenated
	 * with the given filePath.
	 *
	 * @param filePath
	 *
	 * @return
	 */
	public String getAbsoluteFilePath(String filePath) {
		return ((new File(filePath)).isAbsolute()) ? filePath : getDirOfExecutable()+filePath;
	}

}
