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
import framework.core.userDatabase.DatabaseEventListener;
import framework.core.userDatabase.User;
import framework.core.util.Util;


public class PrevMixHandler_UDP_multiplexed extends SubImplementation implements DatabaseEventListener {

	private int port; 
	private InetAddress bindAddress;
	private DatagramSocket previousMixSocket;
	private int maxRequestLength;
	//private int queueBlockSize;
	
	private HashMap<User,Integer> thisToPrevMixIDs;
	
	private RequestThread requestThread;
	//private ReplyThread replyThread;
	private int queueBlockSize;
	
	
	@Override
	public void constructor() {
		//this.queueBlockSize = settings.getPropertyAsInt("QUEUE_BLOCK_SIZE");
		this.bindAddress = settings.getPropertyAsInetAddress("GLOBAL_MIX_BIND_ADDRESS");
		this.port = settings.getPropertyAsInt("GLOBAL_MIX_BIND_PORT");
		this.maxRequestLength = settings.getPropertyAsInt("MAX_REQUEST_LENGTH");
		this.thisToPrevMixIDs = new HashMap<User,Integer>((int)Math.round((double)settings.getPropertyAsInt("MAX_CONNECTIONS") * 1.3d));
		this.requestThread = new RequestThread();
		//this.replyThread = new ReplyThread();
		this.queueBlockSize = settings.getPropertyAsInt("QUEUE_BLOCK_SIZE");
	}

	
	@Override
	public void initialize() {

	}

	
	@Override
	public void begin() {
		try {
			this.previousMixSocket = new DatagramSocket(port, bindAddress);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not open ServerSocket"); 
		}
		System.out.println(anonNode +" listening on " +bindAddress +":" +port);
		this.requestThread.setPriority(Thread.MAX_PRIORITY);
		//this.replyThread.setPriority(Thread.MAX_PRIORITY);
		this.requestThread.start();
		//this.replyThread.start();
	}

	
	@Override
	public void userAdded(User user) {

	}

	
	@Override
	public void userRemoved(User user) {
		thisToPrevMixIDs.remove(user);
	}

	private class RequestThread extends Thread {
		
		@Override
		public void run() {
			while (true) {
				try {
					while (true) {
						Request[] requests = new Request[queueBlockSize];
						for (int i=0; i<requests.length; i++) {
							// receive request
			            	byte[] buf = new byte[maxRequestLength];
			            	DatagramPacket packet = new DatagramPacket(buf, buf.length);
			            	previousMixSocket.receive(packet);
			            	byte[] packetPayload = Arrays.copyOf(packet.getData(), packet.getLength());
			            	int channelIdentifier = Util.byteArrayToInt(Arrays.copyOf(packetPayload, 4));
			            	packetPayload = Arrays.copyOfRange(packetPayload, 4, packetPayload.length);
			            	//System.out.println("received: " +Util.md5(packetPayload) +", from " +channelIdentifier); // TODO: remove
			            	User user = userDatabase.getUser(channelIdentifier);
							if (user == null) {
								user = userDatabase.generateUser(channelIdentifier);
								userDatabase.addUser(user);
								thisToPrevMixIDs.put(user, channelIdentifier);
							}
							if (packetPayload.length > maxRequestLength) {
								System.out.println(anonNode +" wrong size for request received"); 
								continue;
							}
							requests[i] = MixMessage.getInstanceRequest(packetPayload, user);
							//System.out.println(mix +" received this message (ciphertext): " +Util.md5(request.getByteMessage())); // TODO
						}
						anonNode.putInRequestInputQueue(requests);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
}
