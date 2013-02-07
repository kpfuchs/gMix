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
package evaluation.simulator.trafficSource;

import java.security.SecureRandom;


public class NormalizedPareto {

	private final double ALPHA; // shape parameter
	private final double GAMMA;
	private final SecureRandom random = new SecureRandom();
	

	/**
	 * normalized Pareto distribution
	 * @param alpha the shape parameter
	 * @param avg avg result when drawing a sample
	 */
	public NormalizedPareto(double alpha, double avg) {
		if (alpha <= 0)
			throw new RuntimeException("alpha must be > 0");
		if (avg <= 0)
			throw new RuntimeException("avg must be > 0");
		this.ALPHA = alpha;
		this.GAMMA = 1d/(avg*(ALPHA-1d));
	}
	
	
	
	public double drawSample() {
		return (Math.pow(1d-random.nextDouble(), -1d/ALPHA) -1d)/GAMMA;
	}
	
	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		SecureRandom random = new SecureRandom();
		for (int i=0; i<10000; i++) {
			double val = Math.pow(1d-random.nextDouble(), -1d/2.0) -1d;
			if (val < 0.050)
				System.out.println(val); 
		} 
	} 

}
