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
package staticContent.evaluation.loadGenerator.asFastAsPossible;

import staticContent.evaluation.loadGenerator.LoadGenerator;
import staticContent.framework.config.Settings;


public abstract class AFAP_LoadGenerator {

	protected LoadGenerator owner;
	protected Settings settings;

	
	protected AFAP_LoadGenerator(LoadGenerator owner) {
		this.owner = owner;
		this.settings = owner.settings;
	}
	
	
	public static AFAP_LoadGenerator createInstance(LoadGenerator owner) {
		return new AFAP_RR_LoadGenerator(owner);
	}

}