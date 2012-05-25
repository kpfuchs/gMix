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
package framework.core.logger;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import framework.core.config.Paths;


public class Logger {

	private static boolean initDone = false;
	private static String OUTPUT_FILE;
	

	public static void init() {
		Logger.OUTPUT_FILE = Paths.GL_OUTPUT_PATH +System.currentTimeMillis() +"-consoleOutput.txt";
		
		if (!initDone) {
			try {
				PrintStream filePrintStream = new PrintStream(new FileOutputStream(OUTPUT_FILE));
				PrintStream duplicateOutPrintStream = new DuplicatePrintStream(System.out, filePrintStream, false);
				PrintStream duplicateErrPrintStream = new DuplicatePrintStream(System.err, filePrintStream, true);
				System.setOut(duplicateOutPrintStream);
				System.setErr(duplicateErrPrintStream);

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			initDone = true;
		}
	}

}