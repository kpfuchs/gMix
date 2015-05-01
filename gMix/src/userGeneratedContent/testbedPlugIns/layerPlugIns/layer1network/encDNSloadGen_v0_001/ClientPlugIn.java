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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer1network.encDNSloadGen_v0_001;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

import staticContent.framework.EncDnsClient;
import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer1NetworkClient;
import staticContent.framework.interfaces.Layer2RecodingSchemeClient;
import staticContent.framework.interfaces.Layer3OutputStrategyClient;
import staticContent.framework.interfaces.Layer4TransportClient;
import staticContent.framework.message.Reply;
import staticContent.framework.message.Request;
import staticContent.framework.routing.MixList;


public class ClientPlugIn extends Implementation implements Layer1NetworkClient {
	
	private DatagramSocket udpSock = null;
	
	
	@Override
	public void constructor() {
		// nothing to do here
	}
	

	@Override
	public void initialize() {
		try {
            this.udpSock = null;
            int port = settings.getPropertyAsInt("EDNS_LG_BIND_PORT");
            this.udpSock = new DatagramSocket(port);
        } catch (IOException e) {
        	throw new RuntimeException("could not bind socket: " +e.getMessage()); 
        }
	}

	
	@Override
	public void begin() {
		// nothing to do here
	}

	
	@Override
	public void setReferences(
			Layer1NetworkClient layer1,
			Layer2RecodingSchemeClient layer2, 
			Layer3OutputStrategyClient layer3,
			Layer4TransportClient layer4) {
		assert layer1 == this;
	}
	
	
	@Override
	public void connect(MixList mixList) {
		throw new RuntimeException("not supported"); 
	}
	
	
	@Override
	public void connect() {
		throw new RuntimeException("not supported"); 
	}
	
	
	/**
     * Sends a query to the local recursive nameserver and waits for a
     * response.
     * @param query query to be sent
     * @return response received
     */
	@Override
	public void sendMessage(Request request) {
		try {
            // Send the encrypted query to the local recursive nameserver:
            DatagramPacket sendPacket = new DatagramPacket(request.getByteMessage(), request.getByteMessage().length, EncDnsClient.localNS, EncDnsClient.localNSPort);
            udpSock.send(sendPacket);
        } catch(SocketTimeoutException e) {
            if(EncDnsClient.verbosity >= 1)
            	System.err.println("Query timed out.");
        } catch (IOException e) {
            System.err.println("Error when sending query");
            if(EncDnsClient.verbosity >= 1)
            	e.printStackTrace();
        }
	}

	
	@Override
	public Reply receiveReply() {
		throw new RuntimeException("not supported"); 
	}
	

	@Override
	public void disconnect() {
		throw new RuntimeException("not supported"); 
	}


	@Override
	public int availableReplies() {
		throw new RuntimeException("not supported"); 
	}

}
