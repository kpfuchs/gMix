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


import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import userDatabase.User;

import inputOutputHandler.InputOutputHandlerController;

import internalInformationPort.InternalInformationPortController;

import message.BatchSizeMessage;
import message.ChannelEstablishMessage;
import message.ChannelMessage;
import message.Message;
import message.Reply;
import message.Request;

import architectureInterface.OutputStrategyInterface;


/**
 * Controller class of component <code>OutputStrategy</code>. 
 * <p>
 * Collects messages until an output criterion is fulfilled (certain number of 
 * messages collected or timeout reached).
 * <p>
 * Messages are added by component <code>MessageProcessor</code>. When the 
 * output criterion is fulfilled, the collected messages are bypassed to the 
 * <code>InputOutputHandler</code> (component), which sends them to their 
 * destination.
 * 
 * @author Karl-Peter Fuchs
 */
public class OutputStrategyController implements OutputStrategyInterface {

	/** 
	 * Reference on component <code>InternalInformationPort</code>. 
	 * Used to display and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = internalInformationPort.getLogger();
	
	/** 
	 * Reference on component <code>InputOutputHandler</code>. 
	 * Used to bypass collected messages, when output criterion fulfilled.
	 */
	private InputOutputHandlerController inputOutputHandler;
	
	/**
	 * Initial size of the <code>Buffer</code> used to store messages until 
	 * output. Gets resized automatically if it runs out of space.
	 */
	private final int INITIAL_BUFFER_SIZE;
	
	/**
	 * Indicates whether this <code>OutputStrategyController</code> belongs to 
	 * the last mix of the cascade or not.
	 */
	private boolean BELONGS_TO_LAST_MIX;
	
	/**
	 * Indicates whether this <code>OutputStrategyController</code> belongs to 
	 * the first mix of the cascade or not.
	 */
	private boolean BELONGS_TO_FIRST_MIX;
	
	/**
	 * Amount of time, after which the batch is put out, no matter how many 
	 * messages it contains.
	 */
	private final long TIMEOUT;
	
	/** Data structure used to store requests before output. */
	private Batch requestBatch;
	
	/** Data structure used to store replies before output. */
	private Batch replyBatch;
	
	/** Timer used to detect <code>TIMEOUT</code> for requests. */
	private Timer requestTimeoutTimer = new Timer();
	
	/** Timer used to detect <code>TIMEOUT</code> for replies. */
	private Timer replyTimeoutTimer = new Timer();
	
	/**
	 * Minimum number of <code>ChannelEstablishMessage</code>s that must be 
	 * collected, before putting out the request batch (if at least one 
	 * <code>ChannelEstablishMessage</code> is in the batch).
	 * 
	 * @see #requestBatch
	 */
	private int neededChannelEstablishMessages;
	
	/**
	 * Minimum number of <code>ForwardChannelMessage</code>s that must be 
	 * collected, before putting out the request batch. Will be adjusted 
	 * automatically.
	 * 
	 * @see #requestBatch
	 */
	private int neededForwardChannelMessages = 0;
	
	/**
	 * Minimum number of <code>ReplyMessages</code>s that must be collected, 
	 * before putting out the reply batch. Will be adjusted 
	 * automatically.
	 * 
	 * @see #replyBatch
	 */
	private int neededReplyMessages = 0; // will be set dynamically
	
	/** 
	 * Indicates whether the batch sent lastly has already been answered.
	 * <p>
	 * Note: Synchronous batch.
	 */
	private boolean isReplyBatchPending = false;
	
	/** 
	 * Number of messages the upcoming batch will contain (according to the 
	 * <code>OutputStrategy</code> component on this mix' predecessor).
	 * <p>
	 * Used for batch synchronization.
	 * 
	 * @see message.BatchSizeMessage
	 */
	private int batchSize;
	
	/**
	 * Number of <code>ChannelEstablishMessage</code>s currently in request 
	 * batch.
	 * 
	 * @see #neededChannelEstablishMessages
	 */
	private int numberOfChannelEstablishMessages = 0;
	
	/**
	 * Number of <code>ForwardChannelMessage</code>s currently in request 
	 * batch.
	 * 
	 * @see #neededForwardChannelMessages
	 */
	private int numberOfForwardChannelMessages = 0;
	
	/**
	 * Number of <code>ChannelReleaseMessage</code>s currently in request 
	 * batch.
	 */
	private int numberOfChannelReleaseMessages = 0;
	
	
	/**
	 * Generates a new <code>OutputStrategy</code> component, which collects 
	 * messages until an output criterion is fulfilled (certain number of 
	 * messages collected or timeout reached).
	 * <p>
	 * Messages are added by component <code>MessageProcessor</code>. When the 
	 * output criterion is fulfilled, the collected messages are bypassed to 
	 * the <code>InputOutputHandler</code> (component), which sends them to 
	 * their destination.
	 * <p>
	 * Can handle <code>Request</code>s and <code>Replies</code> in parallel.
	 * <p>
	 * Component can't be used before calling 
	 * <code>initialize(InputOutputHandlerController)</code>.
	 * 
	 * @see #initialize(InputOutputHandlerController)
	 */
	public OutputStrategyController() {
		
		this.BELONGS_TO_LAST_MIX = 
			(new Integer(getProperty("NUMBER_OF_FURTHER_MIXES")) == 0)
			? true 
			: false;
		
		this.BELONGS_TO_FIRST_MIX = 
			(new Integer(getProperty("NUMBER_OF_PREVIOUS_MIXES")) == 0)
			? true 
			: false;
		
		this.INITIAL_BUFFER_SIZE = 
			new Integer(getProperty("INITIAL_BUFFER_SIZE"));
		
		this.neededChannelEstablishMessages = 
			new Integer(getProperty("NEEDED_CHANNEL_ESTABLISH_MESSAGES"));
		
		this.TIMEOUT = new Long(getProperty("BATCH_TIMEOUT"));
		
	}
	
	
	/**
	 * Initialization method for this component. Makes this component ready 
	 * for accepting messages.
	 * 
	 * @param inputOutputHandler	Reference on component 
	 * 								<code>InputOutputHandler</code> (used to 
	 * 								send messages after output).
	 */
	public void initialize(InputOutputHandlerController inputOutputHandler) {
		
		this.inputOutputHandler = inputOutputHandler;
		
		
		this.requestBatch = 
			new Batch(	INITIAL_BUFFER_SIZE, 
						true,
						BELONGS_TO_LAST_MIX,
						inputOutputHandler
						);
				
		this.replyBatch = 
			new Batch(	INITIAL_BUFFER_SIZE, 
						false,
						BELONGS_TO_LAST_MIX,
						inputOutputHandler
						);
	
	}

	
	/**
	 * Can be used to add a <code>Request</code>, that shall be put out 
	 * according to the underlying output strategy.
	 * <p>
	 * Return immediately (asynchronous behavior), internal output 
	 * decision is deferred.
	 * 
	 * @param request	<code>Request</code>, that shall be put out according 
	 * 					to the underlying output strategy.
	 */
	public void addRequest(Request request) {
		
		synchronized (requestBatch) {
			
			User channel = request.getChannel();
			
			// indicate that a message for this channel has been added to the 
			// current batch
			channel.setHasMessageInCurrentBatch(true);
			requestBatch.addMessage((Message)request);
			
			// increment suiting message-counter
			if (request instanceof ChannelEstablishMessage) {
				
				numberOfChannelEstablishMessages++;
				
			} else if (request instanceof ChannelMessage) {
				
				numberOfForwardChannelMessages++;
				
			} else { // ChannelReleaseMessage
				
				numberOfChannelReleaseMessages++;
				neededForwardChannelMessages--;
				
			}

			if (requestBatch.size() == 1) { // first message of batch
				
				requestTimeoutTimer = new Timer();
				
				requestTimeoutTimer.schedule(	new OutputTask(requestBatch), 
												TIMEOUT
												);
				
			}
			
			if (isOutputCriterionForRequestBatchFulfilled()) {

				requestTimeoutTimer.cancel();
				putOutRequestBatch();
					
			}
			
		}
		
	}
	
	
	/**
	 * Can be used to add a <code>Reply</code>, that shall be put out 
	 * according to the underlying output strategy.
	 * <p>
	 * Returns immediately (asynchronous behavior), internal output 
	 * decision is deferred.
	 * 
	 * @param reply	<code>Reply</code>, that shall be put out according 
	 * 				to the underlying output strategy.
	 */
	public void addReply(Reply reply) {
		
		synchronized (replyBatch) {
			
			User channel = reply.getChannel();
			isReplyBatchPending = false;
			
			// indicate that a message for this channel has been added to the 
			// current batch
			channel.setHasMessageInCurrentReplyBatch(true);
			replyBatch.addMessage((Message) reply);
			
			if (replyBatch.size() == 1) { // first message of batch
				
				replyTimeoutTimer = new Timer();
				
				replyTimeoutTimer.schedule(	new OutputTask(replyBatch), 
												TIMEOUT
												);
				
			}
			
			if (isOutputCriterionForReplyBatchFulfilled()) {

				replyTimeoutTimer.cancel();
				putOutReplyBatch();
				
			}
			
		}
		
	}

	
	/** 
	 * Number of messages the upcoming batch will contain (according to the 
	 * <code>OutputStrategy</code> component on this mix' predecessor).
	 * <p>
	 * Used for batch synchronization.
	 * 
	 * @see message.BatchSizeMessage
	 */
	public void setBatchSize(int newSize) {
		
		this.batchSize = newSize;
		
	}
	
	
	/**
	 * Indicates whether the output criterion for <code>requestBatch</code> is 
	 * fulfilled or not.
	 * 
	 * @return	Whether the output criterion for <code>requestBatch</code> is 
	 * 			fulfilled or not.
	 */
	private boolean isOutputCriterionForRequestBatchFulfilled() {
		
		if (requestBatch.size() == 0) {
			
			return false;
			
		} else {
			
			if (BELONGS_TO_FIRST_MIX) {
				
				boolean enoughChannelEstablishMessages = 
					(	numberOfChannelEstablishMessages == 0
						||
						numberOfChannelEstablishMessages >= 
							neededChannelEstablishMessages)
					? true 
					: false;
				
				boolean enoughForwardChannelMessages = 
					(	numberOfForwardChannelMessages >= 
							neededForwardChannelMessages)
					? true 
					: false;
				
				return (	!isReplyBatchPending
							&&
							enoughChannelEstablishMessages
							&&
							enoughForwardChannelMessages
							);
				
			} else {
				
				return (	numberOfChannelEstablishMessages 
							+ 
							numberOfForwardChannelMessages
							+ 
							numberOfChannelReleaseMessages
							
						) == batchSize;
				
			}
			
		}
		
	}
	
	
	/**
	 * Indicates whether the output criterion for <code>replyBatch</code> is 
	 * fulfilled or not.
	 * 
	 * @return	Whether the output criterion for <code>replyBatch</code> is 
	 * 			fulfilled or not.
	 */
	private boolean isOutputCriterionForReplyBatchFulfilled() {
		
		if (replyBatch.size() == 0) {
			
			return false;
			
		} else {
			
			return (replyBatch.size() >= neededReplyMessages);
			
		}
		
	}
	
	
	/**
	 * Puts out collected messages in <code>requestBatch</code> and prepares 
	 * variables for next batch.
	 */
	private void putOutRequestBatch() {
		
		synchronized (requestBatch) {
			
			if (!BELONGS_TO_LAST_MIX) {
				// send BatchSizeMessage to next mix for batch synchronization
				
				int batchSizeForNextMix = 
					numberOfForwardChannelMessages
					+ numberOfChannelEstablishMessages
					+ numberOfChannelReleaseMessages;
				
				BatchSizeMessage batchSizeMessage = 
					new BatchSizeMessage(batchSizeForNextMix);
				
				inputOutputHandler.
						addRequest(batchSizeMessage);
				
			}
			
			if (BELONGS_TO_FIRST_MIX) {
				
				// calculate (expected) number of messages for next batch
				neededForwardChannelMessages = 
					neededForwardChannelMessages
					+ numberOfChannelEstablishMessages
					- numberOfChannelReleaseMessages;
				
				neededReplyMessages = neededForwardChannelMessages;
				
			} else { // not first mix
				
				neededReplyMessages = 
					batchSize - numberOfChannelReleaseMessages;
				
			}
			
			// reset message counters
			numberOfChannelEstablishMessages = 0;
			numberOfForwardChannelMessages = 0;
			numberOfChannelReleaseMessages = 0;
			
			isReplyBatchPending = true;
			
			requestBatch.putOutBatch();
			
		}
		
	}
	
	
	/**
	 * Puts out collected messages in <code>replyBatch</code>.
	 */
	private void putOutReplyBatch() {
		
		synchronized (replyBatch) {
			
			replyBatch.putOutBatch();
			
		}
		
	} 

	
	/**
	 * Simply used to shorten method calls (calls 
	 * <code>internalInformationPort.getProperty(key)</code>). Returns the 
	 * property with the specified key from the property file.
	 * 
	 * @param key	The property key.
	 * 
	 * @return		The property with the specified key in the property file.
	 */
	private static String getProperty(String key) {
		
		return internalInformationPort.getProperty(key);
		
	}
	
	
	/**
	 * Simple <code>TimerTask</code>, which puts out the batch it is linked to.
	 * 
	 * @author Karl-Peter Fuchs
	 */
	private final class OutputTask extends TimerTask {

		/**
		 * Indicates whether this <code>OutputTask</code> is linked with 
		 * <code>requestBatch</code> or not (= linked with 
		 * <code>replyBatch</code>).
		 */
		private boolean isRequestTimer;
		
		
		/**
		 * Creates a new <code>OutputTask</code> for the specified 
		 * <code>Batch</code>.
		 * 
		 * @param batch	<code>Batch</code> that shall be put out.
		 */
		protected OutputTask(Batch batch) {
			
			isRequestTimer = (batch == requestBatch) ? true : false;
			
		}
		
		
		/**
		 * Puts out the batch it is linked to.
		 */
		@Override 
		public void run() {
			
			if (isRequestTimer) {
				
				LOGGER.fine("(MessageBuffer) Request-Timeout reached!");
				requestTimeoutTimer.cancel();
				putOutRequestBatch();
				
			} else {
				
				LOGGER.fine("(MessageBuffer) Reply-Timeout reached!");
				replyTimeoutTimer.cancel();
				putOutReplyBatch();
				
			}
			
		}
		
	}
	
}
