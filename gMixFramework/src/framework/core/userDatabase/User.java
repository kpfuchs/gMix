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
package framework.core.userDatabase;

import java.util.HashMap;

import framework.core.util.Util;

import plugIns.layer1network.cascade_TCP_v0_001.ClientHandler_TCP_RR_sync.ChannelData;


/**
 * Data structure used to store user-specific data (for example identifiers, 
 * session keys or buffers).
 */
public final class User {

	public static final int NOT_SET = -1;
	private int identifier;
	private HashMap<Object, UserAttachment> attachments;
	private UserDatabase userDatabase;
	public volatile int layer1Id = NOT_SET; // can be used as a "fast access" identifier on each layer (optional) 
	public volatile int layer2Id = NOT_SET;
	public volatile int layer3Id = NOT_SET;
	public volatile int layer4Id = NOT_SET;
	public volatile int layer5Id = NOT_SET;
	public ChannelData channeldata; // TODO: remove
	public int prevHopAddress = Util.NOT_SET;
	
	
	protected User(int identifier, UserDatabase userDatabase) {
		this.identifier = identifier;
		this.userDatabase = userDatabase;
	}
	
	
	public int getIdentifier() {
		return identifier;
	}
	
	
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof User))
			return false;
		else
			return identifier == ((User)o).getIdentifier();
	}

	
	protected void addAttachment(UserAttachment attachment, Object callingInstance) {
		//System.out.println("add attachment: " +attachment +" (" +callingInstance +")"); // TODO
		if (attachments == null)
			attachments = new HashMap<Object, UserAttachment>();
		assert !attachments.containsKey(callingInstance) : "only one attachment per instance allowed; add the data to the existing attachment";
		attachments.put(callingInstance, attachment);
	}
	
	
	public UserAttachment removeAttachment(UserAttachment attachment, Object callingInstance) {
		return attachments.remove(callingInstance);
	}
	
	
	public <T extends UserAttachment> T getAttachment(Object callingInstance, Class<T> desiredType) {
		//System.out.println("get attachment: " +desiredType +" (" +callingInstance +")"); // TODO
		try {
			if (attachments.get(callingInstance) == null)
				throw new RuntimeException("no data stored for the calling instance " +callingInstance +". this methode requires a reference on the instance the attachment was created in."); 
			return desiredType.cast(attachments.get(callingInstance));
		} catch (Exception e) {
			return null;
		}
	}


	public UserDatabase getUserDatabase() {
		return userDatabase;
	}

}