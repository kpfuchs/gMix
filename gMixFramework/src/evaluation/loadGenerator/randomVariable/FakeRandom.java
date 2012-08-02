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


public class FakeRandom extends RandomVariable {

	private final double doubleValue;
	private final int intValue;
	private final boolean isDouble;
	
	
	public FakeRandom(double value) {
		super(null);
		this.doubleValue = value;
		this.intValue = 0;
		this.isDouble = true;
	}
	
	
	public FakeRandom(int value) {
		super(null);
		this.intValue = value;
		this.doubleValue = 0;
		this.isDouble = false;
	}
	

	@Override
	public double drawDoubleSample() {
		if (isDouble)
			return doubleValue;
		else 
			throw new RuntimeException("double not supported"); 
	}

	
	@Override
	public int drawIntSample() {
		if (isDouble)
			throw new RuntimeException("double not supported");
		else 
			return intValue;
	}
	
}