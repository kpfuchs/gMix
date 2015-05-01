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
package staticContent.evaluation.simulator.annotations.plugin;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * @author alex
 *
 */
public class SimGuiPlugin {
	private static Logger logger = Logger.getLogger(SimGuiPlugin.class);

	private String documentationURL;
	private String id;
	private String displayName;
	private String configName;
	private String pluginLayer;
	private boolean visible;
	private boolean globalFields;
	private String fallbackLayer;
	private boolean allowGlobalFields;

	@Deprecated
	public String getDocumentationURL() {
		return this.documentationURL;
	}

	public String getId() {
		return this.id;
	}

	public String getConfigName() {
		return this.configName;
	}

	public void setDocumentationURL(String documentationURL) {
		this.documentationURL = documentationURL;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setConfigName(String name) {
		this.configName = name;
	}

	public String getPluginLayer() {
		return pluginLayer;
	}

	public void setPluginLayer(String pluginLayer) {
		this.pluginLayer = pluginLayer;
	}

	public boolean isVisible() {
		return this.visible;
	}
	
	public void isVisible( boolean isVisible) {
		this.visible = isVisible;
	}

	public boolean isGlobal() {
		return this.globalFields;
	}
	
	public void  isGlobal( boolean globalFields ) {
		this.globalFields = globalFields;
	}
	
	public void setFallbackLayer( String pluginLayer ) {
		this.fallbackLayer = pluginLayer;		
	}
	
	public String getFallbackLayer() {
		return this.fallbackLayer;		
	}

	public void allowGlobalFields( boolean allowGlobalFields ) {
		this.allowGlobalFields = allowGlobalFields;
	}
	
	public boolean allowGlobalFields() {
		return this.allowGlobalFields;
	}

	public void setDisplayName( String name ){
		this.displayName = name;
	}
	
	public String getDisplayName( ){
		if ( this.displayName != null && !this.displayName.equals(""))
			return this.displayName;
		return this.configName;
	}

	/**
	 * @param pluginClass A class that is annotated with @Plugin
	 * @return The class above pluginClass that is annotated with
	 * @PluginSuperclass. If no such class is found null is returned.
	 * If pluginClass has no @Plugin annotation, null is also returned,
	 * since there is no reason to scan such a class!
	 */
	public Class<?> getPluginSuperclass(Class<?> pluginClass) {
		
		// This can just happen if someone screwed the code!
		if ( !pluginClass.isAnnotationPresent( Plugin.class ) ){
			if ( !pluginClass.isInstance( this )){
				logger.log(Level.ERROR, this + " parameter pluginClass is not an instance of Plugin!!!");
				System.exit(-1);
			}
			return null;
		}
		
		Class<?> thisClass = pluginClass.getSuperclass();
		while ( !thisClass.isAnnotationPresent( PluginSuperclass.class ) && 
				!thisClass.getCanonicalName().equals("java.lang.Object") ){
			logger.log(Level.DEBUG, thisClass.getCanonicalName());
			thisClass = thisClass.getSuperclass();
		}
		
		if ( thisClass.isAnnotationPresent( PluginSuperclass.class ) ){
			return thisClass;
		}
		
		return null;
	}
	
}
