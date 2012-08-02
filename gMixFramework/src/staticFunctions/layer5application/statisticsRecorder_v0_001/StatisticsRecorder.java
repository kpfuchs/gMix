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
package staticFunctions.layer5application.statisticsRecorder_v0_001;

import java.util.Vector;
import java.util.concurrent.TimeUnit;

import framework.core.AnonNode;
import framework.core.config.Settings;
import framework.core.message.Request;
import framework.core.userDatabase.User;
import framework.core.userDatabase.UserAttachment;
import framework.core.userDatabase.UserDatabase;
import framework.core.util.Util;


public class StatisticsRecorder {

	public static boolean DISPLAY_THROUGHPUT;
	public static boolean DISPLAY_STATS_PER_USER;
	public static boolean DISPLAY_QUEUE_STATUS;
	public static boolean DISPLAY_MESSAGE_DWELL_TIMES;
	
	private static int DISPLAY_STAT_PERIOD;
	private static UserDatabase userDatabase;
	private static Settings settings;
	private static boolean initDone = false;
	private static Object synchronizer = new Object();
	private static long sumOfRequestDataReceivedInPriod = 0;
	private static long sumOfRequestDataReceivedInTotal = 0;
	private static long sumOfReplyDataReceivedInPriod = 0;
	private static long sumOfReplyDataReceivedInTotal = 0;
	private static long startTime;
	private static Vector<AnonNode> mixes = new Vector<AnonNode>();
	private static Vector<AnonNode> clients = new Vector<AnonNode>();
	
	private static int[] statDelays = new int[1001];
	private static long excessiveDelayCounter = 0;
	private static long statTotalMessages = 0;
	private static long startStat = 0;
	
	
	// TODO: change to "register(AnonNode)" and allow recording statistics of multiple anonNodes (add parameter "AnonNode" to addRecord()-methods) 
	public static void init(AnonNode owner) {
		if (owner.IS_CLIENT)
			clients.add(owner);
		if (owner.IS_MIX)
			mixes.add(owner);
		if (initDone)
			return;
		initDone = true;
		userDatabase = owner.getUserDatabase();
		settings = owner.getSettings();
		
		DISPLAY_STAT_PERIOD = settings.getPropertyAsInt("DISPLAY_STAT_PERIOD");
		DISPLAY_THROUGHPUT = settings.getPropertyAsBoolean("DISPLAY_THROUGHPUT");
		DISPLAY_STATS_PER_USER = settings.getPropertyAsBoolean("DISPLAY_STATS_PER_USER");
		DISPLAY_QUEUE_STATUS = settings.getPropertyAsBoolean("DISPLAY_QUEUE_STATUS");
		DISPLAY_MESSAGE_DWELL_TIMES = settings.getPropertyAsBoolean("DISPLAY_MESSAGE_DWELL_TIMES");
		new DisplayThread().start();
	}
	
	
	public static void addRequestThroughputRecord(int ammountOfData) {
		if (DISPLAY_THROUGHPUT) {
			synchronized (synchronizer) {
				sumOfRequestDataReceivedInPriod += ammountOfData;
			} 
		}
	}
	
	
	public static void addMessageDwellTimeRecord(Request request) {
		if (DISPLAY_MESSAGE_DWELL_TIMES) {
			synchronized (synchronizer) {
				long dwellTime = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - request.statisticsCreateTime);
				statTotalMessages++;
				if (dwellTime < 100000)
					statDelays[(int) (dwellTime/100l)]++;
				else
					excessiveDelayCounter++;
			}
		}
	}
	
	
	public static void addReplyThroughputRecord(int ammountOfData) {
		if (DISPLAY_THROUGHPUT) {
			synchronized (synchronizer) {
				sumOfReplyDataReceivedInPriod += ammountOfData;
			} 
		}
	}
	
	
	public static void addRequestThroughputRecord(int ammountOfData, User user) {
		if (DISPLAY_THROUGHPUT) {
			synchronized (synchronizer) {
				sumOfRequestDataReceivedInPriod += ammountOfData;
				if (DISPLAY_STATS_PER_USER) {
					UserStatistics stats = user.getAttachment(synchronizer, UserStatistics.class);
					if (stats == null)
						stats = new UserStatistics(user);
					stats.sumOfRequestDataReceivedInPriod += ammountOfData;
				}
			} 
		}
	}
	
	
	public static void addReplyThroughputRecord(int ammountOfData, User user) {
		if (DISPLAY_THROUGHPUT) {
			synchronized (synchronizer) {
				sumOfReplyDataReceivedInPriod += ammountOfData;
				if (DISPLAY_STATS_PER_USER) {
					UserStatistics stats = user.getAttachment(synchronizer, UserStatistics.class);
					if (stats == null)
						stats = new UserStatistics(user);
					stats.sumOfReplyDataReceivedInPriod += ammountOfData;
				}
			} 
		}
	}
	
	
	static class UserStatistics extends UserAttachment {
		boolean ready = false;
		long sumOfRequestDataReceivedInPriod = 0;
		long sumOfRequestDataReceivedInTotal = 0;
		long sumOfReplyDataReceivedInPriod = 0;
		long sumOfReplyDataReceivedInTotal = 0;
		
		public UserStatistics(User owner) {
			super(owner, synchronizer);
		}
	}
	
	
	static class DisplayThread extends Thread {
		
		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(DISPLAY_STAT_PERIOD);
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				}
				synchronized (synchronizer) {
					String output = "STATISTICS:";
					if (DISPLAY_THROUGHPUT)
						output += getThroughputStatistics();
					if (DISPLAY_QUEUE_STATUS && !mixes.isEmpty())
						output += getQueueStatistics();
					if (DISPLAY_MESSAGE_DWELL_TIMES)
						output += getMessageDwellTimeStatistics();
					System.out.println(output); 
				} 
			}
			
		}

		
		private String getMessageDwellTimeStatistics() {
			String output = "\n  DWELL_TIME stats:";
			if (startStat == 0) { // ignore first round (init time etc)
				startStat = System.currentTimeMillis();
				excessiveDelayCounter = 0;
				statTotalMessages = 0;
				statDelays = new int[1001];
			} else {
				output += "\n   " +statTotalMessages +" messages in total";
				output += "\n   " +excessiveDelayCounter + " messages with excessive delays (>=100ms)";
				output += "\n   stats for delays below 100ms: ";
				for (int j=1; j<=statDelays.length; j++) {
					if (statDelays[j-1] != 0)
						output += "\n   " +statDelays[j-1] +"\tx between " +((j-1)*100) +"\tand " +(j*100) +"\tmicroseconds";
				} 
				
				// calculate 95 percentile // TODO
				long sum = 0;
				for (int j=1; j<=statDelays.length; j++) {
					sum += statDelays[j-1];
					double perc = (double)sum / (double)statTotalMessages;
					if (perc >= 0.95d) {
						output += "\n   " +(perc*100d) +"% of messages faster than " +(j*100) +" microseconds";
						break;
					}
				} 	
				excessiveDelayCounter = 0;
				statTotalMessages = 0;
				statDelays = new int[1001];
				startStat = System.currentTimeMillis();
			}
			return output;
		}
		
		
		private String getQueueStatistics() {
			String output = "\n  QUEUE_STATUS:";
			for (AnonNode mix:mixes) {
				output += "\n   " +mix +" status of RequestInputQueue: " +mix.getRequestInputQueue().size() +" of " +(mix.getRequestInputQueue().size()+mix.getRequestInputQueue().remainingCapacity()) +" occupied";
				output += "\n   " +mix +" status of RequestOutputQueue: " +mix.getRequestOutputQueue().size() +" of " +(mix.getRequestOutputQueue().size()+mix.getRequestOutputQueue().remainingCapacity()) +" occupied";
				if (mix.IS_DUPLEX) {
					output += "\n   " +mix +" status of ReplyInputQueue: " +mix.getReplyInputQueue().size() +" of " +(mix.getReplyInputQueue().size()+mix.getReplyInputQueue().remainingCapacity()) +" occupied";
					output += "\n   " +mix +" status of ReplyOutputQueue: " +mix.getReplyOutputQueue().size() +" of " +(mix.getReplyOutputQueue().size()+mix.getReplyOutputQueue().remainingCapacity()) +" occupied";
				}
			} 
			return output;
		}


		private String getThroughputStatistics() {
			long duration = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
			startTime = System.nanoTime();
			sumOfRequestDataReceivedInTotal += sumOfRequestDataReceivedInPriod;
			String output = "\n  total THROUGHPUT on last mix for request-channel(s): ";
			output += Util.humanReadableByteCount((sumOfRequestDataReceivedInPriod/duration)*1000, false) +"/sec";
			output += " (" +Util.humanReadableByteCount((sumOfRequestDataReceivedInPriod/duration)*1000, true) +"/sec";
			output += ", " +userDatabase.getNumberOfUsers() +" users" ;
			output += ", measurePeriod: "+duration  +" ms";
			output += ", transmitted so far: " +Util.humanReadableByteCount(sumOfRequestDataReceivedInTotal, false) +" = " +Util.humanReadableByteCount(sumOfRequestDataReceivedInTotal, true);
			output += ")";
			
			if (sumOfReplyDataReceivedInPriod != 0) {
				sumOfReplyDataReceivedInTotal += sumOfReplyDataReceivedInPriod;
				output += "\n  total THROUGHPUT on last mix for reply-channel(s): ";
				output += Util.humanReadableByteCount((sumOfReplyDataReceivedInPriod/duration)*1000, false) +"/sec";
				output += " (" +Util.humanReadableByteCount((sumOfReplyDataReceivedInPriod/duration)*1000, true) +"/sec";
				output += ", " +userDatabase.getNumberOfUsers() +" users" ;
				output += ", measurePeriod: "+duration  +" ms";
				output += ", transmitted so far: " +Util.humanReadableByteCount(sumOfReplyDataReceivedInTotal, false) +" = " +Util.humanReadableByteCount(sumOfReplyDataReceivedInTotal, true);
				output += ")";
				
				output += "\n  total THROUGHPUT on last mix (request- and reply-channel(s): ";
				output += Util.humanReadableByteCount(((sumOfRequestDataReceivedInPriod+sumOfReplyDataReceivedInPriod)/duration)*1000, false) +"/sec";
				output += " (" +Util.humanReadableByteCount(((sumOfRequestDataReceivedInPriod+sumOfReplyDataReceivedInPriod)/duration)*1000, true) +"/sec";
				output += ", " +userDatabase.getNumberOfUsers() +" users" ;
				output += ", measurePeriod: "+duration  +" ms";
				output += ", transmitted so far: " +Util.humanReadableByteCount(sumOfRequestDataReceivedInTotal+sumOfReplyDataReceivedInTotal, false) +" = " +Util.humanReadableByteCount(sumOfRequestDataReceivedInTotal+sumOfReplyDataReceivedInTotal, true);
				output += ")";
			}
			
			sumOfRequestDataReceivedInPriod = 0;
			sumOfReplyDataReceivedInPriod = 0;
			
			if (DISPLAY_STATS_PER_USER) {
				// TODO: evaluate fairness
				output += "\n  THROUGHPUT per user:";
				for (User user:userDatabase.getAllUsers()) {
					UserStatistics stats = user.getAttachment(synchronizer, UserStatistics.class);
					if (stats != null) {
						if (stats.ready) {
							stats.sumOfRequestDataReceivedInTotal += stats.sumOfRequestDataReceivedInPriod;
							output += "\n   " +user.toString().substring(28) +" (request-channel): ";
							output += Util.humanReadableByteCount((stats.sumOfRequestDataReceivedInPriod/duration)*1000, false) +"/sec";
							output += " (" +Util.humanReadableByteCount((stats.sumOfRequestDataReceivedInPriod/duration)*1000, true) +"/sec";
							output += ", transmitted so far: " +Util.humanReadableByteCount(stats.sumOfRequestDataReceivedInTotal, false) +" = " +Util.humanReadableByteCount(stats.sumOfRequestDataReceivedInTotal, true);
							output += ")";
							
							if (sumOfReplyDataReceivedInTotal != 0) {
								stats.sumOfReplyDataReceivedInTotal += stats.sumOfReplyDataReceivedInPriod;
								output += "\n   " +user.toString().substring(28) +" (reply-channel): ";
								output += Util.humanReadableByteCount((stats.sumOfReplyDataReceivedInPriod/duration)*1000, false) +"/sec";
								output += " (" +Util.humanReadableByteCount((stats.sumOfReplyDataReceivedInPriod/duration)*1000, true) +"/sec";
								output += ", transmitted so far: " +Util.humanReadableByteCount(stats.sumOfReplyDataReceivedInTotal, false) +" = " +Util.humanReadableByteCount(stats.sumOfReplyDataReceivedInTotal, true);
								output += ")";
								
								output += "\n   " +user.toString().substring(28) +" (request- and reply-channel): ";
								output += Util.humanReadableByteCount(((stats.sumOfRequestDataReceivedInPriod+stats.sumOfReplyDataReceivedInPriod)/duration)*1000, false) +"/sec";
								output += " (" +Util.humanReadableByteCount(((stats.sumOfRequestDataReceivedInPriod+stats.sumOfReplyDataReceivedInPriod)/duration)*1000, true) +"/sec";
								output += ", transmitted so far: " +Util.humanReadableByteCount(stats.sumOfRequestDataReceivedInTotal+stats.sumOfReplyDataReceivedInTotal, false) +" = " +Util.humanReadableByteCount(stats.sumOfRequestDataReceivedInTotal+stats.sumOfReplyDataReceivedInTotal, true);
								output += ")";
							}
							stats.sumOfRequestDataReceivedInPriod = 0;
							stats.sumOfReplyDataReceivedInPriod = 0;
							
						} else { // only count intervals where user was already connected at the beginning
							stats.ready = true;
						}
					}
				} 
			}
			return output;
		}
	}
	
}
