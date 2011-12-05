/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2011  Karl-Peter Fuchs
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

package logger;

import java.io.PrintStream;

import simulator.Simulator;

// printstream that writes all data provided to it to 2 printstreams (= creates duplicate stream)
public class DuplicatePrintStream extends PrintStream {

	private PrintStream outputStream2;
	private String prefix;
	
	
	public DuplicatePrintStream(PrintStream outputStream1,
			PrintStream outputStream2, boolean isErrorStream) {
		
		super(outputStream1);
		this.outputStream2 = outputStream2;
		this.prefix = isErrorStream ? "ERROR:" : "";
		
	}

	
	@Override
	public void write(byte buf[], int off, int len) {

		try {
			
			super.write(buf, off, len);
			outputStream2.write(buf, off, len);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	
	@Override
	public void flush() {
		
		super.flush();
		outputStream2.flush();
		
	}

	
	@Override
	public void println(String s) {
		super.println(prefix +Simulator.getNow()+": " +s);
	}

}