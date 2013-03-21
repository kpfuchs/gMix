/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2013  Karl-Peter Fuchs
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
package evaluation.simulator.core.statistics.postProcessor;

import evaluation.simulator.core.statistics.ResultSet;

public enum PostProcessor {

	NONE(			null),
	PER_SECOND(		new PerSecondPostProcessor()),
	PER_MINUTE(		new PerMinutePostProcessor()),
	PER_HOUR(		new PerHourPostProcessor()),
	PER_CLIENT(		new PerClientPostProcessor()),
	PER_MIX(		new PerMixPostProcessor()),
	SORT(			null)
	;
	
	
	public PostProcessorImplementation implementation;
	
	
	private PostProcessor(PostProcessorImplementation implementation) {
		this.implementation = implementation;
	}
	
	
	public double process(double input, ResultSet resultSet, int varyingValueId, int runId) {
		if (this == PostProcessor.NONE || this == PostProcessor.SORT)
			return input;
		else 
			return implementation.process(input, resultSet, varyingValueId, runId);
	}
	
	
	public String getUnit() {
		if (this == PostProcessor.NONE || this == PostProcessor.SORT)
			return "";
		else 
			return implementation.getUnit();
	}
	
}
