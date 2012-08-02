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

import org.apache.commons.math.random.RandomDataImpl;

import framework.core.util.Util;

public abstract class RandomVariable {

	protected String[] parameters;
	protected RandomDataImpl randomDataImpl;
	
	
	public static RandomVariable createRandomVariable(String signature) {
		String distribuionName = Util.getTextBetweenAAndB(signature, ":", "(");
		String parameters = Util.getTextBetweenAAndB(signature, "(", ")");
		parameters = parameters.replace(" ", "");
		String[] splitted = parameters.split(","); 
		
		// TODO: add classloader + possibility for custom (=user-made) variables (specify class name in loadGeneratorConfig.txt instead of random variable name)
		if (distribuionName.equalsIgnoreCase("Binomial"))
			return new Binomial(splitted);
		else if (distribuionName.equalsIgnoreCase("Exponential"))
			return new Exponential(splitted);
		else if (distribuionName.equalsIgnoreCase("Gaussian"))
			return new Gaussian(splitted);
		else if (distribuionName.equalsIgnoreCase("Hypergeometric"))
			return new Hypergeometric(splitted);
		else if (distribuionName.equalsIgnoreCase("Poisson"))
			return new Poisson(splitted);
		else if (distribuionName.equalsIgnoreCase("UniformDouble"))
			return new UniformDouble(splitted);
		else if (distribuionName.equalsIgnoreCase("UniformInt"))
			return new UniformInt(splitted);
		else if (distribuionName.equalsIgnoreCase("Zipf"))
			return new Zipf(splitted);
		else if (distribuionName.equalsIgnoreCase("Weibull"))
			return new Weibull(splitted);
		else
			throw new RuntimeException(
					"not supported random variable specified in " +
					"loadGeneratorConfig.txt: " +signature
				); 
	}
	
	
	public static boolean isRandomVariable(String signature) {
		return signature.contains("[RAND_VAR:");
	}
	
	
	protected RandomVariable(String[] parameters) {
		this.parameters = parameters;
		this.randomDataImpl = new RandomDataImpl();
		this.randomDataImpl.reSeedSecure();
	}
	
	
	public double drawDoubleSample() {
		throw new RuntimeException("double not supported"); 
	}
	
	
	public int drawIntSample() {
		throw new RuntimeException("int not supported"); 
	}

}
