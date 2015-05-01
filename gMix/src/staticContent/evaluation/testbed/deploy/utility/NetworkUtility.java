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
package staticContent.evaluation.testbed.deploy.utility;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class NetworkUtility {

	/**
	 * Tries to determine the local IP address.
	 *
	 * @return local ip address or null
	 *
	 * @throws UnknownHostException
	 */
	public static InetAddress getLocalIp() throws UnknownHostException {
		String hostName     = InetAddress.getLocalHost().getHostName();
		InetAddress addrs[] = InetAddress.getAllByName(hostName);

		for (InetAddress addr: addrs) {			
			if (!addr.isLoopbackAddress()) {
				return addr;
			}
		}

		return null;
	}
	
	public static boolean isAdressLocal(String address) {
		try {
			Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
			
			while(e.hasMoreElements()) {
			    NetworkInterface n = (NetworkInterface) e.nextElement();
			    Enumeration<InetAddress> ee = n.getInetAddresses();
			    while (ee.hasMoreElements()) {
			        InetAddress i = (InetAddress) ee.nextElement();
			        
			        if (i.getHostAddress().equals(address)) return true;
			    }
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public static Set<String> getVirtualIpAddresses() {
		Set<String> result = new HashSet<String>();
		
		try {
			Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
			
			while(e.hasMoreElements()) {
			    NetworkInterface n = (NetworkInterface) e.nextElement();
			    Enumeration<InetAddress> ee = n.getInetAddresses();
			    while (ee.hasMoreElements()) {
			        InetAddress i = (InetAddress) ee.nextElement();			        
			        if (i.getHostAddress().startsWith("10.")) result.add(i.getHostAddress());
			    }
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		return result;
	}
}
