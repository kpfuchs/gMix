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
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import framework.core.config.Settings;
import framework.core.launcher.CommandLineParameters;
import framework.core.routing.MixList;
import framework.core.util.Util;


public class InfoServiceServer {
	// TODO: ssl
	// TODO: sign data stored by mixes
	// TODO: pki
	
	private boolean LOCAL_MODE_ON;
	private boolean DEBUG_MODE_ON;
	private boolean DUPLEX_ON;
	private int NUMBER_OF_MIXES;
	private int PORT;
	private int BACKLOG;
	private String ADDRESS;
	
	private AtomicInteger mixIdCounter = new AtomicInteger(0);
	private AtomicInteger clientIdCounter = new AtomicInteger(0);
	private AtomicInteger mixPort = new AtomicInteger(23301);
	private AtomicBoolean addressExchangePhaseFinished = new AtomicBoolean(false);
	private AtomicBoolean registrationPhaseFinished = new AtomicBoolean(false);
	private AtomicBoolean initPhaseFinished = new AtomicBoolean(false);
	private AtomicBoolean beginPhaseFinished = new AtomicBoolean(false);
	private int addressesReceived = 0;
	private int registeredMixes = 0;
	private int initializedMixes = 0;
	private int readyMixes = 0;
	
	private ServerSocket serverSocket;
	private HashMap<Integer, HashMap<String, byte[]>> mixData = new HashMap<Integer, HashMap<String, byte[]>>();
	private HashMap<Integer, HashMap<String, byte[]>> clientData = new HashMap<Integer, HashMap<String, byte[]>>();
	private HashMap<String, byte[]> anonData = new HashMap<String, byte[]>();
	private Settings settings;
	
	
	public InfoServiceServer(CommandLineParameters commandLineParameters) {
		settings = commandLineParameters.generateSettingsObject();
		
		ProtocolPrimitives.DEBUG = settings.getPropertyAsBoolean("IS_DISPLAY_TRANSACTIONS");
		this.LOCAL_MODE_ON = settings.getPropertyAsBoolean("GLOBAL_LOCAL_MODE_ON");
		if (!LOCAL_MODE_ON)
			System.err.println("WARNING: LOCAL_MODE is set to FALSE; Mixes and Information Service will be available via Network; Only safe for trusted networks!"); 
		this.DEBUG_MODE_ON = settings.getPropertyAsBoolean("GLOBAL_DEBUG_MODE_ON");
		this.DUPLEX_ON = settings.getPropertyAsBoolean("GLOBAL_IS_DUPLEX");
		this.NUMBER_OF_MIXES = settings.getPropertyAsInt("GLOBAL_NUMBER_OF_MIXES");
		this.PORT = settings.getPropertyAsInt("GLOBAL_INFO_SERVICE_PORT");
		this.ADDRESS = settings.getProperty("GLOBAL_INFO_SERVICE_ADDRESS");
		this.BACKLOG = settings.getPropertyAsInt("IS_BACKLOG");
		
		begin();
	}

	
	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		new InfoServiceServer(new CommandLineParameters(args));
	}
	
	
	private void begin() {
		try {
			if (LOCAL_MODE_ON)
				serverSocket = new ServerSocket(PORT, BACKLOG, InetAddress.getByName("localhost"));
			else if (ADDRESS.equalsIgnoreCase("AUTO"))
				serverSocket = new ServerSocket(PORT, BACKLOG, InetAddress.getLocalHost());
			else
				serverSocket = new ServerSocket(PORT, BACKLOG, InetAddress.getByName(ADDRESS));
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(""); 
		}
		new Thread(
        		new Runnable() {
        			public void run() {
        				acceptConnections();
        			}	
        		}
        	).start(); 
	}

	
	private void acceptConnections() {
		System.out.println("Information Service up on " +serverSocket.getLocalSocketAddress()); 
		while(true) {
			try {
				Socket connectionSocket = serverSocket.accept();
				new InfoConnectionHandler(connectionSocket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	
	private class InfoConnectionHandler extends Thread {
		
		private Socket socket;
		private InputStream inputStream;
		private OutputStream outputStream;
		
		
		public InfoConnectionHandler(Socket s) {
			this.socket = s;
			try {
				this.socket.setKeepAlive(true);
				this.inputStream = s.getInputStream();
				this.outputStream = s.getOutputStream();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			start();
		}
		
		
		@Override
		public void run() {
			System.out.println("InfoServiceServer: new connection from client"); // TODO
			while (true) {
				try {
					String command = ProtocolPrimitives.receiveString(inputStream);
					if (command == null) {
						socket.close();
						return;
					} else if (command.equals("REGISTER_MIX"))
						REGISTER_AS_MIX(outputStream, inputStream);
					else if (command.equals("REGISTER_CLIENT"))
						REGISTER_AS_CLIENT(outputStream, inputStream);
					else if (command.equals("GET_VALUE_FROM_MIX"))
						GET_VALUE_FROM_MIX(outputStream, inputStream);
					else if (command.equals("GET_VALUE_FROM_CLIENT"))
						GET_VALUE_FROM_CLIENT(outputStream, inputStream);
					else if (command.equals("GET_VALUE_FROM_ALL_MIXES"))
						GET_VALUE_FROM_ALL_MIXES(outputStream, inputStream);
					else if (command.equals("GET_VALUE"))
						GET_VALUE(outputStream, inputStream);
					else if (command.equals("POST_VALUE"))
						POST_VALUE(outputStream, inputStream);
					else if (command.equals("POST_VALUE_AS_MIX"))
						POST_VALUE_AS_MIX(outputStream, inputStream);
					else if (command.equals("POST_VALUE_AS_CLIENT"))
						POST_VALUE_AS_CLIENT(outputStream, inputStream);
					else if (command.equals("GET_MIX_LIST"))
						GET_MIX_LIST(outputStream, inputStream);
					else if (command.equals("GET_NUMBER_OF_MIXES"))
						GET_NUMBER_OF_MIXES(outputStream);
					else if (command.equals("SUGGEST_MIX_PORT"))
						SUGGEST_MIX_PORT(outputStream);
					else if (command.equals("DEBUG_MODE_ON"))
						DEBUG_MODE_ON(outputStream);
					else if (command.equals("LOCAL_MODE_ON"))
						LOCAL_MODE_ON(outputStream);
					else if (command.equals("DUPLEX_ON"))
						DUPLEX_ON(outputStream);
					else if (command.equals("WAIT_FOR_END_OF_ADDRESS_EXCHANGE_PHASE"))
						WAIT_FOR_END_OF_ADDRESS_EXCHANGE_PHASE(outputStream, inputStream);
					else if (command.equals("WAIT_FOR_END_OF_REGISTRATION_PHASE"))
						WAIT_FOR_END_OF_REGISTRATION_PHASE(outputStream, inputStream);  
					else if (command.equals("WAIT_FOR_END_OF_INITIALIZATION_PHASE"))
						WAIT_FOR_END_OF_INITIALIZATION_PHASE(outputStream, inputStream);
					else if (command.equals("WAIT_FOR_END_OF_BEGIN_PHASE"))
						WAIT_FOR_END_OF_BEGIN_PHASE(outputStream, inputStream);
					else if (command.equals("WAIT_TILL_MIXES_ARE_UP"))
						WAIT_TILL_MIXES_ARE_UP(outputStream, inputStream);
					else if (command.equals("DISCONNECT")) {
						socket.close();
						return;
					} else
						throw new Exception("received unknown command: " +command);
				} catch (Exception e) {
					e.printStackTrace();
					try {socket.close();} catch (IOException ex) {}
					return; // TODO: continue;
				}
			}
		}
	}
	
	
	private void REGISTER_AS_MIX(OutputStream outputStream, InputStream inputStream) throws Exception {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: REGISTER_AS_MIX()");
		int id;
		HashMap<String, byte[]> data;
		synchronized (mixData) {
			id = mixIdCounter.getAndIncrement();
			data = new HashMap<String, byte[]>();
			mixData.put(id, data); 
		}
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: REGISTER_AS_MIX(): result: " +id);
		ProtocolPrimitives.sendInt(outputStream, id);
		outputStream.flush();
	}

	
	public static int client_REGISTER_AS_MIX(OutputStream outputStream, InputStream inputStream) throws UnsupportedEncodingException, IOException {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("int client_REGISTER_AS_MIX");
		ProtocolPrimitives.sendString(outputStream, "REGISTER_MIX");
		outputStream.flush();
		int id = ProtocolPrimitives.receiveInt(inputStream);
		if (ProtocolPrimitives.DEBUG)
			System.out.println("int request_REGISTER_AS_MIX(): result: " +id);
		return id;
	}
	
	
	private void REGISTER_AS_CLIENT(OutputStream outputStream, InputStream inputStream) throws Exception {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: REGISTER_AS_CLIENT()");
		int id;
		HashMap<String, byte[]> data;
		synchronized (clientData) {
			id = clientIdCounter.getAndIncrement();
			data = new HashMap<String, byte[]>();
			clientData.put(id, data); 
		}
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: REGISTER_AS_CLIENT(): result: " +id);
		ProtocolPrimitives.sendInt(outputStream, id);
		outputStream.flush();
	}

	
	public static int client_REGISTER_AS_CLIENT(OutputStream outputStream, InputStream inputStream) throws UnsupportedEncodingException, IOException {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("int client_REGISTER_AS_CLIENT");
		ProtocolPrimitives.sendString(outputStream, "REGISTER_CLIENT");
		outputStream.flush();
		int id = ProtocolPrimitives.receiveInt(inputStream);
		if (ProtocolPrimitives.DEBUG)
			System.out.println("int client_REGISTER_AS_CLIENT(): result: " +id);
		return id;
	}
	

	private void LOCAL_MODE_ON(OutputStream outputStream) throws Exception {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: LOCAL_MODE_ON() (" +LOCAL_MODE_ON +")");
		ProtocolPrimitives.sendBoolean(outputStream, LOCAL_MODE_ON);
		outputStream.flush();
	}
	
	
	public static boolean client_is_LOCAL_MODE_ON(OutputStream outputStream, InputStream inputStream) throws UnsupportedEncodingException, IOException {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("boolean client_is_LOCAL_MODE_ON");
		ProtocolPrimitives.sendString(outputStream, "LOCAL_MODE_ON");
		outputStream.flush();
		boolean result = ProtocolPrimitives.receiveBoolean(inputStream);
		if (ProtocolPrimitives.DEBUG)
			System.out.println("boolean client_is_LOCAL_MODE_ON: result: " +result);
		return result;
	}
	
	
	private void DEBUG_MODE_ON(OutputStream outputStream) throws Exception {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: DEBUG_MODE_ON() (" +DEBUG_MODE_ON +")");
		ProtocolPrimitives.sendBoolean(outputStream, DEBUG_MODE_ON);
		outputStream.flush();
	}
	
	
	public static boolean client_is_DEBUG_MODE_ON(OutputStream outputStream, InputStream inputStream) throws UnsupportedEncodingException, IOException {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("boolean client_is_DEBUG_MODE_ON");
		ProtocolPrimitives.sendString(outputStream, "DEBUG_MODE_ON");
		outputStream.flush();
		boolean result = ProtocolPrimitives.receiveBoolean(inputStream);
		if (ProtocolPrimitives.DEBUG)
			System.out.println("boolean client_is_DEBUG_MODE_ON: result: " +result);
		return result;
	}
	
	
	private void DUPLEX_ON(OutputStream outputStream) throws Exception {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: DUPLEX_ON() (" +DUPLEX_ON +")");
		ProtocolPrimitives.sendBoolean(outputStream, DUPLEX_ON);
		outputStream.flush();
	}
	
	
	public static boolean client_is_DUPLEX_ON(OutputStream outputStream, InputStream inputStream) throws UnsupportedEncodingException, IOException {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("boolean client_is_DUPLEX_ON");
		ProtocolPrimitives.sendString(outputStream, "DUPLEX_ON");
		outputStream.flush();
		boolean result = ProtocolPrimitives.receiveBoolean(inputStream);
		if (ProtocolPrimitives.DEBUG)
			System.out.println("boolean client_is_DUPLEX_ON: result: " +result);
		return result;
	}
	
	
	private void SUGGEST_MIX_PORT(OutputStream outputStream) throws Exception {
		int port = mixPort.getAndIncrement();
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: SUGGEST_MIX_PORT(): " +port);
		ProtocolPrimitives.sendInt(outputStream, port);
		outputStream.flush();
		
	}
	
	
	public static int client_SUGGEST_MIX_PORT(OutputStream outputStream, InputStream inputStream) throws UnsupportedEncodingException, IOException {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("int client_SUGGEST_MIX_PORT");
		ProtocolPrimitives.sendString(outputStream, "SUGGEST_MIX_PORT");
		outputStream.flush();
		int result = ProtocolPrimitives.receiveInt(inputStream);
		if (ProtocolPrimitives.DEBUG)
			System.out.println("int client_SUGGEST_MIX_PORT: result: " +result);
		return result;
	}

	
	private void GET_NUMBER_OF_MIXES(OutputStream outputStream) throws Exception {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: GET_NUMBER_OF_MIXES(): " +NUMBER_OF_MIXES);
		ProtocolPrimitives.sendInt(outputStream, NUMBER_OF_MIXES);
		outputStream.flush();
		
	}
	
	
	public static int client_GET_NUMBER_OF_MIXES(OutputStream outputStream, InputStream inputStream) throws UnsupportedEncodingException, IOException {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("int GET_NUMBER_OF_MIXES");
		ProtocolPrimitives.sendString(outputStream, "GET_NUMBER_OF_MIXES");
		outputStream.flush();
		int result = ProtocolPrimitives.receiveInt(inputStream);
		if (ProtocolPrimitives.DEBUG)
			System.out.println("int GET_NUMBER_OF_MIXES: result: " +result);
		return result;
	}
	
	
	private boolean POST_VALUE_AS_MIX(OutputStream outputStream, InputStream inputStream) throws Exception {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: POST_VALUE_AS_MIX()");
		int mixId = ProtocolPrimitives.receiveInt(inputStream);
		String key = ProtocolPrimitives.receiveString(inputStream);
		byte[] value = ProtocolPrimitives.receiveByteArray(inputStream);
		synchronized (mixData) {
			HashMap<String, byte[]> recordForMix = mixData.get(mixId);
			if (recordForMix == null || key == null || key.equals("") || value == null || value.length == 0)
				return false;
			recordForMix.put(key, value);
			if (ProtocolPrimitives.DEBUG)
				System.out.println("int POST_VALUE_AS_MIX: result: " +mixId +", " +key +", " +Util.md5(value));
			return true;
		}
	}
	
	
	public static void client_POST_VALUE_AS_MIX(OutputStream outputStream, InputStream inputStream, int mixId, String key, byte[] value) throws UnsupportedEncodingException, IOException {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("void client_POST_VALUE_AS_MIX("+mixId +", "+key +", " +Util.md5(value) +")");
		ProtocolPrimitives.sendString(outputStream, "POST_VALUE_AS_MIX");
		ProtocolPrimitives.sendInt(outputStream, mixId);
		ProtocolPrimitives.sendString(outputStream, key);
		ProtocolPrimitives.sendByteArray(outputStream, value);
		outputStream.flush();
	}
	
	
	private void POST_VALUE(OutputStream outputStream, InputStream inputStream) throws Exception {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: POST_VALUE()");
		String key = ProtocolPrimitives.receiveString(inputStream);
		byte[] value = ProtocolPrimitives.receiveByteArray(inputStream);
		synchronized (anonData) {
			anonData.put(key, value);
			if (ProtocolPrimitives.DEBUG)
				System.out.println("int POST_VALUE: result: " +key +", " +Util.md5(value));
		}
	}
	
	
	public static void client_POST_VALUE(OutputStream outputStream, InputStream inputStream, String key, byte[] value) throws UnsupportedEncodingException, IOException {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("void client_POST_VALUE("+key +", " +Util.md5(value) +")");
		ProtocolPrimitives.sendString(outputStream, "POST_VALUE");
		ProtocolPrimitives.sendString(outputStream, key);
		ProtocolPrimitives.sendByteArray(outputStream, value);
		outputStream.flush();
	}
	
	
	private boolean POST_VALUE_AS_CLIENT(OutputStream outputStream, InputStream inputStream) throws Exception {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: POST_VALUE_AS_CLIENT()");
		int clientId = ProtocolPrimitives.receiveInt(inputStream);
		String key = ProtocolPrimitives.receiveString(inputStream);
		byte[] value = ProtocolPrimitives.receiveByteArray(inputStream);
		synchronized (clientData) {
			HashMap<String, byte[]> recordForClient = clientData.get(clientId);
			if (recordForClient == null || key == null || key.equals("") || value == null || value.length == 0)
				return false;
			recordForClient.put(key, value);
			if (ProtocolPrimitives.DEBUG)
				System.out.println("int POST_VALUE_AS_CLIENT: result: " +clientId +", " +key +", " +Util.md5(value));
			return true;
		}
	}

	
	public static void client_POST_VALUE_AS_CLIENT(OutputStream outputStream, InputStream inputStream, int clientId, String key, byte[] value) throws UnsupportedEncodingException, IOException {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("void client_POST_VALUE_AS_CLIENT("+clientId +", "+key +", " +Util.md5(value) +")");
		ProtocolPrimitives.sendString(outputStream, "POST_VALUE_AS_CLIENT");
		ProtocolPrimitives.sendInt(outputStream, clientId);
		ProtocolPrimitives.sendString(outputStream, key);
		ProtocolPrimitives.sendByteArray(outputStream, value);
		outputStream.flush();
	}
	

	public boolean GET_VALUE_FROM_MIX(OutputStream outputStream, InputStream inputStream) throws Exception {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: GET_VALUE_FROM_MIX()");
		int mixId = ProtocolPrimitives.receiveInt(inputStream);
		String key = ProtocolPrimitives.receiveString(inputStream);
		boolean success = true;
		byte[] data;
		synchronized (mixData) {
			HashMap<String, byte[]> recordForMix = mixData.get(mixId);
			if (recordForMix == null || (data = recordForMix.get(key)) == null) {
				success = false;
				throw new RuntimeException(key); 
			}
		}
		if (ProtocolPrimitives.DEBUG)
			System.out.println("GET_VALUE_FROM_MIX() result: " +Util.md5(data));
		ProtocolPrimitives.sendByteArray(outputStream, data);
		outputStream.flush();
		return success;
	}
	
	
	public static byte[] client_GET_VALUE_FROM_MIX(OutputStream outputStream, InputStream inputStream, int mixId, String key) throws UnsupportedEncodingException, IOException {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("byte[] client_GET_VALUE_FROM_MIX("+mixId +", "+key +")");
		ProtocolPrimitives.sendString(outputStream, "GET_VALUE_FROM_MIX");
		ProtocolPrimitives.sendInt(outputStream, mixId);
		ProtocolPrimitives.sendString(outputStream, key);
		outputStream.flush();
		byte[] result = ProtocolPrimitives.receiveByteArray(inputStream);
		if (ProtocolPrimitives.DEBUG)
			System.out.println("byte[] client_GET_VALUE_FROM_MIX: result: " +Util.md5(result));
		return result;
	}
	
	
	public boolean GET_VALUE(OutputStream outputStream, InputStream inputStream) throws Exception {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: GET_VALUE()");
		String key = ProtocolPrimitives.receiveString(inputStream);
		byte[] data;
		synchronized (anonData) {
			data = anonData.get(key);
			if (data == null)
				return false;
		}
		if (ProtocolPrimitives.DEBUG)
			System.out.println("GET_VALUE() result: " +Util.md5(data));
		ProtocolPrimitives.sendByteArray(outputStream, data);
		outputStream.flush();
		return true;
	}
	
	
	public static byte[] client_GET_VALUE(OutputStream outputStream, InputStream inputStream, String key) throws UnsupportedEncodingException, IOException {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("byte[] client_GET_VALUE("+key +")");
		ProtocolPrimitives.sendString(outputStream, "GET_VALUE");
		ProtocolPrimitives.sendString(outputStream, key);
		outputStream.flush();
		byte[] result = ProtocolPrimitives.receiveByteArray(inputStream);
		if (ProtocolPrimitives.DEBUG)
			System.out.println("byte[] client_GET_VALUE: result: " +Util.md5(result));
		return result;
	}
	
	
	public boolean GET_VALUE_FROM_CLIENT(OutputStream outputStream, InputStream inputStream) throws Exception {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: GET_VALUE_FROM_CLIENT()");
		int mixId = ProtocolPrimitives.receiveInt(inputStream);
		String key = ProtocolPrimitives.receiveString(inputStream);
		boolean success = true;
		byte[] data;
		synchronized (clientData) {
			HashMap<String, byte[]> recordForClient = clientData.get(mixId);
			if (recordForClient == null || (data = recordForClient.get(key)) == null) {
				success = false;
				throw new RuntimeException(key); 
			}
		}
		if (ProtocolPrimitives.DEBUG)
			System.out.println("GET_VALUE_FROM_CLIENT() result: " +Util.md5(data));
		ProtocolPrimitives.sendByteArray(outputStream, data);
		outputStream.flush();
		return success;
	}
	
	
	public static byte[] client_GET_VALUE_FROM_CLIENT(OutputStream outputStream, InputStream inputStream, int clientId, String key) throws UnsupportedEncodingException, IOException {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("byte[] client_GET_VALUE_FROM_CLIENT("+clientId +", "+key +")");
		ProtocolPrimitives.sendString(outputStream, "GET_VALUE_FROM_CLIENT");
		ProtocolPrimitives.sendInt(outputStream, clientId);
		ProtocolPrimitives.sendString(outputStream, key);
		outputStream.flush();
		byte[] result = ProtocolPrimitives.receiveByteArray(inputStream);
		if (ProtocolPrimitives.DEBUG)
			System.out.println("byte[] client_GET_VALUE_FROM_CLIENT: result: " +Util.md5(result));
		return result;
	}
	
	
	public boolean GET_VALUE_FROM_ALL_MIXES(OutputStream outputStream, InputStream inputStream) throws Exception {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: GET_VALUE_FROM_ALL_MIXES()");
		String key = ProtocolPrimitives.receiveString(inputStream);
		ProtocolPrimitives.sendInt(outputStream, NUMBER_OF_MIXES);
		outputStream.flush();
		boolean success = true;
		for (int i=0; i<NUMBER_OF_MIXES; i++) {
			byte[] data;
			synchronized (mixData) {
				HashMap<String, byte[]> recordForMix = mixData.get(i);
				if (recordForMix == null || (data = recordForMix.get(key)) == null) {
					success = false;
					throw new RuntimeException(key +"; "+i); 
				}
			}
			ProtocolPrimitives.sendByteArray(outputStream, data);
		}
		outputStream.flush();
		return success;
	}
		
	
	public static byte[][] client_GET_VALUE_FROM_ALL_MIXES(OutputStream outputStream, InputStream inputStream, String key) throws UnsupportedEncodingException, IOException {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("byte[] client_GET_VALUE_FROM_ALL_MIXES("+key +")");
		ProtocolPrimitives.sendString(outputStream, "GET_VALUE_FROM_ALL_MIXES");
		ProtocolPrimitives.sendString(outputStream, key);
		outputStream.flush();
		int numberOfMixes = ProtocolPrimitives.receiveInt(inputStream);
		byte[][] result = new byte[numberOfMixes][];
		for (int i=0; i<numberOfMixes; i++) {
			result[i] = ProtocolPrimitives.receiveByteArray(inputStream);
		}
		return result;
	}
	
	
	public void GET_MIX_LIST(OutputStream outputStream, InputStream inputStream) throws Exception {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: GET_MIX_LIST()");
		synchronized (mixData) {
			ProtocolPrimitives.sendInt(outputStream, NUMBER_OF_MIXES);
			for (int i=0; i<NUMBER_OF_MIXES; i++) { // data for each mix
				ProtocolPrimitives.sendInt(outputStream, i); // mixid
				ProtocolPrimitives.sendByteArray(outputStream, mixData.get(i).get("ADDRESS")); // address
				ProtocolPrimitives.sendInt(outputStream, Util.byteArrayToInt(mixData.get(i).get("PORT"))); // port
			}
		}
		outputStream.flush();
	}
	

	public static MixList client_GET_MIX_LIST(OutputStream outputStream, InputStream inputStream) throws UnsupportedEncodingException, IOException {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("byte[] GET_MIX_LIST()");
		ProtocolPrimitives.sendString(outputStream, "GET_MIX_LIST");
		outputStream.flush();
		MixList mixList = new MixList();
		mixList.numberOfMixes = ProtocolPrimitives.receiveInt(inputStream);
		mixList.mixIDs = new int[mixList.numberOfMixes];
		mixList.addresses = new InetAddress[mixList.numberOfMixes];
		mixList.ports = new int[mixList.numberOfMixes];
		for (int i=0; i<mixList.numberOfMixes; i++) {
			mixList.mixIDs[i] = ProtocolPrimitives.receiveInt(inputStream);
			mixList.addresses[i] = InetAddress.getByAddress(ProtocolPrimitives.receiveByteArray(inputStream));
			mixList.ports[i] = ProtocolPrimitives.receiveInt(inputStream);
		}
		return mixList;
	}
	
	
	public static void client_WAIT_FOR_END_OF_ADDRESS_EXCHANGE_PHASE(OutputStream outputStream, InputStream inputStream) throws UnsupportedEncodingException, IOException {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("void client_WAIT_FOR_END_OF_ADDRESS_EXCHANGE_PHASE()");
		ProtocolPrimitives.sendString(outputStream, "WAIT_FOR_END_OF_ADDRESS_EXCHANGE_PHASE");
		String result = ProtocolPrimitives.receiveString(inputStream);
		if (!result.equals("ADDRESS_EXCHANGE_PHASE_DONE"))
			throw new RuntimeException("received wrong result!");
	}
	
	
	public void WAIT_FOR_END_OF_ADDRESS_EXCHANGE_PHASE(OutputStream outputStream, InputStream inputStream) throws Exception {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: WAIT_FOR_END_OF_ADDRESS_EXCHANGE_PHASE()");
		synchronized(addressExchangePhaseFinished) {
			addressesReceived++;
			if (addressesReceived >= NUMBER_OF_MIXES) {
				System.out.println("InfoService: address exchange phase finished"); 
				addressExchangePhaseFinished.set(true);
				addressExchangePhaseFinished.notifyAll();
				/*synchronized (eventListeners) { // TODO
					for (EipEventListener eipel:eventListeners)
						eipel.registrationPhaseFinished();
				}*/
				
			} else {
				while (addressExchangePhaseFinished.get() == false) {
					try { 
						addressExchangePhaseFinished.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
						continue;	
					}	
				}
			}
		}
		ProtocolPrimitives.sendString(outputStream, "ADDRESS_EXCHANGE_PHASE_DONE");
		outputStream.flush();
	}
	
	
	public void WAIT_FOR_END_OF_REGISTRATION_PHASE(OutputStream outputStream, InputStream inputStream) throws Exception {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: WAIT_FOR_END_OF_REGISTRATION_PHASE()");
		synchronized(registrationPhaseFinished) {
			registeredMixes++;
			if (registeredMixes >= NUMBER_OF_MIXES) {
				System.out.println("InfoService: registration phase finished"); 
				registrationPhaseFinished.set(true);
				registrationPhaseFinished.notifyAll();
				/*synchronized (eventListeners) { // TODO
					for (EipEventListener eipel:eventListeners)
						eipel.registrationPhaseFinished();
				}*/
				
			} else {
				while (registrationPhaseFinished.get() == false) {
					try { 
						registrationPhaseFinished.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
						continue;	
					}	
				}
			}
		}
		ProtocolPrimitives.sendString(outputStream, "REGISTRATION_PHASE_DONE");
		outputStream.flush();
	}
	
	
	public static void client_WAIT_FOR_END_OF_REGISTRATION_PHASE(OutputStream outputStream, InputStream inputStream) throws UnsupportedEncodingException, IOException {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("void client_WAIT_FOR_END_OF_REGISTRATION_PHASE()");
		ProtocolPrimitives.sendString(outputStream, "WAIT_FOR_END_OF_REGISTRATION_PHASE");
		String result = ProtocolPrimitives.receiveString(inputStream);
		if (!result.equals("REGISTRATION_PHASE_DONE"))
			throw new RuntimeException("received wrong result!");
	}
	
	
	public void WAIT_FOR_END_OF_INITIALIZATION_PHASE(OutputStream outputStream, InputStream inputStream) throws Exception {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: WAIT_FOR_END_OF_INITIALIZATION_PHASE()");
		synchronized(initPhaseFinished) {
			initializedMixes++;
			if (initializedMixes >= NUMBER_OF_MIXES) {
				System.out.println("InfoService: initialization phase finished"); 
				initPhaseFinished.set(true);
				initPhaseFinished.notifyAll();
				/*synchronized (eventListeners) { // TODO
					for (EipEventListener eipel:eventListeners)
						eipel.registrationPhaseFinished();
				}*/
				
			} else {
				while (this.initPhaseFinished.get() == false) {
					try { 
						initPhaseFinished.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
						continue;	
					}	
				}
			}
		}
		ProtocolPrimitives.sendString(outputStream, "INITIALIZATION_PHASE_DONE");
		outputStream.flush();
	}
	
	
	public static void client_WAIT_FOR_END_OF_INITIALIZATION_PHASE(OutputStream outputStream, InputStream inputStream) throws UnsupportedEncodingException, IOException {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("void client_WAIT_FOR_END_OF_INITIALIZATION_PHASE()");
		ProtocolPrimitives.sendString(outputStream, "WAIT_FOR_END_OF_INITIALIZATION_PHASE");
		String result = ProtocolPrimitives.receiveString(inputStream);
		if (!result.equals("INITIALIZATION_PHASE_DONE"))
			throw new RuntimeException("received wrong result!");
	}
	
	
	public void WAIT_FOR_END_OF_BEGIN_PHASE(OutputStream outputStream, InputStream inputStream) throws Exception {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: WAIT_FOR_END_OF_BEGIN_PHASE()");
		synchronized(beginPhaseFinished) {
			readyMixes++;
			if (readyMixes >= NUMBER_OF_MIXES) {
				System.out.println("InfoService: all mixes are up (begin phase finished)"); 
				beginPhaseFinished.set(true);
				beginPhaseFinished.notifyAll();
				/*synchronized (eventListeners) { // TODO
					for (EipEventListener eipel:eventListeners)
						eipel.registrationPhaseFinished();
				}*/
				
			} else {
				while (beginPhaseFinished.get() == false) {
					try {
						beginPhaseFinished.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
						continue;	
					}	
				}
			}
		}
		
		ProtocolPrimitives.sendString(outputStream, "BEGIN_PHASE_DONE");
		outputStream.flush();
	}
	
	
	public static void client_WAIT_FOR_END_OF_BEGIN_PHASE(OutputStream outputStream, InputStream inputStream) throws UnsupportedEncodingException, IOException {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("void client_WAIT_FOR_END_OF_BEGIN_PHASE()");
		ProtocolPrimitives.sendString(outputStream, "WAIT_FOR_END_OF_BEGIN_PHASE");
		String result = ProtocolPrimitives.receiveString(inputStream);
		if (!result.equals("BEGIN_PHASE_DONE"))
			throw new RuntimeException("received wrong result!");
	}
	
	
	public void WAIT_TILL_MIXES_ARE_UP(OutputStream outputStream, InputStream inputStream) throws Exception {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: WAIT_TILL_MIXES_ARE_UP()");
		synchronized(beginPhaseFinished) {
			while (beginPhaseFinished.get() == false) {
				try {
					beginPhaseFinished.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;	
				}	
			}
		}
		
		ProtocolPrimitives.sendString(outputStream, "MIXES_ARE_UP");
		outputStream.flush();
	}
	
	
	public static void client_WAIT_TILL_MIXES_ARE_UP(OutputStream outputStream, InputStream inputStream) throws UnsupportedEncodingException, IOException {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("void client_WAIT_TILL_MIXES_ARE_UP()");
		ProtocolPrimitives.sendString(outputStream, "WAIT_TILL_MIXES_ARE_UP");
		String result = ProtocolPrimitives.receiveString(inputStream);
		if (!result.equals("MIXES_ARE_UP"))
			throw new RuntimeException("received wrong result!");
	}
	
	
	public void localWaitTillMixesAreUp() throws IOException {
		if (ProtocolPrimitives.DEBUG)
			System.out.println("server: localWaitTillMixesAreUp()");
		synchronized(beginPhaseFinished) {
			while (beginPhaseFinished.get() == false) {
				try {
					beginPhaseFinished.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;	
				}	
			}
		}
	}
}
