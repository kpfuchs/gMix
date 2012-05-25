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

package userDatabase;

import java.util.HashMap;


/**
 * Data structure used to store user-specific data (for example identifiers, 
 * session keys or buffers).
 * 
 * @author Karl-Peter Fuchs
 *
 */
public final class User {

	private int identifier;
	private HashMap<Object, UserAttachment> attachments;
	private UserDatabaseController userDatabase;
	
	
	protected User(int identifier, UserDatabaseController userDatabase) {
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
		if (attachments == null)
			attachments = new HashMap<Object, UserAttachment>();
		assert !attachments.containsKey(callingInstance) : "only one attachment per instance allowed; add the data to the existing attachment";
		attachments.put(callingInstance, attachment);
	}
	
	
	public UserAttachment removeAttachment(UserAttachment attachment, Object callingInstance) {
		return attachments.remove(callingInstance);
	}
	
	
	public <T extends UserAttachment> T getAttachment(Object callingInstance, Class<T> desiredType) {
		try {
			return desiredType.cast(attachments.get(callingInstance));
		} catch (Exception e) {
			return null;
		}
	}


	public UserDatabaseController getUserDatabase() {
		return userDatabase;
	}

}