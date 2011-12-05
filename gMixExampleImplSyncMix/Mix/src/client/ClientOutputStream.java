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

package client;


import java.io.IOException;
import java.io.OutputStream;


/**
 * OutputStream used by user (application) to send data anonymously via a mix 
 * cascade. Abstracts from the underlying technique.
 * 
 * @author Karl-Peter Fuchs
 */
final class ClientOutputStream extends OutputStream {
	
	/** Reference on the <code>Client</code> using this <code>Stream</code>. */
	private Client client;
	
	/** 
	 * Indicates whether this <code>OutputStream</code> is ready to use or not.
	 */
	private boolean isClosed = false;
	
	
	/**
	 * Generates a new <code>OutputStream</code> that can be used to send data 
	 * anonymously via a mix cascade.
	 * 
	 * @param client	Reference on the <code>Client</code> using this 
	 * 					<code>Stream</code>.
	 */
	protected ClientOutputStream(Client client) {
		
		this.client = client;
		
	}
	

	/**
	 * Closes this <code>OutputStream</code>.
	 * 
	 * @throws IOException	If an I/O error occurs.
	 */
	@Override
	public void close() throws IOException {
		
		this.isClosed = true;		

	}
	
	
	/**
	 * Writes <code>b.length</code> bytes from the specified array to this 
	 * <code>OutputStream</code>.
	 * 
	 * @param	b	Data to be written.
	 * 
	 * @throws IOException	If an I/O error occurs.
	 */
	@Override
	public void write(byte[] b) throws IOException {
		
		if (isClosed) {
			
			throw new IOException("OutputStream closed!");
			
		} else {
			
			client.putInSendBuffer(b);
			
		}

	}
	
	
	/**
	 * Writes <code>len</code> bytes from the specified array starting at 
	 * <code>off</code> to this <code>OutputStream</code>.
	 * 
	 * @param	b	Array containing the data to be written.
	 * @param	off	Start offset in <code>b</code>.
	 * @param	len	Number of bytes to be written (starting at 
	 * 			<code>off</code>).
	 * 
	 * @throws IOException	If an I/O error occurs.
	 */
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		
		if (isClosed) {
			
			throw new IOException("OutputStream closed!");
			
		} else {
			
			byte[] dataToSend = new byte[len];
			System.arraycopy(b, off, dataToSend, 0, len);
			
			client.putInSendBuffer(dataToSend);
			
		} 

	}
	
	
	/**
	 * Writes the specified byte to this <code>OutputStream</code>.
	 * 
	 * @param	b	The byte to be written.
	 * 
	 * @throws IOException	If an I/O error occurs.
	 */
	@Override
	public void write(int b) throws IOException {
		
		if (isClosed) {
			
			throw new IOException("OutputStream closed!");
			
		} else {
			
			byte[] dataToSend = {(byte)b};
			client.putInSendBuffer(dataToSend);
			
		}
		
	}


	/**
	 * Has no effect (since internally, data must be written synchronously 
	 * (request and reply alternately)).
	 */
	@Override
	public void flush() throws IOException {
		
		
	}

}
