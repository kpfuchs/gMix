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
package evaluation.simulator.plugins.mixSendStyle;

import evaluation.simulator.Simulator;
import evaluation.simulator.annotations.plugin.Plugin;
import evaluation.simulator.annotations.property.IntSimulationProperty;
import evaluation.simulator.core.event.Event;
import evaluation.simulator.core.event.EventExecutor;
import evaluation.simulator.core.message.MessageFragment;
import evaluation.simulator.core.message.MixMessage;
import evaluation.simulator.core.message.TransportMessage;
import evaluation.simulator.core.networkComponent.NetworkNode;
import evaluation.simulator.plugins.clientSendStyle.ClientSendStyleEvent;

@Plugin(pluginKey = "WAIT_FOR_FURTHER_DATA_BEFORE_REPLY", pluginName = "Last Mix Wait For Further Data")
public class LastMixWaitForFurtherDataBeforeReply extends MixSendStyleImpl
		implements EventExecutor {

	@IntSimulationProperty(
			name = "Time to wait for further data from distant proxy (ms)", 
			key="TIME_TO_WAIT_FOR_DATA_FROM_DISTANT_PROXY",
			min = 0
	)
	private final int timeToWaitForFurtherDataFromDistantProxy;
	
	private MixMessage[] replies;
	private boolean setupComplete = false;
	private Event[] timeoutEvents;

	public LastMixWaitForFurtherDataBeforeReply(NetworkNode owner,
			Simulator simulator, ReplyReceiver replyReceiver) {

		super(owner, simulator, replyReceiver);
		this.timeToWaitForFurtherDataFromDistantProxy = new Integer(
				Simulator.settings
						.getProperty("TIME_TO_WAIT_FOR_DATA_FROM_DISTANT_PROXY")); // in
																					// ms

	}

	@Override
	public void executeEvent(Event event) {

		if ((ClientSendStyleEvent) event.getEventType() == ClientSendStyleEvent.TIMEOUT_SEND_CURRENT_MIX_REPLY) {

			MixMessage mixMessage = this.replies[(Integer) event
					.getAttachment()];
			this.replies[(Integer) event.getAttachment()] = null;
			this.replyReceiver.incomingReply(mixMessage);

		} else {
			throw new RuntimeException(
					"ERROR: LastMixWaitForFurtherDataBeforeReply received unknown Event: "
							+ event.toString());
		}

	}

	@Override
	public void incomingDataFromServer(TransportMessage noneMixMessage) {

		if (!this.setupComplete) {
			this.setup();
		}

		if (this.replies[noneMixMessage.getOwner().getClientId()] == null) {

			this.replies[noneMixMessage.getOwner().getClientId()] = MixMessage
					.getInstance(false, this.owner, noneMixMessage.getOwner(),
							noneMixMessage.getOwner(), Simulator.getNow(),
							false);
			this.timeoutEvents[noneMixMessage.getOwner().getClientId()] = new Event(
					this, Simulator.getNow()
							+ this.timeToWaitForFurtherDataFromDistantProxy,
					ClientSendStyleEvent.TIMEOUT_SEND_CURRENT_MIX_REPLY);
			this.timeoutEvents[noneMixMessage.getOwner().getClientId()]
					.setAttachment(noneMixMessage.getOwner().getClientId());
			this.simulator.scheduleEvent(this.timeoutEvents[noneMixMessage
					.getOwner().getClientId()], this);

		}

		if (this.replies[noneMixMessage.getOwner().getClientId()]
				.getFreeSpace() >= noneMixMessage.getLength()) { // incomingData
																	// fits in
																	// mixMessage

			this.replies[noneMixMessage.getOwner().getClientId()]
					.addPayloadObject(noneMixMessage);

			if (this.replies[noneMixMessage.getOwner().getClientId()]
					.getFreeSpace() == 0) {
				this.simulator
						.unscheduleEvent(this.timeoutEvents[noneMixMessage
								.getOwner().getClientId()]);
				this.replyReceiver.incomingReply(this.replies[noneMixMessage
						.getOwner().getClientId()]);
				this.replies[noneMixMessage.getOwner().getClientId()] = null;

			}

		} else { // incomingData does not fit in mixMessage -> fragment
					// incomingData

			while (noneMixMessage.hasNextFragment()) {

				MessageFragment fragment = noneMixMessage
						.getFragment(this.replies[noneMixMessage.getOwner()
								.getClientId()].getFreeSpace());
				this.replies[noneMixMessage.getOwner().getClientId()]
						.addPayloadObject(fragment);

				if (this.replies[noneMixMessage.getOwner().getClientId()]
						.getFreeSpace() == 0) { // still data to send, but no
												// more space -> send last and
												// create new mixMessage
					this.simulator
							.unscheduleEvent(this.timeoutEvents[noneMixMessage
									.getOwner().getClientId()]);
					this.replyReceiver
							.incomingReply(this.replies[noneMixMessage
									.getOwner().getClientId()]);
					this.replies[noneMixMessage.getOwner().getClientId()] = MixMessage
							.getInstance(false, this.owner,
									noneMixMessage.getOwner(),
									noneMixMessage.getOwner(),
									Simulator.getNow(), false);
					this.timeoutEvents[noneMixMessage.getOwner().getClientId()] = new Event(
							this,
							Simulator.getNow()
									+ this.timeToWaitForFurtherDataFromDistantProxy,
							ClientSendStyleEvent.TIMEOUT_SEND_CURRENT_MIX_REPLY);
					this.timeoutEvents[noneMixMessage.getOwner().getClientId()]
							.setAttachment(noneMixMessage.getOwner()
									.getClientId());
					this.simulator.scheduleEvent(
							this.timeoutEvents[noneMixMessage.getOwner()
									.getClientId()], this);

				}

			}

		}

	}

	protected void setup() {

		this.setupComplete = true;
		this.replies = new MixMessage[this.simulator.getNumberOfClients()];
		this.timeoutEvents = new Event[this.simulator.getNumberOfClients()];

	}

}
