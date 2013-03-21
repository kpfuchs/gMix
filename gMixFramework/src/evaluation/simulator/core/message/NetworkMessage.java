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
package evaluation.simulator.core.message;

import evaluation.simulator.core.networkComponent.NetworkNode;


public abstract class NetworkMessage {
	
	private NetworkNode source;
	private NetworkNode destination;
	protected boolean isRequest;
	private boolean hasPassedFirstDelayBox;
	private boolean hasPassedSecondDelayBox;
	private Object payload;

	
	public NetworkMessage(boolean isRequest, NetworkNode source, NetworkNode destination, Object payload) {
		
		this.isRequest = isRequest;
		this.source = source;
		this.destination = destination;
		this.payload = payload;

	}


	/**
	 * @return the isRequest
	 */
	public boolean isRequest() {
		return isRequest;
	}


	/**
	 * @param isRequest the isRequest to set
	 */
	public void setRequest(boolean isRequest) { 
		this.isRequest = isRequest;
	}
	
	
	/**
	 * @return the source
	 */
	public NetworkNode getSource() {
		return source;
	}


	/**
	 * @param source the source to set
	 */
	public void setSource(NetworkNode source) {
		this.source = source;
	}


	/**
	 * @return the destination
	 */
	public NetworkNode getDestination() {
		return destination;
	}


	/**
	 * @param destination the destination to set
	 */
	public void setDestination(NetworkNode destination) {
		this.destination = destination;
	}


	/**
	 * @return the length
	 */
	public abstract int getLength();


	/**
	 * @return the payload
	 */
	public Object getPayload() {
		return payload;
	}


	/**
	 * @param payload the payload to set
	 */
	public void setPayload(Object payload) {
		this.payload = payload;
	}


	/**
	 * @return the hasPassedFirstDelayBox
	 */
	public boolean hasPassedFirstDelayBox() {
		return hasPassedFirstDelayBox;
	}
	

	/**
	 * @param hasPassedFirstDelayBox the hasPassedFirstDelayBox to set
	 */
	public void setHasPassedFirstDelayBox(boolean hasPassedFirstDelayBox) {
		this.hasPassedFirstDelayBox = hasPassedFirstDelayBox;
	}
	
	
	public void setHasPassedSecondDelayBox(boolean hasPassedSecondDelayBox) {
		this.hasPassedSecondDelayBox = hasPassedSecondDelayBox;
	}
	
	
	public boolean hasPassedSecondDelayBox() {
		return hasPassedSecondDelayBox;
	}
	
	

}
