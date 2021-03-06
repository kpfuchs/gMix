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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer1network.cascade_UDP_v0_001;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer1NetworkClient;
import staticContent.framework.interfaces.Layer2RecodingSchemeClient;
import staticContent.framework.interfaces.Layer3OutputStrategyClient;
import staticContent.framework.interfaces.Layer4TransportClient;
import staticContent.framework.message.Reply;
import staticContent.framework.message.Request;
import staticContent.framework.routing.MixList;
import staticContent.framework.routing.RoutingMode;


public class ClientPlugIn extends Implementation implements Layer1NetworkClient {
	
	private DatagramSocket mix;
	
	
	@Override
	public void constructor() {
		if (anonNode.IS_DUPLEX)
			throw new RuntimeException("supports simplex only currently");
	}
	

	@Override
	public void initialize() {
		
	}

	
	@Override
	public void begin() {
		
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
		try {
			mix = new DatagramSocket();
			SocketAddress socketAddress = new InetSocketAddress(mixList.addresses[0], mixList.ports[0]);
			mix.connect(socketAddress);
		} catch (IOException e) {
			System.err.println("could not init socket");
		}
	}
	
	
	@Override
	public void connect() {
		if (anonNode.ROUTING_MODE != RoutingMode.GLOBAL_ROUTING)
			throw new RuntimeException("for free route sockets, call connect(MixList mixList)"); 
		connect(globalRoutingPlugInClient.choseRoute());
	}
	
	
	boolean first = true;
	@Override
	public synchronized void sendMessage(Request request) {
		try {
			DatagramPacket packet = new DatagramPacket(request.getByteMessage(), request.getByteMessage().length, mix.getRemoteSocketAddress());
			mix.send(packet);
			/*if (first) {
				try {Thread.sleep(5000);} catch (InterruptedException e1) {e1.printStackTrace();} // TODO
				first = false;
			}*/
		} catch (IOException e1) {
			e1.printStackTrace();
			try {Thread.sleep(5000);} catch (InterruptedException e2) {e2.printStackTrace();}
			connect();
		}
	}

	
	@Override
	public Reply receiveReply() {
		throw new RuntimeException("not yet supported"); 
	}
	

	@Override
	public void disconnect() {
		mix.close();
	}


	@Override
	public int availableReplies() {
		throw new RuntimeException("not yet supported"); 
	}

}
