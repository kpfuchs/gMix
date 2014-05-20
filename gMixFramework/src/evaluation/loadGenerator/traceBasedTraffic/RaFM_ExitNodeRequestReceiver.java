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
package evaluation.loadGenerator.traceBasedTraffic;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import evaluation.loadGenerator.ExitNodeClientData;
import evaluation.loadGenerator.ExitNodeRequestReceiver;
import evaluation.loadGenerator.scheduler.ScheduleTarget;
import evaluation.loadGenerator.scheduler.ThreadPoolScheduler;
import evaluation.traceParser.engine.dataStructure.SendableTransaction;
import framework.core.AnonNode;
import framework.core.config.Settings;
import framework.core.socket.socketInterfaces.StreamAnonSocketMix;
import framework.core.userDatabase.User;
import framework.core.util.Util;


/**
 * This class represents the server-side of the RaFM-LoadGenerator.
 * This means it receives the requests from the clients and sends back the
 * respective replies.
 * 
 * @author Johannes Wendel, Simon Lecheler, kpf
 * 
 */
public class RaFM_ExitNodeRequestReceiver extends ExitNodeRequestReceiver implements ScheduleTarget<RaFM_ReplyRecord> {

	private AnonNode anonNode;
	private Settings settings;
	private ThreadPoolScheduler<RaFM_ReplyRecord> tpScheduler;
	private Object synchronizer = new Object();
	private final boolean DEBUG_ON;
	
	
	protected RaFM_ExitNodeRequestReceiver(AnonNode anonNode) {
		this.anonNode = anonNode;
		this.settings = anonNode.getSettings();
		this.DEBUG_ON = settings.getPropertyAsBoolean("AL-RaFM_DIPLAY_DEBUG_INFO");
		int numberOfThreads = settings.getPropertyAsInt("AL-RaFM-NUMBER_OF_SCHEDULER_THREADS");
		tpScheduler = new ThreadPoolScheduler<RaFM_ReplyRecord>(settings, numberOfThreads);
	}
	

	public static RaFM_ExitNodeRequestReceiver createInstance(AnonNode anonNode) {
		return new RaFM_ExitNodeRequestReceiver(anonNode);
	}

	
	/**
	 * This method is called by the ApplicationLevelHandler when new data
	 * arrives at the server.
	 */
	@Override
	public void dataReceived(ExitNodeClientData client, byte[] dataReceived) {
		RaFM_ExitNodeClientData clientData = (RaFM_ExitNodeClientData)client; 
		if (anonNode.IS_DUPLEX) {
			clientData.buffer = Util.concatArrays(clientData.buffer, dataReceived);
			while (containsCompleteTransaction(clientData.buffer)) {
				ResultSet rs = extractTransaction(clientData.buffer);
				if (DEBUG_ON)
					System.out.println("RaFM_ExitNodeRequestReceiver: Received Request with TA-ID " +rs.transaction.getTransactionId()); 
				clientData.buffer = rs.leftOver;
				scheduleReplies(clientData, rs.transaction);
			}
		}
	}
	
	
	private boolean containsCompleteTransaction(byte[] data) {
		if (data.length < 5)
			return false;
		int len = Util.byteArrayToInt(Arrays.copyOfRange(data, 0, 4));
		if (data.length >= len + 4)
			return true;
		else
			return false;	
	}
	
	
	/**
	 * This method tries to extract a transactions (request) from the bytes
	 * arrived/bypassed
	 */
	private ResultSet extractTransaction(byte[] data) {
		assert containsCompleteTransaction(data);
		ResultSet rs = new ResultSet();
		int len = Util.byteArrayToInt(Arrays.copyOfRange(data, 0, 4));
		rs.transaction = new SendableTransaction(Arrays.copyOfRange(data, 4, len + 4));
		int leftOverLength = data.length - (len + 4);
		if (leftOverLength > 0)
			rs.leftOver = Arrays.copyOfRange(data, len + 4, data.length);
		else
			rs.leftOver = new byte[0];
		return rs;
	}

	
	private class ResultSet {
		byte[] leftOver;
		SendableTransaction transaction;
	}
	

	/**
	 * This method schedules the replies for the request back to the client if 
	 * there are any.
	 */
	private void scheduleReplies(RaFM_ExitNodeClientData client, SendableTransaction incomingMessage) {
		if (incomingMessage.containsReplies()) {
			for (int i=0; i<incomingMessage.getDistinctReplyDelays().length; i++) {
				RaFM_ReplyRecord replyRecord = new RaFM_ReplyRecord(incomingMessage, client.socket.getOutputStream(), i+1);
				tpScheduler.executeIn(TimeUnit.MILLISECONDS.toNanos(incomingMessage.getDistinctReplyDelays()[i]), this, replyRecord);
			}
		} else {
			if (DEBUG_ON)
				System.out.println("RaFM_ExitNodeRequestReceiver: No Replies contained for Client " +incomingMessage.getClientId() +"  TA-ID: " +incomingMessage.getTransactionId());
		}
	}

	
	/**
	 * This method is called by the tpScheduler and sends replies back to the
	 * client.
	 */
	@Override
	public void execute(RaFM_ReplyRecord replyRecord) {
		byte[] reply = replyRecord.transaction.createSendableReply();
		int transactionId = replyRecord.transaction.getTransactionId();
		if (DEBUG_ON)
			System.out.println("RaFM_ExitNodeRequestReceiver: Sending Reply for TA-ID " + transactionId + " with length of " + reply.length + " Bytes and Send Delay of " + replyRecord.replyNumber + " ms");
		try { 
			synchronized (synchronizer) { // TODO: try less restrictive synchronization via replyRecord.outputStream
				replyRecord.outputStream.write(reply);
				replyRecord.outputStream.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	@Override
	public ExitNodeClientData createClientDataInstance(User user, StreamAnonSocketMix socket, Object callingInstance) {
		return new RaFM_ExitNodeClientData(user, socket, callingInstance);
	}
	
	
	public class RaFM_ExitNodeClientData extends ExitNodeClientData {

		public byte[] buffer;
		

		public RaFM_ExitNodeClientData(User user, StreamAnonSocketMix socket, Object callingInstance) {
			super(user, socket, callingInstance);
			this.buffer = new byte[0];
		}
		
		
		public RaFM_ExitNodeClientData(User user, StreamAnonSocketMix socket) {
			this(user, socket, socket);
		}
		
	}
	
}
