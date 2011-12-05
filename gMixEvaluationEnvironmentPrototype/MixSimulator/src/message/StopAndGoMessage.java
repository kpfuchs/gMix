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

package message;

import java.security.SecureRandom;

import networkComponent.Client;
import networkComponent.NetworkNode;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ExponentialDistributionImpl;
import simulator.Settings;
import simulator.Simulator;


public class StopAndGoMessage extends BasicMixMessage {

	private int[] tsMin; // 0: first mix; 1: second mix...
	private int[] tsMax;
	public int[] delay;
	private int tsMinCounter = 0;
	private int tsMaxCounter = 0;
	private int delayCounter = 0;
	private ExponentialDistributionImpl expDist;
	private static SecureRandom secureRandom = new SecureRandom();
	private int identifier;
	private static int idCounter = 0;
	
	
	protected StopAndGoMessage(boolean isRequest, NetworkNode source,
			NetworkNode destination, Client owner, int creationTime,
			boolean isDummy) {
		
		super(isRequest, source, destination, owner, creationTime, isDummy);
		
		this.identifier = idCounter++;
		
		// generate delays
		double securityParameterMu = Settings.getPropertyAsDouble("SGMIX_SECURITY_PARAMETER_MU");
		Simulator simulator = Simulator.getSimulator();
		int numberOfMixes = simulator.getMixes().size();
		this.delay = new int[numberOfMixes];
		this.expDist = new ExponentialDistributionImpl(1d/securityParameterMu);
		this.expDist.reseedRandomGenerator(secureRandom.nextLong());
		boolean useTimeStamps = Settings.getPropertyAsBoolean("SGMIX_USE_TIMESTAMPS");
		
		for (int i=0; i<numberOfMixes; i++) {
			
			try {
				delay[i] = (int)Math.round(expDist.sample() * 1000d);
			} catch (MathException e) {
				e.printStackTrace();
				throw new RuntimeException("ERROR: could not draw sample from exponential distribution!"); 
			}

		}
		
		if (useTimeStamps) {
			
			int minInterMixDelay = Settings.getPropertyAsInt("SGMIX_MIN_INTER_MIX_DELAY");
			int maxInterMixDelay = Settings.getPropertyAsInt("SGMIX_MAX_INTER_MIX_DELAY");
			int minClientMixDelay = Settings.getPropertyAsInt("SGMIX_MIN_CLIENT_MIX_DELAY");
			int maxClientMixDelay = Settings.getPropertyAsInt("SGMIX_MAX_CLIENT_MIX_DELAY");
			int maxClockDeviation = Settings.getPropertyAsInt("SGMIX_MAX_CLOCK_DEVITION");
			
			tsMin = new int[numberOfMixes];
			tsMax = new int[numberOfMixes];
				
			for (int i=0; i<numberOfMixes; i++) {
				if (isRequest) {
					tsMin[i] = getMinTimestampForRequest(i, maxClockDeviation, delay, minInterMixDelay, minClientMixDelay);
					tsMax[i] = getMaxTimestampForRequest(i, maxClockDeviation, delay, maxInterMixDelay, maxClientMixDelay);
				} else {
					tsMin[i] = getMinTimestampForReply(i, maxClockDeviation, delay, minInterMixDelay, minClientMixDelay);
					tsMax[i] = getMaxTimestampForReply(i, maxClockDeviation, delay, maxInterMixDelay, maxClientMixDelay);
				}
				
			}
			
		}
		
	}

	
	// mixNumer: 0,1,...,numberOfMixes
	public static int getMinTimestampForRequest(	int mixNumber, 
													int maxClockDeviation,
													int[] sgDelays, 
													int minInterMixDelay, 
													int minClientMixDelay) {
		
		int sumOfSgDelays = 0;
		for (int i=0; i<mixNumber; i++)
			sumOfSgDelays += sgDelays[i];
		
		int minDelay = minClientMixDelay + mixNumber * minInterMixDelay;
		int maxClockDev = (mixNumber + 1) * maxClockDeviation;

		return Simulator.getNow() + sumOfSgDelays + minDelay - maxClockDev;
	
	}
	
	
	public static int getMaxTimestampForRequest(	int mixNumber, 
													int maxClockDeviation,
													int[] sgDelays, 
													int maxInterMixDelay, 
													int maxClientMixDelay) {

		int sumOfSgDelays = 0;
		for (int i=0; i<mixNumber; i++)
			sumOfSgDelays += sgDelays[i];

		int maxDelay = maxClientMixDelay + mixNumber * maxInterMixDelay;
		int maxClockDev = (mixNumber + 1) * maxClockDeviation;

		return Simulator.getNow() + sumOfSgDelays + maxDelay + maxClockDev;

	}
	
	
	public static int getMinTimestampForReply(	int mixNumber, 
												int maxClockDeviation,
												int[] sgDelays, 
												int minInterMixDelay, 
												int minClientMixDelay) {

		if (mixNumber == 0)
			return Integer.MIN_VALUE; 
		
		int sumOfSgDelays = 0;
		for (int i=0; i<mixNumber; i++)
			sumOfSgDelays += sgDelays[i];

		int minDelay = mixNumber * minInterMixDelay;
		int maxClockDev = mixNumber * maxClockDeviation;

		if (mixNumber == sgDelays.length-1)
			minDelay = minDelay - minInterMixDelay + minClientMixDelay;
		
		return Simulator.getNow() + sumOfSgDelays + minDelay - maxClockDev;

	}
	
	
	public static int getMaxTimestampForReply(	int mixNumber, 
												int maxClockDeviation,
												int[] sgDelays, 
												int maxInterMixDelay, 
												int maxClientMixDelay) {

		if (mixNumber == 0)
			return Integer.MAX_VALUE;  

		int sumOfSgDelays = 0;
		for (int i=0; i<mixNumber; i++)
			sumOfSgDelays += sgDelays[i];

		int maxDelay = mixNumber * maxInterMixDelay;
		int maxClockDev = mixNumber * maxClockDeviation;

		if (mixNumber == sgDelays.length-1)
			maxDelay = maxDelay - maxInterMixDelay + maxClientMixDelay;

		return Simulator.getNow() + sumOfSgDelays + maxDelay - maxClockDev;

	}

	
	// call only once per mix!
	public int getTsMin() {
		return tsMin[tsMinCounter++];
	}
	
	// call only once per mix!
	public int getTsMax() {
		return tsMax[tsMaxCounter++];
	}

	// call only once per mix!
	public int getDelay() {
		return delay[delayCounter++];
	}
	
	
	public String toString() {
		
		String replyOrRequest = super.isRequest() ? "Request" : "Reply";
		
		
		String timeInfo = " ";
		for (int i=0; i<tsMin.length; i++)
			timeInfo += ""+i +": [min:" +tsMin[i] +", expected: "+delay[i] +", max: "+tsMax[i] +"] ";
		
		return "MixMessage ("+replyOrRequest +", owner: "+owner +", id: " +identifier +timeInfo+")";
		
	}
	
}
