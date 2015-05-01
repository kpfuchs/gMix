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

import staticContent.framework.EncDnsClient;
import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer1NetworkClient;
import staticContent.framework.interfaces.Layer2RecodingSchemeClient;
import staticContent.framework.interfaces.Layer5ApplicationClient;


/**
 * local proxy for encdns (EncDNS-Proxy); stub resolver is supposed to 
 * communicate with this component
 *
 * ---
 *
 * This is the EncDNS client proxy (also known as local proxy). It should be
 * executed on the client computer or at least be connected to it via a
 * trustworthy connection. The client proxy will encrypt standard DNS requests
 * sent to it by a stub resolver and pass the encrypted request on to the local
 * recursive nameserver. It will also decrypt the encrypted response received 
 * from the local recursive nameserver and pass the decrypted standard DNS 
 * response on to the stub resolver.
 * 
 * This implementation currently supports UDP only.
 */
public class ClientPlugIn extends Implementation implements Layer5ApplicationClient {

    private DatagramSocket stubResolverSocket; // we will receive data from local client applications (via the stub resolver) via this socket
    private EncDnsClient owner;
    private Layer2RecodingSchemeClient layer2;
    private Layer1NetworkClient layer1; 
	
    
	@Override
	public void constructor() {
		try {
            this.stubResolverSocket = new DatagramSocket(EncDnsClient.bindPort, EncDnsClient.bindAddr);
            System.out.println("listening on " +stubResolverSocket.getLocalAddress() +":" +stubResolverSocket.getLocalPort() +" for DNS queries"); 
        } catch (IOException e) {
            System.err.println("Port busy! Exiting. (Try again as root user?)");
            if(EncDnsClient.verbosity >= 1) {
                e.printStackTrace();
            }
            System.exit(1);
        }
		this.owner = EncDnsClient.getInstance();
	}


	@Override
	public void initialize() {
		this.layer2 = (Layer2RecodingSchemeClient) owner.recodingLayerClient.getImplementation();
		this.layer1 = (Layer1NetworkClient) owner.networkLayerClient.getImplementation();
	}


	@Override
	public void begin() {
		for (int i=0; i<EncDnsClient.threads; i++) 
            	new RequestReplyThread().start();
	}


	public class RequestReplyThread extends Thread {

		@Override
		public void run() {
			byte[] rcvbytes = new byte[EncDnsClient.MAX_MSG_SIZE];
			while (true) {
				DatagramPacket rcvPkt = new DatagramPacket(rcvbytes, rcvbytes.length);
				try {
					stubResolverSocket.receive(rcvPkt);
				} catch (IOException e) {
					System.err.println("Error receiving message from stub resolver");
					if(EncDnsClient.verbosity >= 1)
						e.printStackTrace();
					break;
		        }
				if(EncDnsClient.verbosity >= 1)
                    System.out.println("Received request from stub");
				// Copy received standard DNS query into a byte[]
		        byte[] payload = new byte[rcvPkt.getLength()];
		        System.arraycopy(rcvPkt.getData(), rcvPkt.getOffset(), payload, 0, rcvPkt.getLength());
		        EncDnsRequest request = new EncDnsRequest(rcvPkt.getAddress(), rcvPkt.getPort(), payload);
		        request = (EncDnsRequest) layer2.applyLayeredEncryption(request); // skip layer 3 + 4...
		        if (request == null || request.getByteMessage() == null)
		        	continue;
				layer1.sendMessage(request);
				EncDnsReply reply;
				//do {
					reply = (EncDnsReply) layer1.receiveReply(); // if you plan to edit this this class note that this reply is not necessarily the reply for the request sent above; it's just the next reply that arrived at layer 1
					reply = (EncDnsReply) layer2.extractPayload(reply);
					DatagramPacket sendPacket = new DatagramPacket(reply.getByteMessage(), reply.getByteMessage().length, reply.stubResolverAdr, reply.stubResolverPort);
			        try {
			        	stubResolverSocket.send(sendPacket);
			        } catch (IOException e) {
			        	System.err.println("Error when sending response to stub resolver");
			        	if(EncDnsClient.verbosity >= 1)
			        		e.printStackTrace();
			        }
				//} while (reply.getByteMessage()[3] == (byte) 0x82); // SERVFAIL -> try again
			}
		}
	}
	
}
