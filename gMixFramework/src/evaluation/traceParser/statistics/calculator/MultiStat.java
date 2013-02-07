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


public class MultiStat {

	private double min_min;
	private double avg_min;
	private double max_min;
	
	private double min_avg;
	private double avg_avg;
	private double max_avg;
	
	private double min_max;
	private double avg_max;
	private double max_max;
	
	private Stat minStat;
	private Stat avgStat;
	private Stat maxStat;
	
	
	public MultiStat() {
		this.minStat = new Stat();
		this.avgStat = new Stat();
		this.maxStat = new Stat();
	}
	
	
	public void calculateValues() {
		this.min_min = minStat.getMin(); 
		this.avg_min = minStat.getAvg(); 
		this.max_min = minStat.getMax();
		this.minStat = null;
		this.min_avg = avgStat.getMin(); 
		this.avg_avg = avgStat.getAvg(); 
		this.max_avg = avgStat.getMax();
		this.avgStat = null;
		this.min_max = maxStat.getMin(); 
		this.avg_max = maxStat.getAvg(); 
		this.max_max = maxStat.getMax();
		this.maxStat = null;
	}
	
	
	public void addMinValue(double value) {
		minStat.addValue(value);
	}
	
	
	public void addAvgValue(double value) {
		avgStat.addValue(value);
	}
	
	
	public void addMaxValue(double value) {
		maxStat.addValue(value);
	}
	
	
	private String returnHumanReadableByte(double value) {
		if (value < 1000d)
			return "" + value +" B";
		else
			return Util.humanReadableByteCount(Math.round(value), false);
	}
	
	
	public double getMinMin() {
		return this.min_min;
	}
	
	
	public String getMinMinHumanReadableByte() {
		return returnHumanReadableByte(min_min);
	}
	
	
	public double getAvgMin() {
		return this.avg_min;
	}
	
	
	public String getAvgMinHumanReadableByte() {
		return returnHumanReadableByte(avg_min);
	}
	
	
	public double getMaxMin() {
		return this.max_min;
	}
	
	
	public String getMaxMinHumanReadableByte() {
		return returnHumanReadableByte(max_min);
	}
	
	
	public double getMinAvg() {
		return this.min_avg;
	}
	
	
	public String getMinAvgHumanReadableByte() {
		return returnHumanReadableByte(min_avg);
	}
	
	
	public double getAvgAvg() {
		return this.avg_avg;
	}
	
	
	public String getAvgAvgHumanReadableByte() {
		return returnHumanReadableByte(avg_avg);
	}
	
	
	public double getMaxAvg() {
		return this.max_avg;
	}
	
	
	public String getMaxAvgHumanReadableByte() {
		return returnHumanReadableByte(max_avg);
	}
	
	
	public double getMinMax() {
		return this.min_max;
	}
	
	
	public String getMinMaxHumanReadableByte() {
		return returnHumanReadableByte(min_max);
	}
	
	
	public double getAvgMax() {
		return this.avg_max;
	}
	
	
	public String getAvgMaxHumanReadableByte() {
		return returnHumanReadableByte(avg_max);
	}
	
	
	public double getMaxMax() {
		return this.max_max;
	}

	
	public String getMaxMaxHumanReadableByte() {
		return returnHumanReadableByte(max_max);
	}
}
