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

public class SimulationEndSimulationTimeEndRequirement extends Requirement {
	
	
	public boolean check() {
		SimPropRegistry gcr = SimPropRegistry.getInstance();
		SimProp simProp = gcr.getProperties().get("SIMULATION_TIME_LIMIT_IN_MS");
		String msg = "Requires \"Simulation end condition\" to be set to \"SIMULATION_TIME_END\"";
		
		// create warnings if not existent
		if( simProp.getWarnings() == null ){
			simProp.setWarnings( new HashSet<String>() );
		}
		
		if (equals("SIMULATION_END", "SIMULATION_TIME_END")){
			Set<String> warnings = simProp.getWarnings();
			warnings.remove(msg);
			simProp.setWarnings(warnings);
			simProp.setEnable(true);
			return true;
		} else {
			Set<String> warnings = simProp.getWarnings();
			warnings.add(msg);
			simProp.setWarnings(warnings);
			simProp.setEnable(false);
			return false;
		}
	}
}
