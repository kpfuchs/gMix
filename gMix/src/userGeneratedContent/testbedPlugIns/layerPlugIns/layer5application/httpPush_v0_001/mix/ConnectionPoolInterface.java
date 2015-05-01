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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.mix;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;

import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.Connection;

/**
 * @author bash
 *
 *  This interface show what features a connectionpool has to implement.
 *  
 *  Each ConnecitonPool has to provide a method to register a key to a socket. 
 *  There is also a method to retrieve a selector.
 */
public interface ConnectionPoolInterface {
	
	
    /**
     * Retrieve Selector
     * @return a Selector 
     */
	public Selector getSelector();
	
	/**
	 * Register a key to a nio Socket. 
	 * This method needs the Socket, the connection and the event witch should invoke the socket
	 * @param serverSocket
	 * @param connection
	 * @param event
	 * @return the key of the socket
	 * @throws ClosedChannelException
	 * @throws IOException
	 */
	public SelectionKey registerSocket(SocketChannel serverSocket, Connection connection, int event) throws ClosedChannelException, IOException;
	
	/**
	 * Method to exchange the connection table
	 * @param pool
	 */
	public void setConnectionMap(ConcurrentHashMap<Integer, Connection> pool);
	
	public ConcurrentHashMap<Integer, Connection> getConnectionMap();
	

}
