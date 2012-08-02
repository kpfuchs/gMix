/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2012  Karl-Peter Fuchs
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
 */
package plugIns.layer1network.cascade_UDP_v0_001;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;

import framework.core.controller.SubImplementation;
import framework.core.message.MixMessage;
import framework.core.message.Request;
import framework.core.userDatabase.User;


public class ClientHandler_UDP_FCFS extends SubImplementation {
	
	private int port; 
	private InetAddress bindAddress;
	private DatagramSocket serverSocket;
	private int maxRequestLength;
	private int queueBlockSize;
	//private boolean DUPLEX_ON;
	
	
	@Override
	public void constructor() {
		this.bindAddress = settings.getPropertyAsInetAddress("GLOBAL_MIX_BIND_ADDRESS");
		this.port = settings.getPropertyAsInt("GLOBAL_MIX_BIND_PORT");
		this.maxRequestLength = settings.getPropertyAsInt("MAX_REQUEST_LENGTH");
		this.queueBlockSize = settings.getPropertyAsInt("QUEUE_BLOCK_SIZE");
	}
	

	@Override
	public void initialize() {
		
	}
	

	@Override
	public void begin() {
		try {
			this.serverSocket = new DatagramSocket(port, bindAddress);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not open ServerSocket"); 
		}
		System.out.println(anonNode +" listening on " +bindAddress +":" +port);
		RequestThread r = new RequestThread();
		r.setPriority(Thread.MAX_PRIORITY);
		r.start();
	}

	
	private class RequestThread extends Thread {
		
		HashMap<String, User> users = new HashMap<String, User>();
		
		@Override
		public void run() {
			/*new Thread(// TODO: remove
					new Runnable() {
						public void run() {
							while (true) {
								System.out.println("free in ioh-queue: " +anonNode.getRequestInputQueue().size()); 
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					}
				).start(); */
			
			try {
				int ctr = 0;
				Request[] requests = new Request[queueBlockSize];
				while (true) {
					// receive request
	            	byte[] buf = new byte[maxRequestLength];
	            	DatagramPacket packet = new DatagramPacket(buf, buf.length);
					serverSocket.receive(packet);
					String userId = packet.getAddress().toString() + packet.getPort();
					User user = users.get(userId);
					if (user == null) {
						user = userDatabase.generateUser();
						userDatabase.addUser(user);
						users.put(userId, user);
					}
					if (packet.getLength() > maxRequestLength)
						throw new IOException("warning: user " +user +" sent a too large message");
					byte[] payload = Arrays.copyOf(packet.getData(), packet.getLength());
					Request request = MixMessage.getInstanceRequest(payload, user);
					
					requests[ctr++] = request;
					if (ctr == queueBlockSize) {
						anonNode.putInRequestInputQueue(requests); // might block
						requests = new Request[queueBlockSize];
						ctr = 0;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
