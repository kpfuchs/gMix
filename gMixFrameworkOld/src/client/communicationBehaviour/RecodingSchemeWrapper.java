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

package client.communicationBehaviour;

import recodingScheme.MessageCreator;
import userDatabase.User;
import message.Reply;
import message.Request;
import framework.ClientImplementation;
import framework.LocalClassLoader;


public class RecodingSchemeWrapper extends ClientImplementation implements MessageCreator {

	private MessageCreator messageCreator;
	
	
	@Override
	public void constructor() {
		this.messageCreator = (MessageCreator)LocalClassLoader.instantiateSubImplementation("recodingScheme", settings.getProperty("RECODING_SCHEME").replace(".java", "_MessageCreator.java"), this); 
		this.messageCreator.constructor();
	
	}

	@Override
	public void initialize() {
		this.messageCreator.initialize();
		
	}

	@Override
	public void begin() { 
		this.messageCreator.begin();
	}


	public MessageCreator getMessageCreator() {
		return this.messageCreator;
	}
	
	
 	@Override
	public Request createMessage(byte[] payload) {
		return this.messageCreator.createMessage(payload);
	}


	@Override
	public byte[] extractPayload(Reply reply) {
		return this.messageCreator.extractPayload(reply);
	}


	@Override
	public int getMaxPayloadForNextMessage() {
		return this.messageCreator.getMaxPayloadForNextMessage();
	}

	
	@Override
	public Request createMessage(byte[] payload, User owner) {
		return this.messageCreator.createMessage(payload, owner);
	}

	
	@Override
	public int getMaxPayloadForNextReply(User user) {
		return this.messageCreator.getMaxPayloadForNextReply(user);
	}

	
	@Override
	public Reply createReply(byte[] payload, User owner) {
		return this.messageCreator.createReply(payload, owner);
	}
	
}
