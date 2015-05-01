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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ArrayBlockingQueue;

import staticContent.framework.EncDnsClient;
import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer1NetworkClient;
import staticContent.framework.interfaces.Layer2RecodingSchemeClient;
import staticContent.framework.interfaces.Layer3OutputStrategyClient;
import staticContent.framework.interfaces.Layer4TransportClient;
import staticContent.framework.message.Reply;
import staticContent.framework.message.Request;
import staticContent.framework.routing.MixList;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.encDNS_v0_001.EncDnsReply;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.encDNS_v0_001.EncDnsRequest;


public class ClientPlugIn extends Implementation implements Layer1NetworkClient {
	
	private ArrayBlockingQueue<EncDnsReply> pendingReplies;
	private int currPort = 1025;
	
	
	@Override
	public void constructor() {
		this.pendingReplies = new ArrayBlockingQueue<EncDnsReply>(settings.getPropertyAsInt("REPLY_MSG_BUFFER_SIZE"));
	}
	

	@Override
	public void initialize() {
		// nothing to do here
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
		EncDnsReply reply = ((EncDnsRequest)request).createReplyDataStructure();
		try {
            // Open a socket on a new port:
            DatagramSocket udpSock = null;
            while (udpSock == null) {
                try {
                    int port = getCurrPort();
                    udpSock = new DatagramSocket(port);
                } catch (SocketException e) {
                    // do nothing as this is being handled by the while loop
                }
            }
            // Send the encrypted query to the local recursive nameserver:
            DatagramPacket sendPacket = new DatagramPacket(request.getByteMessage(), request.getByteMessage().length, EncDnsClient.localNS, EncDnsClient.localNSPort);
            udpSock.send(sendPacket);
            // Wait for a response:
            byte[] rcvbytes = new byte[EncDnsClient.MAX_MSG_SIZE];
            DatagramPacket rcvPkt = new DatagramPacket(rcvbytes, rcvbytes.length);
            udpSock.setSoTimeout(EncDnsClient.TIMEOUT); // Make sure we do not wait for ages...
            udpSock.receive(rcvPkt);
            udpSock.close();
            byte[] rcvDNS = new byte[rcvPkt.getLength()];
            System.arraycopy(rcvPkt.getData(), rcvPkt.getOffset(), rcvDNS, 0, rcvPkt.getLength());
            if(EncDnsClient.verbosity >= 1)
                 System.out.println("Received reply from authoritative nameserver");
            reply.setByteMessage(rcvDNS);
        } catch(SocketTimeoutException e) {
            if(EncDnsClient.verbosity >= 1)
            	System.err.println("Query timed out.");
            reply.setByteMessage(null);
        } catch (IOException e) {
            System.err.println("Error when sending query");
            if(EncDnsClient.verbosity >= 1)
             e.printStackTrace();
            reply.setByteMessage(null);
        }
		try {
			pendingReplies.put(reply);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	
    /**
     * Returns the port to use for sending the next query. This will iterate
     * through all unprivileged (i.e. >=1025) ports.
     * @return port to use for next query
     */
    private synchronized int getCurrPort() {
        if(currPort==65535) {
            // on overflow, revert to port 1025 (first unprivileged port)
            currPort = 1025;
        } else {
            currPort++;
        }
        return currPort;
    }
	
	@Override
	public Reply receiveReply() {
		Reply r = null;
		do {
			try {
				r = pendingReplies.take();
			} catch (InterruptedException e) {
				continue;
			}
		} while (r == null);
		return r;
		
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
