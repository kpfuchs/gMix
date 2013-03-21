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

import evaluation.simulator.Simulator;
import evaluation.simulator.core.message.MixMessage;
import evaluation.simulator.core.networkComponent.AbstractClient;
import evaluation.simulator.core.networkComponent.Mix;
import evaluation.simulator.pluginRegistry.ClientSendStyle;
import evaluation.simulator.pluginRegistry.MixSendStyle;
import evaluation.simulator.plugins.clientSendStyle.ClientSendStyleImpl;
import evaluation.simulator.plugins.mixSendStyle.MixSendStyleImpl;


// Cottrell 1995 ("Mixmaster & Remailer Attacks")
// collect incoming messages until "poolSize" messages are reached
// for each further message:
// put new message in pool and randomly chose and put out one message 
// see also: ThresholdPool.java (CottrellPool is a ThresholdPool with n=1)
public class CottrellPool extends OutputStrategyImpl {

	private static SecureRandom secureRandom = new SecureRandom();
	private SimplexCottrellPool requestPool;
	private SimplexCottrellPool replyPool;
	
	
	public CottrellPool(Mix mix, Simulator simulator) {
		super(mix, simulator);
		int poolSize = Simulator.settings.getPropertyAsInt("COTTRELL_POOL_POOL_SIZE");
		this.requestPool = new SimplexCottrellPool(true, poolSize);
		this.replyPool = new SimplexCottrellPool(false, poolSize);
	}

	
	@Override
	public void incomingRequest(MixMessage mixMessage) {		
		requestPool.addMessage(mixMessage);
	}


	@Override
	public void incomingReply(MixMessage mixMessage) {
		replyPool.addMessage(mixMessage);
	}
	
	
	public class SimplexCottrellPool {
		
		private boolean isRequestPool;
		private MixMessage[] collectedMessages;
		private int poolSize;
		private int nextFreeSlot = 0;
		
		
		public SimplexCottrellPool(boolean isRequestPool, int poolSize) {
			
			this.collectedMessages = new MixMessage[poolSize];
			this.isRequestPool = isRequestPool;
			this.poolSize = poolSize;
			
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			
			if (nextFreeSlot < poolSize) {
				
				collectedMessages[nextFreeSlot++] = mixMessage;
				
			} else {
				
				int chosen = secureRandom.nextInt(poolSize+1);
				
				if (chosen == poolSize) {
					
					putOutMessage(mixMessage);
				
				} else {
					
					putOutMessage(collectedMessages[chosen]);
					collectedMessages[chosen] = mixMessage;
					
				}
		
			}

		}
		
		
		private void putOutMessage(MixMessage mixMessage) {
			
			if (isRequestPool)
				mix.putOutRequest(mixMessage);
			else
				mix.putOutReply(mixMessage);
			
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
