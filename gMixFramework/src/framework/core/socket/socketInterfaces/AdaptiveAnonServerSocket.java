package framework.core.socket.socketInterfaces;

import java.util.concurrent.ArrayBlockingQueue;

import framework.core.AnonNode;
import framework.core.config.Settings;
import framework.core.controller.Layer3OutputStrategyMixController;
import framework.core.message.Request;
import framework.core.util.Util;




//ServerSocket can be used to accept connections (generate "normal" socket on connection attempt)
//or to receive datagrams directly
public abstract class AdaptiveAnonServerSocket implements AnonServerSocket, AnonSocketOptions, ServerSocketAddressData{

	protected final static int NOT_SET = -1;
	
	protected int bindPseudonym = NOT_SET;
	protected int bindPort = NOT_SET;
	protected byte[] bindPortAsArray = null;
	
	protected boolean isDuplex = false;
	protected CommunicationMode communicationMode = null;
	protected boolean isConnectionBased = false;
	protected boolean isReliable = false;
	protected boolean isOrderPreserving = false;
	protected boolean isFreeRoute = false;
	
	protected AnonNode owner;
	protected Settings setting;
	
	protected ArrayBlockingQueue<Request> receivedRequests = null;
	protected Layer3OutputStrategyMixController layer3;
	
	// try to generate a socket 
	public AdaptiveAnonServerSocket(	AnonNode owner,
										int bindPseudonym,
										int bindPort,
										CommunicationMode communicationMode,
										boolean isConnectionBased, 
										boolean isReliable, 
										boolean isOrderPreserving, 
										boolean isFreeRoute
										) {
		if (communicationMode == CommunicationMode.DUPLEX && !owner.IS_DUPLEX)
			throw new RuntimeException("the current plug-in config does not suport duplex sockets");
		if (communicationMode == CommunicationMode.SIMPLEX_SENDER)
			throw new RuntimeException("a simplex ServerSocket con only receive data"); 
		if (isConnectionBased && !owner.IS_CONNECTION_BASED)
			throw new RuntimeException("the current plug-in config does not suport connection-based sockets"); 
		if (isReliable && !owner.IS_RELIABLE)
			throw new RuntimeException("the current plug-in config does not suport reliable transfer"); 
		if (isOrderPreserving && !owner.IS_ORDER_PRESERVING)
			throw new RuntimeException("the current plug-in config does not suport order-preserving transfer"); 
		this.layer3 = owner.getOutputStrategyLayerControllerMix();
		this.bindPseudonym = bindPseudonym;
		this.bindPort = bindPort;
		this.bindPortAsArray = Util.intToByteArray(bindPort);
		this.communicationMode = communicationMode;
		if (communicationMode == CommunicationMode.DUPLEX)
			this.isDuplex = true;
		this.isConnectionBased = isConnectionBased;
		this.isReliable = isReliable;
		this.isOrderPreserving = isOrderPreserving;
		this.isFreeRoute = isFreeRoute;
		this.owner = owner;
		this.setting = owner.getSettings();
		
		owner.registerServerSocket(this);
		this.receivedRequests = new ArrayBlockingQueue<Request>(owner.SERVER_SOCKET_QUEUE_SIZE);
		
		
		// TODO: set data type of bindPseudonym to byte[] 
	}
	
	
	public void incomingRequest(Request request) {
		try {
			receivedRequests.put(request);
		} catch (InterruptedException e) {
			e.printStackTrace();
			incomingRequest(request);
		}
	}
	
	// may block
	protected Request getNextRequest() {
		try {
			return receivedRequests.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return getNextRequest();
		}
	}
	
	
	@Override
	public boolean getIsConnectionBased() {
		return this.isConnectionBased;
	}


	@Override
	public boolean getIsReliable() {
		return this.isReliable;
	}


	@Override
	public boolean getIsOrderPreserving() {
		return this.isOrderPreserving;
	}


	@Override
	public boolean getIsFreeRouteSocket() {
		return this.isFreeRoute;
	}


	@Override
	public boolean getIsDuplex() {
		return this.isDuplex;
	}


	@Override
	public CommunicationMode getCommunicationMode() {
		return this.communicationMode;
	}


	@Override
	public int getBindPseudonym() {
		return this.bindPseudonym;
	}


	@Override
	public int getBindPort() {
		return this.bindPort;
	}


	@Override
	public AnonNode getOwner() {
		return this.owner;
	}
}
