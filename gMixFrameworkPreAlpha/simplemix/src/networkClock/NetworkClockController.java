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

package networkClock;

import framework.Controller;
import framework.LocalClassLoader;
import framework.Mix;
import architectureInterface.NetworkClockInterface;


/**
 * Controller class of component <code>NetworkClock</code>. 
 * <p>
 * Provides a clock, which can be synchronized with a SNTP server (See RFC 
 * 2030 for further information).
 * <p>
 * Can retrieve SNTP messages from a SNTP server at regular intervals and 
 * calculate the local clock's offset each time. The local clock's offset 
 * calculation is implemented according to the SNTP algorithm specified in RFC 
 * 2030.  
 * <p>
 * The method <code>getTime()</code> can be used to generate timestamps (The 
 * typical accuracy of a SNTP client/server exchange is fractions of a second).
 * <p>
 * The System's time is never changed.
 * <p>
 * Uses Adam Buckley's class <code>NtpMessage</code> as message format.
 * 
 * 
 * @author Karl-Peter Fuchs
 * 
 * @see NtpMessage
 */
public class NetworkClockController extends Controller implements NetworkClockInterface {

	
	public NetworkClockController(Mix mix) {
		super(mix);
	}


	private NetworkClockInterface networkClockImplementation;
	
	
	@Override
	public long getTime() {
		return networkClockImplementation.getTime();
	}

	
	@Override
	public void instantiateSubclass() {
		this.networkClockImplementation = LocalClassLoader.instantiateNetworkClockImplementation(this);
	}
	
}
