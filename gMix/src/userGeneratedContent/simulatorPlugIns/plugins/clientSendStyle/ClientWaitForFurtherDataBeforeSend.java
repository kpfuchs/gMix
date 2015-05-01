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

@Plugin(pluginKey = "WAIT_FOR_FURTHER_DATA", pluginName = "Wait For Further Data")
public class ClientWaitForFurtherDataBeforeSend extends ClientSendStyleImpl
		implements EventExecutor {

	private MixMessage mixMessage;
	private Event timeoutEvent;

	@IntSimulationProperty(
			name = "Time to wait for further data from user (ms)", 
			key = "TIME_TO_WAIT_FOR_FURTHER_DATA_FROM_USER",
			min = 0)
	private final int timeToWaitForFurtherDataFromUser;

	public ClientWaitForFurtherDataBeforeSend(AbstractClient owner,
			Simulator simulator) {

		super(owner, simulator);
		this.timeToWaitForFurtherDataFromUser = Simulator.settings
				.getPropertyAsInt("TIME_TO_WAIT_FOR_FURTHER_DATA_FROM_USER"); // in
																				// ms

	}

	@Override
	public void executeEvent(Event event) {

		if ((ClientSendStyleEvent) event.getEventType() == ClientSendStyleEvent.TIMEOUT_SEND_CURRENT_MIX_REQUEST) {

			this.owner.sendRequest(this.mixMessage);
			this.mixMessage = null;

		} else {
			throw new RuntimeException(
					"ERROR: ClientWaitForFurtherDataBeforeSend received unknown Event: "
							+ event.toString());
		}

	}

	@Override
	public void incomingDecryptedReply(NetworkMessage reply) {

	}

	@Override
	public void incomingRequestFromUser(TransportMessage request) {

		if (this.mixMessage == null) {
			this.mixMessage = MixMessage.getInstance(true, this.owner,
					this.simulator.getDistantProxy(), this.owner,
					Simulator.getNow(), false);
			this.timeoutEvent = new Event(this, Simulator.getNow()
					+ this.timeToWaitForFurtherDataFromUser,
					ClientSendStyleEvent.TIMEOUT_SEND_CURRENT_MIX_REQUEST);
			this.simulator.scheduleEvent(this.timeoutEvent, this);
		}

		if (this.mixMessage.getFreeSpace() >= request.getLength()) { // incomingData
																		// fits
																		// in
																		// mixMessage

			this.mixMessage.addPayloadObject(request);

			if (this.mixMessage.getFreeSpace() == 0) {
				this.simulator.unscheduleEvent(this.timeoutEvent);
				this.owner.sendRequest(this.mixMessage);
				this.mixMessage = null;

			}

		} else { // incomingData does not fit in mixMessage -> fragment
					// incomingData

			while (request.hasNextFragment()) {

				MessageFragment fragment = request.getFragment(this.mixMessage
						.getFreeSpace());
				this.mixMessage.addPayloadObject(fragment);

				if (this.mixMessage.getFreeSpace() == 0) { // still data to
															// send, but no more
															// space -> send
															// last and create
															// new mixMessage
					this.simulator.unscheduleEvent(this.timeoutEvent);
					this.owner.sendRequest(this.mixMessage);
					this.mixMessage = MixMessage.getInstance(true, this.owner,
							this.simulator.getDistantProxy(), this.owner,
							Simulator.getNow(), false);
					this.timeoutEvent = new Event(
							this,
							Simulator.getNow()
									+ this.timeToWaitForFurtherDataFromUser,
							ClientSendStyleEvent.TIMEOUT_SEND_CURRENT_MIX_REQUEST);
					this.simulator.scheduleEvent(this.timeoutEvent, this);

				}

			}

		}

	}

	@Override
	public void messageReachedServer(TransportMessage request) {

	}

}
