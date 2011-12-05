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

import java.util.Collection;

import architectureInterface.UserDatabaseInterface;
import exception.UnknownUserException;
import exception.UserAlreadyExistingException;
import framework.Implementation;

public class BasicUserDatabase extends Implementation implements UserDatabaseInterface {

	@Override
	public void addUser(User user) throws UserAlreadyExistingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeUser(int identifier) throws UnknownUserException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public User getUser(int identifier) throws UnknownUserException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public User getUserByNextMixIdentifier(int nextMixIdentifier)
			throws UnknownUserException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isExistingUser(int identifier) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Collection<User> getActiveUsers() {
		// TODO Auto-generated method stub
		return null;
	}

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
	public String[] getCompatibleImplementations() {
		return (new String[] {	"outputStrategy.BasicBatch",
								"outputStrategy.BasicPool",
								"inputOutputHandler.BasicInputOutputHandler",
								"keyGenerator.BasicKeyGenerator",
								"messageProcessor.BasicMessageProcessor",
								"externalInformationPort.BasicExternalInformationPort",
								"networkClock.BasicSystemTimeClock",
								"userDatabase.BasicUserDatabase",
								"message.BasicMessage"	
			});
	}

	@Override
	public boolean usesPropertyFile() {
		// TODO Auto-generated method stub
		return false;
	}

}
