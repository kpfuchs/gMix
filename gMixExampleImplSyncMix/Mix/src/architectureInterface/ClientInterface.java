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

package architectureInterface;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Architecture interface for component <code>Client</code>. 
 * <p>
 * Used to anonymize data (e. g. from a user's application) via a cascade of 
 * mixes. Follows the "everything is a stream" concept. From a user 
 * perspective, a client shall behaves quite equal to a socket (<code>
 * java.net.Socket</code>) and therefore abstract from the underlying technique.
 * 
 * @author Karl-Peter Fuchs
 */
public interface ClientInterface {

	
	/**
	 * Must connect (the implementing) client to the mix cascade.
	 * 
	 * @throws IOException I/O problem occurred.
	 */
	public void connect() throws IOException;
	
	
	/**
	 * Must disconnect (the implementing) client from the mix cascade.
	 * 
	 * @throws IOException I/O problem occurred.
	 */
	public void disconnect() throws IOException;
	
	
	/**
	 * Must return an <code>InputStream</code> that can be used to receive data
	 * anonymously via the mix cascade. 
	 * 
	 * @return 	<code>InputStream</code> that can be used to receive data
	 * 			anonymously. 
	 * 
	 * @throws IOException I/O problem occurred.
	 */
	public InputStream getInputStream();
	
	
	/**
	 * Must return an <code>OutputStream</code> that can be used to send data
	 * anonymously via the mix cascade. 
	 * 
	 * @return 	<code>OutputStream</code> that can be used to send data
	 * 			anonymously. 
	 * 
	 * @throws IOException I/O problem occurred.
	 */
	public OutputStream getOutputStream();
	
}
