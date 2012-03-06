/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2011  Karl-Peter Fuchs
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

package client.mixCommunicationHandler;

import infoService.MixList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Vector;

import framework.Util;
import message.MixMessage;
import message.Reply;
import message.Request;


public class Basic_Tcp_v1 extends MixCommunicationHandler {

	private MixList mixList;
	private int timeout;
	private OutputStream mixOutputStream;
	private InputStream mixInputStream;
	private Socket mix;
	private ReplyThread replyThread;
	private Vector<MixEventListener> eventListeners;
	
	
	@Override
	public void constructor() {
		this.eventListeners = new Vector<MixEventListener>();
		this.mixList = client.getInfoServiceClient().getMixList();
	}
	

	@Override
	public void initialize() {
		
		this.timeout = settings.getPropertyAsInt("CLIENT_CONNECTION_TIMEOUT");
	}

	
	@Override
	public void begin() {
		
	}

	
	public void registerEventListener(MixEventListener mel) {
		synchronized (eventListeners) {
			eventListeners.add(mel);
		}
	}
	
	
	public boolean unregisterEventListener(MixEventListener mel) {
		synchronized (eventListeners) {
			return eventListeners.remove(mel);
		}
	}
	
	
	@Override
	public void connect() throws IOException {
		mix = new Socket();
		SocketAddress socketAddress = new InetSocketAddress(mixList.addresses[0], mixList.ports[0]);
		mix.connect(socketAddress, timeout);
		mixOutputStream = mix.getOutputStream();
		mixInputStream = mix.getInputStream();	
		if (client.isDuplex()) {
			this.replyThread = new ReplyThread();
			replyThread.start();
		}	
	}
	
	
	@Override
	public void sendMessage(Request request) throws IOException {
		//System.out.println(client +" sending (ciphertext): " +Util.md5(request.getByteMessage()));
		mixOutputStream.write(Util.intToByteArray(request.getByteMessage().length));
		mixOutputStream.write(request.getByteMessage());
		mixOutputStream.flush();
	}

	
	private class ReplyThread extends Thread {
		
		private volatile boolean die = false;
		private volatile boolean died = false;
		
		
		@Override
		public void run() {
			try {
				while (true) {
					synchronized (replyThread) {
						if (die) {
							died = true;
							replyThread.notify();
							break;
						}	
					}
					int lengthHeader = Util.forceReadInt(mixInputStream);
					byte[] message = Util.forceRead(mixInputStream, lengthHeader);
					Reply reply = MixMessage.getInstanceReply(message, settings);
					synchronized (eventListeners) {
						for (MixEventListener listener:eventListeners)
							listener.replyReceived(reply);
					}
					communicationBehaviour.incomingReply(reply);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
		public void die() {
			synchronized (replyThread) {
				die = true;
				while (!died) {
					try {
						replyThread.wait();
					} catch (InterruptedException e) {
						continue;
					}	
				}
			}
		}
		
	} // end of "ReplyThread"


	@Override
	public void disconnect() throws IOException {
		if (client.isDuplex())
			replyThread.die();
		mix.close();
	}

}
