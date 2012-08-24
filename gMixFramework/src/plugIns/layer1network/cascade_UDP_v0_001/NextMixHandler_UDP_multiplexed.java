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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.SecureRandom;
import java.util.HashMap;

import framework.core.controller.SubImplementation;
import framework.core.message.Request;
import framework.core.routing.MixList;
import framework.core.userDatabase.DatabaseEventListener;
import framework.core.userDatabase.User;
import framework.core.util.Util;


public class NextMixHandler_UDP_multiplexed extends SubImplementation implements DatabaseEventListener {

	private InetAddress nextMixAddress;
	private int nextMixPort;
	private SocketAddress nextMixSocketAddress;
	private DatagramSocket nextMixSocket;	// TODO: dtls
	
	private RequestThread requestThread;
	//private ReplyThread replyThread;
	
	private HashMap<User,Integer> thisToNextMixIDs;
	private HashMap<Integer,User> nextMixToThisIDs;
	private SecureRandom random = new SecureRandom(); // TODO: use standard random provided by framework
	
	
	@Override
	public void constructor() {
		this.requestThread = new RequestThread();
		this.thisToNextMixIDs = new  HashMap<User,Integer>((int)Math.round((double)settings.getPropertyAsInt("MAX_CONNECTIONS") * 1.3d));
		this.nextMixToThisIDs = new  HashMap<Integer,User>((int)Math.round((double)settings.getPropertyAsInt("MAX_CONNECTIONS") * 1.3d));
	}

	@Override
	public void initialize() {
		MixList mixList = anonNode.mixList;
		if (anonNode.PUBLIC_PSEUDONYM+1 < mixList.numberOfMixes) { // TODO
			this.nextMixAddress = mixList.addresses[anonNode.PUBLIC_PSEUDONYM+1];
			this.nextMixPort = mixList.ports[anonNode.PUBLIC_PSEUDONYM+1];
		}
		userDatabase.registerEventListener(this);
	}

	@Override
	public void begin() {
		try {
			this.nextMixSocket = new DatagramSocket();
			this.nextMixSocketAddress = new InetSocketAddress(this.nextMixAddress, this.nextMixPort);
			nextMixSocket.connect(nextMixSocketAddress);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not open ServerSocket"); 
		}
		System.out.println(anonNode +" listening on " +nextMixAddress +":" +nextMixPort);
		requestThread.setPriority(Thread.MAX_PRIORITY);
		requestThread.start();
	}

	@Override
	public void userAdded(User user) {
		synchronized (nextMixToThisIDs) {
			while (true) {
				int idForNextMix = random.nextInt();
				if (nextMixToThisIDs.get(idForNextMix) != null)
					continue;
				this.thisToNextMixIDs.put(user, idForNextMix);
				this.nextMixToThisIDs.put(idForNextMix, user);
				break;
			}
		}
	}

	@Override
	public void userRemoved(User user) {
		synchronized (nextMixToThisIDs) {
			this.nextMixToThisIDs.remove(thisToNextMixIDs.get(user));
			this.thisToNextMixIDs.remove(user);
		}
	}
	
	
	private class RequestThread extends Thread {
		
		@Override
		public void run() {
			
			while (true) {
					Request[] requests = anonNode.getFromRequestOutputQueue();
					for (int i=0; i<requests.length; i++) {
						try {
							byte[] id = null;
							synchronized (nextMixToThisIDs) {
								id = Util.intToByteArray(thisToNextMixIDs.get(requests[i].getOwner()));
							}
							byte[] packetPayload = Util.concatArrays(id, requests[i].getByteMessage());
							DatagramPacket packet = new DatagramPacket(packetPayload, packetPayload.length, nextMixSocketAddress);
							//System.out.println("sending: " +Util.md5(packetPayload) +", for " +Util.byteArrayToInt(id)); // TODO: remove
							nextMixSocket.send(packet);
						} catch (IOException e) {
							e.printStackTrace();
							try {Thread.sleep(1000);} catch (InterruptedException e1) {e1.printStackTrace();} // TODO dos? (prevent log flood in case of error)
							continue;
						} catch (NullPointerException e) {
							e.printStackTrace();
							continue;
						}	
					}
			}
		}	
	}



}
