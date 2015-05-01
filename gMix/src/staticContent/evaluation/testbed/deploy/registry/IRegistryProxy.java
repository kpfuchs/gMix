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

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IRegistryProxy extends Remote{

	/**
	 * Replaces the binding for the specified name in this registry with
	 * the supplied remote reference. If there is an existing binding for
	 * the specified name, it is discarded. This works even if the remote
	 * object is not on the same host as the registry.
	 *
	 * @param name - the name to associate with the remote reference
	 * @param obj - a reference to a remote object (usually a stub)
	 *
	 * @throws RemoteException
	 *
	 * @see java.rmi.registry.Registry
	 */
	public void proxyRebind(String name, Remote obj) throws RemoteException;
}