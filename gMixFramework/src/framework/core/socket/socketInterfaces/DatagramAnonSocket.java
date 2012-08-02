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
package framework.core.socket.socketInterfaces;



public interface DatagramAnonSocket extends AnonSocket {

	public void sendMessage(int destPort, byte[] payload); // only available if fixed route socket
	public void sendMessage(int destinationPseudonym, int destPort, byte[] payload); // only available if free route socket
	public int getMaxSizeForNextMessageSend();
	public AnonMessage receiveMessage(); // may be not available if the implementing socket is simplex
	public int getMaxSizeForNextMessageReceive();
	public int availableReplies(); // returns the number of replies available (needed to determine the number of possible "receiveMessage()" calls without blocking)
	
	public AdaptiveAnonSocket getImplementation();
	
}
