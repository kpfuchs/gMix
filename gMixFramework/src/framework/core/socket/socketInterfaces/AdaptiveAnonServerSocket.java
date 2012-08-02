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
