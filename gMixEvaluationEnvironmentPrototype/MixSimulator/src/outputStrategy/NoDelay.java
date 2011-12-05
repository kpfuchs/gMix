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

import communicationBehaviour.LastMixCommunicationBehaviour;
import simulator.Simulator;
import networkComponent.Mix;
import message.MixMessage;
import message.NoneMixMessage;


// forwards messages without delay (use for base line measurments)
public class NoDelay extends OutputStrategy {
	
	private LastMixCommunicationBehaviour lastMixCommunicationBehaviour = null;
	
	
	protected NoDelay (Mix mix, Simulator simulator) {
		
		super(mix, simulator);
		this.lastMixCommunicationBehaviour = LastMixCommunicationBehaviour.getInstance(mix, simulator, this);
		
	}


	@Override
	public void incomingRequest(MixMessage mixMessage) {
		mix.putOutRequest(mixMessage);
	}


	@Override
	public void incomingReply(MixMessage mixMessage) {
		mix.putOutReply(mixMessage);
	}


	@Override
	public void incomingReply(NoneMixMessage noneMixMessage) {
		
		lastMixCommunicationBehaviour.incomingDataFromDistantProxy(noneMixMessage);
		
	}

}
