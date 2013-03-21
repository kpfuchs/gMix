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
package evaluation.traceParser.engine.fileReader;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import evaluation.traceParser.engine.TraceInfo;
import evaluation.traceParser.engine.converter.ToGMP;
import evaluation.traceParser.engine.dataStructure.Packet;
import framework.core.util.Util;


/**
 * Create Packets from any packet format with a handler available.
 *
 */
public class PacketSource {

	private TraceInfo traceInfo;
	private PacketReader implementation;
	private InputStream trace;
	
	
	public PacketSource(TraceInfo traceInfo) {
		Util.displayWarningOnLowReservedMemory();
		this.traceInfo = traceInfo;
		switch (traceInfo.getTraceFormat()) {
			case GMP:
				handleGMAP();
				break;
			case ERF:
				handleERF();
				break;
			case PCAP:
				handlePCAP();
				break;
			case PM:
			case GMT:
			case GMF:
				throw new RuntimeException("PacketCreator cannot read packets from a flow trace files (flow trace files contain flows, not packets)");
			default:
				throw new RuntimeException("unknown trace format in: " +traceInfo.getPathToTraceFile());
		}
	}
	
	
	public PacketSource(String pathToTraceFolder) {
		this(new TraceInfo(pathToTraceFolder));
	}
	
	
	public Packet readPacket() throws IOException {
		return this.implementation.readPacket();
	}
	
	
	public Packet readPacket(Packet reusePacket) throws IOException {
		return this.implementation.readPacket(reusePacket);
	}
	

	public  long getTotalBytesRead() {
		return this.implementation.getTotalBytesRead();
	}
	
	
	public void close() {
		try {
			this.trace.close();
		} catch (IOException e) {}
	}
	
	
	public void reset() {
		close();
		trace = createInputStream(Util.removeFileExtension(traceInfo.getPathToTraceFile()) + ".gmp");
		this.implementation = new NativePacketReader(trace);
	}
	
	
	private void handleGMAP() { // use native packet reader
		this.implementation = new NativePacketReader(createInputStream(traceInfo.getPathToTraceFile()));
	}
	
	
	private void handleERF() { // use dynamic packet reader to create .gmp and then use native packet reader
		// detect if .gmp file already present: (but was not selected in traceInfo.txt, e.g. because it was created automatically last run)
		boolean alreadyPresent = false;
		String outputFile = Util.removeFileExtension(traceInfo.getPathToTraceFile()) + ".gmp";
		try {
			trace = new FileInputStream(outputFile);
			trace.close();
			alreadyPresent = true;
			System.out.println("detected existing .gmp file"); 
		} catch (Exception e) {}
		if (!alreadyPresent) {
			System.out.println("converting to .gmp"); 
			long start = System.currentTimeMillis();
			new ToGMP(traceInfo.getPathToTraceFile(), outputFile, traceInfo).convert();
			System.out.println("finished converting to .gmp (duration: " +((float)(System.currentTimeMillis()-start)/1000f)+" seconds)"); 
		}
		this.implementation = new NativePacketReader(createInputStream(outputFile));
	}
	
	
	private void handlePCAP() { // use dynamic packet reader to create .pcap and then use native packet reader
		// detect if .gmp file already present: (but was not selected in traceInfo.txt, e.g. because it was created automatically last run)
		boolean alreadyPresent = false;
		String outputFile = Util.removeFileExtension(traceInfo.getPathToTraceFile()) + ".gmp";
		try {
			trace = new FileInputStream(outputFile);
			trace.close();
			alreadyPresent = true;
			System.out.println("detected existing .gmp file"); 
		} catch (Exception e) {}
		if (!alreadyPresent) {
			System.out.println("converting to .gmp"); 
			new ToGMP(traceInfo.getPathToTraceFile(), outputFile, traceInfo).convert();
			System.out.println("finished converting to .gmp"); 
		}
		this.implementation = new NativePacketReader(createInputStream(outputFile));
	}

	
	private InputStream createInputStream(String pathToFile) {
		// detect if file is there:
		try {
			trace = new FileInputStream(pathToFile);
			trace.close();
		} catch (Exception e) {
			throw new RuntimeException("ERROR: could not read trace file from " +pathToFile);
		}
		// detect compression and return stream:
		try {
			trace = new BufferedInputStream(Util.tryDetectCompressionMethod(pathToFile));
		} catch (FileNotFoundException e) {
			System.err.println("ERROR: trace file " +pathToFile +" not found.");
			System.err.println("possible reasons: ");
			System.err.println("   - file not present");
			System.err.println("   - wrong file specified in " +TraceInfo.INFO_FILE_NAME);
			if (traceInfo.getURL() != null)
				System.err.println("if the file is not present, it can be downlaoded from " +traceInfo.getURL());
			if (traceInfo.getComment() != null)
				System.err.println("Comment: " +traceInfo.getComment()); 
			System.exit(1);
		}
		return trace;
	}
	
	
	public PacketIterator iterator() {
		return new PacketIterator(this);
	}

}
