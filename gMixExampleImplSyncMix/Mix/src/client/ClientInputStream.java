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
import java.io.InputStream;


/**
 * InputStream used by user (application) to receive (anonymized) data via the 
 * mix cascade. Abstracts from the underlying technique.
 * 
 * @author Karl-Peter Fuchs
 */
final class ClientInputStream extends InputStream {
	
	/** 
	 * Indicates whether this <code>InputStream</code> is ready to use or not.
	 */
	private boolean isClosed = false;
	
	/** Reference on the <code>Client</code> using this <code>Stream</code>. */
	private Client client;
	
	
	/**
	 * Generates a new <code>InputStream</code> that can be used to receive 
	 * (anonymized) data via the mix cascade.
	 * 
	 * @param client	Reference on the <code>Client</code> using this 
	 * 					<code>Stream</code>.
	 */
	protected ClientInputStream(Client client) {
		
		this.client = client;
		
	}
	
	
	/**
	 * Reads a byte of data from this <code>InputStream</code>.
	 * 
	 * @return	The next byte of data, or "-1" if <code>EOF</code> or this 
	 * 			<code>InputStream</code> was already closed. 
	 * 
	 * @throws IOException	If an I/O error occurs.
	 */
	@Override
	public int read() throws IOException {
		
		if (isClosed) {
			
			return -1;
			
		} else {
			
			return client.receiveDataFromCascade(1)[0];
			
		}

	}
	
	
	/**
	 * Reads up to <code>b.length</code> bytes of data from this 
	 * <code>InputStream</code>. Blocks until input is available.
	 * 
	 * @param	b	Buffer to read data to.
	 * 
	 * @return	Number of bytes read (or "-1" when <code>EOF</code> or 
	 * 			<code>InputStream</code> closed).
	 * 
	 * @throws IOException	If an I/O error occurs.
	 */
	@Override
	public int read(byte[] b) throws IOException {
		
		if (isClosed) {
			
			throw new IOException("InputStream closed!");
			
		} else {
			
			if (b.length == 0) {
				
				return 0;
				
			} else {
				
				byte[] result = client.receiveDataFromCascade(b.length);
				System.arraycopy(result, 0, b, 0, result.length);
				
				return result.length;
				
			}

		}

	}
	
	
	/**
	 * 
	 * Reads up to <code>len</code> bytes of data from this 
	 * <code>InputStream</code>. If <code>len</code> is not zero, the method 
	 * blocks until some input is available; otherwise, no bytes are read and 0 
	 * is returned. 
	 * 
	 * @param	b	Buffer to read data to.
	 * @param	off	Start offset in the destination array <code>b</code>.
	 * @param	len	Maximum number of bytes to be read.
	 * 
	 * @return	Number of bytes read (or "-1" when <code>EOF</code> or 
	 * 			<code>InputStream</code> closed).
	 * 
	 * @throws IOException	If an I/O error occurs.
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		
		if (isClosed) {
			
			throw new IOException("InputStream closed!");
			
		} else {
			
			if (len == 0) {
				
				return 0;
				
			} else {
				
				byte[] result = client.receiveDataFromCascade(len);
				System.arraycopy(result, 0, b, off, result.length);
				
				return result.length;
				
			}

		}

	}
	
	
	/**
	 * Skips over and discards <code>n</code> bytes of data from this 
	 * <code>InputStream</code>. 
	 * 
	 * @param	n	Number of bytes to be skipped over.
	 * 
	 * @return		Number of bytes skipped over.
	 * 
	 * @throws IOException	If an I/O error occurs.
	 */
	@Override
	public long skip(long n) throws IOException {
		
		if (isClosed) {
			
			throw new IOException("InputStream closed!");
			
		} else {
			
			int skip; // number of bytes to skip as an int
			
			if (n > Integer.MAX_VALUE) {
				
				skip = Integer.MAX_VALUE;
				
			} else {
				
				skip = (int)n;
				
			}
			
			byte[] result = client.receiveDataFromCascade(skip);
			
			return (result.length == 0) ? -1 : result.length;
			
		}

	}
	
	
	/**
	 * Closes this <code>InputStream</code>.
	 * 
	 * @throws IOException	If an I/O error occurs.
	 */
	@Override
	public void close() throws IOException {
		
		if (this.isClosed) {
			
			throw new IOException("InputStream already closed!");
			
		} else {
			
			this.isClosed = true;
			
		}		

	}
	
}
