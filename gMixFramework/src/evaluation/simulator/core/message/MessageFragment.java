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


public class MessageFragment extends NetworkMessage implements PayloadObject {

	private TransportMessage associatedNoneMixMessage;
	private int length;
	private boolean isLastFragment;
	
	
	public MessageFragment(TransportMessage associatedNoneMixMessage, int desiredLength, boolean isLastFragment) {
		
		super(associatedNoneMixMessage.isRequest, associatedNoneMixMessage.getSource(), associatedNoneMixMessage.getDestination(), null);
		
		if (desiredLength > associatedNoneMixMessage.getLength())
			throw new RuntimeException("ERROR: message fragment must not be larger than the message itself");
		
		this.length = desiredLength;
		this.associatedNoneMixMessage = associatedNoneMixMessage;
		this.isLastFragment = isLastFragment;
	}

	
	@Override
	public int getLength() {
		return length;
	}


	public TransportMessage getAssociatedTransportMessage() {
		return associatedNoneMixMessage;
	}


	public boolean isLastFragment() {
		return isLastFragment;
	}

}
