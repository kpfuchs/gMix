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

package testEnvironment;


import internalInformationPort.InternalInformationPortController;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;


/** 
 * Can be used to validate if a message sent, was received as well or dropped 
 * for some reason.
 * <p>
 * For performance reasons, only an identifying integer (4 bytes) is saved for 
 * each message for later comparison.
 * 
 * @author Karl-Peter Fuchs
 */
final class ReceivalCheck {
	
	/** 
	 * Reference on component <code>InternalInformationPort</code>. 
	 * Used to display and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = internalInformationPort.getLogger();
	
	/** 
	 * Interval after which a check for lost messages is performed. If a 
	 * message isn't received after twice this interval, it is considered as 
	 * lost.
	 */
	private static long checkInterval;
	
	/** Number of messages "sent" so far. */
	private static int sentMessages = 0;
	
	/** Number of messages "received" so far. */
	private static int receivedMessages = 0;
	
	/** List with messages, that were "sent", but not "received" so far. */
	private static LinkedList<Integer> notYetReceived = 
		new LinkedList<Integer>();
	
	/**
	 * Messages, that were "sent" in the last interval, but haven't been 
	 * "received" yet. Used to check whether any of these messages hasn't 
	 * been "received" even after the next interval. In this case a message is 
	 * considered as lost.
	 */
	private static Integer[] notYetReceivedLastly = null;
	
	/* Loads values from property file and calculates "checkInterval". */
	static {
		
		/** 
		 * Simple <code>TimerTask</code>, which calls the method <code>
		 * checkReceival()</code>.
		 */
		final class ReceivalCheckTask extends TimerTask {

			/** 
			 * Calls the method <code>checkReceival()</code>.
			 */
			@Override 
			public void run() {
				
				checkReceival();
				
			}
			
		}
		
		// load values
		int maxMessageDelay = 
			new Integer(
				internalInformationPort.getProperty("MAX_MESSAGE_DELAY")
				);
		
		int timestampTolerance = 
			new Integer(
				internalInformationPort.getProperty("TIMESTAMP_TOLERANCE")
				);
		
		// calculate checkInterval
		checkInterval =	maxMessageDelay + timestampTolerance;

		// perform receival check every "checkInterval" ms
		Timer timer = new Timer(); 
		
		timer.schedule(new ReceivalCheckTask(), checkInterval, checkInterval); 

	}
	
	
	/** 
	 * Empty constructor.
	 */
	private ReceivalCheck() {
		
	}
	
	
	/**
	 *  Checks if messages, which haven't been "received" in the last interval,
	 *  haven't been "received" it in the actual interval as well (and 
	 *  are therefore considered as lost).
	 */
	private static synchronized void checkReceival() {

		if (notYetReceivedLastly == null) { // not enough data to perform check

			notYetReceivedLastly = notYetReceived.toArray(new Integer[0]);
			return;

		} else {

			boolean messagesGotLost = false;

			/*
			 * Check if messages, which haven't been "received" in the last 
			 * interval, haven't been "received" it in the actual interval as 
			 * well (and are therefore considered as lost).
			 */
			for (Integer i : notYetReceivedLastly) {

				if (notYetReceived.contains(i) == true) {

					LOGGER.warning("(ReceivalCheck) Message " + i+" got lost!");
					notYetReceived.remove(i);
					messagesGotLost = true;

				}

			}

			if (!messagesGotLost) {

				LOGGER.info("(ReceivalCheck) No messages lost!");

			}
		}

		// update notYetReceivedLastly
		notYetReceivedLastly = notYetReceived.toArray(new Integer[0]);

	}
	
	
	/**
	 * Saves the bypassed message identifier in the <code>notYetReceived</code> 
	 * list. If the <code>checkInterval</code> is reached, a check for lost 
	 * messages is performed. Gets called by <code>ClientSimulator</code> every 
	 * time it sends a new message.
	 * 
	 * @see #checkInterval
	 */
	protected static synchronized void addSentMessage(int messageID) {
		
		sentMessages++;
		notYetReceived.add(new Integer(messageID));
		
	}
	
	
	/**
	 * Removes the bypassed message identifier from <code>notYetReceived</code> 
	 * list. Gets called by <code>ClientSimulator</code> every time it receives 
	 * a reply.
	 * 
	 * @see #notYetReceived
	 */
	protected static synchronized void addReceivedMessage(int messageID) {
		
		receivedMessages++;
		
		if (notYetReceived.remove(new Integer(messageID)) == false) {
			
			LOGGER.warning(	"(ReceivalCheck) ServerSimulator received a "
							+"message, that was never sent!"
							);
			
		}
		
	}
	
}
