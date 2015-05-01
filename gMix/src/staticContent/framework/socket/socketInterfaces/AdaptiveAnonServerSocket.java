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
package staticContent.framework.socket.socketInterfaces;

import java.util.concurrent.ArrayBlockingQueue;

import staticContent.framework.AnonNode;
import staticContent.framework.config.Settings;
import staticContent.framework.controller.Layer3OutputStrategyMixController;
import staticContent.framework.controller.Layer4TransportMixController;
import staticContent.framework.message.Request;
import staticContent.framework.util.Util;


//ServerSocket can be used to accept connections (generate "normal" socket on connection attempt)
//or to receive datagrams directly
public abstract class AdaptiveAnonServerSocket implements NoneBlockingAnonSocketOptions, AnonServerSocket, AnonSocketOptions, ServerSocketAddressData {

	protected final static int NOT_SET = -1;
	
	protected int bindPseudonym = NOT_SET;
	protected int bindPort = NOT_SET;
	protected byte[] bindPortAsArray = null;
	
	protected boolean isDuplex = false;
	protected CommunicationDirection communicationDirection = null;
	protected IO_Mode ioMode = null;
	protected boolean isConnectionBased = false;
	protected boolean isReliable = false;
	protected boolean isOrderPreserving = false;
	protected boolean isFreeRoute = false;
	
	protected AnonNode owner;
	protected Settings setting;
	
	protected ArrayBlockingQueue<Request> receivedRequests = null;
	protected Layer3OutputStrategyMixController layer3;
	protected Layer4TransportMixController layer4;
	
	protected boolean isBlocking = false;
	protected IO_EventObserver requestObserver;
	
	
	// try to generate a socket 
	public AdaptiveAnonServerSocket(	AnonNode owner,
										int bindPseudonym,
										int bindPort,
										CommunicationDirection communicationMode,
										IO_Mode ioMode,
										IO_EventObserver requestObserver,
										boolean isConnectionBased, 
										boolean isReliable, 
										boolean isOrderPreserving, 
										boolean isFreeRoute
										) {
		if (communicationMode == CommunicationDirection.DUPLEX && !owner.IS_DUPLEX)
			throw new RuntimeException("the current plug-in config does not suport duplex sockets");
		if (communicationMode == CommunicationDirection.SIMPLEX_SENDER)
			throw new RuntimeException("a simplex ServerSocket con only receive data"); 
		if (isConnectionBased && !owner.IS_CONNECTION_BASED)
			throw new RuntimeException("the current plug-in config does not suport connection-based sockets"); 
		if (isReliable && !owner.IS_RELIABLE)
			throw new RuntimeException("the current plug-in config does not suport reliable transfer"); 
		if (isOrderPreserving && !owner.IS_ORDER_PRESERVING)
			throw new RuntimeException("the current plug-in config does not suport order-preserving transfer"); 
		if (ioMode == IO_Mode.OBSERVER_PATTERN && requestObserver == null)
			throw new RuntimeException("setting IO_Mode to OBSERVER_PATTERN requires to bypass a RequestObserver instance to this socket"); 
		if (ioMode != IO_Mode.OBSERVER_PATTERN && requestObserver != null)
			throw new RuntimeException("a RequestObserver can only be registered for a socket with IO_Mode OBSERVER_PATTERN.\ncall this constructor with IO_Mode OBSERVER_PATTERN or do not bypass a RequestObserver to prevent this error message"); 
		
		this.layer3 = owner.getOutputStrategyLayerControllerMix();
		this.layer4 = owner.getTransportLayerControllerMix();
		this.bindPseudonym = bindPseudonym;
		this.bindPort = bindPort;
		this.bindPortAsArray = Util.intToByteArray(bindPort);
		this.communicationDirection = communicationMode;
		if (communicationMode == CommunicationDirection.DUPLEX)
			this.isDuplex = true;
		this.ioMode = ioMode;
		if (ioMode != IO_Mode.NONE_BLOCKING)
			this.isBlocking = true;
		this.isConnectionBased = isConnectionBased;
		this.isReliable = isReliable;
		this.isOrderPreserving = isOrderPreserving;
		this.isFreeRoute = isFreeRoute;
		this.owner = owner;
		
		this.setting = owner.getSettings();
		
		if (ioMode == IO_Mode.OBSERVER_PATTERN)
			this.requestObserver = requestObserver;
		else
			this.receivedRequests = new ArrayBlockingQueue<Request>(owner.SERVER_SOCKET_QUEUE_SIZE);
		
		// TODO: set data type of bindPseudonym to byte[] 
		owner.registerServerSocket(this);
	}
	
	public AdaptiveAnonServerSocket(	
			AnonNode owner,
			int bindPseudonym,
			int bindPort,
			CommunicationDirection communicationMode,
			IO_Mode ioMode,
			boolean isConnectionBased, 
			boolean isReliable, 
			boolean isOrderPreserving, 
			boolean isFreeRoute
			) {
		this(owner, bindPseudonym, bindPort, communicationMode, ioMode, null, isConnectionBased, isReliable, isOrderPreserving, isFreeRoute);
	}

	
	public void incomingRequest(Request request) {
		if (requestObserver != null) { // notify observer
			requestObserver.incomingRequest(request);
		} else { // store for later async read
			try {
				receivedRequests.put(request);
			} catch (InterruptedException e) {
				e.printStackTrace();
				incomingRequest(request);
			}
		}
	}
	
	
	/**
	 * blocks until a request is available if isBlocking is set to true (see 
	 * method booleanSetBlocking(boolean isBlocking)).
	 * @return
	 */
	protected Request getNextRequest() {
		switch (ioMode) {
			case BLOCKING:
				try {
					return receivedRequests.take();
				} catch (InterruptedException e) {
					e.printStackTrace();
					return getNextRequest();
				}
			case NONE_BLOCKING:
				return receivedRequests.poll();
			case OBSERVER_PATTERN:
				throw new RuntimeException("This method is not available " +
						"when a RequestObserver is registered.\nSee method " +
						"registerRequestObserver(RequestObserver " +
						"requestObserver)");
			default:
				throw new RuntimeException("Unknown IO_Mode: " +ioMode); 
		
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
	public boolean getIsBlocking() {
		return this.isBlocking;
	}


	@Override
	public CommunicationDirection getCommunicationDirection() {
		return this.communicationDirection;
	}
	
	
	@Override
	public IO_Mode getIO_Mode() {
		return this.ioMode;
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
