/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2011  Karl-Peter Fuchs
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

package evaluation.simulator.networkComponent;

import evaluation.simulator.core.DistantProxyEvent;
import evaluation.simulator.core.Event;
import evaluation.simulator.core.MixEvent;
import evaluation.simulator.core.Simulator;
import evaluation.simulator.delayBox.DelayBox;
import evaluation.simulator.message.MixMessage;
import evaluation.simulator.message.NoneMixMessage;


public abstract class DistantProxy extends NetworkNode {

	protected Simulator simulator;
	private boolean simulateReplyChannel;
	private boolean distantProxySupportsDummyTraffic;
	private boolean supportsMixMessages;

	
	public static DistantProxy getInstance(String identifier, Simulator simulator, DelayBox delayBox) {
		
		
		String type = Simulator.settings.getProperty("OUTPUT_STRATEGY");
		
		if (Simulator.settings.getProperty("SIMULATION_SCRIPT").equals("CLIENT-DISTANT_PROXY"))
			return new BasicDistantProxy("BasicDistantProxy", simulator, delayBox);
		else if (type.equals("BATCH") || type.equals("DISTINCT_USER_BATCH") || type.equals("BASIC_SYNCHRONOUS_BATCH") || type.equals("NONE") || type.equals("COTTRELL_POOL") || type.equals("BINOMIAL_POOL") || type.equals("STOP_AND_GO"))
			return new BasicDistantProxy("BasicDistantProxy", simulator, delayBox);
		else if (type.equals("BASIC_DLPA") && Simulator.settings.getProperty("RECEIVER_SUPPORTS_DUMMY_TRAFFIC").equals("TRUE"))
			return new DummySupportingDistantProxy("DummySupportingDistantProxy", simulator, delayBox);
		else if (type.equals("BASIC_DLPA"))
			return new BasicDistantProxy("BasicDistantProxy", simulator, delayBox);
		else
			throw new RuntimeException("ERROR: no suiting DistantProxy for OUTPUT_STRATEGY " +type +"!");
		
	}
	
	
	protected DistantProxy(String identifier, Simulator simulator, DelayBox delayBox, boolean supportsMixMessages) {
		
		super(identifier, simulator, delayBox);
		this.simulator = simulator;
		this.simulateReplyChannel = Simulator.settings.getPropertyAsBoolean("SIMULATE_REPLY_CHANNEL");
		this.distantProxySupportsDummyTraffic = Simulator.settings.getPropertyAsBoolean("RECEIVER_SUPPORTS_DUMMY_TRAFFIC");
		this.supportsMixMessages = supportsMixMessages;
	}

	
	protected abstract void incomingRequest(NoneMixMessage noneMixMessage);
	protected abstract void requestAnswered(NoneMixMessage noneMixMessage);

	
	protected void sendReply(NoneMixMessage noneMixMessage, int delay) {
		sendToPreviousHop(noneMixMessage, delay, MixEvent.INCOMING_REPLY_FROM_DISTANT_PROXY);
	}
	
	// calls requestAnswered(NoneMixMessage noneMixMessage) (implemented in subclass) after "delay" ms
	protected void callRequestAnsweredIn(int delay, NoneMixMessage noneMixMessage) {
		simulator.scheduleEvent(new Event(this, Simulator.getNow() + delay, DistantProxyEvent.REQUEST_ANSWERED, noneMixMessage), this);
	}
	
	
	@Override
	public void executeEvent(Event event) {
		
		if (!(event.getEventType() instanceof DistantProxyEvent)) {
			
			throw new RuntimeException("ERROR: " +super.getIdentifier() +" received wrong Event: " +event.getEventType().toString());
			
		} else {
			
			switch ((DistantProxyEvent)event.getEventType()) {
			
				case INCOMING_REQUEST:
					if (distantProxySupportsDummyTraffic)
						((DummySupportingDistantProxy)this).incomingRequest((MixMessage)event.getAttachment());
					else
						incomingRequest((NoneMixMessage)event.getAttachment());
					break;
				
				case REQUEST_ANSWERED:
					if (simulateReplyChannel)
						requestAnswered((NoneMixMessage)event.getAttachment());
					else
						((NoneMixMessage)event.getAttachment()).getOwner().getClientCommunicationBehaviour().incomingDecryptedReply((NoneMixMessage)event.getAttachment());
					break;

				default: 
					throw new RuntimeException("ERROR: " +super.getIdentifier() +" received unknown Event: " +event.getEventType().toString());
					
			}
			
		}
		
	}


	public boolean supportsMixMessages() {
		return supportsMixMessages;
	}

	
}
