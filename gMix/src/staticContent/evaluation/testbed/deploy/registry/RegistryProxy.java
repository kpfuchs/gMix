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

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import org.apache.log4j.Logger;

public class RegistryProxy extends UnicastRemoteObject implements Serializable, IRegistryProxy {

	private static final long serialVersionUID = -8244620644726010380L;
	private Registry rmiRegistry;
	private static final Logger logger = Logger.getLogger("RegistryProxy");

	public RegistryProxy(Registry rmiRegistry) throws RemoteException {
		super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory(null, null, true));

		this.rmiRegistry = rmiRegistry;
	}

	@Override
	public void proxyRebind(String name, Remote obj) throws RemoteException {
		logger.debug("rebind object with name " + name);
		this.rmiRegistry.rebind(name, obj);
	}
}