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



/**
 * Interface for Improvementclasses
 * Each Improvementclass has to implement the following improvement interface
 * @author bash
 *
 */
public interface ExitImprovementInterface {
	

	/**
	 * Checks if the write out to the connection is allowed
	 * @return true if writeout is allowed, falsse otherwise
	 */
	public boolean isExitWriteOut();
	

	/**
	 * Checks if a body is improveable via the Http Head
	 * @param headerTable
	 * @return if improveable
	 */
	public boolean isWebBodyFromMixImproveable(Hashtable<String, String> headerTable, Connection connection);

	
	/**
     * Checks if a body is improveable via the Http Head
     * @param headerTable
     * @return if improveable
     */
	public boolean isWebBodyToMixImproveable(Hashtable<String, String> headerTable, Connection connection);

	
	/**
	 * Revoke the improvements of the entry site.
	 * This method must be the reverse method for the improvements of the entry side of the header
	 * @param message
	 * @return the http header in http
	 */
	public byte[] toWebHeaderImprovement(byte[] message, Connection connection);
	

	
	/**
	 * Improves Header.
	 * This method improves the header of a http Message
	 * @param message
	 * @return the improved header
	 */
	public byte[] fromWebHeaderImprovement(byte[] message, Connection connection);
	
	/**
	 * Revoke the improvements of the entry site for the body
	 * This Method must be the reverse method for the improvements of the entry site for the body
	 * @param message
	 * @return the body in http format
	 */
	public byte[] toWebBodyImprovement(byte[] message, Connection connection);
	

	/**
	 * Improves the body of a http message.
	 * 
	 * @param message
	 * @return the improved httpbody
	 */
	public byte[] fromWebBodyImprovement(byte[] message, Connection connection);
}
