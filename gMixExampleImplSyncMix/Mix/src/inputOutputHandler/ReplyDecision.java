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

package inputOutputHandler;


import internalInformationPort.InternalInformationPortController;

import java.util.Arrays;
import java.util.logging.Logger;

import userDatabase.User;

import message.Payload;
import message.ReplyMessage;

import networkClock.NetworkClockController;


/**
 * Decides about the optimal reply size and time depending on the current 
 * traffic situation (only used by last mix of cascade).
 * <p>
 * "Traffic situation" means the amount of data available to reply (for each 
 * channel/user) at a given point of time.
 * <p>
 * "Optimal size" means the best tradeoff between padding and the total amount 
 * of (available) data actually sent. A "perfect size" is only possible if all 
 * channels contain the same amount of data. In any other situation, optimizing 
 * one parameter will have a negative effect on the other. So, "the best 
 * tradeoff" depends on situational conditions and therefore can't be 
 * generalized.
 * <p>
 * "Optimal time" means the best point of time for returning the data 
 * available which again is a tradeoff and depends on situational conditions 
 * and therefore can't be generalized.
 * 
 * @author Karl-Peter Fuchs
 */
final class ReplyDecision {

	/** 
	 * Reference on component <code>InternalInformationPort</code>. 
	 * Used to display and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = internalInformationPort.getLogger();
	
	private static NetworkClockController clock = new NetworkClockController();
	
	/** 
	 * Reference on <code>InputOutputHandlerController</code> (used to add
	 * unprocessed <code>Reply</code>ies).
	 * 
	 * @see InputOutputHandlerController#addUnprocessedReply(ReplyMessage)
	 */
	private InputOutputHandlerController inputOutputHandler;
	
	/** Timeout for reply size decision in ms. */
	private final long TIMEOUT;
	
	/** Maximum size a <code>Reply</code> may have in byte. */
	private final int MAX_REPLY_LENGTH;
	
	/** 
	 * Block size of the cryptographic algorithm used to encrypt 
	 * <code>Reply</code>ies in byte. 
	 */
	private final int CIPHER_BLOCK_SIZE;
	
	/** 
	 * Minimum amount of data per <code>Reply</code> in byte (before replying 
	 * is allowed).
	 * 
	 * @see #MIN_FILLED_CHANNELS
	 */
	private final int MIN_VOLUME_PER_CHANNEL;
	
	/** 
	 * Minimum number of channels with data available to be returned (before 
	 * replying  is allowed).
	 * 
	 * @see #MIN_VOLUME_PER_CHANNEL
	 */
	private final float MIN_FILLED_CHANNELS;
	
	/** Amount of time between validations of the traffic situation. */
	private final long VOLUME_DECISSION_CHECK_RATE;
	
	/** 
	 * Tradeoff-indicator between padding and the total amount of (available) 
	 * data actually sent.
	 */
	private double g;
	
	/** 
	 * Timestamp of the first validation (of the traffic situation) for the 
	 * current decision.
	 */
	private long batchBegin;
	
	/** 
	 * Indicates whether the first validation (of the traffic situation) for 
	 * the current decision has already taken place or not (used to calculate 
	 * timeout condition).
	 */
	private boolean isNewDecision = true;
	

	/**
	 * Creates a new <code>ReplyDecision</code> object which can be used to 
	 * decides about the optimal reply size and time depending on the current 
	 * traffic situation by the last mix of a cascade.
	 * <p>
	 * "Traffic situation" means the amount of data available to reply (for 
	 * each channel/user) at a given point of time.
	 * <p>
	 * "Optimal size" means the best tradeoff between padding and the total 
	 * amount of (available) data actually sent. A "perfect size" is only 
	 * possible if all channels contain the same amount of data. In any other 
	 * situation, optimizing one parameter will have a negative effect on the 
	 * other. So, "the best tradeoff" depends on situational conditions and 
	 * therefore can't be generalized.
	 * <p>
	 * "Optimal time" means the best point of time for returning the data 
	 * available which again is a tradeoff and depends on situational 
	 * conditions and therefore can't be generalized.
	 * 
	 * @param inputOutputHandler	Reference on 
	 * 								<code>InputOutputHandlerController</code> 
	 * 								(used to add unprocessed 
	 * 								<code>Reply</code>ies).
	 */
	protected ReplyDecision(InputOutputHandlerController inputOutputHandler) {
	
		this.inputOutputHandler = inputOutputHandler;
		
		this.VOLUME_DECISSION_CHECK_RATE = 
			new Long(getProperty("VOLUME_DECISSION_CHECK_RATE"));
		
		this.MIN_FILLED_CHANNELS = 
			new Float(getProperty("MIN_FILLED_CHANNELS"));
		
		this.MIN_VOLUME_PER_CHANNEL = 
			new Integer(getProperty("MIN_VOLUME_PER_CHANNEL"));
		
		MAX_REPLY_LENGTH =
			new Integer(getProperty("MAX_REPLY_LENGTH")) 
			- 
			4; // -4 since four bytes are needed for length-header
		
		TIMEOUT = new Integer(getProperty("VOLUME_DECISSION_TIMEOUT"));
		
		CIPHER_BLOCK_SIZE = 
			new Integer(getProperty("SYMMETRIC_CYPHER_BLOCK_SIZE"));
		
		g = new Double(getProperty("G"));
		
	}
	
	
	/**
	 * Decides about the optimal reply size and time depending on the current 
	 * traffic situation for the bypassed channels/users.
	 * 
	 * @param channels	The channels/users to be observed.
	 */
	protected void manageReplyProcess(final User[] channels) {
		
		new Thread(
				
				new Runnable() {
									
					public void run() {

						generateReplyBatch(channels);
							
					}
									
				}
						
			).start();
		
	}
	
	
	/**
	 * Decides about the optimal reply size and time depending on the current 
	 * traffic situation for the bypassed channels/users.
	 * 
	 * @param channels	The channels/users to be observed.
	 */
	private void generateReplyBatch(User[] channels) {
		
		int replySize;
		int[] bytesReadSoFar;
		
		while (true) {
			
			try {
				
				Thread.sleep(VOLUME_DECISSION_CHECK_RATE);
				
			} catch (InterruptedException e) {
				
				LOGGER.severe(e.getMessage());
				continue;
				
			}
			
			bytesReadSoFar = getVolumeForEachChannel(channels);
			
			if (shallMixReply(bytesReadSoFar)) {
				
				replySize = getWeightedMedian(bytesReadSoFar);
				processMessages(replySize, channels);
				break;
				
			}
			
		}
		
	}
	
	
	/**
	 * Returns an array containing the amount of data currently available in 
	 * each bypassed channel.
	 * 
	 * @param channels	The channels/users to be observed.
	 * 
	 * @return			Amount of data currently available in each bypassed 
	 * 					channel.
	 */
	private int[] getVolumeForEachChannel(User[] channels) {
		
		int[] bytesReadSoFar = new int[channels.length];
		
		for (int i=0; i<bytesReadSoFar.length; i++) {
			
			bytesReadSoFar[i] = channels[i].availableDataInProxyReadBuffer();
			
		}
		
		return bytesReadSoFar;
		
	}
	
	
	/**
	 * Decides whether it's time to reply or not ("true" when timeout reached 
	 * or <code>isEnoughDataAvailable() == true</code>).
	 * 
	 * @param bytesReadSoFar	Amount of data currently available in each 
	 * 							channel.
	 * 
	 * @return					Whether it's time to reply or not.
	 */
	private boolean shallMixReply(int[] bytesReadSoFar) {
		
		if (isNewDecision) {
			
			batchBegin = clock.getTime();
			isNewDecision = false;
			
		}

		if ((clock.getTime() - batchBegin) >= TIMEOUT) { // timeout reached
			
			isNewDecision = true;
			return true;
			
		} else { // timeout not yet reached
			
			return isEnoughDataAvailable(bytesReadSoFar) ? true : false;
			
		}
		
	}
	
	
	/**
	 * Decides whether "enough" data is available for replying or not. "Enough" 
	 * means <code>MIN_VOLUME_PER_CHANNEL</code> data in 
	 * <code>MIN_FILLED_CHANNELS</code> channels available.
	 * 
	 * @param bytesReadSoFar	Amount of data currently available in each 
	 * 							channel.
	 * 
	 * @return					"Enough" data available or not.
	 * 
	 * @see #MIN_FILLED_CHANNELS
	 * @see #MIN_VOLUME_PER_CHANNEL
	 */
	private boolean isEnoughDataAvailable(int[] bytesReadSoFar) {
		
		int numberOfChannelsReady = 0;
		
		int neededNumberOfChannelsReady = 
			Math.round((float)bytesReadSoFar.length * MIN_FILLED_CHANNELS);
		
		for (int dataInChannel: bytesReadSoFar) {
			
			if (dataInChannel >= MIN_VOLUME_PER_CHANNEL) {
				
				numberOfChannelsReady++;
				
				if (numberOfChannelsReady == neededNumberOfChannelsReady) {
					
					return true;
					
				}
				
			}
			
		}
		
		return false;
		
	}
	
	
	/**
	 * Decides about the "optimal size" of the reply batch (in byte). 
	 * 
	 * @param bytesReadSoFar	Amount of data currently available in each 
	 * 							channel (in byte).
	 * 
	 * @return					"Optimal size" (in byte).
	 */
	private int getWeightedMedian(int[] bytesReadSoFar) {
		
		double b;
		double a;
		double z = bytesReadSoFar.length;
		double lnFloor;
		double lnCeil;
		
		Arrays.sort(bytesReadSoFar);
		
		a = g * (z - 1.0f) + 1.0f;
		
		lnFloor = (double) bytesReadSoFar[(int) Math.floor(a) - 1];
		lnCeil = (double) bytesReadSoFar[(int) Math.ceil(a) - 1];

		b =	(a - Math.floor(a)) * (lnCeil - lnFloor) + lnFloor;
		
		return adjustBlockSizeToCryptographicBlockSize((int) Math.round(b));
		
	}
	
	
	/**
	 * Adjusts the "optimal size" to the <code>CIPHER_BLOCK_SIZE</code> (only 
	 * multiples of the <code>CIPHER_BLOCK_SIZE</code> are useful).
	 * 
	 * @param blockSize	"Optimal size" (not adjusted).
	 * 
	 * @return 			Adjusted "optimal size".
	 * 
	 * @see #CIPHER_BLOCK_SIZE
	 */
	private int adjustBlockSizeToCryptographicBlockSize(int blockSize) {
		
		int blockSizeOverhead = 
			CIPHER_BLOCK_SIZE 
			- 
			((blockSize % CIPHER_BLOCK_SIZE) + Payload.getHeaderLength());
	
		if (blockSizeOverhead != 0) {	// block size doesn't fit
			
			if (blockSizeOverhead > 0) {
				
				blockSize = blockSize + blockSizeOverhead;
				
			} else { // blockSizeOverhead < 0
				
				blockSize = blockSize + blockSizeOverhead + CIPHER_BLOCK_SIZE;
				
			}
			
		}
	
		if (blockSize > MAX_REPLY_LENGTH) {
			
			blockSize -= CIPHER_BLOCK_SIZE;
			
		}
		
		return blockSize;
		
	}
	
	
	/**
	 * Generates <code>Reply</code>ies and passes them to the 
	 * <code>InputOutputHandlerController</code>.
	 * 
	 * @param replySize	Size the replies shall have.
	 * 
	 * @param channels	Channels/users to send replies to.
	 */
	private void processMessages(int replySize, User[] channels) {
		
		for (int i=0; i<channels.length; i++) {
			
			// read as much data as possible
			byte[] data = channels[i].getFromProxyReadBuffer(replySize);
			
			// generate ReplyMessage
			Payload replyPayload = 
				new Payload(
					new byte[replySize + Payload.getHeaderLength()]
					);
			
			replyPayload.setMessage(data, replySize);
			
			ReplyMessage replyMessage = 
				new ReplyMessage(	replyPayload.getBytePayload(), 
									channels[i]
									);
			
			channels[i].setHasMessageInCurrentBatch(false);
			inputOutputHandler.addUnprocessedReply(replyMessage);
			
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
	
}
