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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;

import evaluation.traceParser.engine.Protocol;
import evaluation.traceParser.engine.TraceInfo;
import evaluation.traceParser.engine.dataStructure.Packet;
import evaluation.traceParser.engine.fileReader.DynamicPacketReader;
import evaluation.traceParser.engine.filter.PacketFilter;
import evaluation.traceParser.engine.protocolHeaderParser.PCAPpacket;
import framework.core.util.Util;


/**
 * Converts any trace format with a handler available (see enum 
 * evaluation.traceParser.Porotocol.java and package 
 * evaluation.traceParser.protocolHandler) to the gMix abstact packet format
 * (.gmp).
 */
public class ToGMP {

	private String pathToInFile;
	private String pathToOutFile;
	private PacketFilter filter;
	private InputStream sourceTrace;
	private DynamicPacketReader source;
	private Writer resultTrace;
	
	
	public ToGMP(String pathToInFile, String pathToOutFile, TraceInfo traceInfo) {
		this(pathToInFile, pathToOutFile, traceInfo, null);
	}
	
	
	public ToGMP(String pathToInFile, String pathToOutFile, TraceInfo traceInfo, PacketFilter filter) {
		this.pathToInFile = pathToInFile;
		this.pathToOutFile = pathToOutFile;
		this.filter = filter;
		try {
			this.sourceTrace = Util.tryDetectCompressionMethod(pathToInFile);
			if (traceInfo.getTraceFormat() == Protocol.PCAP) {
				try { // read pcap header
					PCAPpacket.readFileHeader(sourceTrace);
				} catch (IOException e) {
					throw new RuntimeException("ERROR: could not read PCAP file header from " +traceInfo.getPathToTraceFile()); 
				}
			}
			this.source = new DynamicPacketReader(sourceTrace, traceInfo);
		} catch (FileNotFoundException e) {
			System.err.println("ERROR: trace file " +pathToInFile +" not found.");
			System.err.println("possible reasons: ");
			System.err.println("   - file not present");
			System.err.println("   - wrong file specified in " +TraceInfo.INFO_FILE_NAME);
			if (traceInfo.getURL() != null)
				System.err.println("if the file is not present, it can be downlaoded from " +traceInfo.getURL());
			if (traceInfo.getComment() != null)
				System.err.println("Comment: " +traceInfo.getComment()); 
			System.exit(1);
		} catch (Exception e) {
			throw new RuntimeException("ERROR: could not read trace file from " +pathToInFile);
		}
		try {
			this.resultTrace = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new DataOutputStream(new FileOutputStream(pathToOutFile)), 8192)));
		} catch (IOException e) {
			throw new RuntimeException("ERROR: could not open/create file "  +pathToOutFile);
		}
	}
	
	
	public void convert() {
		Packet packet = new Packet();
		while (true) {
			try {
				packet = source.readPacket(packet);
				if (packet == null) {
					try {
						resultTrace.close();
						sourceTrace.close();
					} catch (IOException e1) {}
					if (filter != null)
						filter.finished();
					break;
				}
				if (filter != null) {
					packet = filter.newRecord(packet);
					if (packet != null)
						packet.serialize(resultTrace);
				} else {
					packet.serialize(resultTrace);
				}
			} catch (IOException e) {
				e.printStackTrace();
				new File(pathToOutFile).delete();
				throw new RuntimeException("ERROR: could not read from file " +pathToInFile);
			} 
		}
	}
	
}
