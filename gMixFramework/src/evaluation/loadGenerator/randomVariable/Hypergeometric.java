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

import org.apache.commons.math.MathException;

public class Hypergeometric extends RandomVariable {

	private final String errorMessage =
			"invalid syntax for random variable "
			+this.getClass().getName()
			+"; distribution variable must be in the following form: "
			+"[RAND_VAR:Hypergeometric(int populationSize, int numberOfSuccesses, int sampleSize)]";
	
	private final int numberOfParameters = 3;
	private final int populationSize;
	private final int numberOfSuccesses;
	private final int sampleSize;
	
	
	public Hypergeometric(String[] parameters) {
		super(parameters);
		if (parameters.length != numberOfParameters)
			throw new RuntimeException(errorMessage); 
		try {
			this.populationSize = Integer.parseInt(parameters[0]); 
			this.numberOfSuccesses = Integer.parseInt(parameters[1]); 
			this.sampleSize = Integer.parseInt(parameters[2]); 
			// test:
			this.randomDataImpl.nextHypergeometric(populationSize, numberOfSuccesses, sampleSize);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(errorMessage); 
		}
	}
	

	@Override
	public int drawIntSample() {
		try {
			return randomDataImpl.nextHypergeometric(populationSize, numberOfSuccesses, sampleSize);
		} catch (MathException e) {
			e.printStackTrace();
			throw new RuntimeException(errorMessage); 
		}
	}

}