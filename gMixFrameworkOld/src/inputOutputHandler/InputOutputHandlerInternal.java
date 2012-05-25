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

import framework.SubImplementationInterface;
import message.Reply;
import message.Request;


// "internal" interface of the InputOutputHandler; used by communicationHandlers to provide requests received via network and send replies processed by the recodingScheme/outputStrategy
public interface InputOutputHandlerInternal extends SubImplementationInterface {
	
	public Request getProcessedRequest();
	public Request peekProcessedRequest();
	public void addUnprocessedRequest(Request request);
	public int remainingUnprocessedRequestCapacity();
	public Reply getProcessedReply();
	public Reply peekProcessedReply();
	public void addUnprocessedReply(Reply message);
	public int remainingUnprocessedReplyCapacity();
	
}
