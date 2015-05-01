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
package userGeneratedContent.simulatorPlugIns.pluginRegistry;

import staticContent.evaluation.simulator.Simulator;
import staticContent.evaluation.simulator.core.networkComponent.AbstractClient;
import userGeneratedContent.simulatorPlugIns.plugins.trafficSource.JansenModel;
import userGeneratedContent.simulatorPlugIns.plugins.trafficSource.ParetoModel;
import userGeneratedContent.simulatorPlugIns.plugins.trafficSource.PoissonModel;
import userGeneratedContent.simulatorPlugIns.plugins.trafficSource.RequestReplyModel;
import userGeneratedContent.simulatorPlugIns.plugins.trafficSource.SendConstantModel;
import userGeneratedContent.simulatorPlugIns.plugins.trafficSource.TraceFileModel;
import userGeneratedContent.simulatorPlugIns.plugins.trafficSource.TrafficSourceImplementation;


public enum TrafficSource {

	CONSTANT(new SendConstantModel()),
	POISSON(new PoissonModel()),
	PARETO(new ParetoModel()),
	REQUEST_REPLY(new RequestReplyModel()),
	TRACE_FILE(new TraceFileModel()),
	JANSEN(new JansenModel())//,
	;
	

	private TrafficSourceImplementation implementation;
	
	
	private TrafficSource(TrafficSourceImplementation implementation) {
		this.implementation = implementation;
	}
	

	public AbstractClient[] createClientsArray() {
		return this.implementation.createClientsArray();
	}
	
	
	public void startSending() {
		this.implementation.startSending();
	}

	
	public static TrafficSource getTrafficSource() {
		String desiredTrafficSource = Simulator.settings.getProperty("TYPE_OF_TRAFFIC_GENERATOR");
		for (TrafficSource tc:TrafficSource.values())
			if (tc.name().equalsIgnoreCase(desiredTrafficSource))
				return tc;
		throw new RuntimeException("ERROR: no TrafficSource with the name \"" +desiredTrafficSource  
				+"\" available (Key \"TYPE_OF_TRAFFIC_GENERATOR\" in experiment config file. See " +
				"\"TrafficSource.java\" for available TrafficSources.");
	}
	
}
