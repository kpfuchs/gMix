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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


// warning: this scheduler does not allow scheduling tasks with an execution time earlier than the task with the latest execution time already schedules
public class InOrderYieldWaitScheduler<E> implements Scheduler<E> {

	private final static long SLEEP_PRECISION = TimeUnit.MILLISECONDS.toNanos(2); // TODO: determine for current machine
	private final static long INIT_TIME = System.nanoTime();
	
	private LinkedBlockingQueue<QueueEntry> eventQueue = new LinkedBlockingQueue<QueueEntry>();
	
	private long latestExecutionScheduled = Long.MIN_VALUE;
	private QueueEntry lastEntry = null;
	private boolean isFirstEntry = true;
	private boolean shutdownRequested = false;
	private long tolerance; // in nanosec
	
	
	// @tolerance in microsec (uses microsec as nanosec would imply a higher accuracy than available)
	public InOrderYieldWaitScheduler(long tolerance) {
		this.tolerance = TimeUnit.MICROSECONDS.toNanos(tolerance);
	}
	
	
	@Override
	public void shutdown() {
		this.shutdownRequested = true;
	}
	
	
	@Override
	public synchronized void executeAt(long executionTime, ScheduleTarget<E> scheduleTarget, E attachment) {
		if (isFirstEntry) {
			isFirstEntry = false;
			new ExecutorThread().start();
		}
		if (executionTime < latestExecutionScheduled)
			throw new RuntimeException("scheduleTargets must be added in chronological order"); 
		this.latestExecutionScheduled = executionTime;
		this.lastEntry = new QueueEntry(executionTime, scheduleTarget, attachment);
		this.eventQueue.add(lastEntry);
	}

	
	@Override
	public synchronized void executeIn(long delayInNanoSec, ScheduleTarget<E> scheduleTarget, E attachment) {
		executeAt(now()+delayInNanoSec, scheduleTarget, attachment);
	}
	
	
	@Override
	public long now() {
		return System.nanoTime() - INIT_TIME;
	}
	
	
	@Override
	public void notifyOnOutputOfLast(ScheduleTarget<E> notifyTarget) {
		this.lastEntry.notifyTarget = notifyTarget;
	}
	
	
	private void sleepNanos(long nanoDuration) {
		// see http://andy-malakov.blogspot.de/2010/06/alternative-to-threadsleep.html
		final long end = now() + nanoDuration;
		long timeLeft = nanoDuration;
		try {
			do {
				if (timeLeft > SLEEP_PRECISION)
					Thread.sleep(1);
				else
					Thread.yield();
				timeLeft = end - now();
				if (Thread.interrupted())
					throw new InterruptedException();
			} while (timeLeft > 0);
		} catch (InterruptedException e) {
			sleepNanos(end - now());
		}
	}
	
	
	private class ExecutorThread extends Thread {
		
		@Override
		public void run() {
			exit: while (true) {
				QueueEntry task = null;
				while (true) { // get next event from queue (interrupt from time to time to check if a shutdown was requested)
					try {
						task = eventQueue.poll(2, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (task != null)
						break;
					else if (shutdownRequested)
						break exit;
				}
				if (now() >= task.executionTime) { // execute now
					warnIfDelayed(now() - task.executionTime, task);
					if (task.notifyTarget != null)
						task.notifyTarget.execute(task.attachment);
					task.scheduleTarget.execute(task.attachment);
				} else { // wait till execute
					sleepNanos(task.executionTime - now());
					warnIfDelayed(now() - task.executionTime, task);
					if (task.notifyTarget != null)
						task.notifyTarget.execute(task.attachment);
					task.scheduleTarget.execute(task.attachment);
				}
			}
		}
	}
	
	
	private class QueueEntry {
		
		long executionTime;
		ScheduleTarget<E> scheduleTarget;
		E attachment;
		
		ScheduleTarget<E> notifyTarget;
		
		
		private QueueEntry(long executionTime, ScheduleTarget<E> scheduleTarget, E attachment) {
			this.executionTime = executionTime;
			this.scheduleTarget = scheduleTarget;
			this.attachment = attachment;
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
