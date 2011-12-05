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

package message;


/**
 * <code>InternalMessage</code> used for communication between two
 * <code>OutputStrategy</code> components, located on neighbored mixes. 
 * Transmits the batch size (the first mix used for the batch it is about to 
 * send) to the second mix.
 * 
 * @author Karl-Peter Fuchs
 */
public class BatchSizeMessage extends Message implements Request, 
		InternalMessage {

	
	/** 
	 * Identifier for this type of message. Necessary since messages are 
	 * transmitted as byte streams which don't support the 
	 * <code>instanceOf</code> operator.
	 */
	public static final byte IDENTIFIER = (byte)250;
	
	/** 
	 * Batch size the sending mix used for the upcoming batch (= batch he will 
	 * send after this message).
	 */
	private int batchSize;
	
	
	/**
	 * Creates a new <code>BatchSizeMessage</code> containing the bypassed 
	 * <code>batchSize</code>.
	 * 
	 * @param batchSize	Batch size the sending mix used for the upcoming batch 
	 * 					(= batch he will send after this message)
	 */
	public BatchSizeMessage(int batchSize) {
		
		super(new byte[0]);
		
		this.batchSize = batchSize;
		
	}

	
	/** 
	 * Returns the variable <code>batchSize</code>'s value (= Batch size the 
	 * sending mix used for the upcoming batch (= batch he will send after this 
	 * message)).
	 * 
	 * @return	The variable <code>batchSize</code>'s value.
	 */
	public int getBatchSize() {
		
		return batchSize;
		
	}
	
}
