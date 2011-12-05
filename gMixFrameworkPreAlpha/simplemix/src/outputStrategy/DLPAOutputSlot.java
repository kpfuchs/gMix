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

import inputOutputHandler.InputOutputHandlerController;

import java.util.HashMap;

import message.Message;
import message.Reply;
import message.Request;
import message.TestReply;
import message.TestRequest;

import framework.Mix;


public class DLPAOutputSlot {

	private HashMap<String, Message> messagesToSend;
	private boolean isRequestSlot;
	private long timeOfOutput;
	private InputOutputHandlerController ioh;
	
	
	public DLPAOutputSlot(boolean isRequestSlot, long timeOfOutput, Mix mix) {

		this.messagesToSend = new HashMap<String, Message>(); // TODO: specify size
		this.isRequestSlot = isRequestSlot;
		this.timeOfOutput = timeOfOutput;
		this.ioh = mix.getInputOutputHandler();
		
	}
	
	
	public boolean isUsedBy(String identifier) {
		
		return messagesToSend.containsKey(identifier);
		
	}
	
	
	public void addMessage(Message message) {

		messagesToSend.put(message.getIdentifier(), message);
		
	}
	
	
	public void putOutMessages() {

		// create dummies if needed
		for (String identifier: Message.identifiers) {
			
			Message mixMessage = messagesToSend.get(identifier);
			if (mixMessage == null)
				mixMessage = createDummyMessage(identifier, isRequestSlot);

			
			messagesToSend.put(identifier, mixMessage);

		}
		
		// send messages
		for (Message m:messagesToSend.values()) {
			
			if (isRequestSlot)
				ioh.addRequest((Request)m);
			else
				ioh.addReply((Reply)m);
			
		}
		
		messagesToSend.clear();

	}
	
	
	private Message createDummyMessage(String identifier, boolean isRequest) {
		
		if (isRequest)
			return new TestRequest(identifier);
		else 
			return new TestReply(identifier);
		
	}
	

	public int getNumerOfMessagesContained() {
		return messagesToSend.size();
	}


	public long getTimeOfOutput() {
		return timeOfOutput;
	}
	
}
