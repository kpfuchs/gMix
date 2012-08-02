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
package evaluation.loadGenerator.dynamicSchedule;

import evaluation.loadGenerator.LoadGenerator;
import evaluation.loadGenerator.LoadGenerator.InsertLevel;


public abstract class DynamicScheduleLoadGenerator {

	protected LoadGenerator owner;
	
	
	protected DynamicScheduleLoadGenerator(LoadGenerator owner) {
		this.owner = owner;
		if (!owner.settings.getPropertyAsBoolean("GLOBAL_IS_DUPLEX"))
			throw new RuntimeException("dynamic schedulers require duplex mixes... (the next request is sent not before the receival of the current reply)"); 
	}
	
	
	public static DynamicScheduleLoadGenerator createInstance(LoadGenerator owner) {
		if (owner.INSERT_LEVEL == InsertLevel.APPLICATION_LEVEL)
			return new AL_DynamicScheduleLoadGenerator(owner);
		else // InsertLevel.MIX_PACKET_LEVEL
			throw new RuntimeException("not yet implemented"); 
	}


}
