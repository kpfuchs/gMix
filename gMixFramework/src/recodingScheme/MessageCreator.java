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

package recodingScheme;

import framework.ThreePhaseStart;
import userDatabase.User;
import message.Reply;
import message.Request;


public interface MessageCreator extends ThreePhaseStart {

	public abstract Request createMessage(byte[] payload);
	public abstract Request createMessage(byte[] payload, User owner);
	public abstract int getMaxPayloadForNextMessage();
	public abstract int getMaxPayloadForNextReply(User user);
	public abstract byte[] extractPayload(Reply reply);
	public abstract Reply createReply(byte[] payload, User owner);
	
}
