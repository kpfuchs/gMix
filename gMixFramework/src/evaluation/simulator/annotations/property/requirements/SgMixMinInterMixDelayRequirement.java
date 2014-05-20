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

public class SgMixMinInterMixDelayRequirement extends Requirement {
	
	
@Override
public boolean check() {
	SimPropRegistry gcr = SimPropRegistry.getInstance();
	SimProp thisSimprop = gcr.getProperties().get("SGMIX_MIN_INTER_MIX_DELAY");
	SimProp dependSimprop = gcr.getProperties().get("SGMIX_MAX_INTER_MIX_DELAY");
	
	Integer valuethisSimpProp = (Integer)thisSimprop.getValue();
	Integer valuedependSimprop = (Integer)dependSimprop.getValue();

	String msg = "Depedency Violation: Must be equal or less than \"SGMIX_MAX_INTER_MIX_DELAY\"";

	if( thisSimprop.getErrors() == null ){
		thisSimprop.setErrors( new HashSet<String>() );
	}
	Set<String> errors = thisSimprop.getErrors();
	
	if (!(valuethisSimpProp <= valuedependSimprop)){
		errors.add(msg);
	} else {
		errors.remove(msg);
	}
	thisSimprop.setErrors(errors);
	return true;
}

}
