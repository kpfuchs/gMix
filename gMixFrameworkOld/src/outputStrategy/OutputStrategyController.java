/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2011  Karl-Peter Fuchs
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

package outputStrategy;

import message.Reply;
import message.Request;
import framework.Controller;
import framework.LocalClassLoader;
import framework.Mix;


public class OutputStrategyController extends Controller implements OutputStrategy {

	
	public OutputStrategyController(Mix mix) {
		super(mix);
	}


	private OutputStrategy outputStrategyImplementation;

	
	@Override
	public void addRequest(Request request) {
		outputStrategyImplementation.addRequest(request);
	}


	@Override
	public void addReply(Reply reply) {
		outputStrategyImplementation.addReply(reply);
	}


	@Override
	public void instantiateSubclass() {
		this.outputStrategyImplementation = LocalClassLoader.instantiateImplementation(this, OutputStrategy.class, settings);
	}


	@Override
	public String getPropertyKey() {
		return "OUTPUT_STRATEGY";
	}
		
	
}