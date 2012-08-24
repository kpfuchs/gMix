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

import java.util.Random;

import evaluation.loadGenerator.ClientTrafficScheduleWriter;
import evaluation.loadGenerator.scheduler.ScheduleTarget;


public class MPL_BasicWriter implements ScheduleTarget<MPL_ClientWrapper> {

	//private final boolean IS_DUPLEX;
	private final int DEST_PORT;
	private Random random = new Random();
	
	
	public MPL_BasicWriter(ClientTrafficScheduleWriter<MPL_ClientWrapper> owner, boolean isDuplex, int dstPort) {
		//this.IS_DUPLEX = isDuplex;
		this.DEST_PORT = dstPort;
	}

	
	@Override
	public void execute(MPL_ClientWrapper client) {
		synchronized (client) {
			byte[] payload = new byte[client.socket.getMaxSizeForNextMessageSend()];
			random.nextBytes(payload);
			System.out.println("LOAD_GENERATOR: sending request for client " +client.identifier); 
			client.socket.sendMessage(DEST_PORT, payload);
			//if (LoadGenerator.VALIDATE_IO)
			//	IOTester.findInstance(""+message.getClientId()).addSendRecord(payload);
			//if (IS_DUPLEX) 
			//	ALRR_ClientWrapper.activeTransactions.put(message.getTransactionId(), message);
		}
	}

}
