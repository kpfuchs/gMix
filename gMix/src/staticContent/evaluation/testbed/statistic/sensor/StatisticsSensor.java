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
package staticContent.evaluation.testbed.statistic.sensor;

import java.io.File;
import java.util.Random;

import staticContent.framework.clock.Clock;
import staticContent.framework.config.Settings;

public class StatisticsSensor {

	public static boolean DISPLAY_THROUGHPUT;
	public static boolean DISPLAY_STATS_PER_USER;
	public static boolean DISPLAY_QUEUE_STATUS;
	public static boolean DISPLAY_MESSAGE_DWELL_TIMES;
	
	private static int DISPLAY_STAT_PERIOD;
	private static boolean initDone = false;
	private static Object synchronizer = new Object();
	private static long sumOfRequestDataReceivedInPriod = 0;
	private static long sumOfReplyDataReceivedInPriod = 0;
	private static long startTime;
	
	protected static Clock clock = null;
	
	public static void init() {
		if (initDone)
			return;
		initDone = true;
		
		// create clock
		String generalConfigPath = System.getProperty("user.dir") +"/framework/inputOutput/anonNode/generalConfig.txt";
		if (new File(generalConfigPath).exists()) {
			clock = new Clock(new Settings(generalConfigPath));
		}		
		
		DISPLAY_STAT_PERIOD = 1000; // static
		DISPLAY_THROUGHPUT = true;
		new DisplayThread().start();
	}
	
	
	public static void addRequestThroughputRecord(int ammountOfData) {
		if (DISPLAY_THROUGHPUT) {
			synchronized (synchronizer) {
				sumOfRequestDataReceivedInPriod += ammountOfData;
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
	
	protected static long getTime() {
		if (clock != null) {
			return clock.getTime();
		}
		
		return System.currentTimeMillis();
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
					String output = "\n";
					if (DISPLAY_THROUGHPUT) {
						output += getThroughputStatistics();
						System.out.println(output); 
					}
				} 
			}
			
		}


		private String getThroughputStatistics() {
			long duration = getTime() - startTime;
			startTime = getTime();
			String output = "";
			
			output += "Sensor|THROUGHPUT_REQUEST|"+startTime+"|"+((sumOfRequestDataReceivedInPriod/duration)*1000)+"\n";
			output += "Sensor|THROUGHPUT_REPLY|"+startTime+"|"+((sumOfReplyDataReceivedInPriod/duration)*1000)+"\n";
			output += "Sensor|THROUGHPUT_REQUEST_AND_REPLY|"+startTime+"|"+(((sumOfRequestDataReceivedInPriod+sumOfReplyDataReceivedInPriod)/duration)*1000);
			
			sumOfRequestDataReceivedInPriod = 0;
			sumOfReplyDataReceivedInPriod = 0;
			return output;
		}
	}
	
	
	public static void main(String[] args) {
		StatisticsSensor.init();
		Random r = new Random();
		while(true) {
			int ammountOfData = r.nextInt(1500-20+1)+20;

			StatisticsSensor.addRequestThroughputRecord(ammountOfData);
			
			ammountOfData = r.nextInt(1500-20+1)+20;
			
			StatisticsSensor.addReplyThroughputRecord(ammountOfData);
			
			int sleepTime = r.nextInt(10-0+1)+0;
			
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
