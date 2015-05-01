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
package staticContent.framework.controller.routing;

import java.net.Socket;

import staticContent.framework.clock.Clock;
import staticContent.framework.config.Settings;
import staticContent.framework.controller.LocalClassLoader;
import staticContent.framework.interfaces.routing.RoutingModeInfoService;
import staticContent.framework.routing.RoutingMode;


public class RoutingModeInfoServiceController extends RoutingController implements RoutingModeInfoService {

	
	public RoutingModeInfoServiceController(
			Settings settings, 
			Clock clock
			) {
		
		super(null, settings, null, clock, null);
		
	}

	
	@Override
	protected RoutingImplementation loadImplementation() {
		RoutingMode rm = RoutingMode.getMode(settings);
		switch (rm) {
			case GLOBAL_ROUTING:
				return (RoutingImplementation) LocalClassLoader.instantiateRoutingImplementation(
						"userGeneratedContent.testbedPlugIns.routingPlugIns.globalRouting." +settings.getProperty("GLOBAL_ROUTING_PLUG-IN"), 
						"InfoServicePlugIn.java",
						this,
						RoutingModeInfoService.class
						);
			case SOURCE_ROUTING:
				return (RoutingImplementation) LocalClassLoader.instantiateRoutingImplementation(
						"userGeneratedContent.testbedPlugIns.routingPlugIns.sourceRouting." +settings.getProperty("SOURCE_ROUTING_PLUG-IN"), 
						"InfoServicePlugIn.java",
						this,
						RoutingModeInfoService.class
						);
			case DYNAMIC_ROUTING:
				return (RoutingImplementation) LocalClassLoader.instantiateRoutingImplementation(
						"userGeneratedContent.testbedPlugIns.routingPlugIns.dynamicRouting." +settings.getProperty("DYNAMIC_ROUTING_PLUG-IN"), 
						"InfoServicePlugIn.java",
						this,
						RoutingModeInfoService.class
						);
			default:
				throw new RuntimeException("unknown routing mode: " +rm); 
		} 
	}


	@Override
	public int assignMixIdServerSide(Socket mixEnd) {
		return ((RoutingModeInfoService)super.implementation).assignMixIdServerSide(mixEnd);
	}


	@Override
	public void assignMixIdClientSide(Socket infoServiceEnd) {
		((RoutingModeInfoService)super.implementation).assignMixIdClientSide(infoServiceEnd);
	}

}
