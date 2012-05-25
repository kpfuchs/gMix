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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import message.MixMessage;
import message.Reply;
import message.Request;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;


import framework.Implementation;


//Diaz 2003 ("Generalising Mixes")
//"a timed pool mix that tosses coins and uses a probability function that 
//depends on the number of messages inside the mix at the time of flushing"
//-> normal cumulative distribution
public class BinomialPool_v1 extends Implementation implements OutputStrategy {

	private static SecureRandom secureRandom = new SecureRandom();
	private SimplexBinomialPool requestPool;
	private SimplexBinomialPool replyPool;

	
	@Override
	public void constructor() {
		try {
			BinomialPool_v1.secureRandom = SecureRandom.getInstance(settings.getProperty("PRNG_ALGORITHM"));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException("could not init secureRandom"); 
		}
		int sendingRate = settings.getPropertyAsInt("BinomialPool_v1_SENDING_RATE");
		double maxOutputFraction = settings.getPropertyAsDouble("BinomialPool_v1_MAX_OUTPUT_FRACTION");
		double mean = settings.getPropertyAsDouble("BinomialPool_v1_MEAN");
		double stdDev = settings.getPropertyAsDouble("BinomialPool_v1_STDDEV");
		this.requestPool = new SimplexBinomialPool(true, sendingRate, maxOutputFraction, mean, stdDev);
		this.replyPool = new SimplexBinomialPool(false, sendingRate, maxOutputFraction, mean, stdDev);

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

	
	public class SimplexBinomialPool {

		private boolean isRequestPool;
		private Vector<MixMessage> collectedMessages;
		private boolean isFirstMessage = true;
		private int sendingRate;
		private double maxOutputFraction;
		private NormalDistributionImpl normalDist;
		private Timer timer = new Timer();
		
		
		public SimplexBinomialPool(	boolean isRequestPool, 
				int sendingRate,
				double maxOutputFraction, 
				double mean, 
				double stdDev) {
			
			this.collectedMessages = new Vector<MixMessage>(100);	// TODO: poperty file
			this.isRequestPool = isRequestPool;
			this.sendingRate = sendingRate;
			this.maxOutputFraction = maxOutputFraction;
			this.normalDist = new NormalDistributionImpl(mean, stdDev);
			this.normalDist.reseedRandomGenerator(secureRandom.nextLong());
			
		}
		
		
		public void addMessage(MixMessage mixMessage) {
			
			synchronized (this) {
				
				if (isFirstMessage) {
					isFirstMessage = false;
					timer.scheduleAtFixedRate(new TimeoutTask(this), sendingRate, sendingRate);
				}
				
				collectedMessages.add(mixMessage);
				
			}

		}

		
		public void putOutMessages() {
			
			synchronized (this) {
				
				try {
					
					double coinBias = maxOutputFraction * normalDist.cumulativeProbability(collectedMessages.size());
					
					for (int i=0; i<collectedMessages.size(); i++) {
						
						MixMessage m = collectedMessages.get(i);
						
						if (secureRandom.nextDouble() <= coinBias) {
							
							if (isRequestPool)
								controller.getInputOutputHandler().addRequest((Request)m);
							else
								controller.getInputOutputHandler().addReply((Reply)m);
							
							collectedMessages.remove(i--);
							
						}
						
					}
					
				} catch (MathException e) {
					throw new RuntimeException("ERROR: problem with generating the biased coin! " +e.getStackTrace()); 
				}
				
			}
	
		}
	
		
		private final class TimeoutTask extends TimerTask {

			private SimplexBinomialPool linkedPool;
			
			protected TimeoutTask(SimplexBinomialPool linkedPool) {
				this.linkedPool = linkedPool;
			}
			
			@Override 
			public void run() {
				linkedPool.putOutMessages();
			}
			
		}
			
	}

}
