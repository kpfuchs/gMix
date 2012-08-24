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
import java.nio.ByteBuffer;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import evaluation.loadGenerator.ExitNodeRequestReceiver;
import evaluation.loadGenerator.LoadGenerator;
import framework.core.AnonNode;
import framework.core.socket.socketInterfaces.StreamAnonSocketMix;
import framework.core.socket.stream.BasicOutputStreamMix;
import framework.core.userDatabase.User;
import framework.core.util.IOTester;
import framework.core.util.Util;


public class ALRR_Scheduled_ExitNodeRequestReceiver implements ExitNodeRequestReceiver {

	private AnonNode mix;
	private ScheduledThreadPoolExecutor scheduler;
	private ReplyThread replyThread;
	private Vector<ALRR_ClientData> replyTasks;
	private Vector<ALRR_ClientData> newReplyTasks;
	
	
	public ALRR_Scheduled_ExitNodeRequestReceiver(AnonNode exitNode) {
		this.mix = exitNode;
		if (mix.IS_DUPLEX) {
			this.replyTasks = new Vector<ALRR_ClientData>(1000); // TODO: dynamic
			this.newReplyTasks = new Vector<ALRR_ClientData>(1000); // TODO: dynamic
			this.replyThread = new ReplyThread();
			this.scheduler = new ScheduledThreadPoolExecutor(4); // TODO: dynamic
			this.replyThread.start();
		}	
	}
	
	
	@Override
	public void dataReceived(ClientData clientData, byte[] dataReceived) {
		ALRR_ClientData client = (ALRR_ClientData)clientData;
		byte[] clone;
		if (LoadGenerator.VALIDATE_IO)
			clone = dataReceived.clone();
		while (dataReceived != null) {
			dataReceived = client.currentRequest.addRequestChunk(dataReceived);
			if (client.clientId == Util.NOT_SET)
				client.clientId = client.currentRequest.getClientId();
			if (!client.currentRequest.needMoreRequestChunks()) { // request now received completely 
				// display stats:
				String stats = "DISTANT_PROXY: received request (" +
					"transactionId: " +client.currentRequest.getTransactionId() 
					+"; requestSize: " +client.currentRequest.getRequestSize() +"bytes"
					+"; transfer duration: " +(System.currentTimeMillis()-client.currentRequest.getAbsoluteSendTime()) +"ms";
				if (mix.IS_DUPLEX) {
					stats += "; replySize: " +client.currentRequest.getReplySize() +"bytes";
					stats += "; replyDelay: " +client.currentRequest.getReplyDelayInMicroSec() +"microsec";
				}
				stats += ")";
				System.out.println(stats); 
				// schedule reply if duplex:
				if (mix.IS_DUPLEX) {
					//System.err.println("schedule for in " +client.currentRequest.getReplyDelayInMilliSec() +"ms"); // TODO: remove
					scheduler.schedule(
						new SendReplyTask(client, client.currentRequest), 
						client.currentRequest.getReplyDelayInMicroSec(), 
						TimeUnit.MICROSECONDS
						);
				}
				client.currentRequest = new ApplicationLevelMessage();
			}
		}
		if (LoadGenerator.VALIDATE_IO) {
			IOTester tester = IOTester.findInstance(""+client.clientId);
			tester.addReceiveRecord(clone);
		}
	}
	
	
	@Override
	public ClientData createClientDataInstance(User user, StreamAnonSocketMix socket) {
		return new ALRR_ClientData(user, socket);
	}
	
	
	private class ALRR_ClientData extends ClientData {
		
		ApplicationLevelMessage currentRequest = new ApplicationLevelMessage();
		BasicOutputStreamMix outputStream;
		private ConcurrentLinkedQueue<ApplicationLevelMessage> replyTasks;
		private AtomicInteger availableReplyData = new AtomicInteger(0);
		private ByteBuffer leftOver;
		
		
		public ALRR_ClientData(User user, StreamAnonSocketMix socket) {
			super(user, socket);
			if (mix.IS_DUPLEX) {
				this.outputStream = (BasicOutputStreamMix)socket.getOutputStream();
				this.replyTasks = new ConcurrentLinkedQueue<ApplicationLevelMessage>();
				this.leftOver = ByteBuffer.allocate(0);
				this.leftOver.flip();
			}
		}
		
		
		public void addReplyTask(ApplicationLevelMessage replyTask) {
			replyTasks.add(replyTask);
			availableReplyData.addAndGet(replyTask.getReplySize());
		}
		
		
		public byte[] getReplyData(int amount) {
			if (amount > availableReplyData.get())
				System.err.println("not enough data available");
			else if (amount == 0)
				return null;
			byte[] result = new byte[amount];
			if (leftOver.remaining() >= amount) {
				leftOver.get(result);
			} else { // leftOver.remaining() < amount
				ByteBuffer resultBuffer = ByteBuffer.wrap(result);
				if (leftOver.hasRemaining())
					resultBuffer.put(leftOver);
				while(resultBuffer.hasRemaining()) {
					ApplicationLevelMessage nextEntry;
					nextEntry = replyTasks.remove();
					byte[] payload = nextEntry.createPayloadForReply();
					String stats = "DISTANT_PROXY: sending reply (" +
							"transactionId: " +nextEntry.getTransactionId() 
							+ "; replySize: " +nextEntry.getReplySize() +"bytes"
							+ "; replyDelay: " +nextEntry.getReplyDelayInMilliSec() +"ms"
							+ ")";
					System.out.println(stats); 
					//System.err.println("mix sending reply (" +payload.length +" bytes): " +Arrays.toString(payload)); // TODO: remove
					if (payload.length <= resultBuffer.remaining()) {
						resultBuffer.put(payload);
					} else { // more data than needed
						byte[][] splitted = Util.split(resultBuffer.remaining(), payload);
						resultBuffer.put(splitted[0]);
						leftOver = ByteBuffer.wrap(splitted[1]);
					}
				}	
			}
			availableReplyData.set(availableReplyData.get() - result.length);
			return result;	
		}

		
		public int availableReplyData() {
			return availableReplyData.get();
		}
	}

	
	
	public class SendReplyTask implements Runnable {
		
		private ALRR_ClientData clientData;
		ApplicationLevelMessage replyTask;
		
		
		public SendReplyTask(ALRR_ClientData clientData, ApplicationLevelMessage replyTask) {
			this.clientData = clientData;
			this.replyTask = replyTask;
		}
		
		
		@Override 
		public void run() {
			//System.err.println("execute"); // TODO: remove
			synchronized (newReplyTasks) {
				clientData.addReplyTask(replyTask);
				newReplyTasks.add(clientData);
			}
		}
	}
	
	
	private class ReplyThread extends Thread {
		
		@Override
		public void run() {
			while (true) {
				// add new reply tasks
				synchronized(newReplyTasks) {
					for (ALRR_ClientData replyTask: newReplyTasks)
						replyTasks.add(replyTask);
				}
				// try to write replies
				int writeCtr = 0;
				ALRR_ClientData replyTask;
				for (int i=0; i<replyTasks.size(); i++) { 
					replyTask = replyTasks.elementAt(i);
					try {
						// wait for free solt:
						int tries = 0;
						while (mix.getReplyInputQueue().remainingCapacity() == 0) {
							if (tries++ < 10)
								Thread.yield();
							else
								try {Thread.sleep(1);} catch (InterruptedException e) {e.printStackTrace();continue;} // TODO: wait-notify?
						}
						int payloadSize = Math.min(replyTask.outputStream.getMTU(), replyTask.availableReplyData());
						if (payloadSize > 0) {
							byte[] payload = replyTask.getReplyData(payloadSize);
							if (LoadGenerator.VALIDATE_IO)
								IOTester.findInstance("reply-"+replyTask.clientId).addSendRecord(payload);
							replyTask.socket.getOutputStream().write(payload);
							replyTask.socket.getOutputStream().flush();
							writeCtr++;
						}
					} catch (IOException e) {
						e.printStackTrace();
						continue;
					}
				}
				if (writeCtr == 0)
					try {Thread.sleep(1);} catch (InterruptedException e) {e.printStackTrace();}
			}
		}
	
	}
}
