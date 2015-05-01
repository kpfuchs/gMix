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

import java.net.Socket;

import staticContent.framework.interfaces.ArchitectureInterface;

public interface RoutingModeInfoService extends ArchitectureInterface {

	public final static int REJECT_MIX = -1;
	
	
	/**
	 * called by info service when a mix tries to register; -> either assign 
	 * the mix an identifier (return-value) or refuse his connection by 
	 * returning -GlobalRoutingInfoService.REJECT_MIX
	 * 
	 * note: to determine whether the mix shall be accepted or not; you may 
	 * communicate with the Socket mixEnd, which is operated by the client 
	 * and follows the protocol specified in the method below 
	 * (assignMixIdClientSide())
	 */
	public int assignMixIdServerSide(Socket mixEnd);
	
	
	/**
	 * called by anonNode as soon as he has a connection to the info service; 
	 * 
	 * note: this method is the counterpart to the method above 
	 * (assignMixIdServerSide()); use it to communicate with the info-service 
	 * to negotiate an id
	 */
	public void assignMixIdClientSide(Socket infoServiceEnd);

}
