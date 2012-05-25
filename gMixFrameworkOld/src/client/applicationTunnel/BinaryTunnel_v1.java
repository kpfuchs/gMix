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

package client.applicationTunnel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


// binary tunnel that just forwards data from and to the internal mix socket (=placeholder)
// does not allow multiple connections (that are not multiplexed)
public class BinaryTunnel_v1 extends ApplicationTunnelClient {

	@Override
	public void constructor() {
		// nothing to do here
	}

	
	@Override
	public void initialize() {
		// nothing to do here
		
	}

	
	@Override
	public void begin() {
		// nothing to do here
	}
		
	
	public void connect() throws IOException {
		super.communicationBehaviour.connect();
	}

	
	public void disconnect() throws IOException {
		super.communicationBehaviour.disconnect();		
	}

	
	public InputStream getInputStream() {
		return super.communicationBehaviour.getInputStream();
	}

	
	public OutputStream getOutputStream() {
		return super.communicationBehaviour.getOutputStream();
	}

}
