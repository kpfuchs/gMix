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
package staticContent.evaluation.simulator.annotations.property;

import java.util.Observable;
import java.util.Observer;
import java.util.Set;

// pojo
/**
 * Represents a simprop and implements {@link Observable}. This class is abstract!
 * 
 * @author alex
 *
 */
public abstract class SimProp extends Observable {

	// dependencies
	private Class<? extends Requirement>[] enable_requirements;
	private Class<? extends Requirement>[] value_requirements;
	
	boolean enabled;
	private String id;
	private String name;
	private String plugin;
	private int order;
	private String pluginLayer;
	private String propertykey;
	private String tooltip;
	private String info;
	private boolean isSuperclassProperty;
	private boolean isGlobal;
	private boolean isStatic;
	private Set<String> warnings;
	private Set<String> errors;
	private boolean isPropertyToVary;
	private int position;
	
	/**
	 * @return
	 * 		true if property is variable, otherwise false
	 */
	public boolean isPropertyToVary(){
		return isPropertyToVary;
	}
	
	/**
	 * Sets if a property is variable or not
	 * 
	 * @param bool
	 */
	public void isPropertyToVary(boolean bool){
		isPropertyToVary = bool;
	}

	/**
	 * @return
	 * 		true if the property is enables (not grayed out), otherwise false
	 */
	public boolean getEnable() {
		return this.enabled;
	}

	/**
	 * @return
	 * 		an array of Requirements - see {@link Requirement}
	 */
	public Class<? extends Requirement>[] getEnable_requirements() {
		return this.enable_requirements;
	}

	/**
	 * @return
	 * 		the key / id of the simprop
	 */
	public String getPropertyID() {
		return this.id;
	}

	/**
	 * @return
	 * 		the name of the simprop
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return
	 * 		the key / id of the plugin hte simprop belongs to
	 */
	public String getPluginID() {
		return this.plugin;
	}

	/**
	 * @return
	 * 		the oder / position of the simprop (gui)
	 */
	public int getOrder() {
		return this.order;
	}

	/**
	 * @return
	 * 		the key / id of the layer the simprop belongs to
	 */
	public String getPluginLayerID() {
		return this.pluginLayer;
	}

	/**
	 * @return
	 * 		the key / id of the simprop
	 */
	public String getPropertyKey() {
		return this.propertykey;
	}

	/**
	 * @return
	 * 		the tooltip text
	 */
	public String getTooltip() {
		return this.tooltip;
	}

	/**
	 * @return
	 * 		the value of the simprop
	 */
	public abstract Object getValue();

	public Class<? extends Requirement>[] getValue_requirements() {
		return this.value_requirements;
	}

	// abstract methods
	/**
	 * @return
	 * 		the values type
	 */
	public abstract Class<?> getValueType();

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public abstract String toString();


	/**
	 * Sets the enable state of the simporp
	 * 
	 * @param enableFlag
	 * 		if true the simprop is editable, otherwise it is not (grayed out)
	 */
	public void setEnable(boolean enableFlag) {
		this.enabled = enableFlag;
		changed();
	}
	
	/**
	 * Registers an observer
	 * @param observer
	 * 		reference to the observer
	 */
	public abstract void register(Observer observer);
	
	/**
	 * Unregisters an observer
	 * @param observer
	 * 		reference to the observer
	 */
	public abstract void unregister(Observer observer);
	
	/**
	 * This methos is called when the simprop has changed
	 */
	public abstract void changed();

	/**
	 * Sets the enable requirements
	 * 
	 * @param enable_requirements
	 * 		an array of requirements - see {@link Requirement}
	 */
	public void setEnable_requirements(
			Class<? extends Requirement>[] enable_requirements) {
		this.enable_requirements = enable_requirements;
	}

	/**
	 * Sets the id / key
	 * 
	 * @param id
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Sets the name
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Sets the plugin id / key
	 * 
	 * @param namespace
	 */
	public void setPluginID(String namespace) {
		this.plugin = namespace;
	}

	/**
	 * Sets the order / position (gui)
	 * 
	 * @param order
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Sets the layer id / key
	 * 
	 * @param pluginLayer
	 */
	public void setPluginLayerID(String pluginLayer) {
		this.pluginLayer = pluginLayer;
	}

	/**
	 * Sets the id / key
	 * 
	 * @param key
	 */
	public void setPropertyKey(String key) {
		this.propertykey = key;
	}

	/**
	 * Sets the tooltip text
	 * 
	 * @param tooltip
	 */
	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}

	/**
	 * Sets the value
	 * 
	 * @param value
	 */
	public abstract void setValue(Object value);

	/**
	 * Sets the value requirements
	 * 
	 * @param value_requirements
	 * 		an array of requirements - see {@link Requirement}
	 */
	public void setValue_requirements( Class<? extends Requirement>[] value_requirements) {
		this.value_requirements = value_requirements;
	}

	/**
	 * Sets the global flag
	 * 
	 * @param isGlobal
	 */
	public void setIsGlobal(boolean isGlobal) {
		this.isGlobal = isGlobal;
	}

	/**
	 * @return
	 * 	true if simprop is gloal, otherwise false 
	 */
	public boolean isGlobal() {
		return this.isGlobal;
	}

	/**
	 * @return
	 * 		true if this simprop was defined in a superclass
	 */
	public boolean isSuperclass() {
		return this.isSuperclassProperty;
	}

	/**
	 * @param isSuperclass
	 */
	public void setIsSuperclass(boolean isSuperclass) {
		this.isSuperclassProperty = isSuperclass;
	}

	/**
	 * @param isStatic
	 */
	public void setIsStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}

	/**
	 * @return
	 * 		true if simprop is static, otherwise false
	 */
	public boolean isStatic() {
		return this.isStatic;
	}

	/**
	 * @return
	 * 		simprop's info text
	 */
	public String getInfo() {
		return info;
	}

	/**
	 * Sets info text
	 * 
	 * @param info
	 */
	public void setInfo(String info) {
		this.info = info;
	}

	/**
	 * @return
	 * 		a set of warnings
	 */
	public Set<String> getWarnings() {
		return warnings;
	}

	/**
	 * Sets the warnings
	 * 
	 * @param warnings
	 */
	public void setWarnings(Set<String> warnings) {
		this.warnings = warnings;
		this.changed();
	}

	/**
	 * @return
	 * 		a set of errors
	 */
	public Set<String> getErrors() {
		return errors;
	}

	/**
	 * Sets errors
	 * 
	 * @param errors
	 */
	public void setErrors(Set<String> errors) {
		this.errors = errors;
		this.changed();
	}

	public void setPosition(int position) {
		this.position = position;
		
	}
	
	public int getPosition() {
		return this.position;
		
	}

}
