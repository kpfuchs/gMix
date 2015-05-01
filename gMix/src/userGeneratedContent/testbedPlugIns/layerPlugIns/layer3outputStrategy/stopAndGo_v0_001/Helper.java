/*******************************************************************************
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2014  SVS
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
 *******************************************************************************/
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer3outputStrategy.stopAndGo_v0_001;

import java.security.SecureRandom;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ExponentialDistributionImpl;

import staticContent.framework.clock.Clock;
import staticContent.framework.config.Settings;


public class Helper {

	private int MIN_INTER_MIX_DELAY;
	private int MAX_INTER_MIX_DELAY;
	private int MIN_CLIENT_MIX_DELAY;
	private int MAX_CLIENT_MIX_DELAY;
	private int MAX_CLOCK_DEVITION;
	private ExponentialDistributionImpl expDist;
	private static SecureRandom secureRandom = new SecureRandom();
	private double SECURITY_PARAMETER_MU;
	private boolean USE_TIMESTAMPS;
	private Clock clock;
	
	
	public Helper(Settings settings, Clock clock) {
		this.MIN_INTER_MIX_DELAY = settings.getPropertyAsInt("STOP_AND_GO_MIN_INTER_MIX_DELAY");
		this.MAX_INTER_MIX_DELAY = settings.getPropertyAsInt("STOP_AND_GO_MAX_INTER_MIX_DELAY");
		this.MAX_INTER_MIX_DELAY = settings.getPropertyAsInt("STOP_AND_GO_MAX_INTER_MIX_DELAY");
		this.MIN_CLIENT_MIX_DELAY = settings.getPropertyAsInt("STOP_AND_GO_MIN_CLIENT_MIX_DELAY");
		this.MAX_CLIENT_MIX_DELAY = settings.getPropertyAsInt("STOP_AND_GO_MAX_CLIENT_MIX_DELAY");
		this.MAX_CLOCK_DEVITION = settings.getPropertyAsInt("STOP_AND_GO_MAX_CLOCK_DEVITION");
		this.SECURITY_PARAMETER_MU = settings.getPropertyAsDouble("STOP_AND_GO_SECURITY_PARAMETER_MU");
		this.USE_TIMESTAMPS = settings.getPropertyAsBoolean("STOP_AND_GO_USE_TIMESTAMPS");
		this.expDist = new ExponentialDistributionImpl(1d/SECURITY_PARAMETER_MU);
		this.clock = clock;
	}
	
	
	public byte[][] createHeaders(int numberOfMixes) {
		byte[][] result = new byte[numberOfMixes][];
		this.expDist.reseedRandomGenerator(secureRandom.nextLong());
		// draw delays from sample
		int[] delays = new int[numberOfMixes];
		for (int i=0; i<numberOfMixes; i++) {
			try {
				delays[i] = (int)Math.round(expDist.sample() * 1000d);
			} catch (MathException e) {
				e.printStackTrace();
				throw new RuntimeException("ERROR: could not draw sample from exponential distribution!"); 
			}
		}
		// store delays and create timestamps if needed
		for (int i=0; i<numberOfMixes; i++) {
			if (USE_TIMESTAMPS) {
				result[i] = new TimestampHeader(
						getMinTimestamp(i, delays), 
						getMaxTimestamp(i, delays), 
						delays[i]
						).headerAsArray;
			} else {
				result[i] = new TimestampHeader(delays[i]).headerAsArray;
			}
		}
		return result;
	}
	
	
	// mixNumer: 0,1,...,numberOfMixes
	private long getMinTimestamp(int mixNumber, int[] sgDelays) {
		int sumOfSgDelays = 0;
		for (int i=0; i<mixNumber; i++)
			sumOfSgDelays += sgDelays[i];
		int minDelay = MIN_CLIENT_MIX_DELAY + mixNumber * MIN_INTER_MIX_DELAY;
		int maxClockDev = (mixNumber + 1) * MAX_CLOCK_DEVITION;
		return clock.getTime() + sumOfSgDelays + minDelay - maxClockDev;
	}
	
	
	private long getMaxTimestamp(int mixNumber, int[] sgDelays) {
		int sumOfSgDelays = 0;
		for (int i=0; i<mixNumber; i++)
			sumOfSgDelays += sgDelays[i];
		int maxDelay = MAX_CLIENT_MIX_DELAY + mixNumber * MAX_INTER_MIX_DELAY;
		int maxClockDev = (mixNumber + 1) * MAX_CLOCK_DEVITION;
		return clock.getTime() + sumOfSgDelays + maxDelay + maxClockDev;
	}
	
}
