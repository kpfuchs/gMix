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
package evaluation.traceParser.statistics.calculator;

import framework.core.util.Util;

public class Stat {

	private AvgCalculator avg = new AvgCalculator();
	private MinCalculator min = new MinCalculator();
	private MaxCalculator max = new MaxCalculator();
	
	
	public void addValue(double value) {
		avg.addValue(value);
		min.addValue(value);
		max.addValue(value);
	}
	

	public double getAvg() {
		return avg.getResult();
	}
	
	
	public String getAvgHumanReadableByte() {
		return returnHumanReadableByte(avg.getResult());
	}
	
	
	public double getMin() {
		return min.getResult();
	}
	
	
	public String getMinHumanReadableByte() {
		return returnHumanReadableByte(min.getResult());
	}
	
	
	public double getMinLargerZero() {
		return min.getMinLargerZero();
	}
	
	
	public double getMax() {
		return max.getResult();
	}
	
	
	public String getMaxHumanReadableByte() {
		return returnHumanReadableByte(max.getResult());
	}
	
	
	public double getSum() {
		return avg.getSum();
	}
	
	
	public String getSumHumanReadableByte() {
		return returnHumanReadableByte(avg.getSum());
	}
	
	
	public boolean containsValues() {
		return min.containsValues();
	}
	
	
	private String returnHumanReadableByte(double value) {
		if (value < 1000d)
			return "" + value +" B";
		else
			return Util.humanReadableByteCount(Math.round(value), false);
	}
	
}
