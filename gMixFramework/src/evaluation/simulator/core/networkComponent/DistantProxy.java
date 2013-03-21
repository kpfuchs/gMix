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
package evaluation.simulator.core.networkComponent;

import evaluation.simulator.Simulator;
import evaluation.simulator.core.event.DistantProxyEvent;
import evaluation.simulator.core.event.Event;
import evaluation.simulator.core.event.MixEvent;
import evaluation.simulator.core.message.MixMessage;
import evaluation.simulator.core.message.TransportMessage;
import evaluation.simulator.plugins.mixSendStyle.ReplyReceiver;


public abstract class DistantProxy extends NetworkNode implements ReplyReceiver {

	protected Simulator simulator;
	protected AbstractServer server;
	
	
	public static DistantProxy getInstance(String identifier, Simulator simulator) {
		return new BasicDistantProxy(identifier, simulator);
	}
	
	
	protected DistantProxy(String identifier, Simulator simulator, boolean supportsMixMessages) {
		super(identifier, simulator);
		this.simulator = simulator;
		this.server = AbstractServer.getInstance(this);
	}

	
	protected abstract void incomingRequest(TransportMessage transportMessage);
	protected abstract void requestAnswered(TransportMessage transportMessage);
	protected abstract void incomingRequest(MixMessage mixMessage);
	public abstract void incomingReply(MixMessage mixMessage);

	
	protected void sendReply(TransportMessage noneMixMessage, int delay) {
		sendToPreviousHop(noneMixMessage, delay, MixEvent.INCOMING_REPLY_FROM_DISTANT_PROXY);
	}
	
	
	@Override
	public void executeEvent(Event event) {
		if (!(event.getEventType() instanceof DistantProxyEvent)) {
			throw new RuntimeException("ERROR: " +super.getIdentifier() +" received wrong Event: " +event.getEventType().toString());
		} else {
			switch ((DistantProxyEvent)event.getEventType()) {
				case INCOMING_REQUEST:
					if (event.getAttachment() instanceof TransportMessage)
						incomingRequest((TransportMessage)event.getAttachment());
					else if (event.getAttachment() instanceof MixMessage)
						incomingRequest((MixMessage)event.getAttachment());
					else
						throw new RuntimeException("ERROR: " +super.getIdentifier() +" received unknown Event attachment: " +event.getAttachment()); 
					break;
				default: 
					throw new RuntimeException("ERROR: " +super.getIdentifier() +" received unknown Event: " +event.getEventType().toString());	
			}
		}
	}
	
}
