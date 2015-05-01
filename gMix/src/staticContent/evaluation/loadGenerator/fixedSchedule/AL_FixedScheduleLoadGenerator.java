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
package staticContent.evaluation.loadGenerator.fixedSchedule;

import staticContent.evaluation.loadGenerator.ClientTrafficScheduleWriter;
import staticContent.evaluation.loadGenerator.LoadGenerator;
import staticContent.evaluation.loadGenerator.LoadGenerator.AL_Mode;
import staticContent.evaluation.loadGenerator.applicationLevelTraffic.requestReply.ApplicationLevelMessage;
import staticContent.evaluation.loadGenerator.scheduler.InOrderYieldWaitScheduler;
import staticContent.evaluation.loadGenerator.scheduler.ScheduleTarget;
import staticContent.evaluation.loadGenerator.scheduler.Scheduler;
import staticContent.evaluation.loadGenerator.scheduler.ThreadPoolScheduler;
import staticContent.framework.config.Settings;


public class AL_FixedScheduleLoadGenerator extends FixedScheduleLoadGenerator {

	private boolean readAheadRequired = true;
	private boolean stopExecution = false;
	
	private Scheduler<ApplicationLevelMessage> scheduler;
	private ClientTrafficScheduleWriter<ApplicationLevelMessage> scheduleWriter;
	private ScheduleWriterThread scheduleWriterThread;
	private volatile boolean scheduleWriterActive = false; // used for weak overload detection
	private boolean hasMore = true;
	
	
	// TODO: use more than one scheduler (distribute events round robin-like to schedulers unless two or more events belong to the same client-socket; in that case, schedule all for the same scheduler as sending through a single socket cant be parallelized)
	public AL_FixedScheduleLoadGenerator(LoadGenerator owner) {
		super(owner);
		boolean useYieldWaitScheduler = !settings.isPropertyPresent("USE_SLOW_BUT_ACCURATE_SCHEDULER") ? false : settings.getPropertyAsBoolean("USE_SLOW_BUT_ACCURATE_SCHEDULER");
		if (useYieldWaitScheduler)
			this.scheduler = new InOrderYieldWaitScheduler<ApplicationLevelMessage>(settings);
		else
			this.scheduler = new ThreadPoolScheduler<ApplicationLevelMessage>(settings);
		this.scheduleWriterThread = new ScheduleWriterThread();
		
		// create suiting ClientTrafficScheduleWriter:
		if (owner.AL_MODE == AL_Mode.TRACE_FILE)
			this.scheduleWriter = new ALM_FS_Tracefile(this);
		else if (owner.AL_MODE == AL_Mode.POISSON)
			this.scheduleWriter = new ALM_FS_Poisson(this);
		else if (owner.AL_MODE == AL_Mode.CONSTANT_RATE)
			this.scheduleWriter = new ALM_FS_ConstantRate(this);
		else
			throw new RuntimeException("unsupportd mode: " +owner.AL_MODE); 
		
		// start simulation:
		schedule();
		if (hasMore)
			this.scheduleWriterThread.start();
	}	

	
	public Scheduler<ApplicationLevelMessage> getScheduler() {
		return this.scheduler;
	}
	
	
	public Settings getSettings() {
		return this.settings;
	}
	
	
	public LoadGenerator getLoadGenerator() {
		return this.owner;
	}
	
	
	private class ScheduleWriterThread extends Thread implements ScheduleTarget<ApplicationLevelMessage> {
		
		@Override
		public void run() {
			while (hasMore) {
				synchronized (scheduleWriterThread) {
					while (!readAheadRequired) {
						try {
							scheduleWriterThread.wait();
						} catch (InterruptedException e) {
							continue;
						}
					}
					readAheadRequired = false;
				}
				if (!stopExecution)
					schedule();
			}
		}

		
		@Override
		public void execute(ApplicationLevelMessage attachment) {
			if (scheduleWriterActive)
				System.err.println("TRACE_READER: warning: it seems the "
					+"trace file can't be read fast enough. simulation may "
					+"be inaccurate!");
			synchronized (scheduleWriterThread) {
				readAheadRequired = true;
				scheduleWriterThread.notifyAll();
			}
		}
	}
	
	
	private void schedule() {
		scheduleWriterActive = true;
		long start = System.currentTimeMillis();
		System.out.println("start scheduling"); 
		hasMore = scheduleWriter.scheduleRecords(owner.SCHEDULE_AHEAD/2, scheduler);
		System.out.println("finished scheduling (duration: " +(System.currentTimeMillis()-start) +"ms)"); 
		scheduler.notifyOnOutputOfLast(scheduleWriterThread);
		scheduleWriterActive = false;
	}

}