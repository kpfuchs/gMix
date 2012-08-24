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
package plugIns.layer1network.sourceRouting_TCP_v0_001;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ArrayBlockingQueue;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer1NetworkClient;
import framework.core.interfaces.Layer2RecodingSchemeClient;
import framework.core.interfaces.Layer3OutputStrategyClient;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.routing.MixList;
import framework.core.util.Util;


public class ClientPlugIn extends Implementation implements Layer1NetworkClient {
	
	private int timeout;
	private int requestBufferSize;
	private int replyBufferSize;
	private ArrayBlockingQueue<Reply> replyCache;
	private BufferedOutputStream mixOutputStream;
	private BufferedInputStream mixInputStream;
	private int nextHopId;
	private Socket mix;
	private ServerSocket replySocket;
	private volatile boolean shutdownRequested = false;
	private short RECEIVER_PORT;
	private boolean serverSocketModeOn = false;
	private int replyLength = Util.NOT_SET;
	
	
	@Override
	public void constructor() {
		this.requestBufferSize = settings.getPropertyAsInt("CLIENT_REQUEST_BUFFER_SIZE");
		this.replyBufferSize = settings.getPropertyAsInt("CLIENT_REPLY_BUFFER_SIZE");
		this.timeout = settings.getPropertyAsInt("CLIENT_CONNECTION_TIMEOUT");
		if (!anonNode.IS_CONNECTION_BASED && anonNode.IS_DUPLEX) { // open serverSocket for replies 
			serverSocketModeOn = true;
			while (true) {
				try {
					this.RECEIVER_PORT = (short)Util.getRandomInt(11000, 15000);
					InetAddress bindAddress = settings.getPropertyAsInetAddress("GLOBAL_MIX_BIND_ADDRESS");
					this.replySocket = new ServerSocket(RECEIVER_PORT, 5, bindAddress);
					new ReplyThread().start();
				} catch (IOException e) {
					System.err.println("could not create replySocket... try again"); 
					try {Thread.sleep(5000);} catch (InterruptedException e1) {e1.printStackTrace();}
					continue;
				}
				break;
			}
		}
	}
	

	@Override
	public void initialize() {

	}

	
	@Override
	public void begin() {
		
	}

	
	@Override
	public void setReferences(
			Layer1NetworkClient layer1,
			Layer2RecodingSchemeClient layer2, 
			Layer3OutputStrategyClient layer3) {
		assert layer1 == this;
	}
	
	
	@Override
	public void connect(MixList mixList) {
		connect(mixList.mixIDs[0]);
	}
	
	
	@Override
	public void connect() {
		throw new RuntimeException("this plug-in only supports free routes, not cascades"); 
	}
	
	
	private void connect(int mixID) {
		this.mix = new Socket();
		this.nextHopId = mixID;
		assert anonNode.mixList.getAddress(mixID) != null;
		SocketAddress socketAddress = new InetSocketAddress(anonNode.mixList.getAddress(mixID), anonNode.mixList.getPort(mixID));
		try {
			if (anonNode.DISPLAY_ROUTE_INFO)
				System.out.println("" +anonNode +" connecting to mix " +mixID +"("+anonNode.mixList.getAddress(mixID) +":" +anonNode.mixList.getPort(mixID) +")"); 
			this.mix.connect(socketAddress, timeout);
			this.mixOutputStream = new BufferedOutputStream(mix.getOutputStream(), requestBufferSize);
			this.mixInputStream = new BufferedInputStream(mix.getInputStream(), replyBufferSize);
			if (anonNode.IS_DUPLEX) {
				this.replyCache = new ArrayBlockingQueue<Reply>(5);
			}
		} catch (IOException e) {
			System.err.println("could not connect to mix... try again");
			try {Thread.sleep(5000);} catch (InterruptedException e1) {e1.printStackTrace();}
			connect(mixID);
		}
	}
	
	
	@Override
	public void sendMessage(Request request) {
		if (!anonNode.IS_CONNECTION_BASED)
			connect(request.nextHopAddress);
		assert request.nextHopAddress == this.nextHopId;
		try {
			if (anonNode.DISPLAY_ROUTE_INFO)
				System.out.println("" +anonNode +" sending message to mix " +request.nextHopAddress +" (layer1)"); 
			if (!anonNode.IS_CONNECTION_BASED && anonNode.IS_DUPLEX) // send port for reply
				mixOutputStream.write(Util.shortToByteArray(RECEIVER_PORT));
			mixOutputStream.write(Util.intToByteArray(request.getByteMessage().length));
			mixOutputStream.write(request.getByteMessage());
			mixOutputStream.flush(); 
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("connection lost... try again"); 
			connect(request.nextHopAddress);
		}
		if (!anonNode.IS_CONNECTION_BASED)
			disconnect();
	}

	
	@Override
	public Reply receiveReply() {
		if (serverSocketModeOn) {
			try {
				return replyCache.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return receiveReply();
			}
		} else {
			if (replyCache.size() > 0) {
				try {
					return replyCache.take();
				} catch (InterruptedException e) {
					e.printStackTrace();
					return receiveReply();
				}
			} else {
				return forceReadReply();
			}
		}
	}
	

	@Override
	public void disconnect() {
		try {
			mix.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	@Override
	public int availableReplies() {
		if (serverSocketModeOn) {
			return replyCache.size();
		} else {
			while (true) {
				Reply reply = tryReadReply();
				if (reply == null)
					break;
				else
					putInReplyCache(reply);
			}
			return replyCache.size();
		}
	}	
	
	
	private void putInReplyCache(Reply reply) {
		try {
			replyCache.put(reply);
		} catch (InterruptedException e) {
			e.printStackTrace();
			putInReplyCache(reply);
		}
	}
	
	
	private class ReplyThread extends Thread {
		
		@Override
		public void run() {
			while (!shutdownRequested) {
				try {
					Socket sock = replySocket.accept();
					InputStream in = sock.getInputStream();
					int len = Util.forceReadInt(in);
					byte[] message = Util.forceRead(in, len);
					replyCache.put(MixMessage.getInstanceReply(message));
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				}		
			}
		}
	}
	
	
	private Reply tryReadReply() {
		try {
			if (replyLength == Util.NOT_SET) {
				if (mixInputStream.available() > 4) {
					replyLength = Util.forceReadInt(mixInputStream);
					assert (replyLength + 4) < replyBufferSize;
				} else {
					return null;
				}
			}
			if (mixInputStream.available() >= replyLength) {
				byte[] message = Util.forceRead(mixInputStream, replyLength);
				//System.out.println("habe empfangen auf layer 0 (" +anonNode.toString() +"): " +Util.md5(message));
				replyLength = Util.NOT_SET;
				if (anonNode.DISPLAY_ROUTE_INFO)
					System.out.println("" +anonNode +" received reply (layer1)"); 
				return MixMessage.getInstanceReply(message);
			} else {
				return null;
			}	
		} catch (IOException e) {
			System.err.println("connection lost... try again"); 
			connect();
			return tryReadReply();
		}
	}
	
	
	private Reply forceReadReply() {
		try {
			if (replyLength == Util.NOT_SET) {
				replyLength = Util.forceReadInt(mixInputStream);
				assert (replyLength + 4) < replyBufferSize;
			}
			byte[] message = Util.forceRead(mixInputStream, replyLength);
			//System.out.println("habe empfangen auf layer 0 (" +anonNode.toString() +"): " +Util.md5(message));
			if (anonNode.DISPLAY_ROUTE_INFO)
				System.out.println("" +anonNode +" received reply (layer1)"); 
			replyLength = Util.NOT_SET;
			return MixMessage.getInstanceReply(message);
		} catch (IOException e) {
			System.err.println("connection lost... try again"); 
			connect();
			return receiveReply();
		}
	}
	
	
}
