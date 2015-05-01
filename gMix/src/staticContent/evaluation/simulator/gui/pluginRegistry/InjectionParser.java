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
package staticContent.evaluation.simulator.gui.pluginRegistry;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Parser for simprop injections
 * 
 * @author alex
 *
 */
class InjectionParser {
	
	private static Logger logger = Logger.getLogger(InjectionParser.class);
	public String getLayer() {
		return layer;
	}

	/**
	 * @return
	 * 		the plugin
	 */
	public String getPlugin() {
		return plugin;
	}

	/**
	 * @return
	 * 		the layer config name
	 */
	public String getLayerConfigName() {
		return layerConfigName;
	}

	/**
	 * @return
	 * 		the layer display name
	 */
	public String getLayerDisplayName() {
		return layerDisplayName;
	}

	/**
	 * @return
	 * 		the plugin config name
	 */
	public String getPluginConfigName() {
		return pluginConfigName;
	}

	/**
	 * @return
	 * 		the plugin display name
	 */
	public String getPluginDisplayName() {
		return pluginDisplayName;
	}

	public int getLayerPosition() {
		return layerPosition;
	}

	@Deprecated
	/**
	 * @return
	 * 		the plugin position (gui ordering)
	 */
	public int getPluginPosition() {
		return pluginPosition;
	}

	/**
	 * @return
	 * 		a boolean that indicates whether the simprop is globally visible or not
	 */
	public boolean isGlobalProperty() {
		return globalProperty;
	}

	private String layer = "";
	private String plugin = "";
	private String layerConfigName = "";
	private String layerDisplayName = "";
	private String pluginConfigName = "";
	private String pluginDisplayName = "";
	private int layerPosition = 0;
	private int pluginPosition = 0;
	private boolean globalProperty = true;
	
	/**
	 * Constructor
	 * 
	 * @param arguments
	 * 		the injection string
	 * @param propertyKey
	 * 		the property key / id
	 */
	public InjectionParser( String arguments, String propertyKey ){
		String[] injectionArguments = arguments.split("@");
		
		// Process layer information
		if ( injectionArguments.length >= 1) {
			layer = injectionArguments[0];
			String[] layerSplit = layer.split(",");
			if ( layerSplit.length >= 1 ){
				layerConfigName = layerSplit[0];
				String[] tmp = layerConfigName.split(":");
				if ( tmp.length == 2){
					layerPosition = Integer.valueOf(tmp[0]);
					layerConfigName = tmp[1];
				}
				if ( tmp.length > 2){
					logger.log(Level.DEBUG, "Inject annotation for " + 
							   propertyKey + " is not well formed \n Injection string is " + arguments);
					// This might not be critical, but it is better to quit
					System.exit(-1);
				}
			}
			if ( layerSplit.length >= 2 ){
				layerDisplayName = layerSplit[1];
			}
			if ( layerSplit.length >= 3 ) {
				logger.log(Level.DEBUG, "Inject annotation for " + 
						   propertyKey+ " is not well formed \n Injection string is " + arguments);
				// This might not be critical, but it is better to quit
				System.exit(-1);
			}
		}
		
		// Process the plugin information
		if ( injectionArguments.length >= 2 ) {
			plugin = injectionArguments[1];
			String[] pluginSplit = plugin.split(",");
			if ( pluginSplit.length >= 1 ){
				pluginConfigName = pluginSplit[0];
			}
			if ( pluginSplit.length >= 2 ){
				pluginDisplayName = pluginSplit[1];
			}
			if ( pluginSplit.length >= 3 || pluginSplit.length < 1 ) {
				logger.log(Level.DEBUG, "Inject annotatioin for " + 
						   propertyKey + " is not well formed \n Injection string is " + arguments);
				// This might not be critical, but it is better to quit
				System.exit(-1);
			}
			
			// overwrite visibility since we have got a plugin
			// generally plugins pluginproperties are not global
			globalProperty = false;
		} 
		
		if ( injectionArguments.length >= 3 ) {
			logger.log(Level.ERROR, "Can not inject " + propertyKey + 
					   " due to bad injection arguments! \n Injection string is " + arguments);
			System.exit(-1);
		}else{
			logger.log(Level.DEBUG, "Injected property " + propertyKey + " into " + layer + "@" + plugin);
		}
	}
	
}