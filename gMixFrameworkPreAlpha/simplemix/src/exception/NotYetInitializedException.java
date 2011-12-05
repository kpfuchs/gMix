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

package exception;


public final class NotYetInitializedException extends RuntimeException {
	
	
	static final long serialVersionUID = -18482399309317886L;
	private static final String ERROR_MESSAGE =
			"Access to other components not possible from within constructor!\nAccess other components within the initialize()-method!";
	
	
	public NotYetInitializedException() {
		super(ERROR_MESSAGE);
	}
	
	
	public NotYetInitializedException(String paramString) { 
		super(paramString);
	}
	
	
	public NotYetInitializedException(String paramString, Throwable paramThrowable) {
		super(paramString, paramThrowable);
	}
	
	
	public NotYetInitializedException(Throwable paramThrowable) {
		super(paramThrowable);
	}
	
}
