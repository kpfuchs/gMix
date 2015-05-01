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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.adaptiveMix_v0_001;

import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer5ApplicationClient;


public class ClientPlugIn extends Implementation implements Layer5ApplicationClient {

	
	@Override
	public void constructor() {
		// clients are simulated by the load generator (see evaluation.loadGenerator.LoadGenerator.java)
	}


	@Override
	public void initialize() {
		
	}


	@Override
	public void begin() {

	}
	
}
