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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer1network.encDNS_v0_001;

import java.io.IOException;
import java.net.DatagramSocket;

import staticContent.framework.EncDnsServer;
import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer1NetworkMix;


public class MixPlugIn extends Implementation implements Layer1NetworkMix {
	
    private DatagramSocket _udpSock53;
    
    
    
	@Override
	public void constructor() {
		try {
			 _udpSock53 = new DatagramSocket(EncDnsServer.bindPort, EncDnsServer.bindAddress);
			 //_udpSock53.setReceiveBufferSize(10000);
	     } catch (IOException e) {
	    	 System.err.println("UDP socket error: " +e.getMessage());
	     }
		 System.out.println("listening on " +_udpSock53.getLocalAddress() +":"+_udpSock53.getLocalPort() +" for EncDNS queries"); 
	}

	
	@Override
	public void initialize() {
		
	}

	
	@Override
	public void begin() {
		
	}
	
	
	public DatagramSocket getSocket() {
		return this._udpSock53;
	} 
}
