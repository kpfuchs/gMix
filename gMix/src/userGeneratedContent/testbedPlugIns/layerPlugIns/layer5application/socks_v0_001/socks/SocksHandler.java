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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.socks_v0_001.socks;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import staticContent.framework.util.Util;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.socks_v0_001.Config;

/**
 * Handles SOCKS4 and SOCKS5 protocol.
 * 
 * @author Haseloff, Schmitz, Sprotte
 * 
 */
public class SocksHandler extends Thread {

	private InputStream fromClient;
	private OutputStream toClient;
	private Forwarder toUserForwarder;
	private Forwarder toWANforwarder;
	private DatagramSocket fromToWebserver;
	private int udpWebserverPort;
	// private ForwarderSocks5UDPtoWebserver udpForwarderToWebserver;
	// private ForwarderSocks5UDPtoClient udpForwarderToClient;

	Socket connectS = null;
	ServerSocket ss = null;
	Socket bindS = null;
	Socket udpS = null;
	Config config;

	/**
	 * SOCKS handler for a single socks connection (one handler per connection
	 * required).
	 * 
	 * @param fromClient
	 *            InputStream from client (Mix)
	 * @param toClient
	 *            OutputStream to client (Mix)
	 */
	public SocksHandler(InputStream fromClient, OutputStream toClient,
			Config config) {
		this.fromClient = fromClient;
		this.toClient = toClient;
		this.config = config;
		start();
	}

	/**
	 * Terminates the toUser- and toWANforwarder
	 */
	public void close() {
		if (toUserForwarder != null)
			toUserForwarder.close();
		else
			try {
				fromClient.close();
			} catch (IOException e) {
				if (config.DEBUG)
					e.printStackTrace();
			}

		if (toWANforwarder != null)
			toWANforwarder.close();
		else
			try {
				toClient.close();
			} catch (IOException e) {
				if (config.DEBUG)
					e.printStackTrace();
			}
	}

	/**
	 * Reads a couple of bytes depending on the address type and returns this
	 * address as an InetAddress-Object.
	 * 
	 * @param aTyp
	 *            valid values: 0x01, 0x03, 0x04
	 * @return InetAddress
	 */
	private InetAddress getInetAddress(byte aTyp) {
		byte[] byteAddress = null;
		int length;
		InetAddress address = null;
		try {
			switch (aTyp) {
			case 0x01: // IP v4
				length = 4;
				byteAddress = Util.forceRead(fromClient, length);
				address = InetAddress.getByAddress(byteAddress);
				break;
			case 0x03: // Domain, first byte defines the length of it
				length = Util.unsignedByteToShort((byte) fromClient.read());
				byteAddress = Util.forceRead(fromClient, length);
				String stringAddress = Util
						.getStringWithoutNewLines(byteAddress);
				address = InetAddress.getByName(stringAddress);
				break;
			case 0x04: // IP v6
				length = 16;
				byteAddress = Util.forceRead(fromClient, length);
				address = InetAddress.getByAddress(byteAddress);
				break;
			default:
				this.sendSocks5FailReply((byte) 0x05, (byte) 0x08, (byte) 0x00); // reply
																					// 0x08:
																					// Address
																					// type
																					// not
																					// supported
				System.out
						.println("SocksHandler: i do only support IP V4, Domains or IP V6 addresses");
				if (config.TALK_A_LOT == true) {
					System.out
							.println("SocksHandler: Sent CONNECT reply:	0x08");
				}
				close();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return address;
	}

	/**
	 * Sends a SOCKS5 Method Reply Message to user.
	 * 
	 * +----+--------+ |VER | METHOD | +----+--------+ | 1 | 1 | +----+--------+
	 * 
	 * @param toClient
	 *            OutputStream of Client
	 * @param method
	 *            Chosen authentifiaction method
	 */
	private void sendSocks5MethodReply(byte method) {
		try {
			toClient.write(0x05); // version
			toClient.write(method); // method
			toClient.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * Sends SOCKS5 reply for the connect (0x01) command. The message looks
	 * like:
	 * 
	 * +----+-----+-------+------+----------+----------+ |VER | REP | RSV | ATYP
	 * | BND.ADDR | BND.PORT | +----+-----+-------+------+----------+----------+
	 * | 1 | 1 | X'00' | 1 | Variable | 2 |
	 * +----+-----+-------+------+----------+----------+
	 * 
	 * @param version
	 *            SOCKS Version
	 * @param rep
	 *            SOCKS5 reply field
	 * @param atyp
	 *            valid values: 0x01, 0x03, 0x04
	 * @param socket
	 *            Socket which the message is send to
	 */
	private void sendSocks5ConnectReply(byte version, byte rep, byte atyp,
			Socket socket) { // with Socket
		try {
			toClient.write(version); // version
			toClient.write(rep); // rep (succeeded) TODO: send real result...
			toClient.write(0x00); // reserved
			toClient.write(atyp); // atyp
			toClient.write(socket.getInetAddress().getAddress()); // ip address
			toClient.write(Util.shortToByteArray(socket.getPort())); // port
			toClient.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * Sends SOCKS5 reply for 2nd connection for the BIND command (0x02).
	 * 
	 * @param version
	 *            SOCKS Version
	 * @param rep
	 *            SOCKS5 reply field
	 * @param atyp
	 *            valid values: 0x01, 0x03, 0x04
	 * @param serversocket
	 */
	private void sendSocks5BindReply(byte version, byte rep, byte atyp,
			ServerSocket serversocket) { // with ServerSocket (for bind failed)
		try {
			toClient.write(version); // version
			toClient.write(rep); // rep (succeeded) TODO: send real result...
			toClient.write(0x00); // reserved
			toClient.write(atyp); // atyp
			toClient.write(InetAddress.getLocalHost().getAddress()); // ip
																		// address
			toClient.write(Util.shortToByteArray(serversocket.getLocalPort())); // port
			toClient.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * Sends SOCKS5 reply for UDP Associate command (0x03)
	 * 
	 * @param version
	 *            SOCKS Version
	 * @param rep
	 *            SOCKS5 reply field
	 * @param atyp
	 *            valid values: 0x01, 0x03, 0x04
	 */
	private void sendSocks5UDPReply(byte version, byte rep, byte atyp,
			DatagramSocket fromToWebserver) {
		try {
			toClient.write(version); // version
			toClient.write(rep); // rep (succeeded) TODO: send real result...
			toClient.write(0x00); // reserved
			toClient.write(atyp); // atyp

			byte[] dstaddress = InetAddress.getLocalHost().getAddress();
			toClient.write(dstaddress); // ip address of ServerSockt local proxy

			byte[] dstport = Util.shortToByteArray(fromToWebserver
					.getLocalPort());
			toClient.write(dstport); // port of ServerSockt local proxy
			toClient.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * sendSocks5FailReply sends the same error message (0x04) for:
	 * 
	 * X'03' Network unreachable X'04' Host unreachable X'05' Connection refused
	 * X'06' TTL expired
	 * 
	 * and never sends the following messages: X'01' general SOCKS server
	 * failure X'02' connection not allowed by ruleset
	 * 
	 * @param version
	 *            SOCKS Version
	 * @param rep
	 *            SOCKS5 reply field
	 * @param atyp
	 *            valid values: 0x01, 0x03, 0x04
	 * 
	 * 
	 */
	private void sendSocks5FailReply(byte version, byte rep, byte atyp) { // with
																			// Socket
		try {
			toClient.write(version); // version
			toClient.write(rep); // rep
			toClient.write(0x00); // reserved
			toClient.write(atyp); // atyp

			InetAddress inetAddress = null;
			try {
				inetAddress = InetAddress.getByName("0.0.0.0");
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}

			byte[] dstaddress = inetAddress.getAddress();

			toClient.write(dstaddress); // ip address
			toClient.write(Util.shortToByteArray(00000)); // port
			toClient.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * Sends SOCKS4 reply for connect command (0x01)
	 * 
	 * +----+----+----+----+----+----+----+----+ | VN | CD | DSTPORT | DSTIP |
	 * +----+----+----+----+----+----+----+----+ # of bytes: 1 1 2 4
	 * 
	 * @param socket
	 *            Socket which is connected to webserver
	 * @param rep
	 *            suiting SOCKS4 reply
	 */
	private void sendSocks4ConnectReply(Socket socket, int rep) {
		try {
			byte[] address = socket.getInetAddress().getAddress();
			byte[] port = Util.shortToByteArray(socket.getPort());
			// System.out.println("Length of address:port byte[]:	"+address.length+":"+port.length);
			// System.out.println(Util.unsignedShortToInt(port));

			toClient.write((byte) 0x00);
			// System.out.println(Util.byteArrayToInt(rep));
			toClient.write((byte) rep); // reply code
			toClient.write(port);
			toClient.write(address);
			toClient.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends SOCKS4 reply for BIND command (0x02). Message has the same
	 * structure as the reply for CONNECT command.
	 * 
	 * @param socket
	 *            ServerSocket which the SocksHandler provides for connections
	 *            from webserver
	 * @param rep
	 *            suiting SOCKS4 reply
	 */
	private void sendSocks4BindReply(ServerSocket socket, int rep) {
		try {
			InetAddress address = InetAddress.getLocalHost();
			byte[] port = Util.shortToByteArray(socket.getLocalPort());
			// System.out.println("Length of address:port byte[]:	"+address.length+":"+port.length);
			// System.out.println(Util.unsignedShortToInt(port));

			toClient.write((byte) 0x00);
			toClient.write((byte) rep); // reply code
			toClient.write(port);
			toClient.write(address.getAddress());
			toClient.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates a toUser- and a toWANForwarder and just forwards data from Input-
	 * to Outputstream in two directions.
	 * 
	 * @param from
	 *            Client InputStream from which the data should be read
	 * @param to
	 *            Client OutputStream to which the data should be written
	 * @param socket
	 *            Socket conneceted to webserver
	 */
	private void forwardData(InputStream from, OutputStream to, Socket socket) {
		try {
			toUserForwarder = new Forwarder(from, socket.getOutputStream(),
					config); // from Client to Webserver
			toWANforwarder = new Forwarder(socket.getInputStream(), to, config); // from
																					// Webserver
																					// to
																					// Client
			toUserForwarder.start();
			toWANforwarder.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads SOCKS5 Request from user and interprets SOCKS5 command. Calls
	 * specific SOCKS5 command handle-Method.
	 * 
	 * @param version
	 *            SOCKS version
	 */
	private void handleSocks5(byte version) {
		try {
			if (config.SKIP_ROUNDTRIP == false) {
				int nMethods = Util.unsignedByteToShort((byte) fromClient
						.read());
				byte[] methods = Util.forceRead(fromClient, nMethods);
				if (config.TALK_A_LOT == true) {
					System.out.println("SocksHandler: " + nMethods
							+ " method(s) (in hex): " + Util.toHex(methods));
				}
				// TODO: parse methods (necessary for authentication only; not
				// yet required)

				/**
				 * Proxy -2-> Client +----+--------+ |VER | METHOD |
				 * +----+--------+ | 1 | 1 | +----+--------+
				 */
				// send "NO AUTHENTICATION REQUIRED" message
				byte method = 0x00;

				this.sendSocks5MethodReply((byte) method);
			}

			/**
			 * Client -4-> Proxy -5-> Server
			 * +----+-----+-------+------+----------+----------+ |VER | CMD |
			 * RSV | ATYP | DST.ADDR | DST.PORT |
			 * +----+-----+-------+------+----------+----------+ | 1 | 1 | X'00'
			 * | 1 | Variable | 2 |
			 * +----+-----+-------+------+----------+----------+
			 */
			// read socks request:
			if (config.SKIP_ROUNDTRIP == false) {
				fromClient.read(); // read version
			}
			byte command = (byte) fromClient.read(); // read command
			if (config.TALK_A_LOT == true) {
				System.out.println("SocksHandler: received command:   0x0"+ command);
			}
			/* int reserved = */fromClient.read(); // skip reserved
			byte aTyp = (byte) fromClient.read(); // read aTyp

			// define address typ
			InetAddress address = null;
			address = getInetAddress(aTyp);

			byte[] bPort = Util.forceRead(fromClient, 2); // read DST.PORT
			int port = Util.unsignedShortToInt(bPort);

			switch (command) {
			case 0x01: // CONNECT
				if (config.TALK_A_LOT == true) 
					System.out.println("SocksHandler: Got CONNECT Request from Client.");
				if (config.TALK_A_LOT == true) 
					System.out.println("SocksHandler: destination Webserver: " + address + ":" + port);
				this.handleSocks5Connect(address, port);
				break;
			case 0x02: // BIND
				if (config.TALK_A_LOT == true) 
					System.out.println("SocksHandler: Got BIND Request from Client.");
				if (config.TALK_A_LOT == true) 
					System.out.println("SocksHandler: destination Webserver: " + address + ":" + port);
				this.handleSocks5Bind();
				break;
			case 0x03: // UDP Associate
				if (config.TALK_A_LOT == true) 
					System.out.println("SocksHandler: Got UDP Associate Request from Client.");
				if (config.TALK_A_LOT == true) 
					System.out.println("SocksHandler: source client: " + address + ":" + port);
				this.handleSocks5UDP(address, port);
				break;
			default: // not a valid Socks5 command
				sendSocks5FailReply((byte) 0x05, (byte) 0x07, (byte) 0x01); // reply
																			// 0x07:
																			// Command
																			// not
																			// supported
				System.out.println("SocksHandler: i do only support CONNECT, BIND or UDP command");
				if (config.TALK_A_LOT == true) {
					System.out.println("SocksHandler: Sent CONNECT reply:	0x07");
				}
				close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Handles SOCKS5 connect request from user. Connects to webserver, reply to
	 * user and forwards the data.
	 * 
	 * @param address
	 *            address of webserver to which a connection should be
	 *            established
	 * @param port
	 *            port of webserver to which a connection should be esablished
	 */
	private void handleSocks5Connect(InetAddress address, int port) {
		// try to connect Proxy --> Server
		try {
			connectS = new Socket(address, port);
			/**
			 * send reply (RFC 1928, p. 5) Proxy --> Client, after connection
			 * Proxy <--> Server established
			 * +----+-----+-------+------+----------+----------+ |VER | REP |
			 * RSV | ATYP | BND.ADDR | BND.PORT |
			 * +----+-----+-------+------+----------+----------+ | 1 | 1 | X'00'
			 * | 1 | Variable | 2 |
			 * +----+-----+-------+------+----------+----------+
			 */

			this.sendSocks5ConnectReply((byte) 0x05, (byte) 0x00, (byte) 0x01,
					connectS);
			if (config.TALK_A_LOT == true) {
				System.out.println("SocksHandler: Sent CONNECT reply:	0x01");
			}

			// from now on we will just forward data:
			this.forwardData(fromClient, toClient, connectS);
		} catch (IOException e) {
			if (e instanceof ConnectException) {
				sendSocks5FailReply((byte) 0x05, (byte) 0x04, (byte) 0x01); // reply
																			// 0x04:
																			// Host
																			// unreachable
				System.out.println("SocksHandler: Host unreachable");
			}
			if (config.DEBUG == true) {
				e.printStackTrace();
			}
			close();
		}
	}

	/**
	 * Handles SOCKS5 BIND request from user. Binds ServerSocket to "random"
	 * port and sends first reply to user (includes the port on which the socket
	 * is bound). Waiting for connection form webserver; if established sends
	 * second reply to user.
	 */
	private void handleSocks5Bind() {
		int addRandomPort = Util.getRandomInt(10, 10000);
		int ssPort = config.MIX_BIND_PORT + addRandomPort; // flexible for more
															// binds
		if (config.TALK_A_LOT == true) {
			System.out.println("SocksHandler: Starting new SOCKS 5 BIND ServerSocket");
		}

		try {
			try {
				ss = new ServerSocket(ssPort);
			} catch (IOException e) {
				if (config.TALK_A_LOT == true) {
					System.out.println("SocksHandler: can't bind ServerSocket because port is already in use... I'm trying another one");
				}

				this.handleSocks5Bind();
			}

			System.out.println("SocksHandler: ServerSocket is bound to: "
					+ InetAddress.getLocalHost() + ":" + ssPort);

			// send first reply after ServerSocket is bound
			this.sendSocks5BindReply((byte) 0x05, (byte) 0x00, (byte) 0x01, ss);
			if (config.TALK_A_LOT == true) {
				System.out.println("SocksHandler: Sent ServerSocket is bound reply");
			}

			bindS = ss.accept(); // waits for connection from Webserver

			// send second reply
			if (bindS == null) // Connection between Sever and Proxy failed //
								// TODO: real reply
			{
				System.out.println("SocksHandler: Connection from Webserver to Proxy failed");
				this.sendSocks5BindReply((byte) 0x05, (byte) 0x05, (byte) 0x01,
						ss); // rep: connection refused
				close();
				return;
			} else
			// Connection between Sever and Proxy established
			{
				System.out.println("SocksHandler: Connection from Webserver to Proxy established");
				this.sendSocks5ConnectReply((byte) 0x05, (byte) 0x00,
						(byte) 0x01, bindS);

				this.forwardData(fromClient, toClient, bindS); // secondary
																// connection
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Handles SOCKS5 UDP request from user. Binds DatagramSocket to "random"
	 * port and sends reply to user (includes the port to which the
	 * DatagramSocket is bound). Initializes an UDP forwarder to client and an
	 * UDP forwarder to webserver.
	 * 
	 * @param address
	 *            address of client
	 * @param port
	 *            port of client
	 */
	private void handleSocks5UDP(InetAddress address, int port) {
		// "buf" for client ip and port
		InetAddress clientAddress = address;
		int clientPort = port;

		int addRandomPort = Util.getRandomInt(10, 10000);
		udpWebserverPort = config.MIX_DATAGRAM_PORT + addRandomPort; // flexible
																		// for
																		// more
																		// DatagramSockets

		try {
			fromToWebserver = new DatagramSocket(udpWebserverPort);
		} catch (IOException e) {
			if (config.TALK_A_LOT == true) {
				System.out.println("SocksHandler: can't bind DatagramSocket because port is already in use... I'm trying another one");
			}
			this.handleSocks5UDP(address, port);
		}

		System.out.println("SocksHandler: Bound DatagramSocket to Webserver to " + udpWebserverPort);

		this.sendSocks5UDPReply((byte) 0x05, (byte) 0x00, (byte) 0x01,
				fromToWebserver); // *** Step 6 ***
		if (config.TALK_A_LOT == true) {
			System.out.println("SocksHandler: Sent Socks5UDPReply to Client");
		}

		/* udpForwarderToWebserver = */
		new ForwarderSocks5UDPtoWebserver(fromClient, fromToWebserver, config);

		/* udpForwarderToClient = */
		new ForwarderSocks5UDPtoClient(toClient, clientAddress, clientPort,
				fromToWebserver, config);
	}

	/**
	 * Reads SOCKS4 Request from user and interprets SOCKS5 command. Calls
	 * specific SOCKS4 command handle-Method.
	 * 
	 * @param version
	 *            SOCKS version
	 */
	private void handleSocks4(byte version) {
		/**
		 * Client --> Proxy 1) CONNECT
		 * 
		 * +----+----+----+----+----+----+----+----+----+----+....+----+ | VN |
		 * CD | DSTPORT | DSTIP | USERID |NULL|
		 * +----+----+----+----+----+----+----+----+----+----+....+----+ # of
		 * bytes: 1 1 2 4 variable 1
		 */

		try {
			byte command = (byte) fromClient.read();
			byte[] bPort = Util.forceRead(fromClient, 2); // read DST.PORT
			int port = Util.unsignedShortToInt(bPort);
			InetAddress address = null;
			address = getInetAddress((byte) 0x01); // read IPv4 Address
			// we skip the request check, so we don't need USERID and NULL
			// TODO: request check (SRCIP, DSTIP, DSTPORT, USERID)
			/* int userid = */fromClient.read();

			if (config.TALK_A_LOT == true) 
				System.out.println("SocksHandler: destination Webserver:	" + address + ":" + port);

			if (command == 0x01) // CONNECT command
			{
				if (config.TALK_A_LOT == true) 
					System.out.println("SocksHandler: Got CONNECT command from Client");
				this.handleSocks4Connect(address, port);
			} else if (command == 0x02) // BIND command
			{
				if (config.TALK_A_LOT == true) 
					System.out.println("SocksHandler: Got BIND command from Client");
				this.handleSocks4Bind();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Handles SOCKS4 connect request from user. Connects to webserver, reply to
	 * user and forwards the data.
	 * 
	 * @param address
	 *            address of webserver to which a connection should be
	 *            established
	 * @param port
	 *            port of webserver to which a connection should be esablished
	 */
	private void handleSocks4Connect(InetAddress address, int port) {
		Socket connectS4 = null; // Socket for connection between Proxy and
									// Webserver
		try {
			connectS4 = new Socket(address, port);
			// System.out.println("SocksHandler: connectS4 Status:	"+
			// connectS.isConnected());
			System.out.println("SocksHandler: connection between Proxy and Webserver established");

			this.sendSocks4ConnectReply(connectS4, 90); // 90: request granted
			if (config.TALK_A_LOT == true) {
				System.out.println("SocksHandler: sent CONNECT reply to Client");
			}

			Thread.sleep(1000);

			this.forwardData(fromClient, toClient, connectS4);
			System.out.println("SocksHandler: data forwarding started");
		} catch (IOException e) {
			this.sendSocks4ConnectReply(connectS4, 91); // 91: request rejected
														// or failed
			e.printStackTrace();
			close(); // closing Client connection
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Handles SOCKS4 BIND request from user. Binds ServerSocket to "random"
	 * port and sends first reply to user (includes the port on which the socket
	 * is bound). Waiting for connection form webserver; if established sends
	 * second reply to user.
	 */
	private void handleSocks4Bind() {
		int addRandomPort = Util.getRandomInt(10, 10000);
		int ssPort = config.MIX_BIND_PORT + addRandomPort; // flexible for more
															// binds

		if (config.TALK_A_LOT == true) {
			System.out.println("SocksHandler: Starting new SOCKS 4 BIND ServerSocket");
		}
		try {
			try {
				ss = new ServerSocket(ssPort);
			} catch (IOException e) {
				if (config.TALK_A_LOT == true) {
					System.out.println("SocksHandler: can't bind ServerSocket because port is already in use... I'm trying another one");
				}
				this.handleSocks4Bind();
			}

			System.out.println("SocksHandler: ServerSocket is bound to: " + InetAddress.getLocalHost() + ":" + ssPort);
			this.sendSocks4BindReply(ss, 90);

			bindS = ss.accept(); // waiting for connection from Webserver

			// TODO: CHECK originating host
			if (bindS == null) // Connection between Sever and Proxy failed
			{
				System.out.println("SocksHandler: Connection from Sever to Proxy failed");

				this.sendSocks4BindReply(ss, 91); // rep: connection refused
				close();
				return;
			} else
			// Connection between Sever and Proxy established
			{
				System.out.println("SocksHandler: Connection from Sever to Proxy established");
				this.sendSocks4BindReply(ss, 90);

				this.forwardData(fromClient, toClient, bindS); // secondary
																// connection
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads the first byte of a message from user, to decide which SOCKS
	 * version should be use. Calls the specific handle-Method.
	 */
	@Override
	public void run() {
		// read SOCKS "version identifier/method selection message" (RFC 1928,
		// p. 3)

		/**
		 * Client -1-> Proxy +----+----------+----------+ |VER | NMETHODS |
		 * METHODS | +----+----------+----------+ | 1 | 1 | 1 to 255 |
		 * +----+----------+----------+
		 */
		byte version = 0x00;
		try {
			version = (byte) fromClient.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (config.TALK_A_LOT == true) {
			System.out.println("SocksHandler: reading Identifier Message from Client.");
		}
		if (version == 0x05) {
			if (config.TALK_A_LOT == true) 
				System.out.println("SocksHandler: its a socks 5 connection");
			this.handleSocks5(version);
		} else if (version == 0x04) {
			if (config.TALK_A_LOT == true) 
				System.out.println("SocksHandler: its a socks 4 connection");
			this.handleSocks4(version);
		} else {
			if (config.TALK_A_LOT == true) 
				System.out.println("SocksHandler: connections does not seem to be socks");
			close();
			return;
		}
	}
}
