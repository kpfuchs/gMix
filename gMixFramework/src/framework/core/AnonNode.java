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
package framework.core;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;

import staticFunctions.layer5application.statisticsRecorder_v0_001.StatisticsRecorder;

import framework.core.clock.Clock;
import framework.core.config.Settings;
import framework.core.controller.Controller;
import framework.core.controller.Layer1NetworkClientController;
import framework.core.controller.Layer1NetworkMixController;
import framework.core.controller.Layer2RecodingSchemeClientController;
import framework.core.controller.Layer2RecodingSchemeMixController;
import framework.core.controller.Layer3OutputStrategyClientController;
import framework.core.controller.Layer3OutputStrategyMixController;
import framework.core.controller.Layer4TransportClientController;
import framework.core.controller.Layer4TransportMixController;
import framework.core.controller.Layer5ApplicationClientController;
import framework.core.controller.Layer5ApplicationMixController;
import framework.core.launcher.CommandLineParameters;
import framework.core.launcher.GMixTool;
import framework.core.launcher.ToolName;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.routing.MixList;
import framework.core.routing.RoutingMode;
import framework.core.socket.connectedDatagram.ConnectedDatagramAnonServerSocketImpl;
import framework.core.socket.connectedDatagram.ConnectedDatagramAnonSocketClientImpl;
import framework.core.socket.datagram.DatagramAnonServerSocketImpl;
import framework.core.socket.datagram.DatagramAnonSocketClientImpl;
import framework.core.socket.socketInterfaces.AdaptiveAnonServerSocket;
import framework.core.socket.socketInterfaces.AnonServerSocket;
import framework.core.socket.socketInterfaces.ConnectedDatagramAnonServerSocket;
import framework.core.socket.socketInterfaces.ConnectedDatagramAnonSocket;
import framework.core.socket.socketInterfaces.DatagramAnonServerSocket;
import framework.core.socket.socketInterfaces.DatagramAnonSocket;
import framework.core.socket.socketInterfaces.StreamAnonServerSocket;
import framework.core.socket.socketInterfaces.StreamAnonSocket;
import framework.core.socket.socketInterfaces.AnonSocketOptions.CommunicationMode;
import framework.core.socket.stream.StreamAnonServerSocketImpl;
import framework.core.socket.stream.StreamAnonSocketClientImpl;
import framework.core.userDatabase.UserDatabase;
import framework.core.util.Util;
import framework.infoService.InfoServiceClient;


public class AnonNode extends GMixTool {

	// helper components
	protected Settings settings;
	protected UserDatabase userDatabase;
	protected Clock clock;
	protected InfoServiceClient infoService;
	
	// references to controllers
	protected Layer1NetworkMixController networkLayerMix;
	protected Layer1NetworkClientController networkLayerClient;
	protected Layer2RecodingSchemeMixController recodingLayerMix;
	protected Layer2RecodingSchemeClientController recodingLayerClient;
	protected Layer3OutputStrategyMixController outputStrategyLayerMix;
	protected Layer3OutputStrategyClientController outputStrategyLayerClient;
	protected Layer4TransportMixController transportLayerMix;
	protected Layer4TransportClientController transportLayerClient;
	protected Layer5ApplicationMixController applicationLayerMix;
	protected Layer5ApplicationClientController applicationLayerClient;
	
	// some settings
	public int PUBLIC_PSEUDONYM; // this anon node will be registered under this pseudonym at the info-service; the pseudonym can be used by other anon nodes for addressing
	public boolean IS_CLIENT;
	public boolean IS_MIX;
	public boolean IS_P2P;
	public boolean IS_LAST_MIX; // exit node
	public boolean IS_FIRST_MIX;
	public boolean RECORD_STATISTICS_ON;
	public boolean DISPLAY_ROUTE_INFO;
	public boolean DEBUG_MODE_ON;
	public boolean LOCAL_MODE_ON;
	public boolean REPLY_DETECTION_ON;
	public boolean RS_DEBUG_OUTPUT_ON;
	public RoutingMode ROUTING_MODE;
	public int NUMBER_OF_MIXES;
	public int FREE_ROUTE_LENGTH;
	public int EXPECTED_NUMBER_OF_USERS;
	public String CRYPTO_PROVIDER;
	public int NUMBER_OF_THREADS;
	public int MAX_PAYLOAD;
	public boolean LAYER_1_LINKS_MESSAGES; // when true, layer one will tag messages of the same user with a pseudonym (this is required by some plug-ins; e.g. some recoding schemes will transmit symmetrically encrypted messages after an initial hybridly encrypted message; the recoder plug-in needs the pseudonym to choose the right decryption key in that example)
	public int QUEUE_BLOCK_SIZE;
	public int SOCKET_MIX_BACKEND_QUEUE_SIZE;
	public int SERVER_SOCKET_QUEUE_SIZE;
	public int SERVER_SOCKET_BACKLOG;
	public int TIME_TO_WAIT_FOR_FURTHER_DATA;
	public String LAYER_4_CLIENT_INPUT_STREAM_REPLY_BUFFER_SIZE;
	public String LAYER_4_MIX_INPUT_STREAM_REQUEST_BUFFER_SIZE;
	
	// socket options
	public boolean IS_DUPLEX;
	public boolean IS_RELIABLE;
	public boolean IS_CONNECTION_BASED;
	public boolean IS_ORDER_PRESERVING;
	
	// other
	private Vector<Controller> components = new Vector<Controller>(); // instantiated controllers
	private HashMap<Integer, AdaptiveAnonServerSocket> sockets;
	private ArrayBlockingQueue<Request[]> requestInputQueue;
	private ArrayBlockingQueue<Request[]> requestOutputQueue;
	private ArrayBlockingQueue<Reply[]> replyInputQueue;
	private ArrayBlockingQueue<Reply[]> replyOutputQueue;
	public MixList mixList;
	public ToolName gMixTool;
	
	
	public AnonNode(CommandLineParameters commandLineParameters) {
		System.out.println("AnonNode initializing... (" +commandLineParameters.gMixTool +")");
		
		initAndRegisterAtInfoService(commandLineParameters);
		generateComponents();
		setReferencesBetweenComponents();
		loadImplementations(); // loads the plug-ins
		
		if (IS_MIX)
			infoService.waitForEndOfAddressExchangePhase();
		
		this.mixList = infoService.getMixList();
		if (DISPLAY_ROUTE_INFO && ROUTING_MODE != RoutingMode.CASCADE)
			System.out.println("" +this +" received " +mixList); 
		
		callConstructors();
		if (IS_MIX)
			infoService.waitForEndOfRegistrationPhase();
		
		initializeComponents(); // calls "initialize()" for each implementation
		if (IS_MIX)
			infoService.waitForEndOfInitializationPhase();
		else
			infoService.waitTillMixesAreUp();
		
		// TODO write config to disk
		
		beginMixing(); // calls "begin()" for each implementation
		
		if (IS_MIX)
			infoService.waitForEndOfBeginPhase();
		
		System.out.println("AnonNode up (" +commandLineParameters.gMixTool +": " +this.PUBLIC_PSEUDONYM +")");
	}

	
	private void callConstructors() {
		for (Controller c:components)
			if (c.implementation != null)
				c.implementation.constructor();
	}
	
	
	private void loadImplementations() {
		for (Controller c:components)
			c.instantiateSubclass();
	}
	
	
	private void initializeComponents() {
		for (Controller c:components)
			c.initialize();
	}
	
	
	private void beginMixing() {
		for (Controller c:components)
			c.begin(); 
	}
	
	
	private void initAndRegisterAtInfoService(CommandLineParameters commandLineParameters) {
		
		this.settings = commandLineParameters.generateSettingsObject();
		this.clock = new Clock(settings);
		this.userDatabase = new UserDatabase();
		Util.checkIfBCIsInstalled();
		this.gMixTool = commandLineParameters.gMixTool;
		
		this.infoService = new InfoServiceClient(settings.getPropertyAsInetAddress("GLOBAL_INFO_SERVICE_ADDRESS"), settings.getPropertyAsInt("GLOBAL_INFO_SERVICE_PORT"));
		
		if (commandLineParameters.gMixTool == ToolName.MIX || commandLineParameters.gMixTool == ToolName.P2P)
			this.PUBLIC_PSEUDONYM = infoService.registerAsMix();
		else
			this.PUBLIC_PSEUDONYM = infoService.registerAsClient();
		
		this.LOCAL_MODE_ON = infoService.getIsLocalModeOn();
		if (!LOCAL_MODE_ON) {
			System.err.println("WARNING: LOCAL_MODE is set to FALSE; Mixes and Information Service will be available via Network; Only safe for trusted networks!"); 
			if (settings.getProperty("GLOBAL_MIX_BIND_ADDRESS").equalsIgnoreCase("AUTO"))
				try {
					settings.setProperty("GLOBAL_MIX_BIND_ADDRESS", InetAddress.getLocalHost().getHostAddress());
				} catch (UnknownHostException e) {
					e.printStackTrace();
					throw new RuntimeException("could not detect ip address!"); 
				}
		} else {
			settings.setProperty("GLOBAL_MIX_BIND_ADDRESS", "localhost");
		}
		
		if (settings.getProperty("GLOBAL_MIX_BIND_PORT").equalsIgnoreCase("AUTO")) {
			int port = infoService.getSuggestedPort();
			settings.setProperty("GLOBAL_MIX_BIND_PORT", ""+port);
		}
		
		this.IS_DUPLEX = settings.getPropertyAsBoolean("GLOBAL_IS_DUPLEX");
		this.IS_RELIABLE = settings.getPropertyAsBoolean("GLOBAL_IS_RELIABLE");
		this.IS_CONNECTION_BASED = settings.getPropertyAsBoolean("GLOBAL_IS_CONNECTION_BASED");
		this.IS_ORDER_PRESERVING = settings.getPropertyAsBoolean("GLOBAL_IS_ORDER_PRESERVING");
		this.REPLY_DETECTION_ON = settings.getPropertyAsBoolean("GLOBAL_REPLY_DETECTION_ON");
		this.RS_DEBUG_OUTPUT_ON = settings.getPropertyAsBoolean("GLOBAL_RS_DEBUG_OUTPUT_ON");
		this.ROUTING_MODE = RoutingMode.getMode(settings);
		this.EXPECTED_NUMBER_OF_USERS = settings.getPropertyAsInt("GLOBAL_EXPECTED_NUMBER_OF_USERS");
		this.CRYPTO_PROVIDER = settings.getProperty("GLOBAL_CRYPTO_PROVIDER");
		this.NUMBER_OF_THREADS = settings.getPropertyAsInt("GLOBAL_NUMBER_OF_THREADS");
		this.MAX_PAYLOAD = settings.getPropertyAsInt("GLOBAL_MAX_PAYLOAD");
		this.QUEUE_BLOCK_SIZE = settings.getPropertyAsInt("GLOBAL_QUEUE_BLOCK_SIZE");
		this.RECORD_STATISTICS_ON = settings.getPropertyAsBoolean("GLOBAL_RECORD_STATISTICS_ON");
		MixMessage.recordStatistics = RECORD_STATISTICS_ON;
		this.DISPLAY_ROUTE_INFO = settings.getPropertyAsBoolean("GLOBAL_DISPLAY_ROUTE_INFO");
		this.LAYER_1_LINKS_MESSAGES = settings.getPropertyAsBoolean("GLOBAL_LAYER_1_LINKS_MESSAGES");
		this.SOCKET_MIX_BACKEND_QUEUE_SIZE = settings.getPropertyAsInt("GLOBAL_SOCKET_MIX_BACKEND_QUEUE_SIZE");
		this.SERVER_SOCKET_QUEUE_SIZE = settings.getPropertyAsInt("GLOBAL_SERVER_SOCKET_QUEUE_SIZE");
		this.SERVER_SOCKET_BACKLOG = settings.getPropertyAsInt("GLOBAL_SERVER_SOCKET_BACKLOG");
		this.TIME_TO_WAIT_FOR_FURTHER_DATA = settings.getPropertyAsInt("GLOBAL_TIME_TO_WAIT_FOR_FURTHER_DATA_IN_MICROSEC");
		this.LAYER_4_CLIENT_INPUT_STREAM_REPLY_BUFFER_SIZE = settings.getProperty("GLOBAL_LAYER_4_CLIENT_INPUT_STREAM_REPLY_BUFFER_SIZE");
		this.LAYER_4_MIX_INPUT_STREAM_REQUEST_BUFFER_SIZE = settings.getProperty("GLOBAL_LAYER_4_MIX_INPUT_STREAM_REQUEST_BUFFER_SIZE");

		this.DEBUG_MODE_ON = infoService.getIsDuplexModeOn();
		this.NUMBER_OF_MIXES = infoService.getNumberOfMixes();
		this.FREE_ROUTE_LENGTH = settings.getPropertyAsInt("GLOBAL_FREE_ROUTE_LENGTH");
		if (ROUTING_MODE != RoutingMode.CASCADE && FREE_ROUTE_LENGTH > NUMBER_OF_MIXES)
			throw new RuntimeException("FREE_ROUTE_LENGTH > NUMBER_OF_MIXES!");
		if (ROUTING_MODE != RoutingMode.CASCADE && NUMBER_OF_MIXES == 1)
			throw new RuntimeException("free route mode requires at least two mixes");
			
		if (commandLineParameters.gMixTool == ToolName.CLIENT) {
			this.IS_CLIENT = true;
		} else if (commandLineParameters.gMixTool == ToolName.MIX) {
			this.IS_MIX = true;
			if (PUBLIC_PSEUDONYM == 0)	// TODO
				this.IS_FIRST_MIX = true;
			if (PUBLIC_PSEUDONYM == NUMBER_OF_MIXES-1)
				this.IS_LAST_MIX = true;
		} else if (commandLineParameters.gMixTool == ToolName.P2P) {
			this.IS_MIX = true;
			this.IS_CLIENT = true;
			this.IS_P2P = true;
			if (PUBLIC_PSEUDONYM == 0)	// TODO
				this.IS_FIRST_MIX = true;
			if (PUBLIC_PSEUDONYM == NUMBER_OF_MIXES-1)
				this.IS_LAST_MIX = true;
		} else {
			throw new RuntimeException("not supported tool type specified: " +commandLineParameters.gMixTool); 
		}

		if (IS_MIX) {
			infoService.postValueAsMix(PUBLIC_PSEUDONYM, "ADDRESS", settings.getPropertyAsInetAddress("GLOBAL_MIX_BIND_ADDRESS").getAddress());
			infoService.postValueAsMix(PUBLIC_PSEUDONYM, "PORT", Util.intToByteArray(settings.getPropertyAsInt("GLOBAL_MIX_BIND_PORT")));
			this.sockets = new HashMap<Integer, AdaptiveAnonServerSocket>();
			this.requestInputQueue = new ArrayBlockingQueue<Request[]>(settings.getPropertyAsInt("GLOBAL_REQUEST_INPUT_QUEUE_SIZE"));
			this.requestOutputQueue = new ArrayBlockingQueue<Request[]>(settings.getPropertyAsInt("GLOBAL_REQUEST_OUTPUT_QUEUE_SIZE"));
			this.replyInputQueue = new ArrayBlockingQueue<Reply[]>(settings.getPropertyAsInt("GLOBAL_REPLY_INPUT_QUEUE_SIZE"));
			this.replyOutputQueue = new ArrayBlockingQueue<Reply[]>(settings.getPropertyAsInt("GLOBAL_REPLY_OUTPUT_QUEUE_SIZE"));
		}
		
		if (RECORD_STATISTICS_ON && IS_MIX && IS_LAST_MIX)
			StatisticsRecorder.init(this);
	}
	
	
	private void setReferencesBetweenComponents() {
		for (Controller c:components)
			c.setComponentReferences(networkLayerMix, networkLayerClient, recodingLayerMix, recodingLayerClient, outputStrategyLayerMix, outputStrategyLayerClient, transportLayerMix, transportLayerClient, applicationLayerMix, applicationLayerClient);
	}
	
	
	private void generateComponents() {
		// NOTE: the order of the items is important (do not change it)
		if (IS_MIX) {
			this.outputStrategyLayerMix = new Layer3OutputStrategyMixController(this, settings, userDatabase, clock, infoService);
			this.recodingLayerMix = new Layer2RecodingSchemeMixController(this, settings, userDatabase, clock, infoService);
			this.networkLayerMix = new Layer1NetworkMixController(this, settings, userDatabase, clock, infoService);
			this.transportLayerMix = new Layer4TransportMixController(this, settings, userDatabase, clock, infoService);
			this.applicationLayerMix = new Layer5ApplicationMixController(this, settings, userDatabase, clock, infoService);
		}
		if (IS_CLIENT) { // the lower layer client plug-ins are loaded at runtime when a socket is created (directly or through layer 5 plug-ins)
			this.outputStrategyLayerClient = new Layer3OutputStrategyClientController(this, settings, userDatabase, clock, infoService);
			this.recodingLayerClient = new Layer2RecodingSchemeClientController(this, settings, userDatabase, clock, infoService);
			this.networkLayerClient = new Layer1NetworkClientController(this, settings, userDatabase, clock, infoService);
			this.transportLayerClient = new Layer4TransportClientController(this, settings, userDatabase, clock, infoService);
			this.applicationLayerClient = new Layer5ApplicationClientController(this, settings, userDatabase, clock, infoService);
		}
	}
	
	
	public void registerComponent(Controller controllerToRegister) {
		components.add(controllerToRegister);
	}
	
	
	public StreamAnonServerSocket createStreamAnonServerSocket(
			int bindPort,
			CommunicationMode communicationMode,
			boolean isFreeRoute
			) {
		
		if (!IS_MIX)
			throw new RuntimeException("only mix and p2p anon nodes can create server sockets"); 
		if (sockets.get(bindPort) != null)
			throw new RuntimeException("this port is already in use");
		
		return new StreamAnonServerSocketImpl(
				this,
				PUBLIC_PSEUDONYM, 
				bindPort,
				communicationMode,
				isFreeRoute
				);
		
	}

	
	public DatagramAnonServerSocket createDatagramServerSocket(
			int bindPort, 
			CommunicationMode communicationMode,
			boolean isReliable, 
			boolean isOrderPreserving, 
			boolean isFreeRoute
			) {
		
		if (!IS_MIX)
			throw new RuntimeException("only mix and p2p anon nodes can create server sockets"); 
		if (sockets.get(bindPort) != null)
			throw new RuntimeException("this port is already in use");
		
		return new DatagramAnonServerSocketImpl(
				this,
				PUBLIC_PSEUDONYM,
				bindPort,
				communicationMode,
				isReliable,
				isOrderPreserving,
				isFreeRoute
				);
	}


	public ConnectedDatagramAnonServerSocket createConnectedDatagramServerSocket(
			int bindPort, 
			CommunicationMode communicationMode,
			boolean isReliable, 
			boolean isOrderPreserving, 
			boolean isFreeRoute
			) {
		
		if (!IS_MIX)
			throw new RuntimeException("only mix and p2p anon nodes can create server sockets"); 
		if (sockets.get(bindPort) != null)
			throw new RuntimeException("this port is already in use");
		
		return new ConnectedDatagramAnonServerSocketImpl(	
				this,
				PUBLIC_PSEUDONYM, 
				bindPort,
				communicationMode,
				isReliable,
				isOrderPreserving,
				isFreeRoute
				);
	}
	
	
	public void registerServerSocket(AdaptiveAnonServerSocket serverSocket) {
		if (sockets.get(serverSocket.getBindPort()) != null)
			throw new RuntimeException("this port is already in use: " +serverSocket.getBindPort()); 
		sockets.put(serverSocket.getBindPort(), serverSocket);
	}
	
	
	// return null if no socket available
	public AnonServerSocket getServerSocket(int port) {
		if (!IS_MIX)
			throw new RuntimeException("only mixes can open server sockets");
		return sockets.get(port);
	}

	// return false if no socket found
	public boolean removeSocket(int port) {
		return sockets.remove(port) != null;
	}
	
	
	public DatagramAnonSocket createDatagramSocket(
			CommunicationMode communicationMode,
			boolean isReliable, 
			boolean isOrderPreserving, 
			boolean isFreeRoute
			) {
		if (!IS_CLIENT)
			throw new RuntimeException("only clients/p2p mixes can open none-server sockets");
		return new DatagramAnonSocketClientImpl(
				this,
				communicationMode,
				isReliable,
				isOrderPreserving,
				isFreeRoute
				);
	}

	
	public StreamAnonSocket createStreamSocket(
			CommunicationMode communicationMode,
			boolean isFreeRoute
			) {
		if (!IS_CLIENT)
			throw new RuntimeException("only clients/p2p mixes can open none-server sockets");
		return new StreamAnonSocketClientImpl(	
				this,
				communicationMode,
				isFreeRoute
				);
	}
	
	
	public ConnectedDatagramAnonSocket createConnectedDatagramSocket(
			CommunicationMode communicationMode,
			boolean isReliable, 
			boolean isOrderPreserving, 
			boolean isFreeRoute
			) {
		return new ConnectedDatagramAnonSocketClientImpl(	
				this,
				communicationMode,
				isReliable,
				isOrderPreserving,
				isFreeRoute
				);
	}

	
	// called by layer 3 (output strategy) mix plug-ins
	public void putOutRequest(Request request) {
		if (!isFinalHop(request)) { // put request in output queue, from where it will be sent to the next hop (via layer 1)
			putInRequestOutputQueue(request);
		} else { // final hop: find desired socket and forward request to it
			if (DISPLAY_ROUTE_INFO && ROUTING_MODE != RoutingMode.CASCADE)
				System.out.println("" +this +": i'm the final hop");
			AdaptiveAnonServerSocket destSocket = null;
			int dstPort = 0;
			if (request.getByteMessage().length != 0) {// if not a dummy
				dstPort = Util.byteArrayToShort(Arrays.copyOf(request.getByteMessage(), 2)); // get desired socket
				request.setByteMessage(Arrays.copyOfRange(request.getByteMessage(), 2, request.getByteMessage().length));
				destSocket = sockets.get(dstPort);
				if (destSocket == null) {
					System.err.println("received message for unknown port: " +dstPort);
					return;
				} else {
					destSocket.incomingRequest(request);
				}
			}
			if (this.RECORD_STATISTICS_ON) {
				StatisticsRecorder.addMessageDwellTimeRecord(request);
				StatisticsRecorder.addRequestThroughputRecord(request.getByteMessage().length, request.getOwner());
			}
		}
	}
	
	
	public void putOutRequests(Request[] requests) {
		if (ROUTING_MODE == RoutingMode.CASCADE) {
			if (!IS_LAST_MIX) // put data in request output queue, from where it will be forwarded to the next mix via the layer 1 plug-in
				putInRequestOutputQueue(requests);
			else // forward data to the responsive socket
				for (Request request:requests)
					putOutRequest(request);	
		} else {
			for (Request request:requests)
				putOutRequest(request);
		}
	}
	
	
	private boolean isFinalHop(Request request) {
		if (ROUTING_MODE == RoutingMode.FREE_ROUTE_DYNAMIC_ROUTING) {
			return request.nextHopAddress == MixMessage.NONE ? true : false; 
		} else if (ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) {
			return request.nextHopAddress == MixMessage.NONE ? true : false; 
		} else { // fixed route
			return this.IS_LAST_MIX;
		}
	}
	
	
	public void putOutReply(Reply reply) {
		this.putInReplyOutputQueue(reply);
	}

	
	public void putOutReplies(Reply[] replies) {
		this.putInReplyOutputQueue(replies);
	}
	
	
	public ArrayBlockingQueue<Request[]> getRequestInputQueue() {
		return this.requestInputQueue;
	}
	
	
	public ArrayBlockingQueue<Request[]> getRequestOutputQueue() {
		return this.requestOutputQueue;
	}
	
	
	public ArrayBlockingQueue<Reply[]> getReplyInputQueue() {
		return this.replyInputQueue;
	}
	
	
	public ArrayBlockingQueue<Reply[]> getReplyOutputQueue() {
		return this.replyOutputQueue;
	}
	
	
	public Request[] getFromRequestInputQueue() {
		try {
			return requestInputQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return getFromRequestInputQueue();
		}
	}
	
	
	public Request[] getFromRequestOutputQueue() {
		try {
			return requestOutputQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return getFromRequestOutputQueue();
		}
	}
	
	
	public Reply[] getFromReplyInputQueue() {
		try {
			return replyInputQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return getFromReplyInputQueue();
		}
	}
	
	
	public Reply[] getFromReplyOutputQueue() {
		try {
			return replyOutputQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return getFromReplyOutputQueue();
		}
	}
	
	
	public void putInRequestInputQueue(Request[] requests) {
		assert requests != null;
		assert requests.length != 0;
		if (requests.length > QUEUE_BLOCK_SIZE) {
			Request[][] splitted = Util.splitInChunks(QUEUE_BLOCK_SIZE, requests);
			for (Request[] block:splitted) {
				try {
					requestInputQueue.put(block);
				} catch (InterruptedException e) {
					e.printStackTrace();
					putInRequestInputQueue(block);
				}
			}
		} else {
			try {
				requestInputQueue.put(requests);
			} catch (InterruptedException e) {
				e.printStackTrace();
				putInRequestInputQueue(requests);
			}
		}
	}
	
	
	// 
	public void putInRequestInputQueue(Request request) {
		assert request != null;
		try {
			requestInputQueue.put(new Request[]{request});
		} catch (InterruptedException e) {
			e.printStackTrace();
			putInRequestInputQueue(request);
		}
	}
	
	
	public void putInRequestOutputQueue(Request request) {
		assert request != null;
		try {
			requestOutputQueue.put(new Request[]{request});
		} catch (InterruptedException e) {
			e.printStackTrace();
			putInRequestOutputQueue(request);
		}
	}
	
	
	public void putInRequestOutputQueue(Request[] requests) {
		assert requests != null;
		assert requests.length != 0;
		if (requests.length > QUEUE_BLOCK_SIZE) {
			Request[][] splitted = Util.splitInChunks(QUEUE_BLOCK_SIZE, requests);
			for (Request[] block:splitted) {
				try {
					requestOutputQueue.put(block);
				} catch (InterruptedException e) {
					e.printStackTrace();
					putInRequestOutputQueue(block);
				}
			}
		}
		try {
			requestOutputQueue.put(requests);
		} catch (InterruptedException e) {
			e.printStackTrace();
			putInRequestOutputQueue(requests);
		}
	}
	
	
	public void putInReplyInputQueue(Reply reply) {
		assert reply != null;
		try {
			replyInputQueue.put(new Reply[]{reply});
		} catch (InterruptedException e) {
			e.printStackTrace();
			putInReplyInputQueue(reply);
		}
		if (this.IS_LAST_MIX && this.RECORD_STATISTICS_ON)
			StatisticsRecorder.addReplyThroughputRecord(reply.getByteMessage().length, reply.getOwner());
	}
	
	
	public void putInReplyInputQueue(Reply[] replies) {
		assert replies != null;
		assert replies.length != 0;
		if (replies.length > QUEUE_BLOCK_SIZE) {
			Reply[][] splitted = Util.splitInChunks(QUEUE_BLOCK_SIZE, replies);
			for (Reply[] block:splitted) {
				try {
					replyInputQueue.put(block);
				} catch (InterruptedException e) {
					e.printStackTrace();
					putInReplyInputQueue(block);
				}
			}
		} else {
			try {
				replyInputQueue.put(replies);
			} catch (InterruptedException e) {
				e.printStackTrace();
				putInReplyInputQueue(replies);
			}
		}
		//if (this.RECORD_STATISTICS_ON)
		//	for (Reply reply:replies)
		//		StatisticsRecorder.addReplyThroughputRecord(reply.getByteMessage().length, reply.getOwner());
	}
	
	
	public void putInReplyOutputQueue(Reply reply) {
		assert reply != null;
		try {
			replyOutputQueue.put(new Reply[]{reply});
		} catch (InterruptedException e) {
			e.printStackTrace();
			putInReplyOutputQueue(reply);
		}
	}

	
	public void putInReplyOutputQueue(Reply[] replies) {
		assert replies != null;
		assert replies.length != 0;
		if (replies.length > QUEUE_BLOCK_SIZE) {
			Reply[][] splitted = Util.splitInChunks(QUEUE_BLOCK_SIZE, replies);
			for (Reply[] block:splitted) {
				try {
					replyOutputQueue.put(block);
				} catch (InterruptedException e) {
					e.printStackTrace();
					putInReplyOutputQueue(block);
				}
			}
		} else {
			try {
				replyOutputQueue.put(replies);
			} catch (InterruptedException e) {
				e.printStackTrace();
				putInReplyOutputQueue(replies);
			}
		}
	}
	

	public Settings getSettings() {
		return this.settings;
	}
	
	
	public UserDatabase getUserDatabase() {
		return this.userDatabase;
	}
	
	
	public Clock getClock() {
		return this.clock;
	}
	
	
	public InfoServiceClient getInfoService() {
		return this.infoService;
	}
	
	
	public Layer1NetworkMixController getNetworkLayerControllerMix() {
		return networkLayerMix;
	}


	public Layer1NetworkClientController getNetworkLayerControllerClient() {
		return networkLayerClient;
	}


	public Layer2RecodingSchemeMixController getRecodingLayerControllerMix() {
		return recodingLayerMix;
	}


	public Layer2RecodingSchemeClientController getRecodingLayerControllerClient() {
		return recodingLayerClient;
	}


	public Layer3OutputStrategyMixController getOutputStrategyLayerControllerMix() {
		return outputStrategyLayerMix;
	}


	public Layer3OutputStrategyClientController getOutputStrategyLayerControllerClient() {
		return outputStrategyLayerClient;
	}


	public Layer4TransportMixController getTransportLayerControllerMix() {
		return transportLayerMix;
	}


	public Layer4TransportClientController getTransportLayerControllerClient() {
		return transportLayerClient;
	}


	public Layer5ApplicationMixController getApplicationLayerControllerMix() {
		return applicationLayerMix;
	}


	public Layer5ApplicationClientController getApplicationLayerControllerClient() {
		return applicationLayerClient;
	}
	
	
	@Override
	public String toString() {
		return "AnonNode (" +this.gMixTool +" " +this.PUBLIC_PSEUDONYM +")";
	}
	
	
	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		CommandLineParameters params = new CommandLineParameters(args);
		params.gMixTool = ToolName.MIX;
		new AnonNode(params);
	}

}
