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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;


public class AdvancedStat extends Stat {

	private Vector<Double> tempValues = new Vector<Double>(1000);
	private double[] values = null;
	
	
	public void addValue(double value) {
		super.addValue(value);
		tempValues.add(value);
	}
	

	private void toArray() {
		if (values == null) {
			values = new double[tempValues.size()];
			Iterator<Double> iterator = tempValues.iterator();
			for (int i=0; i<values.length; i++)
				values[i] = iterator.next(); 
			tempValues.clear();
			Arrays.sort(values);
		}
	}
	
	
	public double[] getValues() {
		toArray();
		return values;
	}
	
	
	public void displayAll() {
		toArray();
		for (int i=0; i<values.length; i++)
			System.out.println(i +":\t" +values[i]); 
	}
	
	
	public double getMedianCeil() {
		toArray();
		if (values.length % 2 == 1)
			return values[(values.length / 2)-1];
		else
			return values[(values.length / 2)];
	}
	
	
	public double getMedianFloor() {
		toArray();
		return values[(values.length / 2)-1];
	}
	
	
	public double getMedianAvg() {  // == getPquantile(0.5d)
		toArray();
		if (values.length % 2 == 1) 
			return values[(values.length / 2)-1];
		else
			return (getMedianCeil() + getMedianFloor()) / 2d;
	}
	
	
	public double[] getQuartiles() {
		toArray();
		double[] result = new double[3];
		result[0] = getPquantile(25);
		result[1] = getPquantile(50);
		result[2] = getPquantile(75);
		return result;
	}
	
	
	public double getInterQuartileRange() {
		toArray();
		return getPquantile(75) - getPquantile(25);
	}

	
	public double[] getQuintiles() {
		toArray();
		double[] result = new double[4];
		result[0] = getPquantile(20);
		result[1] = getPquantile(40);
		result[2] = getPquantile(60);
		result[3] = getPquantile(80);
		return result;
	}
	
	
	public double[] getDeciles() {
		toArray();
		double[] result = new double[9];
		for (int i=0; i<9; i++)
			result[i] = getPquantile((i+1)*10);
		return result;
	}
	
	
	public double getInterDecileRange() {
		toArray();
		return getPquantile(90) - getPquantile(10);
	}
	
	
	/** the p-th percentile is the value below which p percent of the 
	 * observations are found.
	 * p between 0 and 100 (%)
	 * example of usage:
	 * System.out.println("10% of observations are found below " + stat.getPquantile(10)); 
	 */ 
	public double getPquantile(int p) {
		toArray();
		if (p<0 || p>=100)
			throw new RuntimeException("p must be between 0 and 100 (0 and 100%)");
		double result;
		if (p * values.length == 0) {
			result = 0d;
		} else if ((p * values.length) % 100 == 0) {
			result = 0.5d * (values[(int) ((values.length * ((double)p/100d)) -1)] + values[(int) (values.length * ((double)p/100d))]);
		} else {
			result = values[(int) Math.round((Math.ceil(values.length * ((double)p/100d)) - 1d))];
		}
		return result;
	}
	
	
	public double[] getPercentiles() {
		toArray();
		double[] result = new double[99];
		for (int i=0; i<99; i++)
			result[i] = getPquantile(i+1);
		return result;
	}
	
	
	/** olympic rank: 2 gold implies no silver*/
	public int getRank(double value) {
		toArray();
		int index = Arrays.binarySearch(values, value);
		while(index < values.length-1 && values[index+1] == value) // assert that we return the max rank
			index++;
		return index;
	}
	
	
	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) { 
		AdvancedStat stat = new AdvancedStat();
		for (int i=1; i<=101; i++)
			stat.addValue(i);
		for (int i=1; i<=99; i++)
			System.out.println("P" +i +":\t" +stat.getPquantile(i) +"\t(i.e. " +i +"% of observations are found below \"" + stat.getPquantile(i) +"\")"); 
		System.out.println(stat.getInterDecileRange()); 
		System.out.println(stat.getInterQuartileRange()); 
	} 
}
