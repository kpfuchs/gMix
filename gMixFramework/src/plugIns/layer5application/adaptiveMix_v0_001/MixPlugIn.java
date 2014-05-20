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
package plugIns.layer5application.adaptiveMix_v0_001;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer5ApplicationMix;


public class MixPlugIn extends Implementation implements Layer5ApplicationMix {

	@Override
	public void constructor() {
		
	}

	
	@Override
	public void initialize() {

	}
	

	@Override
	public void begin() {
		System.out.println("adaptiveMix_v0_001 loaded"); 
		
	}
	
}
