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

package inputOutputHandler.communicationHandler;

import inputOutputHandler.InputOutputHandlerInternal;


public class CascadeManager extends CommunicationHandlerManager {

	protected ClientCommunicationHandler clientCommunicationHandler;
	protected NextMixCommunicationHandler nextMixCommunicationHanlder;
	protected PreviousMixCommunicationHandler previousMixCommunicationHanlder;
	protected ProxyCommunicationHandler proxyCommunicationHanlder;
	protected String clientComHandlerPropertyKey = "CLIENT_COMMUNICATION_HANDLER";
	protected String nextMixComHanlderPropertyKey = "NEXT_MIX_COMMUNICATION_HANDLER";
	protected String previousMixComHanlderPropertyKey = "PREVIOUS_MIX_COMMUNICATION_HANDLER";
	protected String proxyComHanlderPropertyKey = "PROXY_COMMUNICATION_HANDLER";
	protected enum POSITION_OF_MIX_IN_CASCADE {FIRST_MIX_OF_CASCADE, MIDDLE_MIX_OF_CASCADE, LAST_MIX_OF_CASCADE, SINGLE_MIX};
	
	
	@Override
	public void constructor() {
		switch (this.getPosition()) {
		case FIRST_MIX_OF_CASCADE:
			instantiateClientComHandler();
			instantiateNextMixComHandler();
			break;
		case MIDDLE_MIX_OF_CASCADE:
			instantiateNextMixComHandler();
			instantiatePrevMixComHandler();
			break;
		case LAST_MIX_OF_CASCADE:
			instantiateProxyComHandler();
			instantiatePrevMixComHandler();
			break;
		case SINGLE_MIX:
			instantiateClientComHandler();
			instantiateProxyComHandler();
			break;
		}
	}

	
	@Override
	public void initialize() {

	}

	
	@Override
	public void begin() {

	}

	
	private void instantiateClientComHandler() {
		this.clientCommunicationHandler = 
			instantiateCommunicationHandler(
					settings.getProperty(clientComHandlerPropertyKey), 
					owner, 
					ClientCommunicationHandler.class
					);
	}
	
	
	private void instantiateNextMixComHandler() {
		this.nextMixCommunicationHanlder =
			instantiateCommunicationHandler(
					settings.getProperty(nextMixComHanlderPropertyKey), 
					owner, 
					NextMixCommunicationHandler.class
					);
	}
	

	private void instantiatePrevMixComHandler() {
		this.previousMixCommunicationHanlder =
			instantiateCommunicationHandler(
					settings.getProperty(previousMixComHanlderPropertyKey), 
					owner, 
					PreviousMixCommunicationHandler.class
					);
	}

	
	private void instantiateProxyComHandler() {
		this.proxyCommunicationHanlder =
			instantiateCommunicationHandler(
					settings.getProperty(proxyComHanlderPropertyKey), 
					owner, 
					ProxyCommunicationHandler.class
					);
	}
	
	
	protected POSITION_OF_MIX_IN_CASCADE getPosition() {
		if (mix.isSingleMix())
			return POSITION_OF_MIX_IN_CASCADE.SINGLE_MIX;
		else if (mix.isFirstMix())
			return POSITION_OF_MIX_IN_CASCADE.FIRST_MIX_OF_CASCADE;
		else if (!mix.isLastMix())
			return POSITION_OF_MIX_IN_CASCADE.MIDDLE_MIX_OF_CASCADE;
		else
			return POSITION_OF_MIX_IN_CASCADE.LAST_MIX_OF_CASCADE;
	}


	@Override
	public void setCommunicationHandlerLink(
			InputOutputHandlerInternal inputOutputHandlerInternal) {
		// TODO Auto-generated method stub
		
	}
	
}
