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
 */
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects;

/**
 * @author bash
 *  *  Enum 
 *  
 *  It contains the type of the message received by a connection. 
 *  
 */
public enum HttpPartType {

    /**
     * Type of the http messages
     */
    /**
     * Http Header
     */
	Header, 
	/**
	 * Http body not chunked encoded
	 */
	Body, 
	   /**
     * Http body chunked encoded
     */
	BodyChunk, 
	
	/**
	 * Socks auth message
	 */
	SocksAuth, 
	
	/**
	 * Socks reply
	 */
	SocksReply, 
	/**
	 * Socks request
	 */
	SocksRequest

}
