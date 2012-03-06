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

import framework.Implementation;


public class BasicUserDatabase extends Implementation implements UserDatabase {

	private HashMap<String, User> users = new HashMap<String, User>(); 
	
	@Override
	public void constructor() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void begin() {
		// TODO Auto-generated method stub
		
	}
	
	
	@Override
	public void addUser(User user) {
		users.put(""+user.getIdentifier(), user);
		
	}

	
	@Override
	public User removeUser(int identifier) {
		return users.remove(""+identifier);
		
	}
	
	
	@Override
	public User removeUser(User user) {
		return users.remove(""+user.getIdentifier());
	}
	
	

	@Override
	public User getUser(int identifier) {
		return users.get(""+identifier);
	}

	@Override
	public boolean isExistingUser(int identifier) {
		return users.containsKey(""+identifier);
	}

	@Override
	public int getNumberOfUsers() {
		return users.size();
	}

	@Override
	public User[] getAllUsers() {
		return users.values().toArray(new User[0]);
	}

}
