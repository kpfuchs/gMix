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

/**
 * @author bash
 * This class provides the possibility to write detailed log files
 * 
 * The purpose of this class is to be able to log complete datatransfers 
 * 
 * Instantiate this class in another class with a given filename.
 * Then use the writeLine method to print whatever you want. 
 * Each entry is separated by a line of "="
 */
public class TrafficLog {
	FileWriterHandler writer;
	/**
	 * Constructor
	 * Create a log file by the given name
	 */
	public TrafficLog(String name) {
		writer = new FileWriterHandler("/home/bash/Dokumente/");
		try {
			writer.createFile(name);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Method to log a String
	 * @param line
	 */
	public void writeLine(String line) {
		try {
			writer.writeLine(line);
			writer.writeLine("\n =======================================================================\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
