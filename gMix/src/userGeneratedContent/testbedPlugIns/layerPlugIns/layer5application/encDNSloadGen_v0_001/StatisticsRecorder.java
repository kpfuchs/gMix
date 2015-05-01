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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.encDNSloadGen_v0_001;

import java.util.concurrent.TimeUnit;


public class StatisticsRecorder {
	
	private static int DISPLAY_STAT_PERIOD;
	private static int[] rpacketCounterPeriod;
	private static long packetCounterTotal = 0;
	
	private static Object[] synchronizers;
	
	private static long startTime;
	
	private static int treadIdCounter = -1;
	

	public static void init(int DISPLAY_STAT_PERIOD, int NUMBER_OF_THREADS) {
		StatisticsRecorder.DISPLAY_STAT_PERIOD = DISPLAY_STAT_PERIOD;
		synchronizers = new Object[NUMBER_OF_THREADS];
		rpacketCounterPeriod = new int[NUMBER_OF_THREADS];
		for (int i=0; i<synchronizers.length; i++)
			synchronizers[i] = new Object();
		startTime = System.nanoTime();
		new DisplayThread().start();
	}
	
	
	public static void messageSent(int threadId) {
		synchronized (synchronizers[threadId]) {
			rpacketCounterPeriod[threadId]++;
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
				System.out.println("STATISTICS:" +getStatistics());  
			}
			
		}


		private String getStatistics() {
			
			String output = "";
			long duration = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
			
			// aggregate data:
			int requestPacketCounterP = 0;
			for (int i=0; i<synchronizers.length; i++) {
				synchronized (synchronizers[i]) {
					requestPacketCounterP += rpacketCounterPeriod[i];
					rpacketCounterPeriod[i] = 0;
				}
			} 
			packetCounterTotal += requestPacketCounterP;
			output += "sending " +(((double)requestPacketCounterP/(double)duration)*1000d) 
					+" requests/sec (muasure period: " +duration +"ms"
					+", total packets sent so far: " +packetCounterTotal +")";
			
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