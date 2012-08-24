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
import framework.core.interfaces.Layer5ApplicationMix;
import framework.core.userDatabase.UserDatabase;
import framework.infoService.InfoServiceClient;


public class Layer5ApplicationMixController extends Controller implements Layer5ApplicationMix {

	private Implementation[] implementations;
	
	
	public Layer5ApplicationMixController(AnonNode anonNode, Settings settings,
			UserDatabase userDatabase, Clock clock,
			InfoServiceClient infoService) {
		super(anonNode, settings, userDatabase, clock, infoService);
	}

	
	@Override
	public void instantiateSubclass() {
		String[] plugInNames = settings.getProperty("LAYER_5_PLUG-IN_MIX").split(",");
		this.implementations = new Implementation[plugInNames.length];
		for (int i=0; i<plugInNames.length; i++) {
			this.implementations[i] = (Implementation) LocalClassLoader.instantiateImplementation(
					"plugIns.layer5application." +plugInNames[i], 
					"MixPlugIn.java",
					this,
					Layer5ApplicationMix.class
					);
		}
	}
	
	
	@Override
	public void initialize() {
		assert implementations != null;
		for (Implementation impl:implementations)
			impl.constructor();
		for (Implementation impl:implementations)
			impl.callInitialize();
	}


	public void begin() {
		assert implementations != null;
		for (Implementation impl:implementations)
			impl.callBegin();
	}
	

	public void setImplementation(Implementation implementation) {

	}
	
	
	public Implementation getImplementation() {
		throw new RuntimeException("not available for Layer5ApplicationMixController; use getImplementations() instead"); 
	}
	
	
	public Implementation[] getImplementations() {
		return this.implementations;
	}

}