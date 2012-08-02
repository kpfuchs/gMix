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

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;


public class YieldWaitScheduler<E> implements Scheduler<E> {

	private final static long SLEEP_PRECISION = TimeUnit.MILLISECONDS.toNanos(2); // TODO: determine for current machine
	private final static long INIT_TIME = System.nanoTime();
	
	private LinkedList<QueueEntry> eventQueue = new LinkedList<QueueEntry>();
	
	private long latestExecutionScheduled = Long.MIN_VALUE;
	private QueueEntry lastEntry = null;
	private boolean isFirstEntry = true;
	private boolean shutdownRequested = false;
	private long tolerance; // in nanosec
	private int positionTmp;
	private volatile boolean interruptSleep = false;
	private volatile boolean executorThreadWaiting = false;
	private ExecutorThread executorThread;
	
	// @tolerance in microsec (uses microsec as nanosec would imply a higher accuracy than available)
	public YieldWaitScheduler(long tolerance) {
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
			this.executorThread = new ExecutorThread();
			this.executorThread.start();
		}
		QueueEntry queueEntry = new QueueEntry(executionTime, scheduleTarget, attachment);
		if (executionTime > this.latestExecutionScheduled) {
			this.latestExecutionScheduled = executionTime;
			this.lastEntry = queueEntry;
		}
		synchronized (executorThread) {
			int position = findPosition(queueEntry);
			if (position == 0)
				interruptSleep = true;
			this.eventQueue.add(position, queueEntry);
			if (executorThreadWaiting) {
				executorThreadWaiting = false;
				executorThread.notifyAll();
			}
		}
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
			} while (timeLeft > 0 && !interruptSleep);
		} catch (InterruptedException e) {
			if (timeLeft > 0 && !interruptSleep)
				sleepNanos(end - now());
		}
	}
	

	private class ExecutorThread extends Thread {
		
		@Override
		public void run() {
			exit: while (true) {
				QueueEntry task = null;
				synchronized (executorThread) {
					while (eventQueue.size() == 0 && !shutdownRequested) {
						executorThreadWaiting = true;
						try {
							executorThread.wait();
						} catch (InterruptedException e) {
							continue;
						}
					}
					if (!shutdownRequested)
						task = eventQueue.get(0);
					else
						break exit;
				}
				if (now() >= task.executionTime) { // execute now
					warnIfDelayed(now() - task.executionTime, task);
					if (task.notifyTarget != null)
						task.notifyTarget.execute(task.attachment);
					task.scheduleTarget.execute(task.attachment);
					synchronized (executorThread) {
						eventQueue.remove();
					}
				} else { // wait till execute
					sleepNanos(task.executionTime - now());
					if (shutdownRequested)
						break exit;
					synchronized (executorThread) {
						task = eventQueue.remove(); // always use first queue element (may be another element after sleep)
						if (interruptSleep)
							interruptSleep = false;
					}
					warnIfDelayed(now() - task.executionTime, task);
					if (task.notifyTarget != null)
						task.notifyTarget.execute(task.attachment);
					task.scheduleTarget.execute(task.attachment);
				}
			}
		}
	}
	
	
	private class QueueEntry implements Comparable<QueueEntry> {
		
		long executionTime;
		ScheduleTarget<E> scheduleTarget;
		E attachment;
		
		ScheduleTarget<E> notifyTarget;
		
		
		private QueueEntry(long executionTime, ScheduleTarget<E> scheduleTarget, E attachment) {
			this.executionTime = executionTime;
			this.scheduleTarget = scheduleTarget;
			this.attachment = attachment;
		}


		/**
		 * Implements the <code>Comparable</code> interface's <code>compareTo()
		 * </code> method. Compares this <code>Event</code> with the specified 
		 * <code>Event</code> for order (criterion: alphabetic order of this 
		 * <code>Event</code>'s <code>executionTime</code>. Returns a negative 
		 * integer, zero, or a positive integer as this <code>Event</code> is 
		 * less than, equal to, or greater than the specified <code>Event</code>.
		 * 
		 * @param event		The <code>Event</code> to be compared.
		 * 
		 * @return			-1, 0, or 1 as this <code>Event</code> is less than, 
		 * 					equal to, or greater than the specified <code>Event
		 * 					</code>.
		 */
		@Override
		public int compareTo(QueueEntry e) {
			if (this.executionTime < e.executionTime)
				return -1;
			else if (this.executionTime > e.executionTime)
				return 1;
			else
				return 0;
		}
	}
	
	
	private int findPosition(QueueEntry entry) {
		return findPosition(entry, 0, (eventQueue.size() - 1));
	}
	
	
	private int findPosition(QueueEntry entry, int startIndex, int endIndex) {
		if (eventQueue.size() == 0) { // first message
			return 0;
		} else {
			if (startIndex <= endIndex) {
				int mid = (startIndex + endIndex) / 2;
				switch (entry.compareTo(eventQueue.get(mid))) {
					case -1:
						positionTmp = mid;
						findPosition(entry, startIndex, mid - 1);
						break;					
					case  0:
						positionTmp = mid;
						startIndex = endIndex; // stop execution
						break;						
					case  1:
						positionTmp = mid + 1;
						findPosition(entry, mid + 1, endIndex);
						break;
				}
			}
		}
		return positionTmp;
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


/*	@Override
	public void notifyOnOutputOf(E e, ScheduleTarget<E> scheduleTarget) {
		synchronized (executorThread) {
			for (int i=eventQueue.size()-1; i>=0; i--) {
				if (eventQueue.get(i).attachment == e) {
					eventQueue.get(i).notifyTarget = scheduleTarget;
					return;
				}
			}
			throw new RuntimeException("not found"); 
		}
	}*/
	
}
