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

package inputOutputHandler;

import message.Reply;
import message.Request;
import architectureInterface.InputOutputHandlerInterface;
import framework.Controller;
import framework.LocalClassLoader;
import framework.Mix;


public class InputOutputHandlerController extends Controller implements InputOutputHandlerInterface {

	public InputOutputHandlerController(Mix mix) {
		super(mix);
		// TODO Auto-generated constructor stub
	}


	private InputOutputHandlerInterface inputOutputHandlerImplementation;
	
	
	@Override
	public void instantiateSubclass() {
		this.inputOutputHandlerImplementation = LocalClassLoader.instantiateInputOutputHandlerImplementation(this);
	}


	@Override
	public Reply getReply() {
		return inputOutputHandlerImplementation.getReply();
	}


	@Override
	public Request getRequest() {
		return inputOutputHandlerImplementation.getRequest();
	}


	@Override
	public void addRequest(Request request) {
		inputOutputHandlerImplementation.addRequest(request);
	}


	@Override
	public void addRequests(Request[] requests) {
		inputOutputHandlerImplementation.addRequests(requests);
	}


	@Override
	public void addReply(Reply reply) {
		inputOutputHandlerImplementation.addReply(reply);
	}


	@Override
	public void addReplies(Reply[] replies) {
		inputOutputHandlerImplementation.addReplies(replies);
	}

}