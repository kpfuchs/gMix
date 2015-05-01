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
package staticContent.framework.socket.connectedDatagram;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import staticContent.framework.AnonNode;
import staticContent.framework.message.Request;
import staticContent.framework.socket.socketInterfaces.AdaptiveAnonServerSocket;
import staticContent.framework.socket.socketInterfaces.ConnectedDatagramAnonServerSocket;
import staticContent.framework.socket.socketInterfaces.ConnectedDatagramAnonSocketMix;
import staticContent.framework.socket.socketInterfaces.IO_EventObserver;
import staticContent.framework.socket.socketInterfaces.IO_EventObserver_ConnectedDatagram;
import staticContent.framework.util.Util;


public class ConnectedDatagramAnonServerSocketImpl extends AdaptiveAnonServerSocket implements ConnectedDatagramAnonServerSocket {

	private ConcurrentHashMap<Integer, ConnectedDatagramAnonSocketMixImpl> sockets;
	private LinkedBlockingQueue<ConnectedDatagramAnonSocketMixImpl> newConncetions; 
	
	
	public ConnectedDatagramAnonServerSocketImpl(
			AnonNode owner,
			int bindPseudonym,
			int bindPort,
			CommunicationDirection communicationMode,
			IO_Mode ioMode,
			IO_EventObserver requestObserver,
			boolean isReliable,
			boolean isOrderPreserving,
			boolean isFreeRoute
			) {
		
		super(	owner,
				bindPseudonym, 
				bindPort, 
				communicationMode, 
				ioMode,
				requestObserver,
				true,
				isReliable, 
				isOrderPreserving, 
				isFreeRoute);
		if (communicationMode == CommunicationDirection.DUPLEX && !owner.IS_DUPLEX)
			throw new RuntimeException("the current plug-in config does not suport duplex sockets");
		if (communicationMode == CommunicationDirection.SIMPLEX_SENDER)
			throw new RuntimeException("this is a simplex socket (server backend); the server backend can only be \"CommunicationMode.SIMPLEX_RECEIVER\"");
		if (ioMode == IO_Mode.OBSERVER_PATTERN && !(requestObserver instanceof IO_EventObserver_ConnectedDatagram))
			throw new RuntimeException("this socket requires an requestObserver of type IO_EventObserver_ConnectedDatagram");
		this.sockets = new  ConcurrentHashMap<Integer, ConnectedDatagramAnonSocketMixImpl>((int) (owner.EXPECTED_NUMBER_OF_USERS * 1.2));
		this.newConncetions = new LinkedBlockingQueue<ConnectedDatagramAnonSocketMixImpl>();
	}
	
	
	public ConnectedDatagramAnonServerSocketImpl(
			AnonNode owner,
			int bindPseudonym,
			int bindPort,
			CommunicationDirection communicationMode,
			IO_Mode ioMode,
			boolean isReliable,
			boolean isOrderPreserving,
			boolean isFreeRoute
			) {
		
		this(	owner,
				bindPseudonym, 
				bindPort, 
				communicationMode, 
				ioMode,
				null,
				isReliable, 
				isOrderPreserving, 
				isFreeRoute);
	}

	
	@Override
	public ConnectedDatagramAnonSocketMix accept() {
		try {
			return newConncetions.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return accept();
		}
	}

	
	private void putInNewConnectionQueue(ConnectedDatagramAnonSocketMixImpl socket) {
		try {
			newConncetions.put(socket);
		} catch (InterruptedException e) {
			e.printStackTrace();
			putInNewConnectionQueue(socket);
		}
	}
	
	
	@Override
	public void incomingRequest(Request request) {
		int endToEndPseudonym;
		if (owner.LAYER_1_LINKS_MESSAGES) { // use user-id as pseudonym
			endToEndPseudonym = request.getOwner().getIdentifier();
		} else { // extract pseudonym
			endToEndPseudonym = Util.byteArrayToInt(Arrays.copyOf(request.getByteMessage(), 4));
			request.setByteMessage(Arrays.copyOfRange(request.getByteMessage(), 4, request.getByteMessage().length));
		}
		ConnectedDatagramAnonSocketMixImpl socket = sockets.get(endToEndPseudonym);
		if (socket == null) {
			socket = new ConnectedDatagramAnonSocketMixImpl(
					this,
					request.getOwner(),
					owner, 
					endToEndPseudonym,
					communicationDirection,
					isReliable,
					isOrderPreserving, 
					isFreeRoute);
			sockets.put(endToEndPseudonym, socket);
			if (requestObserver != null) { // notify observer
				((IO_EventObserver_ConnectedDatagram)requestObserver).incomingConnection(socket);
			} else { // store for later async read
				putInNewConnectionQueue(socket);
			}
		}
		socket.newIncomingMessage(request);
		if (requestObserver != null) // notify observer
			((IO_EventObserver_ConnectedDatagram)requestObserver).dataAvailable(socket);
	}


	public void disconnect(int endToEndPseudonym) {
		sockets.remove(endToEndPseudonym);
	}


	@Override
	public AdaptiveAnonServerSocket getImplementation() {
		return this;
	}

}
