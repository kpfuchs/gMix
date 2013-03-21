/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2013  Karl-Peter Fuchs
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
package evaluation.simulator.core.statistics.aggregator;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleProcedure;

import java.math.BigDecimal;


public class GetSumHelper implements TDoubleProcedure {
	
	private BigDecimal sum = new BigDecimal(0);
	
	
	public BigDecimal getSum(TDoubleArrayList data) {
		data.forEach(this);
		BigDecimal result = sum;
		sum = new BigDecimal("0");
		return result;
	}
	
	
	@Override
	public boolean execute(double val) {
		sum = sum.add(new BigDecimal(""+val));
		return true;
	}
	
}