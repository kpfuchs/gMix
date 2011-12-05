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

package outputStrategy;

import java.security.SecureRandom;

import message.MixMessage;
import message.NoneMixMessage;
import networkComponent.Mix;
import simulator.Event;
import simulator.EventExecutor;
import simulator.OutputStrategyEvent;
import simulator.Settings;
import simulator.Simulator;
import communicationBehaviour.LastMixCommunicationBehaviour;


// Cottrell 1995 ("Mixmaster & Remailer Attacks")
// delays incoming messages randomly
public class CottrellRandomDelay extends OutputStrategy implements EventExecutor {

	private LastMixCommunicationBehaviour lastMixCommunicationBehaviour = null;
	private static SecureRandom secureRandom = new SecureRandom();
	private int maxDelay;
	
	
	protected CottrellRandomDelay(Mix mix, Simulator simulator) {

		super(mix, simulator);
		this.lastMixCommunicationBehaviour = LastMixCommunicationBehaviour.getInstance(mix, simulator, this);
		this.maxDelay = Settings.getPropertyAsInt("MAX_RANDOM_DELAY");
	}
	
	
	@Override
	public void incomingRequest(MixMessage mixMessage) {
		scheduleOutput(mixMessage, getRandomDelay());
	}


	@Override
	public void incomingReply(MixMessage mixMessage) {
		scheduleOutput(mixMessage, getRandomDelay());
	}

	
	private int getRandomDelay() {
		return secureRandom.nextInt(maxDelay+1);
	}
	

	@Override
	public void incomingReply(NoneMixMessage noneMixMessage) {
		lastMixCommunicationBehaviour.incomingDataFromDistantProxy(noneMixMessage);
	}

	
	private void scheduleOutput(MixMessage mixMessage, int delayTillOutput) {
		simulator.scheduleEvent(new Event(this, Simulator.getNow() + delayTillOutput, OutputStrategyEvent.TIMEOUT), mixMessage);
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
			throw new RuntimeException("ERROR: CottrellRandomDelay received unknown Event: " +e); 
		
	}
	
}
