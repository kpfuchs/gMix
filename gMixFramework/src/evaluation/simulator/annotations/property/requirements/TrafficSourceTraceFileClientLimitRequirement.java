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
package evaluation.simulator.annotations.property.requirements;

import java.util.HashSet;
import java.util.Set;

import evaluation.simulator.annotations.property.Requirement;
import evaluation.simulator.annotations.property.SimProp;
import evaluation.simulator.gui.pluginRegistry.SimPropRegistry;

public class TrafficSourceTraceFileClientLimitRequirement extends Requirement {
	
	
	public boolean check() {

		// getting the current SimPropRegistry
		SimPropRegistry gcr = SimPropRegistry.getInstance();
		// getting the Simproperty at which this requirement should be referenced
		SimProp simProp = gcr.getProperties().get("CLIENT_LIMIT");
		
		//creating a warning message
		String msg = "Requires \"LIMIT_CLIENT_NUMBER\" to be set to \"true\"";


		// create warnings if not existent
		if( simProp.getWarnings() == null ){
			simProp.setWarnings( new HashSet<String>() );
		}
		
		// check property value on which the enable status depends on 
		//in this case the property "SIMULATION_END"
		if (equals("LIMIT_CLIENT_NUMBER", "true")){
			//if it equals REAL_TIME_END we have to remove the warning which was eventually created before
			Set<String> warnings = simProp.getWarnings();
			warnings.remove(msg);
			simProp.setWarnings(warnings);
			// re-enable it
			simProp.setEnable(true);
			//return true because requirement is met
			return true;
		} else {
			// if there is another value we have to add the warning message to the warnings of the property
			Set<String> warnings = simProp.getWarnings();
			warnings.add(msg);
			simProp.setWarnings(warnings);
			// the requirement is not met so we have to disable the simprop and return false
			simProp.setEnable(false);
			return false;
		}
	}
}
