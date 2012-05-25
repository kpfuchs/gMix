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

package simulator.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import simulator.core.Settings;


public class DiskIOHelper {

	
	public static String getFileContent(String fileNameOrPath) {
		
		String result = "";
		
		try {

			BufferedReader br = new BufferedReader(new InputStreamReader(
				new DataInputStream(new FileInputStream(fileNameOrPath))));

			String line;
			while ((line = br.readLine()) != null)
				result += line + "\n";
			
		} catch (Exception e) {
			if (Settings.DEBUG_ON)
				e.printStackTrace();
			throw new RuntimeException(	"ERROR: could not read from file " 
										+fileNameOrPath +"!");
		}
		
		return result;
		
	}
	
	
	public static void writeToFile(String content, String fileNameOrPath) {
		
		try {
			
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
				new DataOutputStream(new FileOutputStream(fileNameOrPath))));
			
			bw.write(content);
			bw.flush();
			bw.close();
			
		} catch (IOException e) {
			if (Settings.DEBUG_ON)
				e.printStackTrace();
			throw new RuntimeException(	"ERROR: could not write to file " 
										+fileNameOrPath +"!");
		}
		
	}
	
}
