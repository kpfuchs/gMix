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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.improvement;

import java.util.Hashtable;

import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.Connection;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.helper.LocalClassLoader;


/**
 * @author bash
 *
 */
public class EntryImprovement {

	
	private EntryImprovementInterface improvementPlugin;
	
	/**
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 * 
	 */
	public EntryImprovement(String improvement) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		
		improvementPlugin =  LocalClassLoader.newInstance("plugIns.layer5application.httpPush_v0_001.plugin", improvement);
		//improvementPlugin = new noImprovementPlugin();
	}
	
	/**
	 * Returns if a message should be written out on a connection
	 * @return true if it should be written out, false otherwise
	 */
	public boolean writeOut() {
		return improvementPlugin.isEntryWriteOut();
	}
	
	
	/**
	 * Method to improve a header
	 * @param message
	 * @param connection
	 * @return the improve message
	 */
	public byte[] improveHeader(byte[] message, Connection connection){
		return improvementPlugin.toMixHeaderImprovement(message, connection);
	}
	
	/**
	 * Method to improve a body
	 * @param message
	 * @param connection
	 * @return improved body
	 */
	public byte[] improveBody(byte[] message, Connection connection){
		return improvementPlugin.toMixBodyImprovement(message, connection);
	}
	
	/**
	 * Method to remove an improvement from a header
	 * @param message
	 * @param connection
	 * @return unimproved header
	 */
	public byte[] unImproveHeader(byte[] message, Connection connection){
		
		return improvementPlugin.fromMixHeaderImprovement(message, connection);
	}
	
	/**
	 * Remove improvement from body
	 * @param message
	 * @param connection
	 * @return unimproved body
	 */
	public byte[] unImproveBody(byte[] message, Connection connection){
		
		return improvementPlugin.fromMixBodyImprovement(message, connection);
	}
	
	/**
	 * Checks via header if a body is improveable sended to the mix
	 * @param headerTable
	 * @param connection
	 * @return true if body is improveable, false otherwise
	 */
	public boolean isBodyToMixImproveable(Hashtable<String, String> headerTable, Connection connection){
		
		return improvementPlugin.isAppBodyToMixImproveable(headerTable, connection);
		
	}
	
	/**
	 * Checks via header if a body is improveable from the mix
	 * @param headerTable
	 * @param connection
	 * @return true if body is improveable, false otherwise
	 */
	   public boolean isBodyFromMixImproveable(Hashtable<String, String> headerTable, Connection connection){
	        
	        return improvementPlugin.isAppBodyFromMixImproveable(headerTable, connection);
	        
	    }

}
