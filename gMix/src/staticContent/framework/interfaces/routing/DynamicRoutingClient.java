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
package staticContent.framework.interfaces.routing;

import staticContent.framework.interfaces.ArchitectureInterface;
import staticContent.framework.routing.MixList;


public interface DynamicRoutingClient extends ArchitectureInterface {

	/** note that only the first hop of the route is guaranteed to be used. */
	public MixList choseRoute();
	
	/** note that only the first hop of the route is guaranteed to be used. */
	public MixList choseRoute(int destPseudonym);
	
}
