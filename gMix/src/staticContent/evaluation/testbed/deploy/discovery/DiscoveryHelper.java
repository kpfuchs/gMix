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
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLSocketFactory;
import javax.rmi.ssl.SslRMIClientSocketFactory;

import org.apache.log4j.Logger;

import staticContent.evaluation.testbed.deploy.utility.ConfigManager;
import staticContent.evaluation.testbed.deploy.utility.NetworkUtility;
import staticContent.evaluation.testbed.deploy.utility.ConfigManager.Type;

public class DiscoveryHelper {

	private Map<String, String> collectedNodes = new ConcurrentHashMap<String, String>();
	private ConfigManager config;
	protected Logger logger = Logger.getLogger(this.getClass());

	private class DiscoveryClientUdpThread extends Thread {
		private DatagramPacket packet;

		public DiscoveryClientUdpThread(DatagramPacket packet) {
			this.packet = packet;
		}

		@Override
		public void run() {
			String data = new String(packet.getData(), 0, packet.getLength());

			collectedNodes.put(data, packet.getAddress().getHostAddress());

			logger.debug("response received from: "+packet.getAddress().getHostAddress());
		}
	}

	private class DiscoveryClientSSLThread extends Thread {
		private String nodeAddress;
		private String registryAddress;
		private int registryPort;

		public DiscoveryClientSSLThread(String nodeAddress, String registryAddress, int registryPort) {
			this.nodeAddress = nodeAddress;
			this.registryAddress = registryAddress;
			this.registryPort = registryPort;
		}

		@Override
		public void run() {
			logger.debug("Try to open discovery connection to "+nodeAddress);
			
			try {
				SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
				Socket s = ssf.createSocket(nodeAddress, config.getInt("testnodeSSLPort"));
				
				String localAddress = s.getLocalAddress().getHostAddress();				
				
				System.setProperty("java.rmi.server.hostname", localAddress);
				
				if (NetworkUtility.isAdressLocal(registryAddress)) {
					registryAddress = localAddress;
				}
				
				String content = (registryAddress+":"+registryPort);

				// do something
				s.getOutputStream().write(content.getBytes(), 0, content.getBytes().length);
				
				logger.debug("Sent registry address "+localAddress+" and port "+registryPort+" to node: "+nodeAddress);

				byte[] uniqueIdBytes = new byte[45];

				s.setSoTimeout(60000);

				s.getInputStream().read(uniqueIdBytes, 0, uniqueIdBytes.length);

				String uniqueId = new String(uniqueIdBytes, 0, uniqueIdBytes.length).trim();

				collectedNodes.put(uniqueId, nodeAddress);

				s.close();
			} catch (SocketException e) {
				// Appears when the timeout of the socket happens.
				// That means the testnode does not answer.
				// -> Do nothing in this case.
				// TODO: Nichtmal loggen?
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	public DiscoveryHelper() {
		config = ConfigManager.getInstance(Type.COORDINATOR);
	}

	/**
	 * Discovers the available test nodes via multicast.
	 * The method blocks until the discovery finished.
	 *
	 * @return a Set with unique testnode names
	 *
	 * @throws IOException
	 */
	public Set<String> getAvailableNodesViaMulticast() throws IOException {
		collectedNodes.clear();

		int sendPort = config.getInt("testnodeMulticastPort");
		int receivePort = config.getInt("coordinatorMulticastPort");
		String registryAddress = config.getString("registryAddress");
		int registryPort = config.getInt("registryPort");

		MulticastSocket socket = new MulticastSocket(receivePort);
		InetAddress group = InetAddress.getByName(config.getString("multicastAddress"));
		socket.joinGroup(group);

		byte[] content = (registryAddress+":"+registryPort).getBytes();

		DatagramPacket packet = new DatagramPacket(content, content.length, group, sendPort);

		logger.debug("Sending multicast paket with registry adress " + registryAddress + " and port "+ registryPort +" to multicast address " + config.getString("multicastAddress"));

		socket.send(packet);

		boolean immediateResponse = config.getBoolean("multicastImmediateResponse");

		socket.setSoTimeout((immediateResponse) ? 10000: 15000);

		try {
			while(true) {
				byte[] buf = new byte[256];
			    packet = new DatagramPacket(buf, buf.length);
			    socket.receive(packet);

			    (new DiscoveryClientUdpThread(packet)).start();
			}
		} catch (SocketTimeoutException e) {
			// Appears when the timeout of the socket happens.
			// That means no further testnode answers.
			// -> The discovery ends here.
			socket.close();
			return collectedNodes.keySet();
		}
	}

	/**
	 * Discovers the available test nodes via SSL connection on the basis of the given list of nodes.
	 * The list should contain the node dns name or the ip address as String.
	 * The method blocks until the discovery finished.
	 *
	 * @param nodes
	 * @return a Set with unique testnode names
	 *
	 * @throws IOException
	 */
	public Set<String> getAvailableNodesWithPreconfiguredList(Collection<String> nodes) throws IOException {
		collectedNodes.clear();

		String registryAddress = config.getString("registryAddress");
		int registryPort = config.getInt("registryPort");		

		List<DiscoveryClientSSLThread> threads = new ArrayList<DiscoveryHelper.DiscoveryClientSSLThread>();

		for(String nodeAddress: nodes) {
			DiscoveryClientSSLThread t = new DiscoveryClientSSLThread(nodeAddress, registryAddress, registryPort);

			threads.add(t);

			t.start();
		}

		while(true) {
			boolean alive = false;

			for(DiscoveryClientSSLThread t: threads) {
				if (t.isAlive()) {
					alive = true;
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
					}
					break;
				}
			}

			if (!alive) break;
		}


		return collectedNodes.keySet();
	}

	/**
	 * Discovers the available test nodes by asking the registry which testnodes are registered.
	 * The method blocks until the discovery finished.
	 *
	 * @return a Set with unique testnode names
	 *
	 * @throws RemoteException
	 */
	public Set<String> getAvailableNodesFromRegistry() throws RemoteException {
		Set<String> result = new HashSet<String>();

		Registry registry = LocateRegistry.getRegistry(config.getString("registryAddress"), config.getInt("registryPort"), new SslRMIClientSocketFactory());

		for (String name : registry.list()) {
			if (name.startsWith("testnode_")) {
				result.add(name);
			}
		}

		return result;
	}

}
