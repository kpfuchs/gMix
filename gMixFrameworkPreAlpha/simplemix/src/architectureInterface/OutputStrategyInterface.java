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

package architectureInterface;

import message.Reply;
import message.Request;


/**
 * Architecture interface for component <code>OutputStrategy</code>. 
 * <p>
 * Must accept messages (<code>Request</code>s and <code>Reply</code>ies) from 
 * component <code>MessageProcessor</code> and put them out (hand them over to 
 * component <code>InputOutputHandler</code>, which sends them to their 
 * destination) according to an underlying strategy (e. g. batch strategy).
 * <p>
 * Must be thread-safe.
 * 
 * @author Karl-Peter Fuchs
 */
public interface OutputStrategyInterface {
	
	
	/**
	 * Can be used to add a <code>Request</code>, that shall be put out 
	 * according to the underlying output strategy.
	 * <p>
	 * Must return immediately (asynchronous behavior), internal output 
	 * decision may be deferred.
	 * 
	 * @param request	<code>Request</code>, that shall be put out according 
	 * 					to the underlying output strategy.
	 */
	public void addRequest(Request request);
	
	
	/**
	 * Can be used to add a <code>Reply</code>, that shall be put out 
	 * according to the underlying output strategy.
	 * <p>
	 * Must return immediately (asynchronous behavior), internal output 
	 * decision may be deferred.
	 * 
	 * @param reply	<code>Reply</code>, that shall be put out according 
	 * 				to the underlying output strategy.
	 */
	public void addReply(Reply reply);
	
}
