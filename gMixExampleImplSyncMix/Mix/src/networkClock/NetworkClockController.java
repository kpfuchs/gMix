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


import internalInformationPort.InternalInformationPortController;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

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
public class NetworkClockController implements NetworkClockInterface {

	/** 
	 * Reference on component <code>InternalInformationPort</code>. 
	 * Used to display and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = internalInformationPort.getLogger();
	
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

	
	/* 
	 * Read properties from property file and start a timer, which synchronizes
	 * the clock in regular intervals
	 */
	static {
		
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
		
		/* Time between clock synchronizations. */
		long updateRate = new Long(
				internalInformationPort.getProperty(
						"TIME_BETWEEN_CLOCK_SYNCHRONIZATIONS")
						);

		useSynchronizedClock = 
			(internalInformationPort.getProperty("USE_SYNCHRONIZED_CLOCK").
					equals("1")
			)
			? true
			: false;

		if (useSynchronizedClock) {
			
			try {

				timeServerSocket = new DatagramSocket();
				
				timeServerHost = 
					InetAddress.getByName(
						internalInformationPort.
							getProperty("TIME_SERVER_HOST")
					);
				
				// synchronize clocks for the first time
				synchronizeClocks();
				
				// synchronize clocks every "updateRate" ms
				Timer timer = new Timer(); 
				
				timer.schedule(	new SynchronizationTask(), 
								updateRate, 
								updateRate
								); 
				
			} catch (SocketException e) {
				
				LOGGER.severe(	"(Clock) Couldn't generate DatagramSocket!"
								+e.getMessage()
								);
				
				System.exit(1);
				
			} catch (UnknownHostException e) {
				
				LOGGER.severe(	"(Clock) Invalid \"TIME_SERVER_HOST\" " 
								+"specified in property file!"
								+e.getMessage()
								);
				
				System.exit(1);
				
			}
			
		}
		
	}
	
	/**
	 * Generates a new <code>NetworkClock</code> component which provides a 
	 * clock, synchronized with a SNTP server.
	 */
	public NetworkClockController() {
		
	}

	
	/**
	 * Initializes the this component.
	 */
	public void initialize() {
		
	}
	
	
	/**
	 * Retrieves an SNTP message from a SNTP server and calculates the local 
	 * clock's offset. The local clock's offset calculation is implemented 
	 * according to the SNTP algorithm specified in RFC 2030.
	 */
	private static void synchronizeClocks() {
		
		LOGGER.fine("(Clock) Trying to synchronize clocks.");
		
		try {
			
			// send request 
			byte[] buffer = new NtpMessage().toByteArray();
			
			DatagramPacket packet = new DatagramPacket(buffer, 
													   buffer.length,
													   timeServerHost,
													   123
													   );
			
			timeServerSocket.send(packet);
			
			// get response
			LOGGER.fine("(Clock) Request sent. Waiting for response.");
			packet = new DatagramPacket(buffer, buffer.length);
			timeServerSocket.receive(packet);
			LOGGER.fine("(Clock) Response received.");			
			
			double localTime = (System.currentTimeMillis() / 1000.0) 
							   + 2208988800.0;
				// time server's format is relative to 1900, Java's to 1970 
				// => + 2208988800.0

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
			
			LOGGER.fine(	"(Clock) Local clock's offset: " + 
					   		localClockOffset + " ms"
			   				);
			
		} catch (IOException e) {
			
			LOGGER.warning(	"(Clock) Clock couldn't be synchronized!"
							+e.getMessage()
							);
			
			System.exit(1);
			
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
	
}
