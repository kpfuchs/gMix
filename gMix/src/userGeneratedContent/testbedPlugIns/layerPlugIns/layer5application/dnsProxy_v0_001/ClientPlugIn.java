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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.dnsProxy_v0_001;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer5ApplicationClient;
import staticContent.framework.routing.RoutingMode;
import staticContent.framework.socket.socketInterfaces.DatagramAnonSocket;
import staticContent.framework.socket.socketInterfaces.AnonSocketOptions.CommunicationDirection;
import staticContent.framework.util.Util;


public class ClientPlugIn extends Implementation implements Layer5ApplicationClient {

    private DatagramSocket stubResolverSocket; // we will receive data from local client applications (via the stub resolver) via this socket
	private DatagramAnonSocket mixSocket;
    private boolean DNSP_DEBUG;
    private int MAX_MSG_SIZE;
    private SecureRandom random = new SecureRandom();
    private Map<Integer, MsgInfo> idMapping = new ConcurrentHashMap<Integer, MsgInfo>(10000);
    private ArrayBlockingQueue<byte[]> buffer = new ArrayBlockingQueue<byte[]>(1000);
    private int INTERNAL_MIX_PORT;
    
	@Override
	public void constructor() {
		this.DNSP_DEBUG = settings.getPropertyAsBoolean("DNSP_DEBUG");
		this.MAX_MSG_SIZE = settings.getPropertyAsInt("DNS_MAX_MSG_SIZE");
		this.INTERNAL_MIX_PORT = settings.getPropertyAsInt("INTERNAL_MIX_PORT");
		try {
            this.stubResolverSocket = new DatagramSocket(settings.getPropertyAsInt("DNS_LISTENING_PORT"));
        } catch (IOException e) {
            System.err.println("Port busy! Exiting.");
            if (DNSP_DEBUG)
            	e.printStackTrace();
        }
		System.out.println("listening on " +stubResolverSocket.getLocalAddress() + ":" +stubResolverSocket.getLocalPort()); 
		System.err.println("Warning: This is a test plug-in - do NOT send any sensitive data via this plug-in!");
	}


	@Override
	public void initialize() {
		
	}


	@Override
	public void begin() {
		this.mixSocket = anonNode.createDatagramSocket(
				CommunicationDirection.DUPLEX,
				false,
				false,
				super.anonNode.ROUTING_MODE != RoutingMode.GLOBAL_ROUTING
				);
		//this.mixSocket.connect(settings.getPropertyAsInt("INTERNAL_MIX_PORT"));
		new ClientListenerThread().start();
		new RequestThread().start();
        new ReplyThread().start();
	}


	public class ClientListenerThread extends Thread {

		@Override
		public void run() {
			byte[] rcvbytes = new byte[MAX_MSG_SIZE];
			while (true) {
				DatagramPacket rcvPkt = new DatagramPacket(rcvbytes, rcvbytes.length);
				try {
					stubResolverSocket.receive(rcvPkt);
				} catch (IOException e) {
					System.err.println("Error receiving message from stub resolver");
					if(DNSP_DEBUG)
						e.printStackTrace();
					break;
		        }
				if(DNSP_DEBUG)
                    System.out.println("client received request from stub");
				byte[] payload = new byte[rcvPkt.getLength()];
		        System.arraycopy(rcvPkt.getData(), rcvPkt.getOffset(), payload, 0, rcvPkt.getLength());
		        //Message dnsQuery = Message.newQuery(Record.fromWire(payload, Section.QUESTION)); // TODO Section.QUESTION correct?
				//.newRecord(Name.fromString(requestedURL+"."), Type.value(dnsQueryType), DClass.IN));
				
		        byte[] msgID = generateMessageID();
				int messageID = Util.byteArrayToInt(msgID);
				if(DNSP_DEBUG)
					System.out.println(" adding msgid " + messageID +" (" +Util.toHex(payload) +")");
		        byte[] query = Util.concatArrays(msgID, payload);
				if (query.length > MAX_MSG_SIZE) {
					System.err.println("warning: received too big request; dropping it");
					continue;
				}
				idMapping.put(messageID, new MsgInfo(rcvPkt.getAddress(), rcvPkt.getPort()));
				buffer.add(query);
			}
		}
		
	}
	
	
	public class RequestThread extends Thread {

		@Override
		public void run() {
			while (true) {
				try {
					Payload payload = new Payload(MAX_MSG_SIZE);
					payload.addMessage(buffer.take());
					while (buffer.peek() != null && buffer.peek().length <= payload.remaining())
						payload.addMessage(buffer.take());
					if(DNSP_DEBUG)
	                    System.out.println("client send mix message  (" +Util.toHex(payload.getBytePayload()) +")");
					mixSocket.sendMessage(INTERNAL_MIX_PORT, payload.getBytePayload());
				} catch (InterruptedException e) {
					continue;
				}
			}
		}
	
	}
	
	
	public class ReplyThread extends Thread {

		@Override
		public void run() {
			while (true) {
				byte[] data = mixSocket.receiveMessage().getByteMessage();
				if(DNSP_DEBUG)
                    System.out.println("client received mix message (" +Util.toHex(data) +")");
				Payload payload = new Payload(data, data.length);
				List<byte[]> dnsReplies = DNSUtils.splitArrayOnPattern(payload.getMessage(), DNSUtils.dnsTerminator, 0);
				for (byte[] reply: dnsReplies) {
					int msgID = Util.byteArrayToInt(Arrays.copyOfRange(reply, 0, 4));
					byte[] dnsMsg = Arrays.copyOfRange(reply, 4, reply.length);
					MsgInfo msgInfo;
					msgInfo = idMapping.remove(msgID);
					if (msgInfo == null) {
						System.err.println("warning: received reply with unknoen id"); 
						continue;
					}
					// send to client application
					DatagramPacket sendPacket = new DatagramPacket(dnsMsg, 0, dnsMsg.length, msgInfo.adr, msgInfo.port);
			        try {
			        	stubResolverSocket.send(sendPacket);
			        } catch (IOException e) {
			        	System.err.println("Error when sending response to stub resolver");
			        }
			        if(DNSP_DEBUG)
	                    System.out.println("client sent reply");
				}
			}
		}
	
	}
	
	private byte[] generateMessageID(){
		return Util.intToByteArray(Math.abs(random.nextInt()));
	}
	
	
    private class MsgInfo {
    	
    	public InetAddress adr;
    	public int port;
    	
    	public MsgInfo(InetAddress adr, int port) {
    		this.adr = adr;
    		this.port = port;
    	}
    	
    }
}
