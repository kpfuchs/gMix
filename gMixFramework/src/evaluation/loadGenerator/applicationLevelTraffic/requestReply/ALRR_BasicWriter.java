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

import evaluation.loadGenerator.ClientTrafficScheduleWriter;
import evaluation.loadGenerator.LoadGenerator;
import evaluation.loadGenerator.scheduler.ScheduleTarget;
import framework.core.util.IOTester;
import framework.core.util.Util;


public class ALRR_BasicWriter implements ScheduleTarget<ApplicationLevelMessage> {

	private final boolean IS_DUPLEX;
	private ClientTrafficScheduleWriter<ApplicationLevelMessage> owner;
	
	
	public ALRR_BasicWriter(ClientTrafficScheduleWriter<ApplicationLevelMessage> owner, boolean isDuplex) {
		this.owner = owner;
		this.IS_DUPLEX = isDuplex;
	} 
	
	
	@Override
	public void execute(ApplicationLevelMessage message) {
		ALRR_ClientWrapper cw = (ALRR_ClientWrapper)owner.getClientWrapper(message.getClientId());
		synchronized (cw.outputStream) {
			try {
				message.setAbsoluteSendTime(System.currentTimeMillis());
				byte[] payload = message.createPayloadForRequest();
				String stats = "LOAD_GENERATOR: sending request ("
						+"client:" +message.getClientId()
						+"; transactionId:" +message.getTransactionId() 
						+"; requestSize: " +message.getRequestSize() +"bytes";
				if (message.getReplySize() != Util.NOT_SET)
					stats += "; replySize: " +message.getReplySize() +"bytes";
				stats += ")";
				System.out.println(stats); 
				if (LoadGenerator.VALIDATE_IO)
					IOTester.findInstance(""+message.getClientId()).addSendRecord(payload);
				cw.outputStream.write(payload);
				cw.outputStream.flush();
				if (IS_DUPLEX) 
					ALRR_ClientWrapper.activeTransactions.put(message.getTransactionId(), message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}	
}
