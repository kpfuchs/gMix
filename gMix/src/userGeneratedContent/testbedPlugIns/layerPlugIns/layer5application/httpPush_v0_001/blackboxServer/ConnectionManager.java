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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.blackboxServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

/**
 * This class manages the connections
 * It creates a new server for each incoming connnection.
 */

/**
 * @author bash
 * 
 */
public class ConnectionManager extends Thread {

	private ServerSocket blackboxMix;
	
	private int lowerBorder = 1500;
	private int upperBorder = 3000;
	private boolean rangedDelay = false;
	private Random delayPool;

	/**
	 * Constructor for the connection manager
	 * @param port
	 */
	public ConnectionManager(int port) {
		delayPool = new Random();
		try {
			blackboxMix = new ServerSocket(port);
			//System.out.println("Waiting for new connection");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private int createDelayValue(){
		int delayValue;
		if (rangedDelay){
			delayValue = (int) (lowerBorder + Math.round(Math.random() * (upperBorder -lowerBorder)));			
		} else {
			delayValue = lowerBorder;
		}
		
		return delayValue;
	}

	public void run() {
		while (true) {
			Socket mixConnection = null;

			try {
				mixConnection = blackboxMix.accept();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


			int delay = createDelayValue();
			MixConnectionHandler newConnection = new MixConnectionHandler(
					mixConnection, delay);
			new Thread(newConnection).start();
		}
	}

}
