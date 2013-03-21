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
package evaluation.simulator.plugins.trafficSource;

//import java.math.BigDecimal;
import java.security.SecureRandom;

public class Pareto {

	private final double ALPHA; // shape parameter
	private final double MIN; // scale parameter
	private final SecureRandom random = new SecureRandom();
	
	
	/**
	 * Pareto distribution
	 * @param alpha the shape parameter
	 * @param min the scale parameter (min value)
	 */
	public Pareto(double alpha, double min) {
		if (alpha <= 0)
			throw new RuntimeException("alpha must be > 0");
		if (min <= 0)
			throw new RuntimeException("min must be > 0");
		this.ALPHA = alpha;
		this.MIN = min;
	}
	
	
	public double drawSample() {
		//return ALPHA * Math.pow(random.nextDouble(), -1d/ALPHA);
		return MIN / Math.pow(1d-random.nextDouble(), 1d/ALPHA);
		//return Math.pow(1d-random.nextDouble(), -1d/ALPHA) -1d;
	}
	
	public double drawSample(double avg) {
		//double gamma = 1d/((ALPHA-1d)*avg);
		//return (Math.pow(1d-rand(), (-1d/ALPHA))-1d) / gamma;
		//avg = 1/(gamma*(alpha-1)); -> gamma = 1/((alpha-1)*avg)
		//sample = ((1-rand(M, N)).^(-1/alpha)-1)./gamma;
		
		
		double min = (avg*(ALPHA-1d))/ALPHA;
		//System.out.println("min: " +min); 
		//System.out.println("res: " +(min / Math.pow(rand(), 1d/ALPHA))); 
		return min / Math.pow(rand(), 1d/ALPHA);
		
		//return ALPHA * Math.pow(random.nextDouble(), -1d/ALPHA);
		//return Math.pow(1d-random.nextDouble(), -1d/ALPHA) -1d;
		
	}
	
	private final static SecureRandom ran = new SecureRandom();
	
	public static double rand() {
		double result = 0d;
		do {
			result = ran.nextDouble();
		} while (result == 0d);
		return result;
	}
	
	
	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	/*public static void main(String[] args) {
		double MIN = 0.05;
		double ALPHA = 2.0;
		SecureRandom random = new SecureRandom();
		for (int i=0; i<10000; i++) {
			double val = MIN / Math.pow(random.nextDouble(), 1d/ALPHA);
			if (val < 0.06)
				System.out.println(val); 
		} 
	} */
	
	
	public static void dblpVsWiki() {
		double alpha = 1.5d;
		double xm = 1.0d;
		
		for (int i=10; i<100; i++)
			System.out.println((double)i*0.1d +": " +getProbabilityDLPA((double)i*0.1d, alpha, xm)); 
		System.out.println(); 
		for (int i=10; i<100; i++)
			System.out.println((double)i*0.1d +": " +getProbabilityWiki((double)i*0.1d, alpha, xm)); 
		System.out.println(); 
		
		System.out.println(getProbabilityDLPA(1, alpha, xm));
		System.out.println(getProbabilityWiki(1, alpha, xm));
	}
	
	public static double getProbabilityDLPA(double x, double alpha, double xm) {
		return 1d - Math.pow(x/xm, (-1d*alpha));
	}
	
	public static double getProbabilityWiki(double x, double alpha, double xm) {
		return Math.pow(xm/x, alpha);
	}

	/*public double pdf(double x) {
		if (x < MIN)
			return 0d;
		else
			return ALPHA * (Math.pow(MIN, ALPHA)/Math.pow(x, ALPHA + 1d));
		//return ALPHA*Math.pow(MIN, ALPHA)*Math.pow(x, -(1d + ALPHA));
	}*/
	
	

	/*public double cdf(double x) {
		if (x < MIN)
			return 0d;
		else
			return 1d - Math.pow(MIN/x, ALPHA);
	}*/

}
