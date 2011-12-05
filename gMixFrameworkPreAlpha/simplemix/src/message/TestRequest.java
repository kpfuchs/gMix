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

package message;

public class TestRequest extends Message implements Request, ExternalMessage {

	public TestRequest() {
		super(new byte[0]);
	}
	
	public TestRequest(String identifier) {
		super(identifier);
	}
	
	
	public long getTsMin() {
		return Long.MIN_VALUE; // TODO
	}
	
	public long getTsMax() {
		return Long.MAX_VALUE; // TODO
	}
	
	public int getDelay() {
		return 100; // TODO
	}
	
}