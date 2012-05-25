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
package framework.core.interfaces;

import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.userDatabase.User;


public interface Layer2RecodingSchemeMix extends ArchitectureInterface {

	public int getMaxSizeOfNextReply();
	public int getMaxSizeOfNextRequest();
	
	public Request generateDummy(int[] route, User user);
	public Request generateDummy(User user);
	public Reply generateDummyReply(int[] route, User user);
	public Reply generateDummyReply(User user);

}
