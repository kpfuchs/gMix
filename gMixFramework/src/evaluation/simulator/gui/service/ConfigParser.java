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
package evaluation.simulator.gui.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.JOptionPane;

import evaluation.simulator.conf.service.SimulationConfigService;
import evaluation.simulator.gui.pluginRegistry.SimPropRegistry;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;


/**
 * @author alex
 *
 */
public class ConfigParser {
	
	private static Logger logger = Logger.getLogger(ConfigParser.class);

	
	/**
	 * Read config file content into a string 
	 * 
	 * @param file
	 * @return
	 * 		a string with config file's content
	 */
	public String cleanupConfigurationForSimulator(File file) {
		StringBuffer content = new StringBuffer();
		BufferedReader reader = null;
		
		List<String> configNamesForPluginLayers = SimPropRegistry.getInstance().getConfigNamesForPluginLayers();
		
		Properties props = new Properties();
		
		try {
			props.load(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(null, "File not found.");
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Could not read from file.");
		} catch (IllegalArgumentException e) {
			JOptionPane.showMessageDialog(null, "Could not read from file.");
		}

		// Load simProps
		List<String> activePlugins = new LinkedList<String>();
		Set<String> generalProperties = new HashSet<String>();
		
		for (String s : configNamesForPluginLayers ) {
				String tmp = props.getProperty(s);
				logger.log(Level.DEBUG, "Set " + s + " to " + tmp);
				activePlugins.add(tmp);
				generalProperties.add(s);
		}

		try {
			reader = new BufferedReader(new FileReader(file));
			String string = null;
			
			// Filter relevant lines in configuration file
			while ((string = reader.readLine()) != null) {
				
				// filter comments
				if ( !string.matches("^#.*") ){
					
					if ( string.matches("^OVERWRITABLE_PARAMETERS = ") ){
						content.append("OVERWRITABLE_PARAMETERS=#").append(System.getProperty("line.separator"));
					} else if ( string.matches("^NONE_OVERWRITABLE_PARAMETERS = ") ) {
						content.append("NONE_OVERWRITABLE_PARAMETERS=#").append(System.getProperty("line.separator"));
					} else if ( string.matches("^OVERWRITABLE_PARAMETERS =") ){
						content.append("OVERWRITABLE_PARAMETERS=#").append(System.getProperty("line.separator"));
					} else if ( string.matches("^NONE_OVERWRITABLE_PARAMETERS =") ) {
						content.append("NONE_OVERWRITABLE_PARAMETERS=#").append(System.getProperty("line.separator"));
					} else {
						string = string.replace(" =", "=");
						string = string.replace("= ", "=");
						string = string.replace(" =", "=");
						string = string.replace("= ", "=");
						string = string.replace(" =", "=");
						string = string.replace("= ", "=");
						content.append(string).append(System.getProperty("line.separator"));
					}
					continue;
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return content.toString();

	}
}
