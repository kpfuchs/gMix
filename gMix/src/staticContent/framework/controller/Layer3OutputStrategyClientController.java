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
package staticContent.framework.controller;

import staticContent.framework.AnonNode;
import staticContent.framework.clock.Clock;
import staticContent.framework.config.Settings;
import staticContent.framework.infoService.InfoServiceClient;
import staticContent.framework.interfaces.Layer3OutputStrategyClient;
import staticContent.framework.userDatabase.UserDatabase;


public class Layer3OutputStrategyClientController extends Controller {


	public Layer3OutputStrategyClientController(AnonNode anonNode,
			Settings settings, UserDatabase userDatabase, Clock clock,
			InfoServiceClient infoService) {
		super(anonNode, settings, userDatabase, clock, infoService);
	}

	
	@Override
	public void instantiateSubclass() {
		// not needed for client-side controllers
	}
	

	public Layer3OutputStrategyClient loadClientPluginInstance() {
		return LocalClassLoader.instantiateImplementation(
				"userGeneratedContent.testbedPlugIns.layerPlugIns.layer3outputStrategy." +settings.getProperty("LAYER_3_PLUG-IN_CLIENT"), 
				"ClientPlugIn.java",
				this,
				Layer3OutputStrategyClient.class
				);
	}
}