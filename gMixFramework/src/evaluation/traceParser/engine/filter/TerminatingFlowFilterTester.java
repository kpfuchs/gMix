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
package evaluation.traceParser.engine.filter;

import java.io.FileNotFoundException;
import java.io.IOException;

import evaluation.traceParser.engine.dataStructure.Flow;
import evaluation.traceParser.engine.fileReader.FlowReader;
import evaluation.traceParser.scenarioExtractor.flowFilter.FlowFilter;


public class TerminatingFlowFilterTester {

	public static String TEST_FILE_1 = "./inputOutput/global/traces/erfTests/auckland8sample/extractedFlows.gmf";
	
	private TerminatingFlowFilter filter;
	private FlowFilter preFilter;
	private String pathToTraceFile;
	
	
	public TerminatingFlowFilterTester(TerminatingFlowFilter filter, String pathToTraceFile, FlowFilter preFilter) {
		this.filter = filter;
		this.preFilter = preFilter;
		this.pathToTraceFile = pathToTraceFile;
		try {
			parseFile();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	public void parseFile() throws FileNotFoundException, IOException {
		FlowReader fr = new FlowReader(pathToTraceFile, preFilter, false);
		System.out.println("FilterTester: start reading trace file in " +pathToTraceFile); 
		for (int i=0; i<Integer.MAX_VALUE; i++) {
			try {
				Flow ap = fr.readFlow();
				if (ap == null) {
					filter.finished();
					break;
				} else {
					filter.newRecord(ap);
				}
			} catch (IOException e) {
				e.printStackTrace();
				return;
			} 
		} 
	}
	
}
