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
package plugIns.layer1network.cascade_TCP_v0_001;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Vector;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer1NetworkClient;
import framework.core.interfaces.Layer2RecodingSchemeClient;
import framework.core.interfaces.Layer3OutputStrategyClient;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.routing.MixList;
import framework.core.routing.RoutingMode;
import framework.core.util.Util;


public class ClientPlugIn extends Implementation implements Layer1NetworkClient {
	
	private int timeout;
	private BufferedOutputStream mixOutputStream;
	private BufferedInputStream mixInputStream;
	private Socket mix;
	private int replyBufferSize;
	private int requestBufferSize;
	private int replyLength = Util.NOT_SET;
	private Vector<Reply> replyCache;
	
	@Override
	public void constructor() {
		this.requestBufferSize = settings.getPropertyAsInt("CLIENT_REQUEST_BUFFER_SIZE");
		this.replyBufferSize = settings.getPropertyAsInt("CLIENT_REPLY_BUFFER_SIZE");
		this.timeout = settings.getPropertyAsInt("CLIENT_CONNECTION_TIMEOUT");
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
		mix = new Socket();
		SocketAddress socketAddress = new InetSocketAddress(mixList.addresses[0], mixList.ports[0]);
		try {
			mix.connect(socketAddress, timeout);
			mixOutputStream = new BufferedOutputStream(mix.getOutputStream(), requestBufferSize);
			if (anonNode.IS_DUPLEX) {
				mixInputStream = new BufferedInputStream(mix.getInputStream(), replyBufferSize);
				replyCache = new Vector<Reply>();
			}
		} catch (IOException e) {
			System.err.println("could not connect to mix... try again");
			try {Thread.sleep(5000);} catch (InterruptedException e1) {e1.printStackTrace();}
			connect();
		}
		
	}
	
	
	@Override
	public void connect() {
		if (anonNode.ROUTING_MODE != RoutingMode.CASCADE)
			throw new RuntimeException("for free route sockets, call connect(MixList mixList)"); 
		connect(anonNode.mixList);
	}
	
	
	@Override
	public void sendMessage(Request request) {
		//System.out.println(client +" sending (ciphertext): " +Util.md5(request.getByteMessage()));
		//System.out.println("msgsize: " +request.getByteMessage().length); 
		try {
			mixOutputStream.write(Util.intToByteArray(request.getByteMessage().length));
			mixOutputStream.write(request.getByteMessage());
			mixOutputStream.flush(); 
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("connection lost... try again"); 
			connect();
		}
	}

	
	@Override
	public Reply receiveReply() {
		if (replyCache.size() > 0)
			return replyCache.remove(0);
		else
			return forceReadReply();
	}
	

	@Override
	public void disconnect(){
		try {
			mix.close();
		} catch (IOException e) {
			e.printStackTrace();
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
			replyLength = Util.NOT_SET; 
			return MixMessage.getInstanceReply(message);
		} catch (IOException e) {
			System.err.println("connection lost... try again"); 
			connect();
			return receiveReply();
		}
	}
	
	
	@Override
	public int availableReplies() {
		while (true) {
			Reply reply = tryReadReply();
			if (reply == null)
				break;
			else
				replyCache.add(reply);
		}
		return replyCache.size();
	}

}
