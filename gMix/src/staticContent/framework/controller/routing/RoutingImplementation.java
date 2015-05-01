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

import staticContent.framework.AnonNode;
import staticContent.framework.clock.Clock;
import staticContent.framework.config.Settings;
import staticContent.framework.infoService.InfoServiceClient;
import staticContent.framework.userDatabase.UserDatabase;


public abstract class RoutingImplementation {

	// references to framework classes
	protected AnonNode anonNode;
	protected Settings settings;
	protected UserDatabase userDatabase;
	protected Clock clock;
	protected InfoServiceClient infoService;
	
	// reference on controller class of this implementation
	protected RoutingController controller;
		
	
	public void setController(RoutingController controller) {
		this.anonNode = controller.anonNode;
		this.settings = controller.settings;
		this.userDatabase = controller.userDatabase;
		this.clock = controller.clock;
		this.infoService = controller.infoService;
		this.controller = controller;
	}
	
}
