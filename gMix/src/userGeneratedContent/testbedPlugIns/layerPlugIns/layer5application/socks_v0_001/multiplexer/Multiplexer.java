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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.socks_v0_001.multiplexer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

import staticContent.framework.socket.socketInterfaces.StreamAnonSocket;
import staticContent.framework.util.Util;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.socks_v0_001.Config;


public class Multiplexer {

	private static SecureRandom rand = new SecureRandom();
	
	//private String destAddress;
	//private int destPort;
	private StreamAnonSocket tunnelEntry;
	private ConcurrentHashMap<Integer, Socket> connections = new ConcurrentHashMap<Integer, Socket>(); // multiplexed connections
	private OutputStream toDemultiplexer;
	private InputStream fromDemultiplexer;
	private Object requestSynchronizer = new Object();
	private Config config;
	
	
	/**
	 * Multiplexes and forwards connections to destAddress:destPort.
	 * Demultiplexes any data from destAddress:destPort. 
	 * DestAddress:destPort must be operated by Demultiplexer.java.
	 * New connections (to be multiplexed) can be added with "addConnection()".
	 * @param destAddress
	 * @param destPort
	 */
	public Multiplexer(StreamAnonSocket anonSocket, Config config) {
		this.tunnelEntry = anonSocket;
		this.config = config;
		connect();
		new ReplyForwarder().start();
	}
	
	
	/**
	 * Connects this multiplexer with the demultiplexer (through a tcp tunnel).
	 */
	private synchronized void connect() {
		try {
			this.toDemultiplexer = tunnelEntry.getOutputStream();
			this.fromDemultiplexer = tunnelEntry.getInputStream();
		} catch (IOException e) {
			System.out.println("socks-client: lost connection"); 
			if (config.DEBUG)
				e.printStackTrace();
		}
		System.out.println("client: multiplexed tunnel to mix established");
	}
	
	
	/** Add new connection that shall be multiplexed. */
	public void addConnection(Socket socketToMultiplex) {
		new RequestForwarder(socketToMultiplex).start();
	}
	
	
	private void requestDisconnect(int id) {
		synchronized(requestSynchronizer) { // inform demultiplexer about disconnect; connection will be closed after the demultiplexer acknowledges the disconnect (see thread "ReplyForwarder" below)
			while (true) {
				try {
					toDemultiplexer.write(Util.intToByteArray(id), 0, 4);
					toDemultiplexer.write(Util.shortToByteArray(-1), 0, 2); // -1 means "DISCONNECT"
					toDemultiplexer.flush();
				} catch (IOException e) {
					if (config.DEBUG)
						e.printStackTrace();
					connect();
					continue;
				}
				break;
			}
		}
	}
	
	
	/**
	 * Forwards data from user applications through the tunnel (adds multiplex header).
	 */
	private class RequestForwarder extends Thread {
		
		byte[] buffer = new byte[config.BUFFER_SIZE];
		Socket userApplication;
		InputStream fromUserApplication;
		int id;
		
		
		public RequestForwarder(Socket userApplication) {
			this.userApplication = userApplication;
			this.id = rand.nextInt();
			connections.put(id, userApplication);
		}
		
		
		@Override
		public void run() {
		    	//System.out.println("Multiplexer: run started");
			try {
				this.fromUserApplication = userApplication.getInputStream();
			} catch (IOException e) {
				if (config.DEBUG)
					e.printStackTrace();
				requestDisconnect(id);
				return;
			}
			while(true) {
				// try to read from user application:
				int readBytes;
				try {
					readBytes = fromUserApplication.read(buffer);
					//System.out.println("Multiplexer: read "+ readBytes +" bytes from application");			
					if (readBytes < 1) {
						requestDisconnect(id);
						return;
					}
				} catch (IOException e) {
					if (config.DEBUG)
						e.printStackTrace();
					requestDisconnect(id);
					return;
				}
				// try to send to demultiplexer:
				while(true) {
					try {
						synchronized (requestSynchronizer) {
							// send data
							toDemultiplexer.write(Util.intToByteArray(id), 0, 4); // send id of this multiplexed stream
							toDemultiplexer.write(Util.shortToByteArray(readBytes), 0, 2); // send length of the data to come
							toDemultiplexer.write(buffer, 0, readBytes); // send the data
							toDemultiplexer.flush();
						}
					} catch (IOException e) {
						System.out.println("lost connection to demultiplexer"); 
						if (config.DEBUG)
							e.printStackTrace();
						connect();
					}	
					break;
				}	
			}
		}
	}
	
	
	/**
	 * Forwards data from the tunnel to user applications (removes multiplex header).
	 */
	private class ReplyForwarder extends Thread {
		
		@Override
		public void run() {
			while (true) {
				int id;
				int len;
				byte[] message = null;
				try {
					id = Util.forceReadInt(fromDemultiplexer); // read id
					len = Util.forceReadShort(fromDemultiplexer); // read length
					if (len == -1) { // disconnect acknowledged -> close connection
						try {
							connections.get(id).close();
						} catch (IOException e) {}
						connections.remove(id);
					} else { // no disconnect
						message = Util.forceRead(fromDemultiplexer, len);
					}
				} catch (IOException e) { // connection do demultiplexer lost
					System.out.println("lost connection to demultiplexer");
					if (config.DEBUG)
						e.printStackTrace();
					connect();
					continue;
				}
				try {
					Socket s = connections.get(id);
					if (s == null || message == null) // already closed
						continue;
					OutputStream osToUserApplication = s.getOutputStream();
					osToUserApplication.write(message);
					osToUserApplication.flush();
				} catch (IOException e) {
					// disconnect handling is done by RequestForwarder -> read next message
					if (config.DEBUG)
						e.printStackTrace();
					continue;
				}
			}
		}
	}
	
}
