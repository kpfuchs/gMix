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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.test;

import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.helper.FileReadHandler;

/**
 * @author bash
 *
 */
public class fileWriterTest {

	/**
	 * 
	 */
	public fileWriterTest() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	    
	    FileReadHandler handler = new FileReadHandler("C:\\Users\\bash\\Desktop\\shorter.txt");
	    System.out.println(handler.readIn());
	    
//		FileWriterHandler writer = new FileWriterHandler("\\log");
//		try {
//			writer.createFile("\\testFile.txt");
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		try {
//			writer.writeLine("testline");
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

	}

}
