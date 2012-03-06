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

package client.communicationBehaviour;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import message.Reply;


public class SendImmediately_v1 extends ClientCommunicationBehaviour {

	private SendImmediatelyOutputStream_v1 streamFromClientToMix; // converts data from user to mix messages (using RecodingSchemeClient) and passes them to the MixCommunicationHandler 
	private BasicInputStream_v1 streamFromMixToClient; // 
	

	@Override
	public void constructor() {
		this.streamFromClientToMix = new SendImmediatelyOutputStream_v1(
				this, 
				client.getMixCommunicationHandler(), 
				client.getRecodingSchemeWrapper()
				);
		
		if (client.isDuplex())
			streamFromMixToClient = new BasicInputStream_v1(client.getRecodingSchemeWrapper());
	}

	
	@Override
	public void initialize() {
	
	}

	
	@Override
	public void begin() {

	}

	
	@Override
	public void connect() throws IOException {
		mixCommunicationHandler.connect();
		
	}

	
	@Override
	public void disconnect() throws IOException {
		mixCommunicationHandler.disconnect();
	}

	
	@Override
	public InputStream getInputStream() {
		return streamFromMixToClient;
	}

	
	@Override
	public OutputStream getOutputStream() {
		return streamFromClientToMix;
	}
	
	
	@Override
	public void incomingReply(Reply reply) {
		streamFromMixToClient.addReply(reply);
	}

}
