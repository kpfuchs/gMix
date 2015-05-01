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
package staticContent.evaluation.testbed.deploy.registry;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import org.apache.log4j.PropertyConfigurator;

import staticContent.evaluation.testbed.deploy.utility.ConfigManager;
import staticContent.evaluation.testbed.deploy.utility.ConfigManager.Type;


/**
 * Ein Registry Service, der die RMI Registry startet und hosted.
 *
 */
public class DiscoveryRegistry {

	private static DiscoveryRegistry discoveryRegistry;
	private int port;
	private String ipString;
	
	
	/**
	 * Creates a new Registry instance.
	 */
	private DiscoveryRegistry() {
		loadSettings();
		startRegistry();
		registerRegistryProxy();
	}
	
	
	private void loadSettings() {
		PropertyConfigurator.configure(System.getProperty("user.dir") +"/inputOutput/testbed/config/log4j.properties");
		ConfigManager config = ConfigManager.getInstance(Type.REGISTRY);
		System.setProperty("javax.net.ssl.keyStore", config.getAbsoluteFilePath(System.getProperty("user.dir") +config.getString("registryKeystorePath")));
        System.setProperty("javax.net.ssl.keyStorePassword", config.getString("registryKeystorePassword"));
        System.setProperty("javax.net.ssl.trustStore", config.getAbsoluteFilePath(System.getProperty("user.dir") +config.getString("registryTruststorePath")));
        System.setProperty("javax.net.ssl.trustStorePassword", config.getString("registryTruststorePassword"));
        this.ipString = config.getString("hostAddress");
        try {
			this.port = ConfigManager.getInstance(Type.REGISTRY).getInt("registryPort");
		} catch (Exception e) {
			this.port = 1099;
		}
	}

	
	/***
	 * starts the rmi registry server. will be used later by test nodes to register.
	 * @param port
	 */
	private void startRegistry() {
		System.out.println("starting RMI registry proxy..."); 
		try {
			SslRMIServerSocketFactory factory = new SslRMIServerSocketFactory(null, null, true);
			LocateRegistry.createRegistry(port, new SslRMIClientSocketFactory(), factory);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private void registerRegistryProxy() {
		try {
			System.out.println("trying to bind registry to ip " +this.ipString); 
			Registry registry = LocateRegistry.getRegistry(this.ipString, this.port, new SslRMIClientSocketFactory());
			RegistryProxy regProxy = new RegistryProxy(registry);
			registry.rebind("registryProxy", regProxy);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		System.out.println("RMI registry proxy started. waiting for connections on port " +port);
	}


	public static void startRegistryServer() {
		if (DiscoveryRegistry.discoveryRegistry == null)
			DiscoveryRegistry.discoveryRegistry = new DiscoveryRegistry();
	}
	
	
	//-Djavax.net.debug=all
	public static void main(String[] args) {
		startRegistryServer();
	}
}
