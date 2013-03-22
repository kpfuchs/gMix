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
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;


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


// Diaz 2003 ("Generalising Mixes")
// "a timed pool mix that tosses coins and uses a probability function that 
// depends on the number of messages inside the mix at the time of flushing"
// -> normal cumulative distribution
public class BinomialPool extends OutputStrategyImpl {

	private SimplexBinomialPool requestBatch;
	private SimplexBinomialPool replyBatch;
	private static SecureRandom secureRandom = new SecureRandom();
	
	
	public BinomialPool(Mix mix, Simulator simulator) {

		super(mix, simulator);
		int sendingInterval = Simulator.settings.getPropertyAsInt("BINOMIAL_POOL_SENDING_INTERVAL_IN_MS");
		double maxOutputFraction = Simulator.settings.getPropertyAsDouble("BINOMIAL_POOL_MAX_OUTPUT_FRACTION");
		double mean = Simulator.settings.getPropertyAsDouble("BINOMIAL_POOL_MEAN");
		double stdDev = Simulator.settings.getPropertyAsDouble("BINOMIAL_POOL_STANDARD_DEVIATION");
		this.requestBatch = new SimplexBinomialPool(true, sendingInterval, maxOutputFraction, mean, stdDev);
		this.replyBatch = new SimplexBinomialPool(false, sendingInterval, maxOutputFraction, mean, stdDev);
	}
	
	
	@Override
	public void incomingRequest(MixMessage mixMessage) {
		requestBatch.addMessage(mixMessage);
	}


	@Override
	public void incomingReply(MixMessage mixMessage) {
		replyBatch.addMessage(mixMessage);
	}
	
	
	public class SimplexBinomialPool implements EventExecutor {
		
		private boolean isRequestPool;
		private Vector<MixMessage> collectedMessages;
		private boolean isFirstMessage = true;
		private int sendingInterval;
		private double maxOutputFraction;
		private NormalDistributionImpl normalDist;
		
		
		public SimplexBinomialPool(	boolean isRequestPool, 
									int sendingInterval,
									double maxOutputFraction, 
									double mean, 
									double stdDev) {
			
			this.collectedMessages = new Vector<MixMessage>(simulator.getClients().size()*2);	
			this.isRequestPool = isRequestPool;
			this.sendingInterval = sendingInterval;
			this.maxOutputFraction = maxOutputFraction;
			this.normalDist = new NormalDistributionImpl(mean, stdDev);
			this.normalDist.reseedRandomGenerator(secureRandom.nextLong());
			
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			
			if (isFirstMessage) {
				isFirstMessage = false;
				scheduleNextOutput();
			}
			
			collectedMessages.add(mixMessage);

		}

		
		public void putOutMessages() {
			
			try {
				
				double coinBias = maxOutputFraction * normalDist.cumulativeProbability(collectedMessages.size());
				
				for (int i=0; i<collectedMessages.size(); i++) {
					
					MixMessage m = collectedMessages.get(i);
					
					if (secureRandom.nextDouble() <= coinBias) {
						
						if (isRequestPool)
							mix.putOutRequest(m);
						else
							mix.putOutReply(m);
						
						collectedMessages.remove(i--);
						
					}
					
				}
				
			} catch (MathException e) {
				throw new RuntimeException("ERROR: problem with generating the biased coin! " +e.getStackTrace()); 
			}
					
		}
		
		
		private void scheduleNextOutput() {
			
			simulator.scheduleEvent(new Event(this, Simulator.getNow() + sendingInterval, OutputStrategyEvent.TIMEOUT), this);
		
		}

		
		@Override
		public void executeEvent(Event e) {
			
			if (e.getEventType() == OutputStrategyEvent.TIMEOUT) {
				putOutMessages();
				scheduleNextOutput();
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
