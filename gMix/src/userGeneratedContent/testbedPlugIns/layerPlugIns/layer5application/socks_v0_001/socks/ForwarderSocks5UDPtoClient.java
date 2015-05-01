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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.socks_v0_001.socks;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import staticContent.framework.util.Util;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.socks_v0_001.Config;


/**
 * Forwards DatagramPackets to client
 * This class receives UDP-Packets (payload) from webserver, adds an
 * SOCKS5 UDP-Relay-Header with client ip/port and sends it to the client.
 * 
 * @author Haseloff, Schmitz, Sprotte
 *
 */
public class ForwarderSocks5UDPtoClient extends Thread
{
    OutputStream toClient;
    InetAddress clientAddress;
    int clientPort;
    DatagramSocket fromToWebserver;
    Config config;
    
    
    /**
     * Constructor
     * 
     * @param toClient		OutputStream should be write to
     * @param clientAddress	InetAdress of client	
     * @param clientPort	Port of client	
     * @param fromToWebserver	DatagramSocket which receives a DatagramPacket from webserver
     */
    public ForwarderSocks5UDPtoClient(OutputStream toClient, InetAddress clientAddress, int clientPort,
	    DatagramSocket fromToWebserver, Config config)
    {
	this.toClient = toClient;
	this.clientAddress = clientAddress;
	this.clientPort = clientPort;
	this.fromToWebserver = fromToWebserver;
	this.config = config;
	start();
    }

    public void run()
    {
	//int counter = 0;
	while (true)
	{
	    //counter++;
	    try
	    {
		// Receive UDP Payload from Webserver
		DatagramPacket recvPacket = new DatagramPacket(new byte[65535], 65535);
		fromToWebserver.receive(recvPacket);

		//InetAddress recvAddress = recvPacket.getAddress();
		//int recvPort = recvPacket.getPort();
		int recvLen = recvPacket.getLength();
		byte[] bufRecvData = recvPacket.getData();
		//System.out.println("UDPForwarder: Request " + counter + " from " + recvAddress + " from Port " + recvPort
		//	+ " length " + recvLen + "\n" + "Data: " + new String(bufRecvData, 0, recvLen));

		byte[] recvData = new byte[recvLen];
		for (int i = 0; i < recvLen; i++)
		{
		    recvData[i] = bufRecvData[i];
		}

		/* Send UDP Request Header and UDP payload (to Client) *Step 11* */
		toClient.write(0x00); //res
		toClient.write(0x00); //res
		toClient.write(0x00); //Fragment Number
		toClient.write(0x01); //AddressType IPv4
		toClient.write(clientAddress.getAddress()); // ip address of Client
		toClient.write(Util.shortToByteArray(clientPort)); // port of Client
		//byte[] buffer = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05 }; // Payload
		toClient.write(recvData);
		toClient.flush();

		if (config.TALK_A_LOT == true)
		{
		    System.out.println("UDPForwarder: UDP Request Header sent to Client.");
		}
	    } catch (IOException e)
	    {
		e.printStackTrace();
	    }
	}
    }
}
