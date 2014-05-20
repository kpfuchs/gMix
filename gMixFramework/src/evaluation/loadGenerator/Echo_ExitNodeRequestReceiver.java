/*******************************************************************************
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2014  SVS
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
 *******************************************************************************/
package evaluation.loadGenerator;

import java.io.IOException;
import framework.core.AnonNode;
import framework.core.socket.socketInterfaces.StreamAnonSocketMix;
import framework.core.socket.stream.BasicOutputStreamMix;
import framework.core.userDatabase.User;


public class Echo_ExitNodeRequestReceiver extends ExitNodeRequestReceiver {

	private AnonNode mix;
	
	
	public Echo_ExitNodeRequestReceiver(AnonNode exitNode) {
		this.mix = exitNode;
		if (LoadGenerator.VALIDATE_IO)
			throw new RuntimeException("LoadGenerator.VALIDATE_IO not supported by Echo_ExitNodeRequestReceiver"); 
	}
	
	
	@Override
	public void dataReceived(ExitNodeClientData clientData, byte[] dataReceived) {
		Echo_ClientData client = (Echo_ClientData)clientData;
		if (mix.IS_DUPLEX) {
			try {
				//System.out.println("sending reply " +dataReceived.length); 
				client.outputStream.write(dataReceived);
				client.outputStream.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	@Override
	public ExitNodeClientData createClientDataInstance(User user, StreamAnonSocketMix socket, Object callingInstance) {
		return new Echo_ClientData(user, socket, callingInstance);
	}
	
	
	private class Echo_ClientData extends ExitNodeClientData {
		
		BasicOutputStreamMix outputStream;
		
		
		public Echo_ClientData(User user, StreamAnonSocketMix socket, Object callingInstance) {
			super(user, socket, callingInstance);
			if (mix.IS_DUPLEX)
				this.outputStream = (BasicOutputStreamMix)socket.getOutputStream();
		}

	}
}