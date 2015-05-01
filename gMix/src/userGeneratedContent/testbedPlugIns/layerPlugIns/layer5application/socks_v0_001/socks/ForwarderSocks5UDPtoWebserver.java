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
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import staticContent.framework.util.Util;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.socks_v0_001.Config;


/**
 * Forwards DatagramPackets to webserver.
 * Reads a DatagramPacket from client with SOCKS5 UDP Relay Header. 
 * Deletes the header and forms a UDP Packet with the destination ip and port included in this header. 
 * 
 * @author Haseloff, Schmitz, Sprotte
 *
 */
public class ForwarderSocks5UDPtoWebserver extends Thread
{
    InputStream fromClient;
    DatagramSocket fromToWebserver;
    Config config;

    public ForwarderSocks5UDPtoWebserver(InputStream fromClient, DatagramSocket fromToWebserver, Config config)
    {
	this.fromClient = fromClient;
	this.fromToWebserver = fromToWebserver;
	this.config = config;
	start();
    }
    
    /**
     * Reads a couple of bytes depending on the address type.
     * 
     * @param aTyp 	valid values: 0x01, 0x03, 0x04
     * @return 		InetAddress
     */
    private InetAddress getInetAddress(byte aTyp)
    {
	byte[] byteAddress = null;
	int length;
	InetAddress address = null;
	try
	{
	    switch (aTyp)
	    {
	    case 0x01: // IP v4
		length = 4;
		byteAddress = Util.forceRead(fromClient, length);
		address = InetAddress.getByAddress(byteAddress);
		break;
	    case 0x03: // Domain, first byte defines the length of it
		length = Util.unsignedByteToShort((byte) fromClient.read());
		byteAddress = Util.forceRead(fromClient, length);
		String stringAddress = Util.getStringWithoutNewLines(byteAddress);
		address = InetAddress.getByName(stringAddress);
		break;
	    case 0x04: // IP v6
		length = 16;
		byteAddress = Util.forceRead(fromClient, length);
		address = InetAddress.getByAddress(byteAddress);
		break;
	    default:
		//this.sendSocks5FailReply((byte) 0x05, (byte) 0x08, (byte) 0x00); // reply 0x08: Address type not supported
		System.out.println("UDPForwarder: i do only support IP V4, Domains or IP V6 addresses");
		//System.out.println("UDPForwarder: Sent CONNECT reply:	0x08");
		//close();
	    }
	} catch (UnknownHostException e)
	{
	    e.printStackTrace();
	} catch (IOException e)
	{
	    e.printStackTrace();
	}
	return address;
    }

    /**
     * Forms an UDP Packet and sends it to webserver.
     * 
     * @param destIP	destination ip, exracted from the header
     * @param destPort	destination port, exracted from the header
     * @param payload	
     */
    private void sendUDPPayloadToWebserver(InetAddress destIP, int destPort, byte[] payload)
    {
	DatagramPacket packet = new DatagramPacket(payload, payload.length, destIP, destPort);
	try
	{
	    fromToWebserver.send(packet);
	} catch (IOException e)
	{

	    e.printStackTrace();
	}
    }

    public void run()
    {
	while (true)
	{
	    /* Receive UDP Request Header and UDP payload from Client *Step 8* */
	    try
	    {
		/*byte[] UDPReserved = */Util.forceRead(fromClient, 2);
		/*byte fragment = (byte) */fromClient.read();
		byte aTyp = (byte) fromClient.read();
		InetAddress destIP = getInetAddress(aTyp);
		byte[] destbPort = Util.forceRead(fromClient, 2); // read DST.PORT
		int destPort = Util.unsignedShortToInt(destbPort);
		//byte[] payload = Util.forceRead(fromClient, 24); // TODO: flexible payload, read until 2 zero bytes
		System.out.println("UDPForwarder: " + fromClient.available());
		byte[] payload = Helper.forceReadUntilNzeroBytesOrEnd(fromClient, Config.N_ZERO_BYTES);

		if (config.TALK_A_LOT == true)
		{
		    System.out.println("UDPForwarder: read UDP Request Header from Client");
		}

		// Send UDP Payload to Webserver
		this.sendUDPPayloadToWebserver(destIP, destPort, payload);
	    } catch (IOException e)
	    {
		e.printStackTrace();
	    }
	}
    }

}
