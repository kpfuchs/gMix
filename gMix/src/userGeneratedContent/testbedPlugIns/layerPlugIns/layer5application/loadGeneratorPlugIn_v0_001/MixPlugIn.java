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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.loadGeneratorPlugIn_v0_001;

import staticContent.evaluation.loadGenerator.LoadGenerator;
import staticContent.evaluation.loadGenerator.LoadGenerator.InsertLevel;
import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer5ApplicationMix;
import staticContent.framework.routing.RoutingMode;


public class MixPlugIn extends Implementation implements Layer5ApplicationMix {

	@Override
	public void constructor() {
		
	}

	
	@Override
	public void initialize() {

	}
	

	@Override
	public void begin() {
		System.out.println("loadGeneratorPlugIn_v0_001 loaded"); 
		if (anonNode.ROUTING_MODE == RoutingMode.GLOBAL_ROUTING) {
			if (anonNode.IS_LAST_MIX) {
				InsertLevel insertLevel = LoadGenerator.getInsertLevel(anonNode);
				if (insertLevel == InsertLevel.APPLICATION_LEVEL) {
					new ApplicationLevelHandler(anonNode);
				} else if (insertLevel == InsertLevel.MIX_PACKET_LEVEL) {
					new MixPacketLevelHandler(anonNode);
				} else
					throw new RuntimeException("unknown InsertLevel: " +insertLevel); 
			}
		} else {
			InsertLevel insertLevel = LoadGenerator.getInsertLevel(anonNode);
			if (insertLevel == InsertLevel.APPLICATION_LEVEL) {
				new ApplicationLevelHandler(anonNode);
			} else if (insertLevel == InsertLevel.MIX_PACKET_LEVEL) {
				new MixPacketLevelHandler(anonNode);
			} else
				throw new RuntimeException("unknown InsertLevel: " +insertLevel); 
		}
	}
	
}
