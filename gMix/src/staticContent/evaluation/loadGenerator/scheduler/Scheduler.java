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
package staticContent.evaluation.loadGenerator.scheduler;

import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import staticContent.framework.config.Settings;
import gnu.trove.TDoubleArrayList;


public abstract class Scheduler<E> {

	protected long toleranceInNs;
	protected boolean LOG_TOLERANCE;
	protected boolean loggingActive = false;
	protected long startRecordingAfterInMs;
	protected long recordDurationInMs;
	private TDoubleArrayList delayDeviationsInNs;
	
	
	public abstract void executeAt(long executionTime, ScheduleTarget<E> scheduleTarget, E attachment);
	public abstract void executeIn(long delayInNanoSec, ScheduleTarget<E> scheduleTarget, E attachment);

	public abstract long now();
	
	public abstract void notifyOnOutputOfLast(ScheduleTarget<E> scheduleTarget); // notify the "scheduleTarget", when the last "element" currently scheduled is executed

	public abstract void shutdown();
	
	
	public Scheduler(Settings settings) {
		this.toleranceInNs = TimeUnit.MICROSECONDS.toNanos(settings.getPropertyAsLong("TOLERANCE"));
		this.LOG_TOLERANCE = (!settings.isPropertyPresent("LOG_TOLERANCE")) ? false:  settings.getPropertyAsBoolean("LOG_TOLERANCE");
		if (LOG_TOLERANCE) {
			this.delayDeviationsInNs = new TDoubleArrayList(100000);
			this.startRecordingAfterInMs = settings.getPropertyAsLong("LOG_TOLERANCE_BEGIN_RECORDING_AFTER");
			this.recordDurationInMs = settings.getPropertyAsLong("LOG_TOLERANCE_RECORDING_DURATION");
			startRecordingAfter();
		}
	}
	
	
	public void executeAt(long executionTime, ScheduleTarget<E> scheduleTarget) {
		executeAt(executionTime, scheduleTarget, null);
	}
	
	
	public void executeIn(long delayInNanoSec, ScheduleTarget<E> scheduleTarget) {
		executeIn(delayInNanoSec, scheduleTarget, null);
	}
	
	
	protected void warnIfDelayed(long unintendedDelayInNs) {
		if (LOG_TOLERANCE)
			this.delayDeviationsInNs.add(unintendedDelayInNs);
		//System.out.println("delay: " +unintendedDelayInNs); 
		if (unintendedDelayInNs > toleranceInNs)
			System.err.println(
					"warning: schedule executor more than " 
					+((float)toleranceInNs/1000000f) +"ms behind schedule. a delay of " 
					+((float)unintendedDelayInNs/1000000f) +"ms was measured"
				); 
	}
	
	
	private void startRecordingAfter() {
		new Thread(
				new Runnable() {
					public void run() {
						try {
							Thread.sleep(startRecordingAfterInMs);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						stopRecordingAfter();
						loggingActive = true;
					}
				}
			).start();
	}
	
	
	private void stopRecordingAfter() {
		new Thread(
				new Runnable() {
					public void run() {
						try {
							Thread.sleep(recordDurationInMs);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						loggingActive = false;
						shutdown();
						double[] all = delayDeviationsInNs.toNativeArray();
						DescriptiveStatistics ds = new DescriptiveStatistics(all);
						//TimeUnit.NANOSECONDS.toMillis(unintendedDelayInNs)
						System.out.println("measured delay-deviations (" +ds.getN() +" entries):");
						System.out.println("max: " +ds.getMax()/1000d +" microSec"); 
						System.out.println("min: " +ds.getMin()/1000d +" microSec"); 
						System.out.println("avg: " +ds.getMean()/1000d +" microSec");
						System.out.println("p50: " +ds.getPercentile(50d)/1000d +" microSec");
						System.out.println("p75: " +ds.getPercentile(75d)/1000d +" microSec");
						System.out.println("p90: " +ds.getPercentile(90d)/1000d +" microSec");
						System.out.println("p95: " +ds.getPercentile(95d)/1000d +" microSec");
						System.out.println("p99: " +ds.getPercentile(99d)/1000d +" microSec");
						System.out.println("stdev: " +ds.getStandardDeviation()/1000d +" microSec"); 
						System.exit(0);
					}
				}
			).start();
	}
}
