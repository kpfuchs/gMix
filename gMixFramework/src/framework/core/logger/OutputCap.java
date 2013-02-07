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
package framework.core.logger;


public class OutputCap {
	
	private long lastWarningDisplayed = 0;
	private long minDelayBetweenOutput;
	private final String message;
	
	
	public OutputCap(long minDelayBetweenOutputinMs) {
		this.message = null;
		this.minDelayBetweenOutput = minDelayBetweenOutputinMs;
	}
	
	
	public OutputCap(String message, long minDelayBetweenOutputinMs) {
		this.message = message;
		this.minDelayBetweenOutput = minDelayBetweenOutputinMs;
	}
	
	
	public boolean putOut() {
		if (message == null)
			throw new RuntimeException("you must provide an error message either " +
					"with constructor \"public OutputCap(String message, long " +
					"minDelayBetweenOutputinMs)\" or method \"public boolean " +
					"putOut(String newMessage)\""
					); 
		return putOut(message);
	}
	
	
	public boolean putOut(String newMessage) {
		long now = System.currentTimeMillis();
		if ((now - lastWarningDisplayed) > minDelayBetweenOutput) {
			System.err.println(newMessage);
			lastWarningDisplayed = System.currentTimeMillis();
			return true;
		} else
			return false;
	}

}