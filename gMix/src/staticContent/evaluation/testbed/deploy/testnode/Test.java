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
package staticContent.evaluation.testbed.deploy.testnode;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.rmi.ssl.SslRMIClientSocketFactory;

import org.apache.log4j.Logger;

import staticContent.evaluation.testbed.deploy.registry.IRegistryProxy;

/**
 * Verwaltet das TestNode Objekt und bindet es an die RMI Registry.
 *
 */
public class Test {
	private static Test instance = null;
	private String uniqueId;
	private final TestNode node;
	protected Logger logger = Logger.getLogger(this.getClass());

	private Test() {
		TestNode newNode;
		try {
			newNode = new TestNode(getUniqueId(), getHostname());
		} catch(Exception e) {
			newNode = null;
			logger.error(e.getMessage(), e);
		}
		this.node = newNode;
	}

	public static Test getInstance() {
		if (instance == null)
			instance = new Test();

		return instance;
	}

	/**
	 * Bindet den TestNode an die RMI Registry.
	 * @param registryHost die Adresse des Nameservice
	 * @param port der Port
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	public void bindToRegistry(String registryHost, int port) throws RemoteException, NotBoundException {
		port = (port >= 0)? port : 1099;

		logger.info("Try to connect to registry at: "+registryHost+" on port "+port);

        Registry registry            = LocateRegistry.getRegistry(registryHost, port, new SslRMIClientSocketFactory());
        IRegistryProxy registryProxy = (IRegistryProxy) registry.lookup("registryProxy");

        registryProxy.proxyRebind(uniqueId, node);
        
        logger.info("Bound TestNode with uniqueId: " + uniqueId +" to registry at: "+registryHost+" on port "+port);
	}

	/**
	 * Bindet den TestNode an die RMI Registry. Als Port wird der Standardport verwendet (normalerweise 1099).
	 * @param registryHost die Adresse des Nameservice
	 * @throws NotBoundException
	 * @throws RemoteException 
	 */
	public void bindToRegistry(String registryHost) throws RemoteException, NotBoundException {
		bindToRegistry(registryHost, -1);
	}

	/**
	 * Returns a unique testnode identifier.
	 *
	 * @return unique identifier
	 */
	public String getUniqueId() {
		if(uniqueId == null) this.uniqueId = "testnode_"+getHostname();
		return this.uniqueId;
	}

	/**
	 * Returns the name of the host.
	 *
	 * @return name of the host
	 *
	 */
	public String getHostname() {
//		return ConfigManager.getInstance(Type.TESTNODE).getString("hostAddress");
		return System.getProperty("java.rmi.server.hostname");
	}
}
