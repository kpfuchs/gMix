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

package outputStrategy;


import inputOutputHandler.InputOutputHandlerController;

import java.util.ArrayList;

import message.Message;
import message.Reply;
import message.Request;

import userDatabase.User;


/**
 * Data structure used to stores processed messages until output is requested 
 * (see <code>putOutBatch()</code>). Then, all messages in the buffer are 
 * submitted to the <code>InputOutputHandler</code>, which sends them to their 
 * destination. Messages are added in sorted manner (alphabetic, ascending 
 * order) to prevent linkability of (incoming and outgoing) messages due to 
 * their position in the input and output stream.
 * <p>
 * This class is thread-safe.
 * 
 * @author Karl-Peter Fuchs
 */
final class Batch {
	
	/**
	 * Indicates whether this <code>Batch</code> is used to collect 
	 * <code>Request</code>s or <code>Reply</code>ies.
	 */
	private final boolean IS_REQUEST_BATCH;
	
	/**
	 * Indicates whether this <code>Batch</code> belongs to the last mix of a 
	 * cascade or not.
	 */
	private final boolean BELONGS_TO_LAST_MIX;
	
	/** ArrayList containing the messages. */
	private ArrayList<Message> buffer;
	
	/**
	 * Position, the message currently processed shall be saved to (in 
	 * <code>buffer</code>). <code>Calculated by findCorrectPosition(Message 
	 * message, int startIndex, int endIndex)</code>.
	 * 
	 * @see #buffer
	 * @see #findCorrectPosition(Message, int, int)
	 */
	private int correctPosition;
	
	/**
	 * Reference on <code>InputOutputHandler</code> used to send messages to 
	 * their destination when <code>putOutBatch()</code> was called.
	 */
	private InputOutputHandlerController inputOutputHandler;
	
	
	/**
	 * Constructs a new <code>Batch</code> that can be used to stores processed 
	 * messages until output is requested (see <code>putOutBatch()</code>). 
	 * 
	 * @param initialMessageBufferSize		Initial size for the data structure 
	 * 										(<code>ArrayList</code>) used to 
	 * 										store messages.
	 * @param isRequestBatch				Indicates whether this 
	 * 										<code>Batch</code> is used to 
	 * 										collect <code>Request</code>s or 
	 * 										<code>Reply</code>ies.
	 * @param belongsToLastMix				Indicates whether this 
	 * 										<code>Batch</code> belongs to the 
	 * 										last mix of a cascade or not.
	 * @param inputOutputHandlerController	Reference on component
	 * 										<code>InputOutputHandler</code> 
	 * 										(used to send messages to their 
	 * 										destination when 
	 * 										<code>putOutBatch()</code> was 
	 * 										called).
	 */
	protected Batch(
			int initialMessageBufferSize,
			boolean isRequestBatch,
			boolean belongsToLastMix,
			InputOutputHandlerController inputOutputHandlerController
			) {
		
		this.IS_REQUEST_BATCH = isRequestBatch;
		this.BELONGS_TO_LAST_MIX = belongsToLastMix;
		this.buffer = new ArrayList<Message>(initialMessageBufferSize);
		this.inputOutputHandler = inputOutputHandlerController;

	}
	
	
	/**
	 * Returns the total number of messages currently in this 
	 * <code>Batch</code>.
	 * 
	 * @return	Total number of messages currently in this <code>Batch</code>.
	 */
	protected synchronized int size() {
		
		return buffer.size();
		
	}
	
	
	/**
	 * Adds the bypassed message to the local buffer (at the correct position 
	 * according to alphabetic, ascending order).
	 * 
	 * @param message Message to be added to local buffer.
	 */
	protected synchronized void addMessage(Message message) {
		
		findCorrectPosition(message, 0, (buffer.size() - 1));
		buffer.add(correctPosition, message);

	}
	
	
	/**
	 * Finds the correct position (alphabetic, ascending order) the bypassed 
	 * message shall be saved to (in <code>buffer</code>). (Recursive; 
	 * divide and conquer).
	 * 
	 * @see #correctPosition
	 * @see #buffer
	 * @see Message#compareTo(Message)
	 */
	private void findCorrectPosition(	Message message, 
										int startIndex, 
										int endIndex
										) {
		
		if (buffer.size() == 0) { // first message

			correctPosition = 0;

		} else {

			if (startIndex <= endIndex) {

				int mid = (startIndex + endIndex) / 2;

				switch (message.compareTo(buffer.get(mid))) {
				
					case -1: // bypassed message is smaller
						
						correctPosition = mid;
						findCorrectPosition(message, startIndex, mid - 1);
						break;					
						
					case  0: // messages are equal
						
						correctPosition = mid;
						startIndex = endIndex; // stop execution
						break;						
						
					case  1: // bypassed message is larger
						
						correctPosition = mid + 1;
						findCorrectPosition(message, mid + 1, endIndex);
						break;
	
				}

			}

		}
		
	}


	/**
	 * Puts out the current batch, by submitting all messages in buffer to 
	 * the <code>InputOutputHandler</code>, which sends them to their 
	 * destination.
	 */
	protected synchronized void putOutBatch() {
		
		if (BELONGS_TO_LAST_MIX && IS_REQUEST_BATCH) {
			
			inputOutputHandler.addRequests(buffer.toArray(new Request[0]));
			
		} else {
			
			for (int i=0; i<buffer.size(); i++) {
				
				Message message = buffer.get(i);
				User channel = message.getChannel();
				
				if (IS_REQUEST_BATCH) {
					
					inputOutputHandler.addRequest(
							(Request) message
							);
					
					// allow client to add a new message (for next batch)
					channel.setHasMessageInCurrentBatch(false);
					
				} else {
					
					inputOutputHandler.addReply((Reply) message);
					

					// allow client to add a new message (for next batch)
					channel.setHasMessageInCurrentReplyBatch(false);
					
				}
				
			}
			
		}
		
		buffer.clear();
		
	}

}
