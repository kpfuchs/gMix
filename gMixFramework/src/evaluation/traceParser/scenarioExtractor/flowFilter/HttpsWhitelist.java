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


public class HttpsWhitelist implements FlowFilter {

	
	/**
	 * rejects all flows that are not HTTPS
	 */
	@Override
	public boolean filterFlow(Flow flow) {
		if (flow.layer4protocol != Protocol.HTTPS)
			return true;
		if (flow.requestSize == 0) // exclude connections where server sent the first message (seems to be not http; only uses HTTP(S) ports)
			return true;
		return false;
	}
	

	/**
	 * returns "Https"
	 */
	@Override
	public String getName() {
		return "Https";
	}

	
	/**
	 * returns "01"
	 */
	@Override
	public String getVersion() {
		return "01";
	}

}