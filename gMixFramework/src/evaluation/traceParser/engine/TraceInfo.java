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
package evaluation.traceParser.engine;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import framework.core.util.Util;


public class TraceInfo {

	private final static String INFO_FILE_NAME = "traceInfo.txt";
	private Properties properties;
	private Protocol traceFormat;
	private String nameOfTraceFile;
	private String pathToTraceFolder;
	private String wanAddress;
	private String lanAddress;
	
	
	public TraceInfo(String pathToTraceFolder) {
		this.properties = new Properties();
		if (pathToTraceFolder.endsWith(".gmf")) {
			int filenameOffset = pathToTraceFolder.lastIndexOf("/") == -1 ? pathToTraceFolder.lastIndexOf("\\") +1 : pathToTraceFolder.lastIndexOf("/") +1;
			this.nameOfTraceFile = pathToTraceFolder.substring(filenameOffset, pathToTraceFolder.length());
			this.pathToTraceFolder = pathToTraceFolder.substring(0, filenameOffset);
			this.traceFormat = Protocol.GMF;
			return;
		}
		try {
			if (!(pathToTraceFolder.endsWith("/") || pathToTraceFolder.endsWith("\\")))
				pathToTraceFolder += "/";
			this.pathToTraceFolder = pathToTraceFolder;
			properties.load(new FileInputStream(pathToTraceFolder +INFO_FILE_NAME));	 
		} catch(IOException e) {
			// TODO: auto detect
			throw new RuntimeException("ERROR: no property file (traceInfo.txt) found for the trace file in " +pathToTraceFolder +"\n");
	    }
		this.nameOfTraceFile = properties.getProperty("NAME_OF_TRACE_FILE");
		this.wanAddress = properties.getProperty("WAN_ADDRESS");
		this.lanAddress = properties.getProperty("LAN_ADDRESS");
		if (wanAddress == null || wanAddress.equals(""))
			this.wanAddress = null;
		else
			this.wanAddress = wanAddress.trim().replace(":", "").toUpperCase();
		if (lanAddress == null || lanAddress.equals(""))
			this.lanAddress = null;
		else
			this.lanAddress = lanAddress.trim().replace(":", "").toUpperCase();
		if (nameOfTraceFile == null || nameOfTraceFile.equals(""))
			throw new RuntimeException("ERROR: name of trace file not specified in property file " +pathToTraceFolder +INFO_FILE_NAME +" (key NAME_OF_TRACE_FILE not set)"); 
		
		String traceFormatAsString = properties.getProperty("TRACE_FORMAT");
		if (traceFormatAsString == null || traceFormatAsString.equals("")) // TODO: try to auto detect
			throw new RuntimeException("ERROR: no trace format specified in property file " +pathToTraceFolder +INFO_FILE_NAME +" (key TRACE_FORMAT not set)"); 
		else if (traceFormatAsString.equalsIgnoreCase("ERF"))
			this.traceFormat = Protocol.ERF;
		else if (traceFormatAsString.equalsIgnoreCase("PCAP") || traceFormatAsString.equalsIgnoreCase("LIBPCAP") || traceFormatAsString.equalsIgnoreCase("TCPDUMP")) 
			this.traceFormat = Protocol.PCAP;
		else if (traceFormatAsString.equalsIgnoreCase("GMP")) 
			this.traceFormat = Protocol.GMP;
		else if (traceFormatAsString.equalsIgnoreCase("GMF")) 
			this.traceFormat = Protocol.GMF;
		else {
			try {
				this.traceFormat = Protocol.valueOf(traceFormatAsString);
			} catch (Exception e) {
				throw new RuntimeException("ERROR: not supported trace format (" +traceFormat +") specified in property file " +pathToTraceFolder +INFO_FILE_NAME +" (key TRACE_FORMAT)"); 
				
			}
		}	
		
	}
	
	
	public Protocol getTraceFormat() {
		return traceFormat;
	}
	
	
	public void setTraceFormat(Protocol traceFormat) {
		this.traceFormat = traceFormat;
	}
	
	
	public String getNameOfTraceFile() {
		return nameOfTraceFile;
	}
	
	
	public void setNameOfTraceFile(String nameOfTraceFile) {
		this.nameOfTraceFile = nameOfTraceFile;
	}
	
	
	public String getNameOfTraceFileWithoutExtension() {
		return Util.removeFileExtension(this.nameOfTraceFile);
	}
	
	
	public String getPathToGmf() {
		return getPathToTraceFolder() +getNameOfTraceFileWithoutExtension() +".gmf";
	}
	
	
	public String getPathToGmf(String fileName) {
		return getPathToTraceFolder() +fileName +".gmf";
	}
	
	
	public String getPathToTraceFolder() {
		return pathToTraceFolder;
	}
	
	
	public void setPathToTraceFolder(String pathToTraceFolder) {
		this.pathToTraceFolder = pathToTraceFolder;
	}
	

	public String getPathToTraceFile() {
		return pathToTraceFolder + nameOfTraceFile;
	}
	
	
	public String getWanAddress() {
		return wanAddress;
	}
	
	
	public void setWanAddress(String wanAddress) {
		this.wanAddress = wanAddress;
	}
	
	
	public String getLanAddress() {
		return lanAddress;
	}
	
	
	public void setLanAddress(String lanAddress) {
		this.lanAddress = lanAddress;
	}

}
