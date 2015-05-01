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
package staticContent.framework.routing;

import staticContent.framework.config.Settings;

public enum RoutingMode {

	GLOBAL_ROUTING,
	SOURCE_ROUTING,
	DYNAMIC_ROUTING;
	
	
	public static RoutingMode getMode(Settings settings) {
		String modeAsString = settings.getProperty("GLOBAL_ROUTING_MODE");
		if (modeAsString.equals("GLOBAL_ROUTING"))
			return RoutingMode.GLOBAL_ROUTING;
		else if (modeAsString.equals("SOURCE_ROUTING"))
			return RoutingMode.SOURCE_ROUTING;
		else if (modeAsString.equals("DYNAMIC_ROUTING"))
			return RoutingMode.DYNAMIC_ROUTING;
		else
			throw new RuntimeException("unkown GLOBAL_ROUTING_MODE mode specified");
	}
	
}
