/*******************************************************************************
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2014  SVS
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
 *******************************************************************************/
package framework.core.socket.socketInterfaces;

public interface NoneBlockingAnonSocketOptions {

	/** 
	 * BLOCKING (or sync) mode means that the getNextRequest() (or read()) 
	 * method of the socket will block the calling thread until a Request is available.
	 * 
	 * NONE_BLOCKING (or async) mode means that the getNextRequest() (or 
	 * read()) method will always return immediately (with return value null, 
	 * if no request is available).
	 * 
	 * OBSERVER_PATTERN (software design pattern): An observer can be 
	 * registered that will be notified about incoming Requests. On an incoming 
	 * Request, a layer 4 thread (i.e. "the socket") will call the 
	 * incomingRequest(Request request) method of the observer. The observer 
	 * (an instance of RequestObserver) must be bypassed to the socket 
	 * constructor when this IO_Mode is chosen.
	 */
	public enum IO_Mode {BLOCKING, NONE_BLOCKING, OBSERVER_PATTERN}
	
	public IO_Mode getIO_Mode();
	public boolean getIsBlocking();
	
}
