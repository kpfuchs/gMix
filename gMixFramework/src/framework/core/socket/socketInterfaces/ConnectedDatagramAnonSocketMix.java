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

import java.io.IOException;

import framework.core.userDatabase.User;


public interface ConnectedDatagramAnonSocketMix extends AnonSocket {
	
	public void disconnect() throws IOException;
	public boolean isConnected();
	
	public byte[] receiveMessage() throws IOException;
	public void sendMessage(byte[] payload) throws IOException;  // may be not available if the implementing socket is simplex
	public int getMaxSizeForNextMessageSend() throws IOException;
	public int getMaxSizeForNextMessageReceive() throws IOException; // may be not available if the implementing socket is simplex

	public User getUser();
	
	public AdaptiveAnonSocket getImplementation();
}
