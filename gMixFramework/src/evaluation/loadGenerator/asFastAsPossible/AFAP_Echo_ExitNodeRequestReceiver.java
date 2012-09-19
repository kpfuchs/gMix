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
package evaluation.loadGenerator.asFastAsPossible;

import java.io.IOException;

import evaluation.loadGenerator.ExitNodeRequestReceiver;
import framework.core.AnonNode;
import framework.core.config.Settings;
import framework.core.socket.socketInterfaces.StreamAnonSocketMix;
import framework.core.userDatabase.User;


public class AFAP_Echo_ExitNodeRequestReceiver implements ExitNodeRequestReceiver {

	
	protected AnonNode anonNode;
	protected Settings settings;
	
	
	protected AFAP_Echo_ExitNodeRequestReceiver(AnonNode anonNode) {
		this.anonNode = anonNode;
		this.settings = anonNode.getSettings();
	}
	
	
	public static AFAP_Echo_ExitNodeRequestReceiver createInstance(AnonNode anonNode) {
		return new AFAP_Echo_ExitNodeRequestReceiver(anonNode);
	}

	
	@Override
	public void dataReceived(ClientData client, byte[] dataReceived) {
		if (anonNode.IS_DUPLEX) {
			try {
				client.socket.getOutputStream().write(dataReceived);
				client.socket.getOutputStream().flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	
	@Override
	public ClientData createClientDataInstance(User user,
			StreamAnonSocketMix socket) {
		return new ClientData(user, socket);
	}
}
