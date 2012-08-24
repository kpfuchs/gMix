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
package evaluation.loadGenerator.dynamicSchedule;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import evaluation.loadGenerator.LoadGenerator;
import evaluation.loadGenerator.applicationLevelTraffic.requestReply.ApplicationLevelMessage;
import evaluation.loadGenerator.applicationLevelTraffic.requestReply.ALRR_ClientWrapper;
import evaluation.loadGenerator.applicationLevelTraffic.requestReply.EndOfFileReachedException;
import evaluation.loadGenerator.scheduler.ScheduleTarget;
import evaluation.loadGenerator.scheduler.Scheduler;
import evaluation.loadGenerator.scheduler.ThreadPoolScheduler;
import framework.core.AnonNode;
import framework.core.config.Paths;
import framework.core.config.Settings;
import framework.core.launcher.ToolName;
import framework.core.routing.RoutingMode;
import framework.core.socket.socketInterfaces.AnonSocketOptions.CommunicationMode;
import framework.core.util.IOTester;
import framework.core.util.Util;


public class ALM_DS_Tracefile {

	private Settings settings;
	private HashMap<Integer, ALRR_ClientWrapper> clientReferences;
	private ALRR_ClientWrapper[] clientsArray;
	private long experimentStart; // in nanosec
	private AnonNode client;
	private BufferedReader traceReader;
	private int lineCounter = 0;
	private Scheduler<ALRR_ClientWrapper> scheduler;
	private boolean readAheadRequired = true;
	private boolean stopExecution = false;
	private volatile boolean traceFileReaderActive = false; // used for weak overload detection
	private boolean hasMore = true;
	private int READ_AHEAD;
	private TraceFileReaderThread traceFileReaderThread;
	private ReplyReceiverThread replyReceiverThread;
	private RequestSender requestSender;
	
	
	public ALM_DS_Tracefile(LoadGenerator owner) {
		this.settings = owner.settings;
		this.scheduler = new ThreadPoolScheduler<ALRR_ClientWrapper>(settings.getPropertyAsInt("TOLERANCE")); // TODO: offer further schedulers
		this.traceFileReaderThread = new TraceFileReaderThread();
		this.replyReceiverThread = new ReplyReceiverThread();
		this.requestSender = new RequestSender();
		this.experimentStart = scheduler.now() + TimeUnit.SECONDS.toNanos(2);
		this.READ_AHEAD = settings.getPropertyAsInt("AL-TRACE_FILE-READ_AHEAD");
		System.out.println("LOAD_GENERATOR: start at " +experimentStart);
		
		// try to load trace-file:
		System.out.println("TRACE_READER: loading trace file"); 
		String traceFilePath = Paths.getProperty("LG_TRACE_FILE_PATH") + settings.getProperty("AL-TRACE_FILE-NAME");
		resetReader(traceFilePath);
		
		// create client
		owner.commandLineParameters.gMixTool = ToolName.CLIENT;
		this.client = new AnonNode(owner.commandLineParameters);
		
		// determine number of clients and lines; create ClientWrapper objects:
		this.clientReferences = new HashMap<Integer, ALRR_ClientWrapper>(1000); // TODO: dynamic
		String line;
		int ctr = 0;
		try {
			while ((line = traceReader.readLine()) != null) {
				ctr++;
				Integer id = Integer.parseInt(line.split("(,|;)")[1]);
				ALRR_ClientWrapper cw = clientReferences.get(id);
				if (cw == null) {
					cw = new ALRR_ClientWrapper(id);
					clientReferences.put(id, cw);
				}
				cw.TOTAL_TRANSACTIONS++;
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("TRACE_READER: error traversing trace file"); 
		}
		System.out.println("TRACE_READER: found traces for " +clientReferences.size() +" clients in the trace file (" +ctr +" trace entries)");
		resetReader(traceFilePath);
				
		// create array etc.:
		this.clientsArray = new ALRR_ClientWrapper[clientReferences.size()];
		int i = 0;
		for (ALRR_ClientWrapper cw: clientReferences.values()) {
			clientsArray[i++] = cw;
			cw.traceEntries = new ConcurrentLinkedQueue<ApplicationLevelMessage>();
		} 
						
		// generate and connect sockets
		for (ALRR_ClientWrapper cw: clientsArray) // generate sockets
			cw.socket = client.createStreamSocket(CommunicationMode.DUPLEX, client.ROUTING_MODE != RoutingMode.CASCADE);
		// connect sockets:
		int port = settings.getPropertyAsInt("SERVICE_PORT1");
		System.out.println("LOAD_GENERATOR: connecting clients..."); 
		for (ALRR_ClientWrapper cw: clientsArray) 
			try {
				cw.socket.connect(port);
				cw.outputStream = new BufferedOutputStream(cw.socket.getOutputStream());
				cw.inputStream = cw.socket.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// read first trace entries
		this.traceFileReaderThread.readAhead(); // let this thread read first entries to assure they are available in the next loop
		this.traceFileReaderThread.start(); // let TraceFileReaderThread read further entries (in the future)
		// schedule first request for each client
		long now = scheduler.now();
		for (ALRR_ClientWrapper cw: clientsArray) {
			cw.currentTraceEntry = cw.traceEntries.poll();
			cw.transactionCounter++;
			scheduler.executeIn(
					(now - experimentStart) + cw.currentTraceEntry.getSendDelayInNanoSec(), 
					requestSender, 
					cw
					);
		}
		
		// start reply receiver:
		this.replyReceiverThread.start();
	}

	
	private class TraceFileReaderThread extends Thread {
		
		@Override
		public void run() {
			while (hasMore) {
				synchronized (traceFileReaderThread) {
					while (!readAheadRequired) {
						try {
							traceFileReaderThread.wait();
						} catch (InterruptedException e) {
							continue;
						}
					}
					readAheadRequired = false;
				}
				if (!stopExecution)
					readAhead();
			}
		}

		
		private void readAhead() {
			synchronized (traceReader) {
				traceFileReaderActive = true;
				System.out.println("start reading from logfile (" +(READ_AHEAD/2) +" lines)"); 
				long start = System.currentTimeMillis();
				ApplicationLevelMessage currentEntry = null;
				for (int i=0; i<(READ_AHEAD/2); i++) {
					try {
						currentEntry = new ApplicationLevelMessage(traceReader, ++lineCounter);
					} catch (EndOfFileReachedException e) {
						lineCounter--;
						hasMore = false;
					}
					ALRR_ClientWrapper cw = clientReferences.get(currentEntry.getClientId());
					cw.traceEntries.add(currentEntry);
				} 
				currentEntry.isNotifyEvent = true;
				System.out.println("finished reading from logfile (" +(READ_AHEAD/2) +" lines, " +(System.currentTimeMillis()-start) +" ms)"); 
				traceFileReaderActive = false;
			}
		}
		
		
		// read till a line for the client with id "clientId" is found (and read some more lines)
		// called when RequestSender runs out of messages
		public void readAhead(int clientId) {
			synchronized (traceReader) {
				int ctr = 0;
				traceFileReaderActive = true;
				System.out.println("start reading from logfile"); 
				long start = System.currentTimeMillis();
				ApplicationLevelMessage currentEntry = null;
				while (true) {
					try {
						currentEntry = new ApplicationLevelMessage(traceReader, ++lineCounter);
					} catch (EndOfFileReachedException e) {
						lineCounter--;
						hasMore = false;
					}
					ctr++;
					if (!hasMore)
						throw new RuntimeException("no record for client " +clientId); 
					ALRR_ClientWrapper cw = clientReferences.get(currentEntry.getClientId());
					cw.traceEntries.add(currentEntry);
					if (cw.identifier == clientId)
						break;
				}
				if (hasMore) {
					for (int i=0; i<(READ_AHEAD/2); i++) { // read further lines
						try {
							currentEntry = new ApplicationLevelMessage(traceReader, ++lineCounter);
						} catch (EndOfFileReachedException e) {
							lineCounter--;
							hasMore = false;
						}
						ctr++;
						ALRR_ClientWrapper cw = clientReferences.get(currentEntry.getClientId());
						cw.traceEntries.add(currentEntry);
					} 
					currentEntry.isNotifyEvent = true;
				}
				System.out.println("finished reading from logfile (" +ctr +" lines, " +(System.currentTimeMillis()-start) +" ms)"); 
				traceFileReaderActive = false;
			}
		}
		
		
		public void wakeUp() {
			if (traceFileReaderActive)
				System.err.println("TRACE_READER: warning: it seems the "
					+"trace file can't be read fast enough. simulation may "
					+"be inaccurate!");
			synchronized (traceFileReaderThread) {
				readAheadRequired = true;
				traceFileReaderThread.notifyAll();
			}
		}
	}
	
	
	private class ReplyReceiverThread extends Thread {
		
		@Override
		public void run() {
			while (true) { // read replies...
				int ctr = 0;
				for (ALRR_ClientWrapper cw: clientsArray) { // ...for each client
					int available;
					try {
						available = cw.inputStream.available();
					} catch (IOException e) {
						e.printStackTrace();
						continue;
					}
					if (available > 0) { // if data available
						synchronized (cw) {
							try {
								ctr++;
								// read available data:
								byte[] arrivedData = new byte[available];
								int read = cw.inputStream.read(arrivedData);
								assert read == arrivedData.length: ""+read; // assured by mix io-streams
								if (LoadGenerator.VALIDATE_IO)
									IOTester.findInstance("reply-"+cw.identifier).addReceiveRecord(arrivedData);
								ApplicationLevelMessage reply = cw.currentTraceEntry;
								reply.addReplyChunk(arrivedData);
								if (!reply.needMoreReplyChunks()) { // reply now received completely 
									// display stats:
									long delay = System.currentTimeMillis() - reply.getAbsoluteSendTime();
									System.out.println(
											"LOAD_GENERATOR: received reply (" +
											"transactionId:" +reply.getTransactionId() 
											+"; replySize: " +reply.getReplySize() +"bytes"
											+"; delay: " +delay +"ms"
											+")"
											);
									// schedule next request
									cw.currentTraceEntry = cw.traceEntries.poll();
									cw.transactionCounter++;
									
									if (cw.currentTraceEntry == null) {
										if (cw.transactionCounter == cw.TOTAL_TRANSACTIONS) {
											continue;
										} else {
											System.err.println("warning AL-TRACE_FILE-READ_AHEAD set to a too low value; the messages of client " +cw.identifier +" may be deffered"); 
											traceFileReaderThread.readAhead(cw.identifier);
											cw.currentTraceEntry = cw.traceEntries.poll();
										}
									}
									if (cw.currentTraceEntry.isNotifyEvent)
										traceFileReaderThread.wakeUp();
									scheduler.executeIn(
											cw.currentTraceEntry.getSendDelayInNanoSec(), 
											requestSender, 
											cw
											);
								}
							} catch (IOException e) {
								e.printStackTrace();
								continue;
							} 
						}
					}
				} 
				if (ctr == 0) // TODO wait-notify?
					try {Thread.sleep(1);} catch (InterruptedException e) {e.printStackTrace();}
			}
		}
	}

	
	private class RequestSender implements ScheduleTarget<ALRR_ClientWrapper> {

		@Override
		public void execute(ALRR_ClientWrapper cw) {
			synchronized (cw) {
				ApplicationLevelMessage message = cw.currentTraceEntry;
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
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	
	private void resetReader(String traceFilePath) {
		try {
			this.traceReader = new BufferedReader(new FileReader(traceFilePath));
		} catch (FileNotFoundException e) {
			System.err.println("TRACE_READER: could not find trace file: " + traceFilePath);
		}
	}
	
}
