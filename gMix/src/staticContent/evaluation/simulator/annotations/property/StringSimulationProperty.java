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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author alex
 *
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StringSimulationProperty {

	// dependencies
	public Class<? extends Requirement>[] enable_requirements() default {};

	// general

	/**
	 * @return
	 * 		simprop's name
	 */
	public String name() default "";

	/**
	 * @return
	 * 		the positon of the simprop (within the plugin).
	 * 		The higher the value the higher the position of the GUI-element
	 */
	public int position() default 50;

	/**
	 * @return
	 * 		a string of possible values (somme seperated)
	 */
	public String possibleValues() default "";
	
	/**
	 * @return
	 * 		true if multiselection is enabled, otherwise false
	 */
	public boolean multiSelection() default false;

	/**
	 * @return
	 * 		simprop's key / id
	 */
	public String key() default "";

	/**
	 * @return
	 * 		the tooltip text
	 */
	public String tooltip() default "No Tooltip available";
	
	/**
	 * @return
	 * 		the info text
	 */
	public String info() default "";

	/**
	 * @return
	 * 		the injection string
	 */
	public String inject() default "";

	/**
	 * @return
	 * 		true if simprop is global, otherwise false
	 */
	public boolean global() default false;

	/**
	 * @return
	 * 		an array of requirements - see {@link Requirement}
	 */
	public Class<? extends Requirement>[] value_requirements() default {};

	/**
	 * @return
	 * 		true if simprop is static, otherwise false
	 */
	public boolean isStatic() default false;
	
	/**
	 * @return
	 * 		a regular expression (used to validate the user input - not implemented yet)
	 */
	public String regex() default "";
	
	/**
	 * @return
	 * 		true if property is variable
	 */
	public boolean property_to_vary() default true;

}