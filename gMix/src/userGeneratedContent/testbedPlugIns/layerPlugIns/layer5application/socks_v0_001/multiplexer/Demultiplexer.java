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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;

import staticContent.framework.socket.socketInterfaces.StreamAnonSocketMix;
import staticContent.framework.util.Util;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.socks_v0_001.Config;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.socks_v0_001.socks.SocksHandler;


public class Demultiplexer {

	
	private StreamAnonSocketMix tunnelExit;
	private ConcurrentHashMap<Integer, StreamData> connections = new ConcurrentHashMap<Integer, StreamData>();
	private OutputStream osToMultiplexer;
	private InputStream isFromMultiplexer;
	private Object replySynchronizer = new Object();
	private volatile boolean isConnected;
	private Config config;

	
	public Demultiplexer(StreamAnonSocketMix client, Config config) {
		synchronized (replySynchronizer) {
			this.tunnelExit = client;
			this.osToMultiplexer = tunnelExit.getOutputStream();
			this.isFromMultiplexer = this.tunnelExit.getInputStream();
			this.config = config;
			isConnected = true;
			new RequestReceiver().start();
		}
	}

	
	/**
	 * called after a lost connection on reconnect (TODO: implement calling
	 * part...)
	 * 
	 * @param tunnelExit
	 */
	public void setTunnelExit(StreamAnonSocketMix tunnelExit) {
		synchronized (replySynchronizer) {
			this.tunnelExit = tunnelExit;
			this.osToMultiplexer = tunnelExit.getOutputStream();
			this.isFromMultiplexer = new BufferedInputStream(
					tunnelExit.getInputStream());
			isConnected = true;
			replySynchronizer.notifyAll();
		}
	}

	
	private void waitForConnection() {
		synchronized (replySynchronizer) {
			while (isConnected == false) { // wait for data if necessary
				try {
					replySynchronizer.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				}
			}
		}
	}

	
	/**
	 * Data structure with information about a multiplexed stream.
	 */
	private class StreamData {
		int id;
		ByteArrayOutputStream buffer;
		SocksHandler socksHandler;
		InputStream fromUser;
		OutputStream toUser;
	}

	
	private class RequestReceiver extends Thread {

		@Override
		public void run() {
			while (true) {
				int id;
				int len;
				byte[] message;
				StreamData streamData;
				try {
					id = Util.forceReadInt(isFromMultiplexer); // read id
					if (config.TALK_A_LOT)
						System.out.println("Demultiplexer: mp-id: " +id);streamData = connections.get(id);
					if (streamData == null) { // new multiplexed stream
						streamData = new StreamData();
						connections.put(id, streamData);
						streamData.id = id;
						streamData.buffer = new ByteArrayOutputStream();
						streamData.fromUser = new DemuxInputStream(streamData.buffer);
						streamData.toUser = new DemuxOutputStream(osToMultiplexer, id);
						streamData.socksHandler = new SocksHandler(streamData.fromUser, streamData.toUser, config);
					}
					len = Util.forceReadShort(isFromMultiplexer); // read length
					if (config.TALK_A_LOT)
						System.out.println("Demultiplexer: read " +len +" bytes from Multiplexer");
					if (len < 0) {// DISCONNECT
						streamData.socksHandler.close(); // the disconnect will be acknowledged automatically when the DemuxOutputStream is closed
						connections.remove(streamData.id);
					} else { // read data and store it in buffer (for socks
								// handler)
						message = Util.forceRead(isFromMultiplexer, len);
						synchronized (streamData.buffer) {
							streamData.buffer.write(message);
							streamData.buffer.notifyAll();
						}
					}
				} catch (IOException e) { // connection do multiplexer lost
					e.printStackTrace();
					waitForConnection();
					continue;
				}
			}
		}
	}

}
