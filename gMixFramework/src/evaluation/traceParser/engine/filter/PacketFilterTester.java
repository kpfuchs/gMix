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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import evaluation.traceParser.engine.dataStructure.Packet;
import evaluation.traceParser.engine.fileReader.PacketSource;
import evaluation.traceParser.engine.protocolHeaderParser.ERFpacket;
import framework.core.util.Util;


public class PacketFilterTester {
	
	public static String ERF_TEST_FILE_SHORT = "./inputOutput/global/traces/erfTests/wiresharkSample/";
	public static String AUCK_8 = "./inputOutput/global/traces/erfTests/auckland8sample/";
	public static String AUCK_10 = "./inputOutput/global/traces/erfTests/auckland10sample/";
	public static String GMF_TEST_FILE_1 = "./inputOutput/global/traces/erfTests/synt/";
	public static String PCAP_TEST_FILE_1 = "./inputOutput/global/traces/pcapTests/localCapture/";
	public static String PCAP_TEST_FILE_2 = "./inputOutput/global/traces/pcapTests/localCapture2/";
	public static String PCAP_TEST_FILE_3 = "./inputOutput/global/traces/pcapTests/localCapture3/";
	
	private PacketFilter filter;
	private String pathToTraceFile;
	
	
	public PacketFilterTester(PacketFilter filter, String pathToTraceFile) {
		this.filter = filter;
		this.pathToTraceFile = pathToTraceFile;
		parseFile();
	}


	public void parseFile() {
		PacketSource apc = new PacketSource(pathToTraceFile);
		System.out.println("FilterTester: start reading trace file in " +pathToTraceFile); 
		long start = System.currentTimeMillis();
		while (true) {
			try {
				Packet ap = apc.readPacket();
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
		System.out.println("FilterTester: finished reading trace file. duration: " +(System.currentTimeMillis() - start) +"ms");
	}
	
	
	/**
	 * Comment
	 *
	 * @param args Not used.
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(Util.tryDetectCompressionMethod(AUCK_8 + "/20031203-110000.gz"));
		long start = System.currentTimeMillis();
		byte[] buf = new byte[100];
		long read = 0;
		while (true) {
			int len = bis.read(buf);
			if (len == -1)
				break;
			read += len;
		}
		bis.close();
		System.out.println("BufferedInputStream dur: " +(System.currentTimeMillis() - start) +"ms, read: " +Util.humanReadableByteCount(read, false)); 
	
		bis = new BufferedInputStream(Util.tryDetectCompressionMethod(AUCK_8 + "/20031203-110000.gz"));
		start = System.currentTimeMillis();
		
		while (true) {
			byte[] ar = ERFpacket.readERFpacket(bis);
			if (ar == null)
				break;
		}
		bis.close();
		System.out.println("ERFpacket.readERFpacket dur: " +(System.currentTimeMillis() - start) +"ms, read: " +Util.humanReadableByteCount(read, false)); 
	
		
		InputStream is = Util.tryDetectCompressionMethod(AUCK_8 + "/20031203-110000.gmap");
		
		//bis = new BufferedInputStream(Util.tryDetectCompressionMethod(ERF_TEST_FILE_LONG + "/20031203-110000.gmap"));
		start = System.currentTimeMillis();
		String line;
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		while (true) {
			line = br.readLine();
			if (line == null)
				break;
		}
		bis.close();
		System.out.println("NativePacketReader dur: " +(System.currentTimeMillis() - start) +"ms, read: " +Util.humanReadableByteCount(read, false)); 
	
		
		
	} 
	
}

