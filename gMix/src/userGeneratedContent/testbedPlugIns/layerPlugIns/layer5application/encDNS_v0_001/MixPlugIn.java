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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.encDNS_v0_001;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import staticContent.framework.EncDnsServer;
import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer5ApplicationMix;


public class MixPlugIn extends Implementation implements Layer5ApplicationMix {

	private LocalResolver localResolver;
	
	
	@Override
	public void constructor() {
		if (EncDnsServer.resolveToLocalhost || EncDnsServer.useInternalResolver)
			this.localResolver = new LocalResolver(EncDnsServer.resolveToLocalhost, settings);
	}

	
	@Override
	public void initialize() {
		// stateless plugin -> nothing to do
	}

	
	@Override
	public void begin() {
		// stateless plugin -> nothing to do
	}

	
	/**
     * Sends a query to the remote recursive nameserver and waits for a
     * response.
     * @param query query to be sent
     * @return response received
     */
    public byte[] sendQueryAndListenForReply(byte[] query) {
    	try {
        	if (EncDnsServer.resolveToLocalhost || EncDnsServer.useInternalResolver) { 
        		return localResolver.resolve(query);
        	} else {
        		// Open a socket on a new port
                DatagramSocket udpSock = null;
                while (udpSock == null) {
                    try {
                        int port = 1024 + EncDnsServer.rnd.nextInt(64512);
                        udpSock = new DatagramSocket(port);
                    } catch (SocketException e) {
                        // do nothing as this is being handled by the while loop
                    }
                }
                // Send the decrypted query to the remote recursive nameserver
                DatagramPacket sendPacket = new DatagramPacket(query, query.length, EncDnsServer.nsaddr, EncDnsServer.port);
                udpSock.send(sendPacket);
                
                // Wait for a response
                byte[] rcvbytes = new byte[EncDnsServer.MAX_MSG_SIZE];
                DatagramPacket rcvPkt = new DatagramPacket(rcvbytes, rcvbytes.length);
                udpSock.setSoTimeout(EncDnsServer.timeout); // Make sure we do not wait for ages...
                udpSock.receive(rcvPkt);
                udpSock.close();
                byte[] rcvDNS = new byte[rcvPkt.getLength()];
                System.arraycopy(rcvPkt.getData(), rcvPkt.getOffset(), rcvDNS, 0, rcvPkt.getLength());
                if(EncDnsServer.verbosity >= 1) {
                    System.out.println("Received reply from aNS");
                }
                return rcvDNS;
        	}
        } catch (SocketTimeoutException e) {
            if(EncDnsServer.verbosity >= 1) {
                System.err.println("Query timed out!");
            }
            return null;
        } catch (IOException e) {
            System.err.println("IO error when querying nameserver");
            if(EncDnsServer.verbosity >= 1) {
                System.err.println(e);
            }
            return null;
        }
    }

}
