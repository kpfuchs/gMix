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
package evaluation.loadGenerator.applicationLevelTraffic.requestReply;

import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

import evaluation.loadGenerator.LoadGenerator;
import framework.core.config.Settings;
import framework.core.util.IOTester;
import framework.core.util.Util;


public class ALRR_ReplyReceiver extends Thread {

	private Vector<ApplicationLevelReplyReceivedObserver> observers;
	private ALRR_ClientWrapper[] clientsArray;
	
	
	public ALRR_ReplyReceiver(ALRR_ClientWrapper[] clientsArray, Settings settings) {
		this.clientsArray = clientsArray;
		this.observers = new Vector<ApplicationLevelReplyReceivedObserver>();
		ALRR_ClientWrapper.init(settings.getPropertyAsInt("GLOBAL_EXPECTED_NUMBER_OF_USERS"));
	}
	
	
	public void registerObserver(ApplicationLevelReplyReceivedObserver observer) {
		synchronized (observers) {
			this.observers.add(observer);
		}
	}
	
	
	@Override
	public void run() {
		while (true) { // read replies...
			int ctr = 0;
			for (ALRR_ClientWrapper cw: clientsArray) { // ...for each client
				try {
					int available = cw.inputStream.available();
					if ((cw.headerRead && available > 0) || (!cw.headerRead && available >= 4)) { // if (enough) data available
						ctr++;
						// read available data:
						int len = cw.headerRead ? available: (available-(available%4));// don't read half headers
						byte[] arrivedData = new byte[len];
						int read = cw.inputStream.read(arrivedData);
						assert read == arrivedData.length: ""+read; // assured by mix io-streams
						if (LoadGenerator.VALIDATE_IO)
							IOTester.findInstance("reply-"+cw.identifier).addReceiveRecord(arrivedData);
						//if (read != arrivedData.length)
						//	arrivedData = Util.split(read, arrivedData)[0];
						// extract replies:
						byte[] remaining = arrivedData;
						ApplicationLevelMessage message;
						while (remaining != null) {
							// read transaction id if not yet done and load the current ApplicationLevelMessage-Instance:
							if (!cw.headerRead) {
								int transactionId = Util.byteArrayToInt(Arrays.copyOf(remaining, 4));
								message = ALRR_ClientWrapper.activeTransactions.get(transactionId);
								assert message != null: "received a transaction id in a reply message that was never sent: " +transactionId;
								cw.currentTraceEntry = message;
								cw.headerRead = true;
							} else {
								message = cw.currentTraceEntry;
							}
							remaining = message.addReplyChunk(remaining);
							if (!message.needMoreReplyChunks()) { // reply now received completely 
								// display stats:
								long delay = System.currentTimeMillis() - message.getAbsoluteSendTime();
								System.out.println(
										"LOAD_GENERATOR: received reply (" +
										"transactionId: " +message.getTransactionId() 
										+"; replySize: " +message.getReplySize() +"bytes"
										+"; delay: " +delay +"ms"
										+")"
										); 
								synchronized (observers) {
									for (ApplicationLevelReplyReceivedObserver observer: observers) 
										observer.replyReceived(message);
								}
								cw.headerRead = false;
								cw.currentTraceEntry = null;
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
			} 
			if (ctr == 0) // TODO wait-notify?
				try {Thread.sleep(1);} catch (InterruptedException e) {e.printStackTrace();}
		}
	}
	
}
