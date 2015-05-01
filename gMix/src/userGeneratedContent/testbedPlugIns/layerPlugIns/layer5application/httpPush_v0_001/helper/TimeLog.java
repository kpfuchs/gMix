/*******************************************************************************
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2014  SVS
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
 *******************************************************************************/
/**
 * 
 */
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.helper;

import java.io.FileNotFoundException;
import java.io.IOException;

import staticContent.framework.clock.Clock;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.HttpPartType;

/**
 * @author bash
 * 
 */
public class TimeLog {

	private FileWriterHandler writer;
	private Clock clock;

	/**
	 * 
	 */
	public TimeLog(String path, String filename, Clock clock) {
		this.clock = clock;
		writer = new FileWriterHandler(path);
		try {
			writer.createFile(filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("Probleme beim erstellen der Datei");
		}
		try {
			writer.writeLine("TimeStamp\t\tConId\tSide\tType\tSize\tContent\n");
		} catch (IOException e) {
			e.printStackTrace();
			closeLog();
			System.out.println("Fehler beim Datei schreiben!");
		}

	}

	public void logEntry(int connectionId, String side, HttpPartType type,
			int messageId, int size, String content) {
		
		java.util.Date time = new java.util.Date(clock.getTime());
		String timestamp = time.toString();
		String tab = "\t";
		String entry = timestamp + tab + tab + String.valueOf(connectionId)
				+ tab + side + tab + type.toString() + tab
				+ String.valueOf(messageId) + tab + String.valueOf(size) + tab
				+ content + "\n";
		try {
			writer.writeLine(entry);
			
		} catch (IOException e) {
			e.printStackTrace();
			closeLog();
			System.out.println("Fehler beim Datei schreiben!");
		}
	}

	public void logEntryResHead(int connectionId, int messageId, int size,
			String content) {
		logEntry(connectionId, "Response", HttpPartType.Header, messageId,
				size, content);
	}

	public void logEntryResBody(int connectionId, int messageId, int size,
			String content) {
		logEntry(connectionId, "Response", HttpPartType.Body, messageId, size,
				content);
	}

	public void logEntryResBodyChunk(int connectionId, int messageId, int size,
			String content) {
		logEntry(connectionId, "Response", HttpPartType.BodyChunk, messageId,
				size, content);
	}

	public void logEntryReqHead(int connectionId, int messageId, int size,
			String content) {
		logEntry(connectionId, "Request", HttpPartType.Header, messageId, size,
				content);
	}

	public void logEntryReqBody(int connectionId, int messageId, int size,
			String content) {
		logEntry(connectionId, "Request", HttpPartType.Body, messageId, size,
				content);
	}

	public void logEntryReqBodyChunk(int connectionId, int messageId, int size,
			String content) {
		logEntry(connectionId, "Request", HttpPartType.BodyChunk, messageId,
				size, content);
	}

	public void logEntryReqSocksAuth(int connectionId, int messageId, int size,
			String content) {
		logEntry(connectionId, "Request", HttpPartType.SocksAuth, messageId,
				size, content);
	}

	public void logEntryReqSocksRequest(int connectionId, int messageId,
			int size, String content) {
		logEntry(connectionId, "Request", HttpPartType.SocksRequest, messageId,
				size, content);
	}

	public void logEntryResSocksReply(int connectionId, int messageId,
			int size, String content) {
		logEntry(connectionId, "Response", HttpPartType.SocksReply, messageId,
				size, content);
	}

	public void closeLog() {
		try {
			writer.closeFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
