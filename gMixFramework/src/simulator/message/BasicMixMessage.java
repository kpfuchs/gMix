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

package simulator.message;

import java.util.Vector;

import simulator.core.Settings;
import simulator.networkComponent.Client;
import simulator.networkComponent.NetworkNode;



public class BasicMixMessage extends MixMessage {

	private int payloadSize;
	private int maxPayloadSize; // byte
	private int headerSize; // byte
	private int totalSize = maxPayloadSize + headerSize; // byte
	private Vector<PayloadObject> payloadObjectsContained; // z.B. mehrere DNS-Requests
	private int noneMixMessagesContained = 0;
	private int messageFragmentsContained = 0;
	
	
	protected BasicMixMessage(boolean isRequest, NetworkNode source,
			NetworkNode destination, Client owner, int creationTime,
			boolean isDummy) {
		
		super(isRequest, source, destination, owner, creationTime, isDummy, null);
		this.maxPayloadSize = isRequest ? new Integer(Settings.getProperty("MIX_REQUEST_PAYLOAD_SIZE")) : new Integer(Settings.getProperty("MIX_REPLY_PAYLOAD_SIZE"));
		this.headerSize = isRequest ? new Integer(Settings.getProperty("MIX_REQUEST_HEADER_SIZE")) : new Integer(Settings.getProperty("MIX_REPLY_HEADER_SIZE"));
		this.totalSize = maxPayloadSize + headerSize;
		payloadObjectsContained = new Vector<PayloadObject>(10,10); // TODO statt "10,10" berechnen...
		super.setPayload(payloadObjectsContained);
		
	}


	@Override
	public int getLength() {
		return totalSize;
	}
	

	@Override
	public int getPayloadLength() {
		return payloadSize;
	}


	@Override
	public int getNumberOfMessagesContained() {
		return noneMixMessagesContained;
	}


	@Override
	public int getMaxPayloadLength() {
		return maxPayloadSize;
	}


	@Override
	public PayloadObject[] getPayloadObjectsContained() {
		return this.payloadObjectsContained.toArray(new PayloadObject[0]);
	}


	@Override
	public boolean addPayloadObject(PayloadObject payloadObject) {
		
		if (payloadObject instanceof NoneMixMessage) {
			this.noneMixMessagesContained++;
		} else if (payloadObject instanceof MessageFragment) {
			this.messageFragmentsContained++;
			if (((MessageFragment)payloadObject).isLastFragment())
				this.noneMixMessagesContained++;
		} else
			throw new RuntimeException("ERROR: unknown PayloadObject" +payloadObject); 
		
		if (payloadObject.getLength() <= maxPayloadSize - payloadSize) { // enough space
			
			payloadSize += payloadObject.getLength();
			payloadObjectsContained.add(payloadObject);
			if (payloadObject instanceof NoneMixMessage)
				((NoneMixMessage)payloadObject).setAssociatedMixMessage(this);
			
			return true;
			
		} else { // not enough space
			
			return false;
			
		}
	}


	@Override
	public NoneMixMessage[] getNoneMixMessagesContained() {
		
		if (isDummy)
			return new NoneMixMessage[0];
		
		Vector<NoneMixMessage> noneMixMessagesContained = new Vector<NoneMixMessage>(payloadObjectsContained.size());
		
		for (PayloadObject po: payloadObjectsContained)
			if (po instanceof NoneMixMessage)
				noneMixMessagesContained.add((NoneMixMessage)po);
			else if (((MessageFragment)po).isLastFragment())
				noneMixMessagesContained.add(((MessageFragment)po).getAssociatedNoneMixMessage());
		
		return noneMixMessagesContained.toArray(new NoneMixMessage[0]);
		
	}
	
}
