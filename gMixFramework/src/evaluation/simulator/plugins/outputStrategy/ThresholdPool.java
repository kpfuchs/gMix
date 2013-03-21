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

import java.security.SecureRandom;
import java.util.Vector;


import evaluation.simulator.Simulator;
import evaluation.simulator.core.message.MixMessage;
import evaluation.simulator.core.networkComponent.AbstractClient;
import evaluation.simulator.core.networkComponent.Mix;
import evaluation.simulator.pluginRegistry.ClientSendStyle;
import evaluation.simulator.pluginRegistry.MixSendStyle;
import evaluation.simulator.plugins.clientSendStyle.ClientSendStyleImpl;
import evaluation.simulator.plugins.mixSendStyle.MixSendStyleImpl;



// Dingledine 2002:
// The mix fires when n + f messages accumulate in the mix. A pool of f 
// messages, chosen uniformly at random from all the messages, is retained in 
// the mix. (Consider these messages as feedback into the mix.) The other n 
// are forwarded on.
public class ThresholdPool extends OutputStrategyImpl {

	private static SecureRandom secureRandom = new SecureRandom();
	private SimplexTresholdPool requestPool;
	private SimplexTresholdPool replyPool;
	
	
	public ThresholdPool(Mix mix, Simulator simulator) {
		super(mix, simulator);
		int poolSize = Simulator.settings.getPropertyAsInt("THRESHOLD_POOL_POOL_SIZE");
		int threshold = Simulator.settings.getPropertyAsInt("THRESHOLD_POOL_THRESHOLD");
		this.requestPool = new SimplexTresholdPool(true, poolSize, threshold);
		this.replyPool = new SimplexTresholdPool(false, poolSize, threshold);
	}

	
	@Override
	public void incomingRequest(MixMessage mixMessage) {		
		requestPool.addMessage(mixMessage);
	}


	@Override
	public void incomingReply(MixMessage mixMessage) {
		replyPool.addMessage(mixMessage);
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
	
			collectedMessages.clear();
			
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
