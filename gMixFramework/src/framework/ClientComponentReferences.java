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

package framework;

import infoService.InfoServiceClient_v1;
import client.ClientController;
import client.MixSocket;
import client.applicationTunnel.ApplicationTunnelClient;
import client.communicationBehaviour.ClientCommunicationBehaviour;
import client.communicationBehaviour.RecodingSchemeWrapper;
import client.mixCommunicationHandler.MixCommunicationHandler;


public interface ClientComponentReferences {

	public ApplicationTunnelClient getApplicationTunnelClient();
	public ClientController getClient(); // the instance of class "Client", associated with these components
	public MixSocket getMixSocket();
	public ClientCommunicationBehaviour getClientCommunicationBehaviour();
	public MixCommunicationHandler getMixCommunicationHandler();
	public RecodingSchemeWrapper getRecodingSchemeWrapper();
	public Settings getSettings();
	public InfoServiceClient_v1 getInfoServiceClient();

}
