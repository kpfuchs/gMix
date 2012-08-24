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
package evaluation.loadGenerator.mixPacketLevelTraffic;

import evaluation.loadGenerator.ClientWrapper;
import evaluation.loadGenerator.LoadGenerator;
import framework.core.socket.socketInterfaces.DatagramAnonSocket;
import framework.core.util.IOTester;


public class MPL_ClientWrapper extends ClientWrapper {

	public DatagramAnonSocket socket;
	//public static ConcurrentHashMap<Integer, MixPacketLevelMessage> activeTransactions;


	public MPL_ClientWrapper(int identifier) {
		super(identifier);
		if (LoadGenerator.VALIDATE_IO) {
			IOTester.createInstance(""+identifier);
			IOTester.createInstance("reply-"+identifier);
		}
	}
	
	
	/*public static void init(int expectedNumberOfConcurrentTransactions) {
		activeTransactions = new ConcurrentHashMap<Integer, MixPacketLevelMessage>(expectedNumberOfConcurrentTransactions);
	}*/
	
}
