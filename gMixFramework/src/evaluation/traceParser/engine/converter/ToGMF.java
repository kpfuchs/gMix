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
package evaluation.traceParser.engine.converter;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;

import evaluation.traceParser.engine.TraceInfo;
import evaluation.traceParser.engine.dataStructure.Flow;
import evaluation.traceParser.engine.fileReader.PacketSource;
import evaluation.traceParser.engine.filter.PacketFilter;
import framework.core.util.Util;


public class ToGMF {

	private PacketSource packetSource;
	private TraceInfo traceInfo;
	private PacketFilter filter;
	private String pathToResultTrace;
	private String pathToTempTrace;
	private Writer resultTrace;
	private final static int MAX_FLOWS_PER_RUN = 1000000;
	
	
	public ToGMF(PacketSource packetSource, TraceInfo traceInfo) {
		this(packetSource, traceInfo, null);
	}
	
	
	public ToGMF(PacketSource packetSource, TraceInfo traceInfo, PacketFilter filter) {
		this.packetSource = packetSource;
		this.traceInfo = traceInfo;
		this.filter = filter;
		this.pathToResultTrace = Util.removeFileExtension(traceInfo.getPathToTraceFile()) +".gmf";
		this.pathToTempTrace = Util.removeFileExtension(traceInfo.getPathToTraceFile()) +".tmp";
		try {
			this.resultTrace = new BufferedWriter(new OutputStreamWriter(new DataOutputStream(new FileOutputStream(pathToTempTrace))));
		} catch (IOException e) {
			throw new RuntimeException("ERROR: could not open/create file "  +pathToTempTrace);
		}
	}
	

	public void convert() {
		System.out.println("searching for TCP flows in " +traceInfo.getNameOfTraceFile() +" (weak check)");
		long start = System.currentTimeMillis();
		HashSet<String> flowIdentifiers = TCPflowFinder.findFlows(packetSource, filter);
		long dur = System.currentTimeMillis() - start;
		int flows = flowIdentifiers.size();
		int runs = (flows / MAX_FLOWS_PER_RUN) + ((flows % MAX_FLOWS_PER_RUN) > 0 ? 1: 0);
		System.out.println("found " +flows +" potential flows");
		//System.out.println("length of trace file (unpacked): " +Util.humanReadableByteCount(packetSource.getTotalBytesRead(), false));
		System.out.println("scanning took " +((float)dur/1000f) +" seconds");
		System.out.println("will iterate over the trace " +runs +" time(s) to extract TCP flows");
		
		// split HashMap: // TODO: serialize not needed parts for better performance?
		String[] flowIdentifiersArray = flowIdentifiers.toArray(new String[0]);
		flowIdentifiers.clear();
		flowIdentifiers = null;
		String[][] chunks = Util.splitInChunks(MAX_FLOWS_PER_RUN, flowIdentifiersArray);
		// extract flows:
		TCPFlowExtractor extractor = new TCPFlowExtractor();
		for (int i=0; i<chunks.length; i++) {
			System.out.println("starting run " +(i+1));
			HashSet<String> currentFlowIds = new HashSet<String>(Arrays.asList(chunks[i]));
			chunks[i] = null;
			try {
				packetSource.reset();
				extractor.extractFlows(currentFlowIds, resultTrace, packetSource);
			} catch (IOException e) {
				closeTrace();
				throw new RuntimeException("could not write to file " +pathToTempTrace); 
			}
		}
		sortTrace();
		closeTrace();
	}
	

	private void sortTrace() {
		closeTrace();
		System.out.println("sorting result trace"); 
		try {
			Flow.sort(pathToTempTrace, pathToResultTrace);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("ERROR: could not write flow to trace file "  +pathToResultTrace);
		}
		new File(pathToTempTrace).delete();
		System.out.println("finished writing result trace"); 
	}
	
	
	private void closeTrace() {
		try {
			resultTrace.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("ERROR: could not write flow to trace file "  +pathToResultTrace);
		}
	}
	
}
