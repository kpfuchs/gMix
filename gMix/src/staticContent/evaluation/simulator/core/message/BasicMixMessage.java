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
package staticContent.evaluation.simulator.core.message;

import java.util.Vector;

import staticContent.evaluation.simulator.Simulator;
import staticContent.evaluation.simulator.annotations.property.IntSimulationProperty;
import staticContent.evaluation.simulator.core.networkComponent.AbstractClient;
import staticContent.evaluation.simulator.core.networkComponent.NetworkNode;

public class BasicMixMessage extends MixMessage {

	@IntSimulationProperty(
			name = "Mix request playload size (byte)",
			key = "MIX_REQUEST_PAYLOAD_SIZE",
			position = 1,
			inject = "1:RECODING_SCHEME,Recoding Scheme")
	private int requestPlayloadSize;
	
	@IntSimulationProperty(
			name = "Mix reply playload size (byte)",
			key = "MIX_REPLY_PAYLOAD_SIZE",
			position = 2,
			inject = "2:RECODING_SCHEME,Recoding Scheme")
	private int replyPlayloadSize;
	
	@SuppressWarnings("unused")
	@IntSimulationProperty(
			name = "Mix request header size (byte)",
			key = "MIX_REQUEST_HEADER_SIZE",
			position = 3,
			inject = "3:RECODING_SCHEME,Recoding Scheme")
	private int requestHeaderSize;
	
	@SuppressWarnings("unused")
	@IntSimulationProperty(
			name = "Mix reply header size (byte)",
			key = "MIX_REPLY_HEADER_SIZE",
			position = 4,
			inject = "4:RECODING_SCHEME,Recoding Scheme")
	private int replyHeaderSize;

	private int payloadSize;
	private final int maxPayloadSize; // byte
	private final int headerSize; // byte
	private final int totalSize; // byte
	private final Vector<PayloadObject> payloadObjectsContained;
	private int transportMessagesContained = 0;
	//private int messageFragmentsContained = 0;

	// RECORDING_SCHEME

	protected BasicMixMessage(boolean isRequest, NetworkNode source,
			NetworkNode destination, AbstractClient owner, long creationTime,
			boolean isDummy) {

		super(isRequest, source, destination, owner, creationTime, isDummy, null);
		// this.maxPayloadSize = isRequest ? Simulator.settings.getPropertyAsInt("MIX_REQUEST_PAYLOAD_SIZE") : Simulator.settings.getPropertyAsInt("MIX_REPLY_PAYLOAD_SIZE");
		requestPlayloadSize = Simulator.settings.getPropertyAsInt("MIX_REQUEST_PAYLOAD_SIZE");
		replyPlayloadSize = Simulator.settings.getPropertyAsInt("MIX_REPLY_PAYLOAD_SIZE");
		this.maxPayloadSize = isRequest ? requestPlayloadSize : replyPlayloadSize;
		
		this.headerSize = isRequest ? Simulator.settings.getPropertyAsInt("MIX_REQUEST_HEADER_SIZE") : Simulator.settings.getPropertyAsInt("MIX_REPLY_HEADER_SIZE");
		this.totalSize = this.maxPayloadSize + this.headerSize;
		this.payloadObjectsContained = new Vector<PayloadObject>(10,10); // TODO calculate instead of fixed 10,10
		super.setPayload(this.payloadObjectsContained);

	}


	@Override
	public int getLength() {
		return this.totalSize;
	}


	@Override
	public int getPayloadLength() {
		return this.payloadSize;
	}


	@Override
	public int getNumberOfMessagesContained() {
		return this.transportMessagesContained;
	}


	@Override
	public int getMaxPayloadLength() {
		return this.maxPayloadSize;
	}


	@Override
	public PayloadObject[] getPayloadObjectsContained() {
		return this.payloadObjectsContained.toArray(new PayloadObject[0]);
	}


	@Override
	public boolean addPayloadObject(PayloadObject payloadObject) {
		if (payloadObject instanceof TransportMessage) {
			this.transportMessagesContained++;
		} else if (payloadObject instanceof MessageFragment) {
			//this.messageFragmentsContained++;
			if (((MessageFragment)payloadObject).isLastFragment()) {
				this.transportMessagesContained++;
			}
		} else {
			throw new RuntimeException("ERROR: unknown PayloadObject" +payloadObject);
		}

		if (payloadObject.getLength() <= (this.maxPayloadSize - this.payloadSize)) { // enough space

			this.payloadSize += payloadObject.getLength();
			this.payloadObjectsContained.add(payloadObject);
			if (payloadObject instanceof TransportMessage) {
				((TransportMessage)payloadObject).setAssociatedMixMessage(this);
			}

			return true;

		} else { // not enough space

			return false;

		}
	}


	@Override
	public TransportMessage[] getTransportMessagesContained() {

		if (this.isDummy) {
			return new TransportMessage[0];
		}

		Vector<TransportMessage> transportMessagesContained = new Vector<TransportMessage>(this.payloadObjectsContained.size());

		for (PayloadObject po: this.payloadObjectsContained) {
			if (po instanceof TransportMessage) {
				//System.out.println("its a complete TransportMessage (" +(TransportMessage)po +"), size: " +((TransportMessage)po).getLength());
				transportMessagesContained.add((TransportMessage)po);
			} else {
				if (((MessageFragment)po).isLastFragment()) {
					//System.out.println("its the final fragment (" +((MessageFragment)po).getAssociatedTransportMessage() +", "  +(MessageFragment)po +"), size: " +((MessageFragment)po).getLength());
					transportMessagesContained.add(((MessageFragment)po).getAssociatedTransportMessage());
				} else {
					//System.out.println("its a fragment (" +((MessageFragment)po).getAssociatedTransportMessage() +", " +(MessageFragment)po +"), size: " +((MessageFragment)po).getLength());
				}
			}
		}
		return transportMessagesContained.toArray(new TransportMessage[0]);

	}

}
