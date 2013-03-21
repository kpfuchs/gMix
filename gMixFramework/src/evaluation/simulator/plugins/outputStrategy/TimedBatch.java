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
package evaluation.simulator.plugins.outputStrategy;

import java.util.Vector;


import evaluation.simulator.Simulator;
import evaluation.simulator.core.event.Event;
import evaluation.simulator.core.event.EventExecutor;
import evaluation.simulator.core.message.MixMessage;
import evaluation.simulator.core.networkComponent.AbstractClient;
import evaluation.simulator.core.networkComponent.Mix;
import evaluation.simulator.pluginRegistry.ClientSendStyle;
import evaluation.simulator.pluginRegistry.MixSendStyle;
import evaluation.simulator.plugins.clientSendStyle.ClientSendStyleImpl;
import evaluation.simulator.plugins.mixSendStyle.MixSendStyleImpl;


// Dingledine 2002: Timed Mix
// "The mix fires (flushes all messages) every t seconds"
public class TimedBatch extends OutputStrategyImpl {

	private SimplexTimedMix requestBatch;
	private SimplexTimedMix replyBatch;
	
	
	public TimedBatch(Mix mix, Simulator simulator) {
		super(mix, simulator);
		int sendingRate = Simulator.settings.getPropertyAsInt("TIMED_BATCH_SENDING_RATE");
		this.requestBatch = new SimplexTimedMix(true, sendingRate);
		this.replyBatch = new SimplexTimedMix(false, sendingRate);
	}
	
	
	@Override
	public void incomingRequest(MixMessage mixMessage) {
		requestBatch.addMessage(mixMessage);
	}


	@Override
	public void incomingReply(MixMessage mixMessage) {
		replyBatch.addMessage(mixMessage);
	}
	
	
	public class SimplexTimedMix implements EventExecutor {
		
		private boolean isRequestTimedMix;
		private Vector<MixMessage> collectedMessages;
		private boolean isFirstMessage = true;
		private int sendingRate;
		
		
		public SimplexTimedMix(boolean isRequestTimedMix, int sendingRate) {
			
			this.collectedMessages = new Vector<MixMessage>(simulator.getClients().size()*2);	
			this.isRequestTimedMix = isRequestTimedMix;
			this.sendingRate = sendingRate;
			
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			
			if (isFirstMessage) {
				isFirstMessage = false;
				scheduleNextOutput();
			}
			
			collectedMessages.add(mixMessage);

		}

		
		public void putOutMessages() {
			
			for (MixMessage m:collectedMessages)
				if (isRequestTimedMix)
					mix.putOutRequest(m);
				else
					mix.putOutReply(m);
			
			this.collectedMessages = new Vector<MixMessage>(collectedMessages.size() * 2);	
			scheduleNextOutput();		
			
		}
		
		
		private void scheduleNextOutput() {
			
			simulator.scheduleEvent(new Event(this, Simulator.getNow() + sendingRate, OutputStrategyEvent.TIMEOUT), this);
		
		}

		
		@Override
		public void executeEvent(Event e) {
			
			if (e.getEventType() == OutputStrategyEvent.TIMEOUT) {
				putOutMessages();
			} else 
				throw new RuntimeException("ERROR: TimedBatch received unknown Event: " +e); 
			
		}
		
	}
	
	@Override
	public ClientSendStyleImpl getClientSendStyle(AbstractClient client) {
		return ClientSendStyle.getInstance(client);
	}


	@Override
	public MixSendStyleImpl getMixSendStyle() {
		return MixSendStyle.getInstance(mix, mix);
	}
	

}