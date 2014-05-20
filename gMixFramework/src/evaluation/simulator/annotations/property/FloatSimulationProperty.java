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
package evaluation.simulator.annotations.property;

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
public @interface FloatSimulationProperty {
	
	// dependencies
	public Class<? extends Requirement>[] enable_requirements() default {};

	// general
//	public String id() default "";

	/**
	 * @return
	 * 		maximum value
	 */
	public float max() default Float.MAX_VALUE;

	/**
	 * @return
	 * 		minimum value
	 */
	public float min() default Float.NEGATIVE_INFINITY;

	/**
	 * @return
	 * 		simprop display name
	 */
	public String name() default "";

	/**
	 * @return
	 * 		the positon of the simprop (within the plugin).
	 * 		The higher the value the higher the position of the GUI-element
	 */
	public int position() default 50;

	/**
	 * Unique
	 * 
	 * @return
	 * 		simprop config name
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
	 * 		the injection strin
	 */
	public String inject() default "";

	/**
	 * @return
	 * 		the global flag
	 */
	public boolean global() default false;

	/**
	 * @return
	 * 		array of value requirements - see {@link Requirement}
	 */
	public Class<? extends Requirement>[] value_requirements() default {};

	/**
	 * @return
	 * 		the static flag
	 */
	public boolean isStatic() default false;
	
	/**
	 * @return
	 * 		true if auto checkbox is enabled, otherwise false
	 */
	public boolean enableAuto() default false;
	
	/**
	 * @return
	 * 		true if unlimited checkbox is enabled, otherwise false
	 */
	public boolean enableUnlimited() default false;
	
	/**
	 * @return
	 * 		the stepsize for spinner gui element
	 */
	public float stepSize() default 0.1f;
	
	/**
	 * @return
	 * 		identifying string of the gui element to manipulate the simprop
	 */
	public String guiElement() default "spinner";
	
	/**
	 * Returns true if the property is variable otherwise false
	 * @return
	 * 		property to vary flag
	 */
	public boolean property_to_vary() default true;

}