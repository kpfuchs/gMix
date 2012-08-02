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
package evaluation.loadGenerator.scheduler;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class ThreadPoolScheduler<E> implements Scheduler<E> {

	private ScheduledThreadPoolExecutor scheduler;
	private final static long INIT_TIME = System.nanoTime();
	private long latestExecutionScheduled = Long.MIN_VALUE;
	private QueueEntry lastEntry = null;
	private long tolerance; // in nanosec
	
	
	// @tolerance in microsec (uses microsec as nanosec would imply a higher accuracy than available)
	public ThreadPoolScheduler(long tolerance) {
		this.tolerance = TimeUnit.MICROSECONDS.toNanos(tolerance);
		this.scheduler = new ScheduledThreadPoolExecutor(4); // TODO: dynamic
	}


	@Override
	public synchronized void executeAt(long executionTime, ScheduleTarget<E> scheduleTarget, E attachment) {
		QueueEntry queueEntry = new QueueEntry(executionTime, scheduleTarget, attachment);
		if (executionTime > this.latestExecutionScheduled) {
			this.latestExecutionScheduled = executionTime;
			this.lastEntry = queueEntry;
		}
		long delay = executionTime - now();
		if (delay < 0)
			delay = 0;
		scheduler.schedule(queueEntry, delay, TimeUnit.NANOSECONDS);
	}


	@Override
	public synchronized void executeIn(long delayInNanoSec, ScheduleTarget<E> scheduleTarget, E attachment) {
		if (delayInNanoSec < 0)
			delayInNanoSec = 0;
		long executionTime = now() + delayInNanoSec;
		QueueEntry queueEntry = new QueueEntry(executionTime, scheduleTarget, attachment);
		if (executionTime > this.latestExecutionScheduled) {
			this.latestExecutionScheduled = executionTime;
			this.lastEntry = queueEntry;
		}
		scheduler.schedule(queueEntry, delayInNanoSec, TimeUnit.NANOSECONDS);
	}


	@Override
	public long now() {
		return System.nanoTime() - INIT_TIME;
	}


	@Override
	public void notifyOnOutputOfLast(ScheduleTarget<E> notifyTarget) {
		this.lastEntry.notifyTarget = notifyTarget;
	}


	@Override
	public void shutdown() {
		this.scheduler.shutdownNow();
	}
	
	
	private class QueueEntry implements Callable<E> {
		
		long executionTime;
		ScheduleTarget<E> scheduleTarget;
		E attachment;
		
		ScheduleTarget<E> notifyTarget;
		
		
		private QueueEntry(long executionTime, ScheduleTarget<E> scheduleTarget, E attachment) {
			this.executionTime = executionTime;
			this.scheduleTarget = scheduleTarget;
			this.attachment = attachment;
		}


		@Override
		public E call() throws Exception {
			warnIfDelayed(now() - this.executionTime, this);
			if (this.notifyTarget != null)
				this.notifyTarget.execute(this.attachment);
			this.scheduleTarget.execute(this.attachment);
			return this.attachment;
		}
	}
	
	
	private void warnIfDelayed(long unintendedDelay, QueueEntry task) {
		if (unintendedDelay > tolerance)
			System.err.println(
					"warning: schedule executor more than " 
					+((float)tolerance/1000000f) +"ms behind schedule (" 
					+((float)unintendedDelay/1000000f) +"ms) for " 
					+task.attachment
				); 
	}
	
}
