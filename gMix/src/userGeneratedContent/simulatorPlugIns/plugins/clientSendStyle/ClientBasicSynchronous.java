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
package userGeneratedContent.simulatorPlugIns.plugins.clientSendStyle;

import java.util.Vector;

import staticContent.evaluation.simulator.Simulator;
import staticContent.evaluation.simulator.annotations.plugin.Plugin;
import staticContent.evaluation.simulator.annotations.property.IntSimulationProperty;
import staticContent.evaluation.simulator.core.event.Event;
import staticContent.evaluation.simulator.core.event.EventExecutor;
import staticContent.evaluation.simulator.core.message.MessageFragment;
import staticContent.evaluation.simulator.core.message.MixMessage;
import staticContent.evaluation.simulator.core.message.NetworkMessage;
import staticContent.evaluation.simulator.core.message.TransportMessage;
import staticContent.evaluation.simulator.core.networkComponent.AbstractClient;

@Plugin(pluginKey = "SEND_SYNCHRONOUS", 
	pluginName = "Send Synchronous")
public class ClientBasicSynchronous extends ClientSendStyleImpl implements
		EventExecutor {

	private final Vector<TransportMessage> requestWaitingQueue = new Vector<TransportMessage>(
			10, 10);

	@IntSimulationProperty(name = "Basic synchronous send interval (ms)",
			key = "BASIC_SYNCHRONOUS_SEND_INTERVAL_IN_MS",
			tooltip = "Basic synchronous send interval in ms",
			min = 0)
	int sendInterval;

	public ClientBasicSynchronous(AbstractClient owner, Simulator simulator) {
		super(owner, simulator);
		
		sendInterval = new Integer(
				Simulator.settings
						.getProperty("BASIC_SYNCHRONOUS_SEND_INTERVAL_IN_MS"));
		this.scheduleNextSend();
	}

	@Override
	public void executeEvent(Event event) {
		if (event.getEventType() != ClientSendStyleEvent.SEND_NEXT_MIX_MESSAGE) {
			throw new RuntimeException("ERROR! received unsupported event!"
					+ event);
		}
		this.sendMessage();
	}

	@Override
	public void incomingDecryptedReply(NetworkMessage reply) {

	}

	@Override
	public void incomingRequestFromUser(TransportMessage request) {
		this.requestWaitingQueue.add(request);
	}

	@Override
	public void messageReachedServer(TransportMessage request) {

	}

	private void scheduleNextSend() {
		Event sendNextMessageEvent = new Event(this, Simulator.getNow()
				+ this.sendInterval, ClientSendStyleEvent.SEND_NEXT_MIX_MESSAGE);
		this.simulator.scheduleEvent(sendNextMessageEvent, this);
	}

	private void sendMessage() {
		if (this.requestWaitingQueue.size() == 0) { // no data to send -> send
													// dummy
			this.owner.sendRequest(MixMessage.getInstance(true, this.owner,
					this.simulator.getDistantProxy(), this.owner,
					Simulator.getNow(), true));
		} else { // data available to send -> send as much data as available
					// (limit: free space in mixMessage)
			MixMessage mixMessage = MixMessage.getInstance(true, this.owner,
					this.simulator.getDistantProxy(), this.owner,
					Simulator.getNow(), false);
			for (int i = 0; i < this.requestWaitingQueue.size(); i++) {
				TransportMessage transportMessage = this.requestWaitingQueue
						.get(i);
				if ((mixMessage.getFreeSpace() >= transportMessage.getLength())
						&& !transportMessage.isFragmented()) { // transportMessage
																// fits in
																// mixMessage
																// completely
					this.requestWaitingQueue.remove(i);
					i--;
					mixMessage.addPayloadObject(transportMessage);
				} else { // add Fragment
					if (transportMessage.hasNextFragment()) {
						MessageFragment messageFragment = transportMessage
								.getFragment(mixMessage.getFreeSpace());
						mixMessage.addPayloadObject(messageFragment);
					}
					if (!transportMessage.hasNextFragment()) {
						this.requestWaitingQueue.remove(i);
						i--;
					}
				}
				if (mixMessage.getFreeSpace() == 0) {
					break;
				}
			}
			this.owner.sendRequest(mixMessage);
		}
		this.scheduleNextSend();
	}

}
