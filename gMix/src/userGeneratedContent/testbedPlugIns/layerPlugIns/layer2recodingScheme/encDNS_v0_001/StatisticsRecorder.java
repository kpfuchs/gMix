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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer2recodingScheme.encDNS_v0_001;

import java.util.concurrent.TimeUnit;


public class StatisticsRecorder {

	private static boolean DISPLAY_THROUGHPUT;
	private static boolean DISPLAY_PACKETS;
	
	private static int DISPLAY_STAT_PERIOD;

	private static long[] sumOfRequestDataReceivedInPeriod;
	private static long[] sumOfReplyDataReceivedInPeriod;
	private static long sumOfRequestDataReceivedInTotal = 0;
	private static long sumOfReplyDataReceivedInTotal = 0;
	
	private static int[] requestPacketCounterPeriod;
	private static int[] replyPacketCounterPeriod;
	private static int requestPacketCounterTotal = 0;
	private static int replyPacketCounterTotal = 0;
	
	private static Object[] synchronizers;
	
	private static long startTime;
	
	private static int treadIdCounter = -1;
	

	public static void init(boolean DISPLAY_THROUGHPUT, boolean DISPLAY_PACKETS, int DISPLAY_STAT_PERIOD, int NUMBER_OF_THREADS) {
		StatisticsRecorder.DISPLAY_THROUGHPUT = DISPLAY_THROUGHPUT;
		StatisticsRecorder.DISPLAY_PACKETS = DISPLAY_PACKETS;
		StatisticsRecorder.DISPLAY_STAT_PERIOD = DISPLAY_STAT_PERIOD;
		sumOfRequestDataReceivedInPeriod = new long[NUMBER_OF_THREADS];
		sumOfReplyDataReceivedInPeriod = new long[NUMBER_OF_THREADS];
		requestPacketCounterPeriod = new int[NUMBER_OF_THREADS];
		replyPacketCounterPeriod = new int[NUMBER_OF_THREADS];
		synchronizers = new Object[NUMBER_OF_THREADS];
		for (int i=0; i<synchronizers.length; i++)
			synchronizers[i] = new Object();
		if (DISPLAY_THROUGHPUT || DISPLAY_PACKETS)
			new DisplayThread().start();
	}
	
	
	public static void addRequestThroughputRecord(int ammountOfData, int threadId) {
		if (DISPLAY_THROUGHPUT || DISPLAY_PACKETS) {
			synchronized (synchronizers[threadId]) {
				sumOfRequestDataReceivedInPeriod[threadId] += ammountOfData;
				requestPacketCounterPeriod[threadId]++;
			} 
		}
	}
	
	
	public static void addReplyThroughputRecord(int ammountOfData, int threadId) {
		if (DISPLAY_THROUGHPUT || DISPLAY_PACKETS) {
			synchronized (synchronizers[threadId]) {
				sumOfReplyDataReceivedInPeriod[threadId] += ammountOfData;
				replyPacketCounterPeriod[threadId]++;
			} 
		}
	}
	
	
	
	static class DisplayThread extends Thread {
		
		@Override
		public void run() {
			startTime = System.nanoTime();
			while (true) { 
				try {
					Thread.sleep(DISPLAY_STAT_PERIOD);
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				}
				System.out.println("STATISTICS:" +getStatistics());  
			}
			
		}


		private String getStatistics() {
			
			String output = "";
			long duration = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
			
			// aggregate data:
			long sumOfRequestDataP = 0;
			long sumOfReplyDataP = 0;
			int requestPacketCounterP = 0;
			int replyPacketCounterP = 0;
			for (int i=0; i<synchronizers.length; i++) {
				synchronized (synchronizers[i]) {
					sumOfRequestDataP += sumOfRequestDataReceivedInPeriod[i];
					sumOfRequestDataReceivedInPeriod[i] = 0;
					sumOfReplyDataP += sumOfReplyDataReceivedInPeriod[i];
					sumOfReplyDataReceivedInPeriod[i] = 0;
					requestPacketCounterP += requestPacketCounterPeriod[i];
					requestPacketCounterPeriod[i] = 0;
					replyPacketCounterP += replyPacketCounterPeriod[i];
					replyPacketCounterPeriod[i] = 0;
				}
			} 
			
			if (DISPLAY_THROUGHPUT) {
				
				sumOfRequestDataReceivedInTotal += sumOfRequestDataP;
				sumOfReplyDataReceivedInTotal += sumOfReplyDataP;
				
				output += "\n  total request-THROUGHPUT: ";
				output += humanReadableByteCount((sumOfRequestDataP/duration)*1000, false) +"/sec";
				output += " (" +humanReadableByteCount((sumOfRequestDataP/duration)*1000, true) +"/sec";
				output += ", measurePeriod: "+duration  +" ms";
				output += ", transmitted so far: " +humanReadableByteCount(sumOfRequestDataReceivedInTotal, false) +" = " +humanReadableByteCount(sumOfRequestDataReceivedInTotal, true);
				output += ")";
				
				if (sumOfReplyDataP != 0) {
					
					output += "\n  total reply-THROUGHPUT: ";
					output += humanReadableByteCount((sumOfReplyDataP/duration)*1000, false) +"/sec";
					output += " (" +humanReadableByteCount((sumOfReplyDataP/duration)*1000, true) +"/sec";
					output += ", measurePeriod: "+duration  +" ms";
					output += ", transmitted so far: " +humanReadableByteCount(sumOfReplyDataReceivedInTotal, false) +" = " +humanReadableByteCount(sumOfReplyDataReceivedInTotal, true);
					output += ")";
					
				}
			}
			
			
			if (DISPLAY_PACKETS) {
				requestPacketCounterTotal += requestPacketCounterP;
				replyPacketCounterTotal += replyPacketCounterP;
				
				output += "\n  total requests/sec: ";
				output += (((double)requestPacketCounterP/(double)duration)*1000d);
				output += ", measurePeriod: "+duration  +" ms";
				output += ", total request packets so far: " +requestPacketCounterTotal;
				output += ")";
				
				if (replyPacketCounterTotal != 0) {
					
					output += "\n  total replies/sec: ";
					output += ((double)(replyPacketCounterP/(double)duration)*1000d);
					output += ", measurePeriod: "+duration  +" ms";
					output += ", total request packets so far: " +replyPacketCounterTotal;
					output += ")";
					
				}
			}
			
			startTime = System.nanoTime();
			return output;
		}
	}
	
	public static synchronized int getThreadId() {
		return ++treadIdCounter;
	}
	
	public static String humanReadableByteCount(long bytes, boolean si) {
		// see http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
}