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
import java.util.Vector;

import message.MixMessage;
import message.NoneMixMessage;
import networkComponent.Mix;
import simulator.Settings;
import simulator.Simulator;

import communicationBehaviour.LastMixCommunicationBehaviour;


// Dingledine 2002:
// The mix fires when n + f messages accumulate in the mix. A pool of f 
// messages, chosen uniformly at random from all the messages, is retained in 
// the mix. (Consider these messages as feedback into the mix.) The other n 
// are forwarded on.
public class ThresholdPool extends OutputStrategy {

	private LastMixCommunicationBehaviour lastMixCommunicationBehaviour;
	private static SecureRandom secureRandom = new SecureRandom();
	private SimplexTresholdPool requestPool;
	private SimplexTresholdPool replyPool;
	
	
	protected ThresholdPool(Mix mix, Simulator simulator) {

		super(mix, simulator);
		
		int poolSize = new Integer(Settings.getProperty("THRESHOLD_POOL_POOL_SIZE"));
		int threshold = new Integer(Settings.getProperty("THRESHOLD_POOL_THRESHOLD"));
		this.requestPool = new SimplexTresholdPool(true, poolSize, threshold);
		this.replyPool = new SimplexTresholdPool(false, poolSize, threshold);
		this.lastMixCommunicationBehaviour = LastMixCommunicationBehaviour.getInstance(mix, simulator, this);
		
	}

	
	@Override
	public void incomingRequest(MixMessage mixMessage) {		
		requestPool.addMessage(mixMessage);
	}


	@Override
	public void incomingReply(MixMessage mixMessage) {
		replyPool.addMessage(mixMessage);
	}


	@Override
	public void incomingReply(NoneMixMessage noneMixMessage) {
		lastMixCommunicationBehaviour.incomingDataFromDistantProxy(noneMixMessage);
	}
	
	
	public class SimplexTresholdPool {
		
		private boolean isRequestPool;
		private Vector<MixMessage> collectedMessages;
		private int threshold;
		private int numberOfMessagesToPutOut;
		
		
		public SimplexTresholdPool(boolean isRequestPool, int poolSize, int threshold) {
			
			this.collectedMessages = new Vector<MixMessage>(poolSize*2);
			this.isRequestPool = isRequestPool;
			this.threshold = threshold;
			this.numberOfMessagesToPutOut = threshold - poolSize;
			
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			
			collectedMessages.add(mixMessage);
			
			if (collectedMessages.size() == threshold)
				putOutMessages();

		}
		
		
		public void putOutMessages() {
			
			for (int i=0; i<numberOfMessagesToPutOut; i++) {
					
				int chosen = secureRandom.nextInt(collectedMessages.size());
					
				if (isRequestPool)
					mix.putOutRequest(collectedMessages.remove(chosen));
				else
					mix.putOutReply(collectedMessages.remove(chosen));

			}
	
		}
		
	}
	
}
