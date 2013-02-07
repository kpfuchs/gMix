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
package evaluation.traceParser.statistics;

import evaluation.traceParser.engine.dataStructure.Flow;
import evaluation.traceParser.engine.dataStructure.Transaction;
import evaluation.traceParser.engine.filter.TerminatingFlowFilter;
import evaluation.traceParser.engine.filter.TerminatingFlowFilterTester;


public class AduStatistics implements TerminatingFlowFilter {

	@Override
	public Flow newRecord(Flow flow) {
		for (Transaction t:flow.transactions) {
			if (t.getRequestSize() != 0) // not server inited
				System.out.println(t.getRequestSize()); 
			if (t.containsReplies()) {
				int[] replySizes = t.getDistinctReplySizes();
				for (int i=0; i<replySizes.length; i++)
					System.out.println(replySizes[i]); 
			}
		} 
		return flow;
	}
	
	
	@Override
	public void finished() {

	}
	

	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		new TerminatingFlowFilterTester(new AduStatistics(), "./inputOutput/global/traces/erfTests/auckland8sample/auck8_5min_1000user_dpe.gmf", null);
	}
}
