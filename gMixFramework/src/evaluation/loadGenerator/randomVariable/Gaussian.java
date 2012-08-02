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
package evaluation.loadGenerator.randomVariable;

public class Gaussian extends RandomVariable {

	private final String errorMessage =
			"invalid syntax for random variable "
			+this.getClass().getName()
			+"; distribution variable must be in the following form: "
			+"[RAND_VAR:Gaussian(double mu, double sigma)]";
	
	private final int numberOfParameters = 2;
	private final double mu;
	private final double sigma;
	
	
	public Gaussian(String[] parameters) {
		super(parameters);
		if (parameters.length != numberOfParameters)
			throw new RuntimeException(errorMessage); 
		try {
			this.mu = Double.parseDouble(parameters[0]); 
			this.sigma = Double.parseDouble(parameters[1]); 
			// test:
			this.randomDataImpl.nextGaussian(mu, sigma);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(errorMessage); 
		}
	}
	

	@Override
	public double drawDoubleSample() {
		return randomDataImpl.nextGaussian(mu, sigma);
	}

}