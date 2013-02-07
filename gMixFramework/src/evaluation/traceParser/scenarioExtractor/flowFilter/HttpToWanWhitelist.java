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
package evaluation.traceParser.scenarioExtractor.flowFilter;

import evaluation.traceParser.engine.Protocol;
import evaluation.traceParser.engine.dataStructure.Flow;
import evaluation.traceParser.engine.dataStructure.Flow.FlowDirection;

public class HttpToWanWhitelist implements FlowFilter {

	
	/**
	 * rejects all flows that are not both HTTP and outgoing 
	 * (FlowDirection.TO_WAN)
	 */
	@Override
	public boolean filterFlow(Flow flow) {
		if (flow.layer4protocol != Protocol.HTTP)
			return true;
		if (flow.flowDirection != FlowDirection.TO_WAN)
			return true;
		if (flow.requestSize == 0) // exclude connections where server sent the first message (seems to be not http; only uses HTTP(S) ports)
			return true;
		return false;
	}
	

	/**
	 * returns "HttpToWan"
	 */
	@Override
	public String getName() {
		return "HttpToWan";
	}

	
	/**
	 * returns "01"
	 */
	@Override
	public String getVersion() {
		return "01";
	}

}