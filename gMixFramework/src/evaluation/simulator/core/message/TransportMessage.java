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

import evaluation.simulator.core.networkComponent.AbstractClient;
import evaluation.simulator.core.networkComponent.NetworkNode;


public class TransportMessage extends NetworkMessage implements PayloadObject {

	private int length;
	private long creationTime;
	private AbstractClient owner;
	private MixMessage associatedMixMessage;
	private boolean isFragmented = false;
	private int dataContainedInFragments = 0;
	public EndToEndMessage reltedEndToEndMessage;
	
	
	public TransportMessage(boolean isRequest, NetworkNode source,
			NetworkNode destination, long creationTime, 
			AbstractClient owner, int length, EndToEndMessage reltedEndToEndMessage) {
		super(isRequest, source, destination, null);
		constructorHelper(length, /*id,*/ creationTime, owner, reltedEndToEndMessage);
	}
	

	private void constructorHelper(int length, long creationTime, 
			AbstractClient owner, EndToEndMessage reltedEndToEndMessage) {
		this.length = length;
		this.creationTime = creationTime;
		this.owner = owner;
		this.reltedEndToEndMessage = reltedEndToEndMessage;
	}

	
	public boolean hasNextFragment() {
		return !(dataContainedInFragments == length);
	}
	
	
	public MessageFragment getFragment(int maxFragmentSize) {
		if (dataContainedInFragments >= length)
			throw new RuntimeException("ERROR: nothing left to fragment"); 
		this.isFragmented = true;
		boolean isLastFragment = false;
		int stillToSend = length - dataContainedInFragments;
		if (stillToSend <= maxFragmentSize)
			isLastFragment = true;
		int fragmentSize = (stillToSend < maxFragmentSize) ? stillToSend : maxFragmentSize;
		this.dataContainedInFragments += fragmentSize;
		return new MessageFragment(this, fragmentSize, isLastFragment);
	}
	
	
	public boolean isFragmented() {
		return isFragmented;
	}
	
	
	@Override
	public int getLength() {
		return length;
	}
	
	
	public void setLength(int length) {
		this.length = length;
	}


	/**
	 * @return the creationTime
	 */
	public long getCreationTime() {
		return creationTime;
	}

	
	/**
	 * @param creationTime the creationTime to set
	 */
	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}

	
	/**
	 * @return the owner
	 */
	public AbstractClient getOwner() {
		return owner;
	}

	
	/**
	 * @param owner the owner to set
	 */
	public void setOwner(AbstractClient owner) {
		this.owner = owner;
	}
	
	
	public void setAssociatedMixMessage(MixMessage mixMessage) {
		this.associatedMixMessage = mixMessage;
	}
	
	
	public MixMessage getAssociatedMixMessage() {
		return associatedMixMessage;
	}
	
	
	@Override
	public void setRequest(boolean isRequest) { 
		
		this.isFragmented = false;
		this.dataContainedInFragments = 0;
		this.isRequest = isRequest;
		
	}

}
