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
package plugIns.layer3outputStrategy.binomialPool_v0_001;

import java.security.SecureRandom;
import java.util.Vector;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer3OutputStrategyMix;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;


//Diaz 2003 ("Generalising Mixes")
//"a timed pool mix that tosses coins and uses a probability function that 
//depends on the number of messages inside the mix at the time of flushing"
//-> normal cumulative distribution
public class MixPlugIn extends Implementation implements Layer3OutputStrategyMix {

	private static SecureRandom secureRandom = new SecureRandom();
	private SimplexBinomialPool requestPool;
	private SimplexBinomialPool replyPool;
	private int DEFAULT_POOL_SIZE;
	private int SENDING_RATE;
	private double MAX_OUTPUT_FRACTION;
	private double MEAN;
	private double STDDEV;
	
	
	@Override
	public void constructor() {
		this.DEFAULT_POOL_SIZE = settings.getPropertyAsInt("BINOMIAL_POOL_DEFAULT_POOL_SIZE");
		this.SENDING_RATE = settings.getPropertyAsInt("BINOMIAL_POOL_SENDING_RATE");
		this.MAX_OUTPUT_FRACTION = settings.getPropertyAsDouble("BINOMIAL_POOL_MAX_OUTPUT_FRACTION");
		this.MEAN = settings.getPropertyAsDouble("BINOMIAL_POOL_MEAN");
		this.STDDEV = settings.getPropertyAsDouble("BINOMIAL_POOL_STDDEV");
		this.requestPool = new SimplexBinomialPool(true);
		this.replyPool = new SimplexBinomialPool(false);
	}
	
	
	@Override
	public void initialize() {
		// no need to do anything
	}

	
	@Override
	public void begin() {
		// no need to do anything
	}

	
	@Override
	public void addRequest(Request request) {
		requestPool.addMessage((MixMessage) request);
	}

	
	@Override
	public void addReply(Reply reply) {
		replyPool.addMessage((MixMessage) reply);
	}

	
	public class SimplexBinomialPool implements Runnable {

		private boolean isRequestPool;
		private Vector<MixMessage> collectedMessages;
		private boolean isFirstMessage = true;
		private NormalDistributionImpl normalDist;
		private ScheduledThreadPoolExecutor scheduler;
		
		
		public SimplexBinomialPool(	boolean isRequestPool) {
			this.collectedMessages = new Vector<MixMessage>(DEFAULT_POOL_SIZE);
			this.isRequestPool = isRequestPool;
			this.scheduler = new ScheduledThreadPoolExecutor(1);
			this.normalDist = new NormalDistributionImpl(MEAN, STDDEV);
			this.normalDist.reseedRandomGenerator(secureRandom.nextLong());
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			synchronized (this) {
				if (isFirstMessage) {
					isFirstMessage = false;
					scheduler.scheduleAtFixedRate(this, SENDING_RATE, SENDING_RATE, TimeUnit.MILLISECONDS);
				}
				collectedMessages.add(mixMessage);
			}
		}

		
		public void putOutMessages() {
			synchronized (this) {
				try {
					double coinBias = MAX_OUTPUT_FRACTION * normalDist.cumulativeProbability(collectedMessages.size());
					for (int i=0; i<collectedMessages.size(); i++) {
						MixMessage m = collectedMessages.get(i);
						if (secureRandom.nextDouble() <= coinBias) {
							if (isRequestPool)
								anonNode.putOutRequest((Request)m);
							else
								anonNode.putOutReply((Reply)m);
							collectedMessages.remove(i--);
						}
					}
				} catch (MathException e) {
					throw new RuntimeException("ERROR: problem with generating the biased coin! " +e.getStackTrace()); 
				}
			}
		}

		
		@Override
		public void run() {
			putOutMessages();
		}
			
	}

	
	@Override
	public int getMaxSizeOfNextReply() {
		return super.recodingLayerMix.getMaxSizeOfNextReply();
	}


	@Override
	public int getMaxSizeOfNextRequest() {
		return super.recodingLayerMix.getMaxSizeOfNextRequest();
	}
	
}
