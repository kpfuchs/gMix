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
package evaluation.simulator.core.message;

import evaluation.simulator.Simulator;
import evaluation.simulator.annotations.property.StringSimulationProperty;
import evaluation.simulator.core.networkComponent.AbstractClient;
import evaluation.simulator.core.networkComponent.NetworkNode;
import evaluation.simulator.plugins.outputStrategy.StopAndGoMessage;


public abstract class MixMessage extends NetworkMessage {

	@StringSimulationProperty(name = "Message format",
			key = "MESSAGE_FORMAT",
			position = 5,
			inject = "0:RECODING_SCHEME,Recoding Scheme",
			possibleValues="BASIC_MIX_MESSAGE,STOP_AND_GO_MESSAGE")
	private static String type;

	protected long creationTime;
	protected AbstractClient owner;
	protected int replyCounter = 0;
	protected boolean isDummy;
	public long timeOfArrival;


	public static MixMessage getInstance(boolean isRequest, NetworkNode source,
			NetworkNode destination, AbstractClient owner, long creationTime,
			boolean isDummy) {

		type = Simulator.settings.getProperty("MESSAGE_FORMAT");
		String outputStrategy = Simulator.settings.getProperty("OUTPUT_STRATEGY");
		if (outputStrategy.equals("STOP_AND_GO") || type.equals("STOP_AND_GO_MESSAGE")) {
			return new StopAndGoMessage(isRequest, source, destination, owner, creationTime, isDummy);
		}
		if (type.equals("BASIC_MIX_MESSAGE")) {
			return new BasicMixMessage(isRequest, source, destination, owner, creationTime, isDummy);
		} else {
			throw new RuntimeException("ERROR: Unknown MESSAGE_FORMAT!");
		}

	}


	protected MixMessage(boolean isRequest, NetworkNode source, NetworkNode destination, AbstractClient owner, long creationTime, boolean isDummy, Object payload) {
		super(isRequest, source, destination, payload);

		this.creationTime = creationTime;
		this.isDummy = isDummy;
		this.owner = owner;

	}


	/**
	 * @return the creationTime
	 */
	public long getCreationTime() {
		return this.creationTime;
	}



	/**
	 * @param creationTime the creationTime to set
	 */
	public void setCreationTime(int creationTime) {
		this.creationTime = creationTime;
	}


	/**
	 * @return the isDummy
	 */
	public boolean isDummy() {
		return this.isDummy;
	}



	/**
	 * @param isDummy the isDummy to set
	 */
	public void setDummy(boolean isDummy) {
		this.isDummy = isDummy;
	}


	/**
	 * @return the owner
	 */
	public AbstractClient getOwner() {
		return this.owner;
	}


	/**
	 * @param owner the owner to set
	 */
	public void setOwner(AbstractClient owner) {
		this.owner = owner;
	}


	/*public String toString() {
		String replyOrRequest = super.isRequest() ? "Request" : "Reply";
		return "MixMessage ("+replyOrRequest +", owner: "+owner +")";
	}*/


	public abstract int getPayloadLength();
	public abstract int getMaxPayloadLength();
	public abstract int getNumberOfMessagesContained();
	public abstract PayloadObject[] getPayloadObjectsContained();
	public abstract TransportMessage[] getTransportMessagesContained();
	public abstract boolean addPayloadObject(PayloadObject payloadObject);


	public int getFreeSpace() {
		return this.getMaxPayloadLength() - this.getPayloadLength();
	}


	public int getPaddingLength() {
		return this.getMaxPayloadLength() - this.getPayloadLength();
	}


	public double getPaddingPercentage() {
		return ((double)this.getPaddingLength()/(double)this.getMaxPayloadLength())* 100d;
	}


	public double getPayloadPercentage() {
		return ((double)this.getPayloadLength()/(double)this.getMaxPayloadLength())* 100d;
	}


	public boolean areAllRepliesReceived() {

		if (this.getNumberOfMessagesContained() == ++this.replyCounter) {
			this.replyCounter = 0;
			return true;
		} else {
			return false;
		}

	}

}
