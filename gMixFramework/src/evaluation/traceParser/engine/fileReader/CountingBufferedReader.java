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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


public class CountingBufferedReader {

	private final static int DEFAULT_LINE_LENGTH = 1024;
	
	protected long position = 0;
	protected long positionOfLastLine = 0;
	protected long positionOfNextLine = 0;
	private BufferedReader reader;
	private int cur;
	private int next = -1;
	private boolean isCounting;
	
	
	public CountingBufferedReader(String pathToFile) throws FileNotFoundException, IOException {
		this.reader = new BufferedReader(new FileReader(pathToFile));
		this.isCounting = true;
		this.next = this.reader.read();
	}
	
	
	/** 
	 * with this constructor, the counting function is disabled. the bypassed 
	 * BufferedReader will be used directly (results in faster performance).
	 */
	public CountingBufferedReader(BufferedReader reader) throws FileNotFoundException, IOException {
		this.reader = reader;
		this.isCounting = false;	
	}
	
	
	/**
	 * valid line terminators: ['\n'], ['\r'] and ['\r''\n']
	 * @return
	 * @throws IOException
	 */
	public String readLine() throws IOException {
		if (isCounting) {
			StringBuffer line = new StringBuffer(DEFAULT_LINE_LENGTH);
			int c;
			while (true) {
				c = next();
				if (c == -1) { // eof
					if (line.length() == 0)
						return null;
					else
						return line.toString();
				} else if (c == '\n' || c == '\r') {
					if (c == '\r' && peekNext() == '\n') // skip ['\n'] if ['\r''\n']
						next();
					positionOfLastLine = positionOfNextLine;
					positionOfNextLine = position;
					return line.toString();
				} else {
					line.append((char)c);
				}
			} 
		} else {
			return reader.readLine();
		}
		
	}
	
	
	private int next() throws IOException {
		this.cur = next;
		this.next = reader.read();
		this.position++;
		return cur;
	}
	
	
	private final int peekNext() throws IOException {
		return next;
	}
	
	
	public long getPositionOfLastLine() {
		return this.positionOfLastLine;
	}
	
	
	public long getPositionOfNextLine() {
		return this.positionOfNextLine;
	}
	
	
	public final void close() throws IOException {
		reader.close();
	}
	
}
