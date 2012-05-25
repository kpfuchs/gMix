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
package evaluation.simulator.outputStrategy;


import evaluation.simulator.communicationBehaviour.LastMixCommunicationBehaviour;
import evaluation.simulator.core.Event;
import evaluation.simulator.core.EventExecutor;
import evaluation.simulator.core.OutputStrategyEvent;
import evaluation.simulator.core.Simulator;
import evaluation.simulator.message.MixMessage;
import evaluation.simulator.message.NoneMixMessage;
import evaluation.simulator.message.StopAndGoMessage;
import evaluation.simulator.networkComponent.Mix;


// Kesdogan et. al. 1998: Stop-and-Go MIXes: Providing Probabilistic Anonymity in an Open System
public class StopAndGo extends OutputStrategy implements EventExecutor {

	private LastMixCommunicationBehaviour lastMixCommunicationBehaviour = null;
	private boolean useTimeStamps;
	
	
	protected StopAndGo(Mix mix, Simulator simulator) {

		super(mix, simulator);
		this.lastMixCommunicationBehaviour = LastMixCommunicationBehaviour.getInstance(mix, simulator, this);
		this.useTimeStamps = Simulator.settings.getPropertyAsBoolean("SGMIX_USE_TIMESTAMPS");
		
	}
	
	
	@Override
	public void incomingRequest(MixMessage mixMessage) {
		
		StopAndGoMessage sgMessage = (StopAndGoMessage)mixMessage; 
		
		if (useTimeStamps)
			if (Simulator.getNow() < sgMessage.getTsMin() || Simulator.getNow() > sgMessage.getTsMax())
				return;
			
		scheduleOutput(sgMessage, sgMessage.getDelay());
	
	}


	@Override
	public void incomingReply(MixMessage mixMessage) {
		
		StopAndGoMessage sgMessage = (StopAndGoMessage)mixMessage; 
		
		if (useTimeStamps)
			if (Simulator.getNow() < sgMessage.getTsMin() || Simulator.getNow() > sgMessage.getTsMax())
				return;
		
		scheduleOutput(sgMessage, sgMessage.getDelay());
		
	}
	

	@Override
	public void incomingReply(NoneMixMessage noneMixMessage) {
		lastMixCommunicationBehaviour.incomingDataFromDistantProxy(noneMixMessage);
	}

	
	private void scheduleOutput(MixMessage mixMessage, int delayTillOutput) {
		simulator.scheduleEvent(new Event(this, Simulator.getNow() + delayTillOutput, OutputStrategyEvent.TIMEOUT, mixMessage), this);
	}
	
	
	@Override
	public void executeEvent(Event e) {
		
		if (e.getEventType() == OutputStrategyEvent.TIMEOUT) {
			
			MixMessage mixMessage = (MixMessage)e.getAttachment();
			
			if (mixMessage.isRequest())
				mix.putOutRequest(mixMessage);
			else
				mix.putOutReply(mixMessage);
			
		} else 
			throw new RuntimeException("ERROR: StopAndGo received unknown Event: " +e); 
		
	}
	
}