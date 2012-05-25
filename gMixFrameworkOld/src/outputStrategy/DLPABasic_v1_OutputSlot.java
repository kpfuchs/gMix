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

import java.util.Arrays;
import java.util.HashMap;

import recodingScheme.RecodingSchemeController;

import userDatabase.User;
import userDatabase.UserDatabaseController;

import message.MixMessage;
import message.Reply;
import message.Request;

import framework.Mix;


public class DLPABasic_v1_OutputSlot {

	private HashMap<User, MixMessage> messagesToSend;
	private boolean isRequestSlot;
	private long timeOfOutput;
	private InputOutputHandlerController ioh;
	private UserDatabaseController userDB;
	private RecodingSchemeController recodingScheme;
	
	
	public DLPABasic_v1_OutputSlot(boolean isRequestSlot, long timeOfOutput, Mix mix) {
		this.messagesToSend = new HashMap<User, MixMessage>(); // TODO: specify size
		this.isRequestSlot = isRequestSlot;
		this.timeOfOutput = timeOfOutput;
		this.ioh = mix.getInputOutputHandler();
		this.userDB = mix.getUserDatabase();
		this.recodingScheme = mix.getRecodingScheme();
	}
	
	
	public boolean isUsedBy(User user) {
		synchronized (this) {
			return messagesToSend.containsKey(user);
		}
	}
	
	
	public void addMessage(MixMessage mixMessage) {
		synchronized (this) {
			messagesToSend.put(mixMessage.getOwner(), mixMessage);
		}
	}
	
	
	public void putOutMessages() {

		// create dummies if needed
		int normalMessages;
		int dummyCounter = 0;
		synchronized (this) { // TODO: remove
			
			normalMessages = messagesToSend.size();
			for (User user: userDB.getAllUsers()) {
				MixMessage mixMessage = messagesToSend.get(user);
				if (mixMessage == null) {
					if (isRequestSlot)
						mixMessage = recodingScheme.generateDummy(user);
					else 
						mixMessage = recodingScheme.generateDummyReply(user);
					dummyCounter++;
				}
				messagesToSend.put(user, mixMessage);
			}
			
			System.out.println("putting out slot (" +dummyCounter +" dummies and " +normalMessages +" normal messages)"); // TODO: remove 
			
			MixMessage[] messages = messagesToSend.values().toArray(new MixMessage[0]);
			Arrays.sort(messages);
			
			// send messages
			for (MixMessage m:messages) {
				
				if (isRequestSlot)
					ioh.addRequest((Request)m);
				else
					ioh.addReply((Reply)m);
				
			}
			
			messagesToSend.clear();
			
		}

	}
	

	public int getNumerOfMessagesContained() {
		synchronized (this) {
			return messagesToSend.size();
		}
	}


	public long getTimeOfOutput() {
		synchronized (this) {
			return timeOfOutput;
		}
	}
	
}
