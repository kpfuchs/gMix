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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer5ApplicationClient;
import staticContent.framework.routing.RoutingMode;
import staticContent.framework.socket.socketInterfaces.StreamAnonSocket;
import staticContent.framework.socket.socketInterfaces.AnonSocketOptions.CommunicationDirection;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.Connection;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.entryClient.ApplicationConnectionPool;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.entryClient.EntryDataFromMix;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.entryClient.EntryDataToMix;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.entryClient.MixConnection;


/**
 * @author bash
 * 
 *         This class provides the client on entry side for network improvement
 *         measures. It contains all needed subclasses.
 *         
 *         It handles the complete communication between the webbrowser and the mix. 
 *         The communication with the webbrowser is managed by the ApplicationConnectionPool (see ApplicationConnectionPool).
 *         The communication with the mix is handled by the MixEntryConnection (see MixEntryConnection).
 *         It also contains the improvementthreads.
 *         
 *         There is a hashtable to identify a connection by the connectionId. (Key: ConnectionId; Value: Connection)
 *         
 *         
 *         For automatic test purpose there is a control channel.
 *         It allows to control the client by remote. So it is possible to start and stop the client via script.
 *         The control server is able to send a stop or restart signal. Therefore it has to connect via tcp on port 4060
 *         If the control server send a 1 the client resets the client. If the server send a 2 the client stops.
 */// TODO: make sure DNS-requests are sent through the anonymous tunnel as well
public class ClientPlugIn extends Implementation implements Layer5ApplicationClient {

	private MixConnection mixConnection;
	
    private ConcurrentHashMap<Integer, Connection> connectionMap;
    private int improvmentThreadCounter;
    
	/**
	 * this socket will accept SOCKS connections from user applications (e.g.
	 * web browsers)
	 */
	//private ServerSocket userServerSocket; //

	/**
	 * the anonymous tunnel the socks connections will be tunneled through (all
	 * connections open via userServerSocket will be multiplexed through this
	 * tunnel)
	 */
	//private Multiplexer multiplexedAnonTunnel;

	/**
     * The ApplicationConnectionPool manage all connection to a browser
     */
    private ApplicationConnectionPool connectionPool;

    /**
     * This queue contains the connection which contains data from the outside
     */
    public LinkedBlockingQueue<Connection> readableConnections;

    /**
     * This queue contains the connection which contains data from the mix
     */
    public LinkedBlockingQueue<Connection> writeableConnections;

    private EntryDataToMix[] improvementThreads;
    private EntryDataFromMix[] unImprovementThreads;
    //private boolean DEBUG;
    //private boolean TALK_A_LOT;
    //private boolean SKIP_ROUNDTRIP;
    
	
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
		//this.config = new Config(settings);
		this.readableConnections = new LinkedBlockingQueue<Connection>();
        this.writeableConnections = new LinkedBlockingQueue<Connection>();
        this.connectionMap = new ConcurrentHashMap<Integer, Connection>();
        improvmentThreadCounter = settings.getPropertyAsInt("HP_THREAD_COUNTER");
        //this.DEBUG = settings.getPropertyAsBoolean("HP_DEBUG");
        //this.TALK_A_LOT = settings.getPropertyAsBoolean("HP_TALK_A_LOT");
        //this.SKIP_ROUNDTRIP = settings.getPropertyAsBoolean("HP_SKIP_ROUNDTRIP");
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
		
		//this.multiplexedAnonTunnel = new Multiplexer(anonSocket, settings);
		//openSocksServerSocket();
		System.err.println("Warning: This is a test plug-in - do NOT send any sensitive data via this plug-in!");
		mixConnection = new MixConnection(anonSocket, connectionMap, writeableConnections);
        mixConnection.start();
        startConnectionPool();
        startImprovement();
        
        
		//new AcceptorThread().start();
	}

	
	/*public void openSocksServerSocket() {
		//synchronized (multiplexedAnonTunnel) {
			if (userServerSocket != null && userServerSocket.isBound()) // already
																		// open
				return;
			InetAddress address = null;
			try {
				address = InetAddress.getByName(settings.getProperty("HP_LISTENING_IP_ADDRESS"));
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			try {
				userServerSocket = new ServerSocket(settings.getPropertyAsInt("HP_LISTENING_PORT"), 1000, address);
				System.out.println("client: waiting for socks connections on " + address +":" + settings.getPropertyAsInt("HP_LISTENING_PORT"));
			} catch (IOException e) {
				if (DEBUG)
					e.printStackTrace();
				throw new RuntimeException("client: couldn't bind client socks socket " + address +":" + settings.getPropertyAsInt("HP_LISTENING_PORT"));
			}
		//}
	}*/

	
	/*private class AcceptorThread extends Thread {

		/**
		 * Accepts new socks connections (from user applications) and hands them
		 * over to the multiplexer. If SKIP_ROUNDTRIP is set, answers the
		 * Version Identifier/Method Selection Method from user and don't hands
		 * this message over to the multiplexer.
		 *//*
		@Override
		public void run() {
			Socket clientSocket = null;
			while (true) {
				if (TALK_A_LOT == true) 
					System.out.println("client: waiting for connection from user application (e.g. web browser)");
				try {
					clientSocket = userServerSocket.accept();
					if (TALK_A_LOT == true) 
						System.out.println("client: received a connection from a user application "
									+ clientSocket.getInetAddress()
									+ ":"
									+ clientSocket.getPort());

					if (SKIP_ROUNDTRIP == true) {
						InputStream fromClient = clientSocket.getInputStream();
						OutputStream toClient = clientSocket.getOutputStream();

						// read SOCKS
						// "version identifier/method selection message" (RFC
						// 1928, p. 3)

						/**
						 * Client -1-> Proxy +----+----------+----------+ |VER |
						 * NMETHODS | METHODS | +----+----------+----------+ | 1
						 * | 1 | 1 to 255 | +----+----------+----------+
						 *//*
						byte version = 0x00;
						try {
							version = (byte) fromClient.read();
						} catch (IOException e) {
							e.printStackTrace();
						}

						if (TALK_A_LOT == true) {
							System.out.println("Client: reading Identifier Message from Client.");
						}

						if (version == 0x05) {
							try {
								int nMethods = Util
										.unsignedByteToShort((byte) fromClient
												.read());
								byte[] methods = Util.forceRead(fromClient,
										nMethods);

								if (TALK_A_LOT == true) {
									System.out.println("Client: " + nMethods
											+ " method(s) (in hex): "
											+ Util.toHex(methods));
								}
								// TODO: parse methods (necessary for
								// authentication only)
								// send "NO AUTHENTICATION REQUIRED" message
								byte method = 0x00;
								SocksHandler.sendSocks5MethodReply(toClient, (byte) method);
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
					if (DEBUG)
						e.printStackTrace();
					openSocksServerSocket();
					continue;
				}
			}
		}

	}*/

	
    /**
     * Method to start the thread to improve the messages
     */
    public void startImprovement() {
        improvementThreads = new EntryDataToMix[improvmentThreadCounter];
        unImprovementThreads = new EntryDataFromMix[improvmentThreadCounter];
        for (int i = 0; i < improvmentThreadCounter; i++) {
            // System.out.println("TEST");
            try {
                improvementThreads[i] = new EntryDataToMix(readableConnections, mixConnection, settings);
                improvementThreads[i].start();
                unImprovementThreads[i] = new EntryDataFromMix(writeableConnections, settings);
                unImprovementThreads[i].start();
            } catch (ClassNotFoundException e) {
                System.err.println("Plugin not found, please check path!");
                e.printStackTrace();
            } catch (InstantiationException e) {
                System.err.println("Could not instatiate the plugin!");
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                System.err.println("Plugin not accessable, please check rights!");
                e.printStackTrace();
            }

        }

    }

    /**
     * Starts the entry connection pool
     */
    public void startConnectionPool() {
        connectionPool = new ApplicationConnectionPool(settings.getPropertyAsInt("HP_LISTENING_PORT"), settings.getPropertyAsInt("HP_BUFFER_SIZE"), readableConnections, connectionMap, settings);
        connectionPool.start();
    }

    /**
     * Method to reset the client
     * It clears the connection table and restarts the improvementthreads
     */
    public void resetConnectionPool() {

        ConcurrentHashMap<Integer, Connection> pool = connectionPool.getConnectionMap();
        
        for(Thread t: improvementThreads) {
            t.interrupt();
        
        }
        
        for(Thread t: unImprovementThreads) {
            t.interrupt();
            
        }
        startImprovement();
        for (Connection con : pool.values()) {
            try {
                con.getServerSocket().close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        connectionPool.setConnectionMap(new ConcurrentHashMap<Integer, Connection>());
        System.out.println("Control: Client reseted");
    }

    /**
     * This class establishes the control channel by startup
     */
    /*public void establishControlChannel() {
        InetAddress address = null;
        System.out.println("control: Awaits controlconnection via 4060");

        try {
            address = InetAddress.getByName("10.1.1.31");

            ServerSocket anonServerSocket = new ServerSocket(4060, 30, address);
            System.out.println("control: waiting for controlconnections on " + address + ": 4060"
                   );

            Socket client = anonServerSocket.accept();
            System.out.println("control: controlconnection accepted (from " + client.getInetAddress() + ":"
                    + client.getPort() + ")");
            fromControlStream = client.getInputStream();
            toControlStream = client.getOutputStream();
            
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e1) {

            e1.printStackTrace();
            throw new RuntimeException("controlchannel could not bind" + address + ": 4060");
        }

    }
    */
    
    /**
     * This method checks the controlstream for controlsignals
     * @throws IOException
     */
   /* public void checkControlStream() throws IOException {
        int readBytes;
        boolean flow = true;
        while(flow) {
            int command = fromControlStream.read();
            switch (command) {
            case -1:
                flow = false;
                break;
            case 1:
                resetConnectionPool();
                break;
            case 2: 
                stopClient();
                break;
            default:
                break;
            }
        }
    }*/
    
    
    /**
     * Method to stop the client
     * 
     */
    public void stopClient() {
        for(Thread t: improvementThreads) {
            t.interrupt();
            
        }
        
        for(Thread t: unImprovementThreads) {
            t.interrupt();
            
        }
        mixConnection.stopConnection();
        connectionPool.stop();
        System.exit(0);
    }
}