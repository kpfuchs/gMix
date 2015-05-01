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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * This class is for time evaluation an logging procedure
 * @author bash
 *
 */
public class FileWriterHandler {

	private String path;
	private File fileHandler;
	private FileChannel channel;
	private FileOutputStream outputFile;
	/**
	 * 
	 */
	public FileWriterHandler( String path) {
		
		File fileDir = new File(path);

		if (!fileDir.exists()){
			System.out.println("filehandler: dir create");
			fileDir.mkdir();
			path = fileDir.getPath();
		} else {
			this.path = path;
		}
		
	}

	/**
	 * Creates a new file
	 * @param filename
	 * @throws FileNotFoundException
	 */
	public void createFile(String filename) throws FileNotFoundException{
		fileHandler = new File(path + File.separator + filename);
	    outputFile = null;  
	    try {
	      outputFile = new FileOutputStream(fileHandler, true);
	    } catch (FileNotFoundException e) {
	      e.printStackTrace(System.err);
	    } 
	    channel = outputFile.getChannel(); 
		
	}
	
	/**
	 * Method to create new log
	 */
	public void writeLine(String line) throws IOException{  
	      byte[] bytes = null;
		try {
			bytes = line.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	      ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
	      
	      buffer = ByteBuffer.wrap(bytes);
	      channel.write(buffer);
	      
	}
	
	
	/**
	 * Close file
	 * @throws IOException
	 */
	public void closeFile() throws IOException{
		//TimeStamp	ConId	Side	Type	Size	Content
		
			outputFile.close();

	}
}
