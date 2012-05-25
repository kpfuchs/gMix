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
package framework.core.clock;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;

import framework.core.config.Settings;


public class NtpClock {

	/** 
	 * The local clock's offset (compared to the SNTP server) (accuracy: ~10ms).
	 */
	private static long localClockOffset;
	
	/** Datagram socket to communicate with SNTP server. */
	private static DatagramSocket timeServerSocket;
	
	/** 
	 * The SNTP server's address. A list of servers can be found 
	 * <a href="http://www.hullen.de/helmut/filebox/DCF77/ntpsrvr.html"> here
	 * </a>. Supports URLs and IPv4 Addresses.
	 */
	private static InetAddress timeServerHost;
	
	/** Indicates whether to synchronize clock (with SNTP server) or not. */
	private static boolean useSynchronizedClock;
	
	private Settings settings;
	
	
	public NtpClock(Settings settings) {
		this.settings = settings;
		clockSync();
	}


	/**
	 * Retrieves an SNTP message from a SNTP server and calculates the local 
	 * clock's offset. The local clock's offset calculation is implemented 
	 * according to the SNTP algorithm specified in RFC 2030.
	 */
	private static void synchronizeClocks() {
		System.out.println("(Clock) Trying to synchronize clocks."); 
		try {
			// send request 
			byte[] buffer = new NtpMessage().toByteArray();
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, timeServerHost, 123);
			timeServerSocket.send(packet);
			
			// get response
			System.out.println("(Clock) Request sent. Waiting for response."); 
			packet = new DatagramPacket(buffer, buffer.length);
			timeServerSocket.receive(packet);
			System.out.println("(Clock) Response received.");			
			double localTime = (System.currentTimeMillis() / 1000.0) + 2208988800.0; // time server's format is relative to 1900, Java's to 1970 => + 2208988800.0
			NtpMessage ntpMessage = new NtpMessage(packet.getData());
			// calculate local clock's offset in ms
			localClockOffset = (long)((((ntpMessage.receiveTimestamp - 
										 ntpMessage.originateTimestamp) +
										 (ntpMessage.transmitTimestamp - 
										 localTime)) / 2)
										 * 1000 // use ms for appropriate 
										 		// accuracy (before casting to 
										 		// long)
										 );
			System.out.println("(Clock) Local clock's offset: " + localClockOffset + " ms");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	/**
	 * Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT 
	 * (The typical accuracy of a SNTP client/server exchange is fractions of a 
	 * second.)
	 * 
	 * @return 	The number of milliseconds since January 1, 1970, 00:00:00 GMT.
	 */
	public long getTime() {
		
		return ((useSynchronizedClock)
				? (System.currentTimeMillis() + localClockOffset)
				: System.currentTimeMillis()
				);
		
	}
	
	
	private void clockSync() {
		
		/** 
		 * Simple <code>TimerTask</code>, which calls the method <code>
		 * synchronizeClocks()</code>.
		 */
		final class SynchronizationTask extends TimerTask {

			/** 
			 * Calls the method <code>synchronizeClocks()</code>.
			 */
			@Override 
			public void run() {
				synchronizeClocks();
			}
		}
		useSynchronizedClock = settings.getPropertyAsBoolean("USE_SYNCHRONIZED_CLOCK");
		if (useSynchronizedClock) {
			try {
				timeServerSocket = new DatagramSocket();
				timeServerHost = settings.getPropertyAsInetAddress("TIME_SERVER_HOST");
				// synchronize clocks for the first time
				synchronizeClocks();
				// synchronize clocks every "updateRate" ms
				long updateRate = settings.getPropertyAsLong("TIME_BETWEEN_CLOCK_SYNCHRONIZATIONS"); // time between clock synchronizations
				if (updateRate != 0)
					new Timer().schedule(new SynchronizationTask(), updateRate, updateRate); 
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}
	}

}
