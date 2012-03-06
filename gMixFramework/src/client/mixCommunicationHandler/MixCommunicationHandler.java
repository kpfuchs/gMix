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

package client.mixCommunicationHandler;

import java.io.IOException;

import framework.ClientImplementation;
import framework.observer.ObservationSubject;
import message.Request;


public abstract class MixCommunicationHandler extends ClientImplementation implements ObservationSubject<MixEventListener> {
	
	/** must block till message is sent */
	public abstract void sendMessage(Request request) throws IOException;
	public abstract void connect() throws IOException;
	public abstract void disconnect() throws IOException;
	
	
}
