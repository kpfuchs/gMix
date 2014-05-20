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
package evaluation.simulator.plugins.clientSendStyle;

import java.util.Vector;

import evaluation.simulator.Simulator;
import evaluation.simulator.annotations.plugin.Plugin;
import evaluation.simulator.annotations.property.IntSimulationProperty;
import evaluation.simulator.core.message.MessageFragment;
import evaluation.simulator.core.message.MixMessage;
import evaluation.simulator.core.message.NetworkMessage;
import evaluation.simulator.core.message.TransportMessage;
import evaluation.simulator.core.networkComponent.AbstractClient;

@Plugin(pluginKey = "WAIT_FOR_REPLY", pluginName = "Wait For Reply")
public class ClientWaitForReply extends ClientSendStyleImpl {
	
	private boolean isFirstCall;
	private final Vector<TransportMessage> requestWaitingQueue = new Vector<TransportMessage>(
			10, 10);

	public ClientWaitForReply(AbstractClient owner, Simulator simulator) {
		super(owner, simulator);
		this.isFirstCall = true;
	}

	@Override
	public void incomingDecryptedReply(NetworkMessage reply) {
		this.sendMessage();
	}

	@Override
	public void incomingRequestFromUser(TransportMessage request) {

		this.requestWaitingQueue.add(request);

		if (this.isFirstCall) {
			this.isFirstCall = false;
			this.sendMessage();
		}

	}

	@Override
	public void messageReachedServer(TransportMessage request) {
		if (!this.simulateReplyChannel) {
			this.sendMessage();
		}
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

				TransportMessage noneMixMessage = this.requestWaitingQueue
						.get(i);

				if ((mixMessage.getFreeSpace() >= noneMixMessage.getLength())
						&& !noneMixMessage.isFragmented()) { // noneMixMessage
																// fits in
																// mixMessage
																// completely

					this.requestWaitingQueue.remove(i);
					i--;
					mixMessage.addPayloadObject(noneMixMessage);

				} else { // add Fragment

					if (noneMixMessage.hasNextFragment()) {

						MessageFragment messageFragment = noneMixMessage
								.getFragment(mixMessage.getFreeSpace());
						mixMessage.addPayloadObject(messageFragment);

					}

					if (!noneMixMessage.hasNextFragment()) {
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

	}

}
