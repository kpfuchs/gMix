/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2012  Karl-Peter Fuchs
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
 */
package framework.core.controller;

import framework.core.AnonNode;
import framework.core.clock.Clock;
import framework.core.config.Settings;
import framework.core.interfaces.Layer2RecodingSchemeClient;
import framework.core.userDatabase.UserDatabase;
import framework.infoService.InfoServiceClient;


public class Layer2RecodingSchemeClientController extends Controller {

	
	public Layer2RecodingSchemeClientController(AnonNode anonNode, Settings settings,
			UserDatabase userDatabase, Clock clock,
			InfoServiceClient infoService) {
		super(anonNode, settings, userDatabase, clock, infoService);
	}

	
	@Override
	public void instantiateSubclass() {
		// not needed for client-side controllers
	}
	
	
	public Layer2RecodingSchemeClient loadClientPluginInstance() {
		return LocalClassLoader.instantiateImplementation(
				"plugIns.layer2recodingScheme." +settings.getProperty("LAYER_2_PLUG-IN_CLIENT"), 
				"ClientPlugIn.java",
				this,
				Layer2RecodingSchemeClient.class
				);
	}

}