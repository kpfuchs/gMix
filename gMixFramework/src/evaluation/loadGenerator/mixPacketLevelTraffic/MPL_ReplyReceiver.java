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

import framework.core.config.Settings;


public class MPL_ReplyReceiver extends Thread {

	private MPL_ClientWrapper[] clientsArray;
	
	
	public MPL_ReplyReceiver(MPL_ClientWrapper[] clientsArray, Settings settings) {
		this.clientsArray = clientsArray;
		//MPL_ClientWrapper.init(settings.getPropertyAsInt("GLOBAL_EXPECTED_NUMBER_OF_USERS"));
	}

	
	@Override
	public void run() {
		while (true) { // read replies...
			int ctr = 0;
			for (MPL_ClientWrapper cw: clientsArray) { // ...for each client
				int availableReplies = cw.socket.availableReplies();
				if (availableReplies > 0) {
					ctr++;
					/*AnonMessage message = */cw.socket.receiveMessage();
					//if (LoadGenerator.VALIDATE_IO)
					//	IOTester.findInstance("reply-"+cw.identifier).addReceiveRecord(message.getByteMessage());
					System.out.println("LOAD_GENERATOR: received reply for client " +cw.identifier); 
				}
			} 
			if (ctr == 0) // TODO wait-notify?
				try {Thread.sleep(1);} catch (InterruptedException e) {e.printStackTrace();}
		}
	}
	
}
