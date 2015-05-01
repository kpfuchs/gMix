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
package staticContent.evaluation.testbed.deploy.discovery;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.net.ssl.SSLServerSocketFactory;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import staticContent.evaluation.testbed.deploy.testnode.Test;
import staticContent.evaluation.testbed.deploy.utility.ConfigManager;
import staticContent.evaluation.testbed.deploy.utility.NetworkUtility;
import staticContent.evaluation.testbed.deploy.utility.OSDetector;
import staticContent.evaluation.testbed.deploy.utility.ConfigManager.Type;

public class DiscoveryNode {
	protected Logger logger = Logger.getLogger(this.getClass());

	/**
	 * This Thread handles a single multicast discovery request.
	 */
	private class DiscoveryMulticastServerThread extends Thread {
		private DatagramPacket packet;

		public DiscoveryMulticastServerThread(DatagramPacket packet) {
			this.packet = packet;
		}

		@Override
		public void run() {
			String data = new String(packet.getData(), 0, packet.getLength());
			String[] dataTuple = data.split(":");
			String registryHost = dataTuple[0];
			int registryPort = Integer.parseInt(dataTuple[1]);

			logger.debug("DiscoveryMulticastServerThread received message with registry location "+ registryHost +" at port "+registryPort);

		    InetAddress returnAdress = packet.getAddress();

		    try {
				if (returnAdress.equals(NetworkUtility.getLocalIp().getHostAddress())) {
					returnAdress = InetAddress.getLoopbackAddress();
				}
			} catch (UnknownHostException e) {
				logger.error(e.getMessage(), e);
			}
		    
	    	try {
	    		//create test node
		    	Test testNode = Test.getInstance();

		    	byte[] reply = testNode.getUniqueId().getBytes();

	    		int sendPort = config.getInt("coordinatorMulticastPort");

				DatagramPacket pack = new DatagramPacket(reply, reply.length, returnAdress, sendPort);

				DatagramSocket socket = new DatagramSocket();

				boolean immediateResponse = config.getBoolean("multicastImmediateResponse");

				if (immediateResponse) {
					socket.send(pack);
				}

				if (NetworkUtility.isAdressLocal(registryHost)) {
					registryHost = InetAddress.getLoopbackAddress().getHostAddress();
				}

				// test node rmi binding
				testNode.bindToRegistry(registryHost,registryPort);

				if (!immediateResponse) {
					socket.send(pack);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * This Thread handles a single discovery request over an SSL connection.
	 */
	private class DiscoverySSLServerThread extends Thread {
		private Socket s;

		public DiscoverySSLServerThread(Socket s) {
			this.s = s;
		}

		@Override
		public void run() {
			try {
				System.setProperty("java.rmi.server.hostname", s.getLocalAddress().getHostAddress());
				
				byte[] inputBytes = new byte[32];

				s.getInputStream().read(inputBytes, 0, inputBytes.length);				

				String data = new String(inputBytes, 0, inputBytes.length).trim();
				String[] dataTuple = data.split(":");
				String registryHost = dataTuple[0];
				int registryPort = Integer.parseInt(dataTuple[1]);
				logger.debug("DiscoverySSLServerThread received message with registry location "+ registryHost +" at port "+registryPort);
			    
		    	try{
		    		//create test node
			    	Test testNode = Test.getInstance();

			    	byte[] reply = testNode.getUniqueId().getBytes();
					
					if (NetworkUtility.isAdressLocal(registryHost)) {
						registryHost = InetAddress.getLoopbackAddress().getHostAddress();
					}

					// test node rmi binding
					testNode.bindToRegistry(registryHost, registryPort);

					s.getOutputStream().write(reply, 0, reply.length);
		    	} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	private ConfigManager config;

	public DiscoveryNode() {
		config = ConfigManager.getInstance(Type.TESTNODE);
	}

	/**
	 * Starts an SSL Server to handle discovery requests over SSL connection.
	 *
	 * @throws IOException
	 */
	public void startSSLServer() throws IOException {
		int receivePort = config.getInt("testnodeSSLPort");

		SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
	    ServerSocket ss = ssf.createServerSocket(receivePort);

	    logger.info("Node waiting for incoming SSL connections...");

	    while (true) {
	      Socket s = ss.accept();

	      (new DiscoverySSLServerThread(s)).start();
	    }
	}

	/**
	 * Starts a UDP multicast Server to handle discovery requests via multicast.
	 *
	 * @throws IOException
	 */
	private void startMulticastServer() throws IOException {
		int receivePort = config.getInt("testnodeMulticastPort");

		MulticastSocket socket = new MulticastSocket(receivePort);
		InetAddress group = InetAddress.getByName(config.getString("multicastAddress"));
		socket.joinGroup(group);

		logger.info("Multicast listener started.");

		byte[] buf;
		DatagramPacket packet;
		while(true) {
		    buf = new byte[21];
		    packet = new DatagramPacket(buf, buf.length);
		    socket.receive(packet);

		    logger.debug("packet received from: "+packet.getAddress().getHostAddress());

		    (new DiscoveryMulticastServerThread(packet)).start();
		}
	}

	/**
	 * Starts a new thread that starts a UDP multicast Server to handle discovery requests via multicast.
	 */
	public void startMulticastHandler() {
		final DiscoveryNode dn = this;

		Thread multicastHandler = new Thread() {

			@Override
			public void run() {
				try {
					dn.startMulticastServer();
				} catch (Exception e) {
					logger.error("Creation of Multicast handler failed.", e);
				}
			}
        };

        multicastHandler.start();
	}

	/**
	 * Starts a new thread that starts an SSL Server to handle discovery requests over SSL connection.
	 */
	public void startSSLConnectionHandler() {
		final DiscoveryNode dn = this;

		Thread sslConnectionHandler = new Thread() {
			@Override
			public void run() {
				try {
					dn.startSSLServer();
				} catch (Exception e) {
					logger.error("Creation of SSL connection handler failed.", e);
				}
			}
        };

        sslConnectionHandler.start();
	}

	/**
	 * Creates a testnode instance and registers it at the registry.
	 *
	 * @throws RemoteException
	 * @throws NotBoundException
	 * @throws UnknownHostException
	 */
	public void registerTestnode() throws RemoteException, NotBoundException, UnknownHostException {
		try{
    		//create test node
	    	Test testNode = Test.getInstance();

			String registryHost = config.getString("registryAddress");
			int registryPort = config.getInt("registryPort");
			
			if (NetworkUtility.isAdressLocal(registryHost)) {
				registryHost = InetAddress.getLoopbackAddress().getHostAddress();
			}

			// test node rmi binding
			testNode.bindToRegistry(registryHost, registryPort);
    	} catch (ConnectException | NotBoundException e) {
			logger.error("Connect to registry failed. Try to connect again...");
			registerTestnode();			
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		System.setProperty("javax.net.debug", "all");

		if (OSDetector.isUnknown() || OSDetector.isWindows()) {
			System.out.println("Sorry, your OS is not supported. Exiting...");
			System.exit(1);
		}

		try {
			// Set log4j configuration file path
			PropertyConfigurator.configure(System.getProperty("user.dir") +"/inputOutput/testbed/config/log4j.properties");

			ConfigManager cm = ConfigManager.getInstance(Type.TESTNODE);

			System.setProperty("javax.net.ssl.keyStore", cm.getAbsoluteFilePath(System.getProperty("user.dir") +cm.getString("testnodeKeystorePath")));
	        System.setProperty("javax.net.ssl.keyStorePassword", cm.getString("testnodeKeystorePassword"));
	        System.setProperty("javax.net.ssl.trustStore", cm.getAbsoluteFilePath(System.getProperty("user.dir") +cm.getString("testnodeTruststorePath")));
	        System.setProperty("javax.net.ssl.trustStorePassword", cm.getString("testnodeTruststorePassword"));

	        String testnodeRegisterMode = cm.getString("testnodeRegisterMode");

	        DiscoveryNode server = new DiscoveryNode();

	        switch(testnodeRegisterMode) {
	        	case "OnCreation" :
	        		server.registerTestnode();
	        		break;
	        	case "Multicast" :
	        		server.startMulticastHandler();
	        		break;
	        	case "OnCreationAndSSL" :
	        		server.registerTestnode();
	        		server.startSSLConnectionHandler();
	        		break;
	        	case "SSL" :
	        		server.startSSLConnectionHandler();
	        		break;
	        	case "Both" :
	        	default:
	        		server.startMulticastHandler();
	        		server.startSSLConnectionHandler();
	        }

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
