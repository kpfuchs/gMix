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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import framework.core.controller.Layer2RecodingSchemeMixController;
import framework.core.controller.SubImplementation;
import framework.core.message.MixMessage;
import framework.core.message.Request;
import framework.core.userDatabase.User;
import framework.core.util.Util;


public class ClientHandler_TCP_FCFS_sync_multithread_1 extends SubImplementation {
	//TODO: add timeout for inactive users
	
	private int port; 
	private InetAddress bindAddress;
	private int backlog;
	private ServerSocket serverSocket;
	//private int maxRequestLength;
	//private boolean DUPLEX_ON;
	//private int queueBlockSize;
	
	
	@Override
	public void constructor() {
		if (anonNode.IS_DUPLEX)
			throw new RuntimeException("not supported"); 
		this.bindAddress = settings.getPropertyAsInetAddress("GLOBAL_MIX_BIND_ADDRESS");
		this.port = settings.getPropertyAsInt("GLOBAL_MIX_BIND_PORT");
		this.backlog = settings.getPropertyAsInt("BACKLOG");
		//this.maxRequestLength = anonNode.getRecodingLayerControllerMix().getMaxSizeOfNextRequest(); // settings.getPropertyAsInt("MAX_REQUEST_LENGTH");
		//this.queueBlockSize = settings.getPropertyAsInt("QUEUE_BLOCK_SIZE");
		
	}

	
	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}

	
	@Override
	public void begin() {
		try {
			this.serverSocket = new ServerSocket(port, backlog, bindAddress);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not open ServerSocket"); 
		}
		System.out.println(anonNode +" listening on " +bindAddress +":" +port);
		new AcceptorThread().start();
	}

	
	private class AcceptorThread extends Thread {
		
		@Override
		public void run() {
			new Thread(// TODO: remove
					new Runnable() {
						public void run() {
							while (true) {
								System.out.println(anonNode.getRequestInputQueue().size()); 
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					}
				).start(); 
			int counter = 0;
			while (true) {
				try {
					Socket client = serverSocket.accept();
					if (++counter%100 == 0)
						System.out.println(counter +" connections"); 
					User user = userDatabase.generateUser();
					userDatabase.addUser(user);
					new RequestThread(user, client.getInputStream()).start();
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
			}
		}
		
	}
	
	
	private class RequestThread extends Thread {
		
		private User user;
		private InputStream inputStream;
		private Layer2RecodingSchemeMixController recodingScheme;
		
		public RequestThread(User user, InputStream inputStream) {
			this.user = user;
			this.inputStream = inputStream;
			this.recodingScheme = anonNode.getRecodingLayerControllerMix();
		}
		
		@Override
		public void run() {
			try {
				while (true) {
					int messageLength = Util.forceReadInt(inputStream);
					if (messageLength > recodingScheme.getMaxSizeOfNextRequest())
						throw new IOException("warning: user " +user +" sent a too large message");
					Request request = MixMessage.getInstanceRequest(Util.forceRead(inputStream, messageLength), user);
					anonNode.putInRequestInputQueue(request); // might block
				}
			} catch (IOException e) {
				System.err.println("warning: connection to " +user +" lost");
				e.printStackTrace();
			}
		}
	}

}
