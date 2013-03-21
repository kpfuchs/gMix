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
package evaluation.simulator.plugins.clientSendStyle;

import java.util.Vector;

import evaluation.simulator.Simulator;
import evaluation.simulator.core.event.Event;
import evaluation.simulator.core.event.EventExecutor;
import evaluation.simulator.core.message.MessageFragment;
import evaluation.simulator.core.message.MixMessage;
import evaluation.simulator.core.message.NetworkMessage;
import evaluation.simulator.core.message.TransportMessage;
import evaluation.simulator.core.networkComponent.AbstractClient;


public class ClientBasicSynchronous extends ClientSendStyleImpl implements EventExecutor {

	private int sendInterval;
	private Vector<TransportMessage> requestWaitingQueue = new Vector<TransportMessage>(10,10);
	
	
	public ClientBasicSynchronous(AbstractClient owner, Simulator simulator) {
		super(owner, simulator);
		this.sendInterval = new Integer(Simulator.settings.getProperty("BASIC_SYNCHRONOUS_SEND_INTERVAL_IN_MS"));
		scheduleNextSend();
	}

	
	@Override
	public void incomingRequestFromUser(TransportMessage request) {
		requestWaitingQueue.add(request);
	}
	
	
	private void sendMessage() {
		if (requestWaitingQueue.size() == 0) { // no data to send -> send dummy
			owner.sendRequest(MixMessage.getInstance(true, owner, simulator.getDistantProxy(), owner, Simulator.getNow(), true));
		} else { // data available to send -> send as much data as available (limit: free space in mixMessage)
			MixMessage mixMessage = MixMessage.getInstance(true, owner, simulator.getDistantProxy(), owner, Simulator.getNow(), false);
			for (int i=0; i<requestWaitingQueue.size(); i++) {
				TransportMessage transportMessage = requestWaitingQueue.get(i);
				if (mixMessage.getFreeSpace() >= transportMessage.getLength() && !transportMessage.isFragmented()) { // transportMessage fits in mixMessage completely
					requestWaitingQueue.remove(i);
					i--;
					mixMessage.addPayloadObject(transportMessage);
				} else { // add Fragment
					if (transportMessage.hasNextFragment()) {
						MessageFragment messageFragment = transportMessage.getFragment(mixMessage.getFreeSpace());
						mixMessage.addPayloadObject(messageFragment);
					}
					if (!transportMessage.hasNextFragment()) {
						requestWaitingQueue.remove(i);
						i--;
					}
				}
				if (mixMessage.getFreeSpace() == 0)
					break;
			}
			owner.sendRequest(mixMessage);
		}
		scheduleNextSend();
	}

	
	private void scheduleNextSend() {
		Event sendNextMessageEvent = new Event(this, Simulator.getNow() + sendInterval, ClientSendStyleEvent.SEND_NEXT_MIX_MESSAGE);
		simulator.scheduleEvent(sendNextMessageEvent, this);
	}

	
	@Override
	public void executeEvent(Event event) {
		if (event.getEventType() != ClientSendStyleEvent.SEND_NEXT_MIX_MESSAGE)
			throw new RuntimeException("ERROR! received unsupported event!" +event);
		sendMessage();	
	}

	
	@Override
	public void incomingDecryptedReply(NetworkMessage reply) {
		
	}
	

	@Override
	public void messageReachedServer(TransportMessage request) {

	}

}
