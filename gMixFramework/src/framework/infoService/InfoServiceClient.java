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
package framework.infoService;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import framework.core.routing.MixList;
import framework.core.util.Util;


public class InfoServiceClient {

	private boolean connected = false;
	private InetAddress infoServiceAddress;
	private int infoServicePort;
	private Socket infoServiceSocket;	// TODO: ssl
	private InputStream inputStream;
	private OutputStream outputStream;
	
	
	public InfoServiceClient(InetAddress infoServiceAddress, int infoServicePort) {
		this.infoServiceAddress = infoServiceAddress;
		this.infoServicePort = infoServicePort;
	}
	
	
	public synchronized int registerAsMix() {
		connect();
		try {
			return InfoServiceServer.client_REGISTER_AS_MIX(outputStream, inputStream);
		} catch (Exception e) {
			e.printStackTrace();
			return registerAsMix();
		}
	}


	public synchronized int registerAsClient() {
		connect();
		try {
			return InfoServiceServer.client_REGISTER_AS_CLIENT(outputStream, inputStream);
		} catch (Exception e) {
			e.printStackTrace();
			return registerAsClient();
		}
	}

	
	public synchronized int getNumberOfMixes() {
		connect();
		try {
			return InfoServiceServer.client_GET_NUMBER_OF_MIXES(outputStream, inputStream);
		} catch (Exception e) {
			e.printStackTrace();
			return getNumberOfMixes();
		}
	}
	
	
	public synchronized MixList getMixList() {
		connect();
		try {
			return InfoServiceServer.client_GET_MIX_LIST(outputStream, inputStream);
		} catch (Exception e) {
			e.printStackTrace();
			return getMixList();
		}
	}

	
	public synchronized byte[] getValueFromMix(int mixId, String key) {
		connect();
		try {
			return InfoServiceServer.client_GET_VALUE_FROM_MIX(outputStream, inputStream, mixId, key);
		} catch (Exception e) {
			e.printStackTrace();
			return getValueFromMix(mixId, key);
		}
	}
	
	
	public synchronized byte[] getValue(String key) {
		connect();
		try {
			return InfoServiceServer.client_GET_VALUE(outputStream, inputStream, key);
		} catch (Exception e) {
			e.printStackTrace();
			return getValue(key);
		}
	}
	
	
	public synchronized byte[] getValueFromClient(int clientId, String key) {
		connect();
		try {
			return InfoServiceServer.client_GET_VALUE_FROM_CLIENT(outputStream, inputStream, clientId, key);
		} catch (Exception e) {
			e.printStackTrace();
			return getValueFromClient(clientId, key);
		}
	}
	
	
	// TODO
	/*public synchronized byte[][] getValues(int[] mixIds, String key) {
		connect();
		try {
			return InfoServiceServer_v2.client_GET_VALUE_...(outputStream, inputStream, mixIds, key);
		} catch (Exception e) {
			e.printStackTrace();
			return getValues(mixIds, key);
		}
	}*/

	
	public synchronized byte[][] getValueFromAllMixes(String key) {
		connect();
		try {
			return InfoServiceServer.client_GET_VALUE_FROM_ALL_MIXES(outputStream, inputStream, key);
		} catch (Exception e) {
			e.printStackTrace();
			return getValueFromAllMixes(key);
		}
	}

	
	public synchronized void postValueAsMix(int mixId, String key, byte[] value) {
		connect();
		try {
			InfoServiceServer.client_POST_VALUE_AS_MIX(outputStream, inputStream, mixId, key, value);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("postValue(" +key +", " +Util.md5(value) +") failed"); // TODO: try x times again before throwing RuntimeException
		}
	}

	
	public synchronized void postValue(String key, byte[] value) {
		connect();
		try {
			InfoServiceServer.client_POST_VALUE(outputStream, inputStream, key, value);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("postAnonValue(" +key +", " +Util.md5(value) +") failed"); // TODO: try x times again before throwing RuntimeException
		}
	}
	
	
	public synchronized void postValueAsClient(int clientid, String key, byte[] value) {
		connect();
		try {
			InfoServiceServer.client_POST_VALUE_AS_CLIENT(outputStream, inputStream, clientid, key, value);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("postValueAsClient(" +key +", " +Util.md5(value) +") failed"); // TODO: try x times again before throwing RuntimeException
		}
	}
	
	
	public synchronized int getSuggestedPort() {
		connect();
		try {
			return InfoServiceServer.client_SUGGEST_MIX_PORT(outputStream, inputStream);
		} catch (Exception e) {
			e.printStackTrace();
			return getSuggestedPort();
		}
	}


	public synchronized boolean getIsDebugModeOn() {
		connect();
		try {
			return InfoServiceServer.client_is_DEBUG_MODE_ON(outputStream, inputStream);
		} catch (Exception e) {
			e.printStackTrace();
			return getIsDebugModeOn();
		}
	}
	

	public synchronized boolean getIsDuplexModeOn() {
		connect();
		try {
			return InfoServiceServer.client_is_DUPLEX_ON(outputStream, inputStream);
		} catch (Exception e) {
			e.printStackTrace();
			return getIsDuplexModeOn();
		}
	}

	
	public synchronized boolean getIsLocalModeOn() {
		connect();
		try {
			return InfoServiceServer.client_is_LOCAL_MODE_ON(outputStream, inputStream);
		} catch (Exception e) {
			e.printStackTrace();
			return getIsLocalModeOn();
		}
	}
	
	
	public synchronized void waitForEndOfAddressExchangePhase() {
		connect();
		try {
			InfoServiceServer.client_WAIT_FOR_END_OF_ADDRESS_EXCHANGE_PHASE(outputStream, inputStream);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("waitForEndOfRegistrationPhase() failed"); // TODO: try x times again before throwing RuntimeException
		}
	}
	
	
	public synchronized void waitForEndOfRegistrationPhase() {
		connect();
		try {
			InfoServiceServer.client_WAIT_FOR_END_OF_REGISTRATION_PHASE(outputStream, inputStream);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("waitForEndOfRegistrationPhase() failed"); // TODO: try x times again before throwing RuntimeException
		}
	}

	
	public synchronized void waitForEndOfInitializationPhase() {
		connect();
		try {
			InfoServiceServer.client_WAIT_FOR_END_OF_INITIALIZATION_PHASE(outputStream, inputStream);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("waitForEndOfInitializationPhase() failed"); // TODO: try x times again before throwing RuntimeException
		}
	}
	
	
	public synchronized void waitForEndOfBeginPhase() {
		connect();
		try {
			InfoServiceServer.client_WAIT_FOR_END_OF_BEGIN_PHASE(outputStream, inputStream);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("waitForEndOfBeginPhase() failed"); // TODO: try x times again before throwing RuntimeException
		}
	}

	
	public synchronized void waitTillMixesAreUp() {
		connect();
		try {
			InfoServiceServer.client_WAIT_TILL_MIXES_ARE_UP(outputStream, inputStream);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("waitTillMixesAreUp() failed"); // TODO: try x times again before throwing RuntimeException
		}
	}

	
	public synchronized void connect() {
		if (connected)
			return;
		while (true) {
			try {
				this.infoServiceSocket = new Socket();
				this.infoServiceSocket.setKeepAlive(true); // permanent connection
				SocketAddress address = new InetSocketAddress(infoServiceAddress, infoServicePort);
				System.out.println(infoServiceAddress +" " +infoServicePort); // TODO: remove
				this.infoServiceSocket.connect(address);
				this.outputStream = infoServiceSocket.getOutputStream();
				this.inputStream = infoServiceSocket.getInputStream();
				this.connected = true;
				break;
			} catch (IOException e) {
				System.out.println("could not connect to info service - trying again"); 
				try {Thread.sleep(5000);} catch (InterruptedException e1) {e1.printStackTrace();}
				continue;
			}
		}

	}
	
	
	public synchronized void disconnect() {
		try {
			ProtocolPrimitives.sendString(outputStream, "DISCONNECT");
			this.outputStream.flush();
			this.infoServiceSocket.close();
			this.connected = false;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
