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
import evaluation.loadGenerator.LoadGenerator.AL_Mode;


public class AL_DynamicScheduleLoadGenerator extends DynamicScheduleLoadGenerator {

	
	public AL_DynamicScheduleLoadGenerator(LoadGenerator owner) {
		super(owner);
		if (owner.AL_MODE == AL_Mode.TRACE_FILE)
			new ALM_DS_Tracefile(owner);
		else
			throw new RuntimeException("unsupportd mode: " +owner.AL_MODE); 
	}	

}
