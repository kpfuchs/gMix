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

package inputOutputHandler;


import message.ChannelEstablishMessage;
import message.ChannelMessage;
import message.Request;


/**
 * Handles communication with a proxy server.
 * <p>
 * Not implemented! Currently any <code>Request</code> data is echoed. 
 * Nevertheless, suiting (synchronized) buffers (and methods for manipulation) 
 * are already present in <code>userDatabase.User</code> ensuring simple 
 * integration.
 * 
 * @see userDatabase.User#putInProxyWriteBuffer(byte[])
 * @see userDatabase.User#putInProxyReadBuffer(byte[])
 * @see userDatabase.User#getFromProxyReadBuffer(int)
 * @see userDatabase.User#getFromProxyWriteBuffer(int)
 * @see userDatabase.User#availableDataInProxyReadBuffer()
 * @see userDatabase.User#availableDataInProxyWriteBuffer()
 * 
 * @author Karl-Peter Fuchs
 */
final class ProxyConnectionHandler extends Thread {

	/** 
	 * Reference on <code>InputOutputHandlerController()</code> (Used to get 
	 * processed requests which shall be submitted to the proxy).
	 * 
	 * @see InputOutputHandlerController#getProcessedRequest()
	 */
	private InputOutputHandlerController inputOutputHandler;
	
	
	/**
	 * Generates a new <code>ProxyConnectionHandler</code>, which handles 
	 * communication with a proxy server.
	 * <p>
	 * Not implemented! Currently any <code>Request</code> data is echoed. 
	 * Nevertheless, suiting (synchronized) buffers (and methods for manipulation) 
	 * are already present in <code>userDatabase.User</code> ensuring simple 
	 * integration.
	 * 
	 * @see userDatabase.User#putInProxyWriteBuffer(byte[])
	 * @see userDatabase.User#putInProxyReadBuffer(byte[])
	 * @see userDatabase.User#getFromProxyReadBuffer(int)
	 * @see userDatabase.User#getFromProxyWriteBuffer(int)
	 * @see userDatabase.User#availableDataInProxyReadBuffer()
	 * @see userDatabase.User#availableDataInProxyWriteBuffer()
	 */
	protected ProxyConnectionHandler(
			InputOutputHandlerController inputOutputHandler
			) {

		this.inputOutputHandler = inputOutputHandler;
		
		start();
		
	}

	
	/**
	 * Waits for processed <code>Requests</code> and puts their payload in the 
	 * suiting <code>User</code>'s proxy buffer.
	 * 
	 * @see userDatabase.User#putInProxyWriteBuffer(byte[])
	 */
	@Override
	public void run() {
		
		while (true) {
			
			Request request = inputOutputHandler.getProcessedRequest();
					// blocking method
			
			request.getChannel().setTimestampOfLastActivity();
			
			// write data to suiting buffer
			if (request instanceof ChannelEstablishMessage) {
				
				ChannelEstablishMessage crm = 
					(ChannelEstablishMessage)request;
				
				byte[] dataToSend = crm.getPayload().getMessage();
				request.getChannel().putInProxyWriteBuffer(dataToSend);
				
			} else { // ForwardChannelMessage
				
				ChannelMessage fcm = (ChannelMessage)request;
				
				byte[] dataToSend = fcm.getPayload().getMessage();
				request.getChannel().putInProxyWriteBuffer(dataToSend);
				
			}
			
		}
		
	}

}
