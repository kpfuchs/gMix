
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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.socks_v0_001;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer5ApplicationClient;
import staticContent.framework.routing.RoutingMode;
import staticContent.framework.socket.socketInterfaces.StreamAnonSocket;
import staticContent.framework.socket.socketInterfaces.AnonSocketOptions.CommunicationDirection;
import staticContent.framework.util.Util;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.socks_v0_001.Config;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.socks_v0_001.multiplexer.Multiplexer;


// TODO: make sure DNS-requests are sent through the anonymous tunnel as well
public class ClientPlugIn extends Implementation implements
		Layer5ApplicationClient {

	/**
	 * this socket will accept SOCKS connections from user applications (e.g.
	 * web browsers)
	 */
	private ServerSocket userServerSocket; //

	/**
	 * the anonymous tunnel the socks connections will be tunneled through (all
	 * connections open via userServerSocket will be multiplexed through this
	 * tunnel)
	 */
	private Multiplexer multiplexedAnonTunnel;

	private Config config;

	
	@Override
	public void constructor() {
		if (!super.anonNode.IS_DUPLEX)
			throw new RuntimeException("Socks requires a DUPLEX anonymous channel"); 
		if (!super.anonNode.IS_CONNECTION_BASED)
			throw new RuntimeException("Socks requires a CONNECTION_BASED anonymous channel"); 
		if (!super.anonNode.IS_ORDER_PRESERVING)
			throw new RuntimeException("Socks requires an ORDER_PRESERVING anonymous channel"); 
		if (!super.anonNode.IS_RELIABLE)
			throw new RuntimeException("Socks requires a RELIABLE anonymous channel"); 
		this.config = new Config(settings);
	}

	
	@Override
	public void initialize() {

	}
	

	@Override
	public void begin() {
		StreamAnonSocket anonSocket = null;
		try {
			anonSocket = super.anonNode.createStreamSocket(
					CommunicationDirection.DUPLEX,
					super.anonNode.ROUTING_MODE != RoutingMode.GLOBAL_ROUTING);
			anonSocket.connect(1080);
		} catch (IOException e) {
			System.err.println("Client SOCKS-Proxy: could not connect to mixes");
			e.printStackTrace();
			return;
		}
		this.multiplexedAnonTunnel = new Multiplexer(anonSocket, config);
		openSocksServerSocket();
		System.err.println("Warning: This is a test plug-in - do NOT send any sensitive data via this plug-in!");
		new AcceptorThread().start();
	}

	
	public void openSocksServerSocket() {
		synchronized (multiplexedAnonTunnel) {
			if (userServerSocket != null && userServerSocket.isBound()) // already
																		// open
				return;
			InetAddress address = null;
			try {
				address = InetAddress.getByName(config.CLIENT_SOCKS_IP_ADDRESS);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			try {
				userServerSocket = new ServerSocket(config.CLIENT_SOCKS_PORT,
						1000, address);
				System.out.println("\n\nWAITING FOR SOCKS CONNECTIONS ON " + address +":" + config.CLIENT_SOCKS_PORT +"\n");
			} catch (IOException e) {
				if (config.DEBUG)
					e.printStackTrace();
				throw new RuntimeException("client: couldn't bind client socks socket " + address +":" + config.CLIENT_SOCKS_PORT);
			}
		}
	}

	
	private class AcceptorThread extends Thread {

		/**
		 * Accepts new socks connections (from user applications) and hands them
		 * over to the multiplexer. If SKIP_ROUNDTRIP is set, answers the
		 * Version Identifier/Method Selection Method from user and don't hands
		 * this message over to the multiplexer.
		 */
		@Override
		public void run() {
			Socket clientSocket = null;
			while (true) {
				if (config.TALK_A_LOT == true) 
					System.out.println("client: waiting for connection from user application (e.g. web browser)");
				try {
					clientSocket = userServerSocket.accept();
					if (config.TALK_A_LOT == true) 
						System.out.println("client: received a connection from a user application "
									+ clientSocket.getInetAddress()
									+ ":"
									+ clientSocket.getPort());

					if (config.SKIP_ROUNDTRIP == true) {
						InputStream fromClient = clientSocket.getInputStream();
						OutputStream toClient = clientSocket.getOutputStream();

						// read SOCKS
						// "version identifier/method selection message" (RFC
						// 1928, p. 3)

						/**
						 * Client -1-> Proxy +----+----------+----------+ |VER |
						 * NMETHODS | METHODS | +----+----------+----------+ | 1
						 * | 1 | 1 to 255 | +----+----------+----------+
						 */
						byte version = 0x00;
						try {
							version = (byte) fromClient.read();
						} catch (IOException e) {
							e.printStackTrace();
						}

						if (config.TALK_A_LOT == true) {
							System.out.println("Client: reading Identifier Message from Client.");
						}

						if (version == 0x05) {
							try {
								int nMethods = Util
										.unsignedByteToShort((byte) fromClient
												.read());
								byte[] methods = Util.forceRead(fromClient,
										nMethods);

								if (config.TALK_A_LOT == true) {
									System.out.println("Client: " + nMethods
											+ " method(s) (in hex): "
											+ Util.toHex(methods));
								}
								// TODO: parse methods (necessary for
								// authentication only)
								// send "NO AUTHENTICATION REQUIRED" message
								byte method = 0x00;
								this.sendSocks5MethodReply(toClient,
										(byte) method);
							} catch (IOException e) {
								e.printStackTrace();
							}
						} else if (version == 0x04) {

						} else {
							System.out.println("Client: connection does not seem to be socks");
							return;
						}
					}
					multiplexedAnonTunnel.addConnection(clientSocket);
				} catch (IOException e) {
					System.err.println("client: connection attempt from user application failed");
					if (config.DEBUG)
						e.printStackTrace();
					openSocksServerSocket();
					continue;
				}
			}
		}
		

		/**
		 * Sends a SOCKS5 Method Reply Message to user.
		 * 
		 * +----+--------+ |VER | METHOD | +----+--------+ | 1 | 1 |
		 * +----+--------+
		 * 
		 * @param toClient
		 *            OutputStream of Client
		 * @param method
		 *            Chosen authentifiaction method
		 */
		private void sendSocks5MethodReply(OutputStream toClient, byte method) {
			try {
				toClient.write(0x05); // version
				toClient.write(method); // method
				toClient.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}

