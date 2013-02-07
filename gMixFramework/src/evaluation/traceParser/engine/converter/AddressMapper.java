/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2012  Karl-Peter Fuchs
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
 */
package evaluation.traceParser.engine.converter;

import java.util.HashMap;


public class AddressMapper {

	public static HashMap<String, Integer> clientAdrToId = new HashMap<String, Integer>(1000);
	public static HashMap<Integer, String> clientIdToAdr = new HashMap<Integer, String>(1000);
	public static int clinetIdCounter = 0;
	public static HashMap<String, Integer> serverAdrToId = new HashMap<String, Integer>(1000);
	public static HashMap<Integer, String> serverIdToAdr = new HashMap<Integer, String>(1000);
	public static int serverIdCounter = Integer.MAX_VALUE;
	
	
	public static synchronized int getClientId(String address) {
		Integer result = clientAdrToId.get(address);
		if (result == null) {
			clinetIdCounter++;
			clientAdrToId.put(address, clinetIdCounter);
			clientIdToAdr.put(clinetIdCounter, address);
			return clinetIdCounter;
		} else {
			return result;
		}	
	}
	
	
	public static synchronized boolean isKnownClientId(String address) {
		return clientAdrToId.get(address) != null;
	}
	
	
	public static synchronized String getClientAddress(int id) {
		String result = clientIdToAdr.get(id);
		assert result != null;
		return result;	
	}

	
	public static synchronized int getServerId(String address) {
		Integer result = serverAdrToId.get(address);
		if (result == null) {
			serverIdCounter--;
			serverAdrToId.put(address, serverIdCounter);
			serverIdToAdr.put(serverIdCounter, address);
			return serverIdCounter;
		} else {
			return result;
		}	
	}
	
	
	public static synchronized boolean isKnownServerId(String address) {
		return serverAdrToId.get(address) != null;
	}
	
	
	public static synchronized String getServerAddress(int id) {
		String result = serverIdToAdr.get(id);
		assert result != null;
		return result;	
	}
	
	
	public static synchronized String getAddress(int id) {
		String result = serverIdToAdr.get(id);
		if (result == null)
			result = clientIdToAdr.get(id);
		assert result != null;
		return result;	
	}
	
	
	public static synchronized boolean isKnownId(String address) {
		return serverAdrToId.get(address) != null || clientAdrToId.get(address) != null;
	}
}
