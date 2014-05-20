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
package evaluation.loadGenerator.scheduler;

import java.util.concurrent.TimeUnit;

import framework.core.config.Settings;


public abstract class Scheduler<E> {

	protected long tolerance; // in nanosec
	
	public abstract void executeAt(long executionTime, ScheduleTarget<E> scheduleTarget, E attachment);
	public abstract void executeIn(long delayInNanoSec, ScheduleTarget<E> scheduleTarget, E attachment);

	public abstract long now();
	
	public abstract void notifyOnOutputOfLast(ScheduleTarget<E> scheduleTarget); // notify the "scheduleTarget", when the last "element" currently scheduled is executed

	public abstract void shutdown();
	
	
	public Scheduler(Settings settings) {
		this.tolerance = TimeUnit.MICROSECONDS.toNanos(settings.getPropertyAsLong("TOLERANCE"));
	}
	
	
	public void executeAt(long executionTime, ScheduleTarget<E> scheduleTarget) {
		executeAt(executionTime, scheduleTarget, null);
	}
	
	
	public void executeIn(long delayInNanoSec, ScheduleTarget<E> scheduleTarget) {
		executeIn(delayInNanoSec, scheduleTarget, null);
	}
	
	
	protected void warnIfDelayed(long unintendedDelayInNs) {
		if (unintendedDelayInNs > tolerance)
			System.err.println(
					"warning: schedule executor more than " 
					+((float)tolerance/1000000f) +"ms behind schedule. a delay of " 
					+((float)unintendedDelayInNs/1000000f) +"ms was measured"
				); 
	}
	
}
