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

import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import staticContent.evaluation.simulator.annotations.property.Requirement;
import staticContent.evaluation.simulator.annotations.property.SimProp;

public class DependencyChecker {

	private static Logger logger = Logger.getLogger(DependencyChecker.class);
	public static Boolean errorsInConfig;

	/**
	 * Checks all Requirements of all Simproperties
	 * @param gcr - the whole reqistry
	 */
	public static void checkAll(SimPropRegistry gcr) {
		errorsInConfig = false;

		Set<Entry<String, SimProp>> allSimProps = gcr.getAllSimProps();

		Requirement enableRequirement = null;
		Requirement valueRequirement = null;

		// Marker for errors in instantiate
		Boolean errorValue;
		Boolean errorEnabled;

		// Marker if the simprop is disabled so that
		// 1. no further check for enabled are made
		// 2. ValueChecks are not executed
		Boolean disabled;

		for (Entry<String, SimProp> entry : allSimProps) {
			errorValue = false;
			errorEnabled = false;
			disabled = false;

			// *********** Check Enabled
			// Get Value from Annotation
			Class<? extends Requirement>[] enableRequirements = (Class<? extends Requirement>[]) (entry
					.getValue()).getEnable_requirements();
			// Annotation is always null if it is not set
			if (enableRequirements != null) {
				// Iterate over all annotated Classes
				for (Class<? extends Requirement> requirement : enableRequirements) {
					// Checks if the simprop is not already disabled.
					if (!disabled) {
						try {
							// Try to instante the Requirement
							enableRequirement = requirement.newInstance();
						} catch (InstantiationException e) {
							logger.log(Level.ERROR, e.getMessage());
							errorEnabled = true;
						} catch (IllegalAccessException e) {
							logger.log(Level.ERROR, e.getMessage());
							errorEnabled = true;
						}
						if (!errorEnabled) {
							disabled = !enableRequirement.check();
						}
					}
				}
			}

			// Checks if the simprop is not already disabled.
			if (!disabled) {
				// *********** Check Enabled
				// Get Value from Annotation
				Class<? extends Requirement>[] valueRequirements = (Class<? extends Requirement>[]) (entry
						.getValue()).getValue_requirements();
				if (valueRequirements != null) {
					for (Class<? extends Requirement> requirement : valueRequirements) {
						try {
							// Try to instante the Requirement
							valueRequirement = requirement.newInstance();
						} catch (InstantiationException e) {
							logger.log(Level.ERROR, e.getMessage());
							errorValue = true;
						} catch (IllegalAccessException e) {
							logger.log(Level.ERROR, e.getMessage());
							errorValue = true;
						}
						if (!errorValue) {
							valueRequirement.check();
						}
					}

				}
			}

		}

	}
}
