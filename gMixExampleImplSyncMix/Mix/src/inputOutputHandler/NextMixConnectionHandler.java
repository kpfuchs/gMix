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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import message.BatchSizeMessage;
import message.ChannelEstablishMessage;
import message.ChannelReleaseMessage;
import message.ChannelMessage;
import message.ReplyMessage;
import message.Request;

import userDatabase.User;
import userDatabase.UserDatabaseController;

import util.Util;

import exception.InformationRetrieveException;
import exception.UnknownUserException;

import externalInformationPort.ExternalInformationPortController;
import externalInformationPort.Information;


/**
 * Handles communication with the next mix in the cascade.
 * <p>
 * Establishes a (TCP) connection with the next mix which is used to transmit 
 * messages (<code>Request</code>s and <code>Reply</code>ies) of all users 
 * connected to the cascade's fist mix (multiplex channel).
 * <p>
 * (De-)multiplexes <code>Request</code>s/<code>Reply</code>ies using an 
 * (encrypted) header (see generateInterMixHeader()).
 * <p>
 * <code>Reply</code>ies are put in the 
 * <code>InputOutputHandlerController</code>'s 
 * <code>replyInputQueue</code> 
 * (see <code>InputOutputHandlerController.addUnprocessedReply()</code>).
 * <code>Request</code>s are taken from the 
 * <code>InputOutputHandlerController</code>'s 
 * <code>requestOutputQueue</code> 
 * (see <code>InputOutputHandlerController.getProcessedRequest()</code>).
 * <p>
 * Note: Authentication is NOT implemented!
 * 
 * @author Karl-Peter Fuchs
 */
final class NextMixConnectionHandler extends Thread {
	
	/** 
	 * Reference on component <code>InternalInformationPort</code>. 
	 * Used to display and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = internalInformationPort.getLogger();
	
	/** 
	 * The mix' position in the cascade this object belongs to. "1" means 
	 * "first mix", "2" means "a middle mix" and "3" means "last mix" of 
	 * cascade.
	 */
	private final int POSITION_OF_MIX_IN_CASCADE;
	
	/**
	 * Block size of the cryptographic algorithm used to encrypt the 
	 * multiplex-header.
	 */
	private final int INTER_MIX_BLOCK_SIZE;
	
	/** Address of the next mix (in the cascade). */
	private final InetAddress NEXT_MIX_ADDRESS;
	
	/** Port number of the next mix (in the cascade). */
	private final int NEXT_MIX_PORT;
	
	/** 
	 * Port number of the next mix' <code>ExternalInformationProvider</code> 
	 * component (used to receive key for enrypting/decrypting 
	 * multiplex-headers).
	 */
	private final int NEXT_MIX_INFO_PORT;
	
	/** 
	 * Reference on component <code>UserDatabase</code> (Used to add/remove 
	 * users).
	 */
	private UserDatabaseController userDatabase;
	
	/** 
	 * Reference on <code>InputOutputHandlerController()</code> (Used to get 
	 * processed replies and add unprocessed requests).
	 * 
	 * @see InputOutputHandlerController#addUnprocessedRequest(Request)
	 * @see InputOutputHandlerController#getProcessedReply()
	 */
	private InputOutputHandlerController inputOutputHandler;
	
	/** 
	 * Reference on component <code>ExternalInformationPort</code> (Used to 
	 * provide the cryptographic key used to encrypt/decrypt multiplex-headers 
	 * to the previous mix. 
	 */
	private ExternalInformationPortController externalInformationPort;
	
	/** Socket for communicating with next mix. */
	private Socket nextMixSocket;
	
	/** InputStream for communicating with next mix. */
	private InputStream nextMixInputStream;
	
	/** OutputStream for communicating with next mix. */
	private OutputStream nextMixOutputStream;
	
	/** Cipher for encrypting multiplex-headers of requests. */
	private Cipher interMixEncryptCipherWithNextMix;
	
	/** Cipher for decrypting multiplex-headers of replies. */
	private Cipher interMixDecryptCipherWithNextMix;
	
	/** Key used to encrypt data between this mix and the next one. */
	private SecretKey interMixKeyWithNextMix = null;
	
	/** 
	 * Initialization vector used to encrypt data between this mix and the next 
	 * one.
	 */
	private IvParameterSpec interMixIVWithNextMix = null;
	
	/** 
	 * Indicates that method <code>handleReplies()</code> wants to send 
	 * replies (to next mix), but the connection is lost (Used for 
	 * synchronization, when reestablishing connection).
	 * 
	 * @see #handleReplies()
	 * @see #connectToNextMix()
	 */
	private AtomicBoolean handleRepliesIsWaitingForConnection = 
		new AtomicBoolean(false);
	
	
	/**
	 * Constructs a new <code>NextMixConnectionHandler</code> which handles 
	 * communication with the next mix in the cascade. 
	 * <p>
	 * Establishes a (TCP) connection with the next mix which is used to transmit 
	 * messages (<code>Request</code>s and <code>Reply</code>ies) of all users 
	 * connected to the cascade's fist mix (multiplex channel).
	 * <p>(De-)multiplexes <code>Request</code>s/<code>Reply</code>ies using an 
	 * (encrypted) header (see generateInterMixHeader()).
	 * <p>
	 * <code>Reply</code>ies are put in the 
	 * <code>InputOutputHandlerController</code>'s 
	 * <code>replyInputQueue</code> 
	 * (see <code>InputOutputHandlerController.addUnprocessedReply()</code>).
	 * <code>Request</code>s are taken from the 
	 * <code>InputOutputHandlerController</code>'s 
	 * <code>requestOutputQueue</code> 
	 * (see <code>InputOutputHandlerController.getProcessedRequest()</code>).
	 * <p>
	 * Note: Authentication is NOT implemented!
	 */
	protected NextMixConnectionHandler(
			InputOutputHandlerController inputOutputHandler,
			UserDatabaseController userDatabase,
			ExternalInformationPortController externalInformationPort
			) {
		
		this.NEXT_MIX_ADDRESS = 
			InputOutputHandlerController.tryToGenerateInetAddress(
					getProperty("NEXT_MIX_ADDRESS")
					);
		
		this.NEXT_MIX_PORT = 
				new Integer(getProperty("NEXT_MIX_PORT"));
		
		this.NEXT_MIX_INFO_PORT = 
			new Integer(getProperty("NEXT_MIX_INFO_PORT"));
		
		this.POSITION_OF_MIX_IN_CASCADE = 
			new Integer(getProperty("POSITION_OF_MIX_IN_CASCADE"));
		
		this.INTER_MIX_BLOCK_SIZE = 
			new Integer(getProperty("INTER_MIX_BLOCK_SIZE"));
		
		this.userDatabase = userDatabase;
		this.inputOutputHandler = inputOutputHandler;
		this.externalInformationPort = externalInformationPort;
		
		LOGGER.config(	"(MIX" +POSITION_OF_MIX_IN_CASCADE +") "
						+"Address of next mix: " +NEXT_MIX_ADDRESS
						+":" +NEXT_MIX_PORT
						);
		
		setUpInterMixCiphers();
		
		start();
		
		new Thread(
				
				new Runnable() {
									
					public void run() {

						handleReplies();
							
					}
									
				}
						
			).start();
		
	}
	
	
	/**
	 * Waits for <code>Reply</code>ies sent by next mix.
	 */
	private void handleReplies() {
		
		while (true) { // receive messages from next mix
			
			try {
				
				waitForConnection();
				
				byte[] lengthOfReplyAsByteArray = new byte[4];
				lengthOfReplyAsByteArray = Util.forceRead(nextMixInputStream, lengthOfReplyAsByteArray);
				
				int lengthOfReply = 
					Util.byteArrayToInt(lengthOfReplyAsByteArray);
				
				if (lengthOfReply < 1) {
					
					LOGGER.severe(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
									+" IOH) Illegal size for reply message "
									+"received!"
									);
					
					nextMixSocket.close();
					continue;
					
				}
				
				// read encrypted block, which contains the inter mix header
				byte[] blockWithHeader = new byte[INTER_MIX_BLOCK_SIZE];
				blockWithHeader = Util.forceRead(nextMixInputStream, blockWithHeader);
				
				// decrypt blockWithHeader
				blockWithHeader = 
					interMixDecryptCipherWithNextMix.
						update(blockWithHeader);
				
				int channelIdentifier = 
					Util.byteArrayToInt(
							Arrays.copyOfRange(blockWithHeader, 1, 5)
							);
				
				User channel = null;
				
				try {
					
					channel = 
						userDatabase.getUserByNextMixIdentifier(
							channelIdentifier
							);
					
				} catch (UnknownUserException e) {
					
					LOGGER.severe(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
									+" IOH) Received reply message for "
									+"unknown channelID!"
									);
	
					nextMixInputStream.skip(	lengthOfReply 
												- 
												INTER_MIX_BLOCK_SIZE
												);
					// "minus blockSize", because header is already read. 
					// and the header's length is determined by blockSize
			
					continue;
					
				}
				
				if (channel.getHasMessageInCurrentReplyBatch()) {
					// next Mix is trying to send multiple messages on one 
					// channel!
					
					LOGGER.severe(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
									+" IOH) Next Mix is trying to send "
									+"multiple replies on one channel!"
									);
					
					nextMixInputStream.skip(	lengthOfReply 
												- 
												INTER_MIX_BLOCK_SIZE
												);
					// "minus blockSize", because header is already read. 
					// and the header's length is determined by blockSize
					
					continue;
					
				}
				// channel existent and unused for this batch
				// -> accept message
				
				byte[] restOfMessage = 
					new byte[lengthOfReply - INTER_MIX_BLOCK_SIZE];
				// "minus blockSize", because header is already read. 
				// and the header's length is determined by blockSize
				
				restOfMessage = Util.forceRead(nextMixInputStream, restOfMessage);
				
				byte[] message = 
					Util.mergeArrays(
						Arrays.copyOfRange(	blockWithHeader, 
											5, 
											INTER_MIX_BLOCK_SIZE), 
						restOfMessage
						);
				
				ReplyMessage replyMessage = 
					new ReplyMessage(message, channel);
				
				LOGGER.finer(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
								+" IOH) Accepted message from " 
								+nextMixSocket.getInetAddress() 
								+": " 
								+replyMessage
								);
				
				inputOutputHandler.addUnprocessedReply(replyMessage);
				
			} catch (IOException e) {
				
				waitForConnection();
				continue;
				
			}
			
		}
		
	}
	
	
	/**
	 * Waits until a connection to the next mix is (re)established.
	 */
	private void waitForConnection() {
		
		synchronized (handleRepliesIsWaitingForConnection) {
			
			if (	nextMixSocket == null 
					|| 
					!nextMixSocket.isConnected()
					) {
				
				// wait for connection
				handleRepliesIsWaitingForConnection.set(true);
				
				while (	nextMixSocket == null 
						|| 
						!nextMixSocket.isConnected()
						) {
					
					try {
						
						// wait for new connection
						// = wait for notification at "run()"
						handleRepliesIsWaitingForConnection.wait();
						
					} catch (InterruptedException e) {
						
						LOGGER.severe(e.getMessage());
						continue;
						
					}
					
				}
				
				handleRepliesIsWaitingForConnection.set(false);
			
			}
			
		}
		
	}


	/**
	 * Sends processed messages (Requests) to next mix.
	 * 
	 * @see InputOutputHandlerController#getProcessedRequest()
	 */
	@Override
	public void run() {
		
		connectToNextMix();
		
		// send (mixed) messages from "requestOutputQueue" to next mix (if  
		// new messages are available)
		while (true) {
				
			// wait for next message
			Request request = inputOutputHandler.getProcessedRequest();
			
			if (request.getChannel() != null) {
				
				request.getChannel().setTimestampOfLastActivity();
				
			}
			
			byte[] messageToSend = 
				Util.mergeArrays(	generateInterMixHeader(request), 
									getPayload(request)
									);
			
			messageToSend = encryptInterMixHeader(messageToSend);
			
			// send data
			try {
				
				nextMixOutputStream.write(messageToSend);
				nextMixOutputStream.flush();
				
			} catch (IOException e) {
					
				LOGGER.warning(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
								+" IOH) Connection to next mix (" 
								+NEXT_MIX_ADDRESS 
								+":" 
								+NEXT_MIX_PORT 
								+") lost."
								);
					
				connectToNextMix(); // reestablish connection
				continue;
				
			}
			
		}
		
	}
	
	
	/**
	 * Establishes a (permanent) connection to the next mix of the cascade.
	 */
	private void connectToNextMix() {
		
		LOGGER.info(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
						+" IOH) Trying to connect to next mix ("
						+NEXT_MIX_ADDRESS
						+":" 
						+NEXT_MIX_PORT
						+")"
						);
		
		if (nextMixSocket != null) { // connection lost => reset ciphers
			
			resetInterMixCiphers();
			
		}
			
		while (true) { // try to connect to next mix
			
			try {
				
				synchronized (handleRepliesIsWaitingForConnection) {
					
					nextMixSocket = new Socket();
					nextMixSocket.setKeepAlive(true);// permanent connection
					
					SocketAddress receiverAddress = 
						new InetSocketAddress(	NEXT_MIX_ADDRESS, 
												NEXT_MIX_PORT
												);
					
					nextMixSocket.connect(receiverAddress);
					nextMixOutputStream = nextMixSocket.getOutputStream();
					nextMixInputStream = nextMixSocket.getInputStream();
					
					// notify "handleReplies()" and exit loop, since no 
					// IOException has occurred (= connection is 
					// established successfully)
					
					if (handleRepliesIsWaitingForConnection.get()) {
						
						handleRepliesIsWaitingForConnection.notify();
						
					}
					
					break; 	// exit loop, when no IOException has occurred 
							// (= connection is established successfully)
					
				}
				
				
			} catch (IOException e) { // connection failed

				LOGGER.warning(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
								+" IOH) Connection to next mix ("
								+NEXT_MIX_ADDRESS
								+":" 
								+NEXT_MIX_PORT 
								+") could not be established!"
								);
				
				LOGGER.info(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
								+" IOH) Trying to connect again..."
								);
				
			}
			
		}
		
		LOGGER.info(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
						+" IOH) Connection to next mix ("
						+NEXT_MIX_ADDRESS
						+":" 
						+NEXT_MIX_PORT
						+") established."
						);
		
	}
	
	
	/**
	 * Adds an (encrypted) header to a <code>Request</code> containing 
	 * an identifier for the suiting channel (the identifier is unique for any 
	 * neighboured mixes).
	 * 
	 * @param request	<code>Request</code> without header.
	 * 
	 * @return 			<code>Request</code> with header.
	 */
	private byte[] generateInterMixHeader(Request request) {
		
		byte[] interMixHeader = new byte[5];
		int channelIdentifier = request.getNextMixChannelID();
		
		byte[] channelIdentifierAsArray = 
			Util.intToByteArray(channelIdentifier);
		
		System.arraycopy(	channelIdentifierAsArray, 
							0, 
							interMixHeader, 
							1, 
							4
							);
		
		if (request instanceof ChannelEstablishMessage) {
			
			interMixHeader[0] = ChannelEstablishMessage.IDENTIFIER;
		
		} else if (request instanceof ChannelMessage) {
			
			interMixHeader[0] = ChannelMessage.IDENTIFIER;
			
		} else if (request instanceof ChannelReleaseMessage) {
			
			interMixHeader[0] = ChannelReleaseMessage.IDENTIFIER;
			
		} else { // BatchSizeMessage
			
			BatchSizeMessage bsm = (BatchSizeMessage)request;
			interMixHeader[0] = BatchSizeMessage.IDENTIFIER;
			byte[] batchSize = Util.intToByteArray(bsm.getBatchSize());
			
			System.arraycopy(	batchSize, 
								0, 
								interMixHeader, 
								1, 
								4
								);
			
		}
		
		return interMixHeader;
		
	}
	
	
	/**
	 * Returns the payload of the bypassed <code>Request</code>.
	 * 
	 * @param request	<code>Request</code> containing the payload to be 
	 * 					returned.
	 * 
	 * @return			Payload of the bypassed <code>Request</code>.
	 */
	private byte[] getPayload(Request request) {
		
		byte[] payload;
		
		if (request instanceof ChannelEstablishMessage) {
			
			ChannelEstablishMessage cem = 
				(ChannelEstablishMessage)request;
			
			payload = cem.getPayload().getBytePayload();
			
		} else if (request instanceof ChannelMessage) {
			
			ChannelMessage fcm = 
				(ChannelMessage)request;
			
			payload = fcm.getPayload().getBytePayload();
			
		} else {
			
			payload = new byte[0];
			
		}
		
		return payload;
		
	}
	
	
	/**
	 * Encrypts the block containing the multiplex-header of the bypassed 
	 * message.
	 * 
	 * @param message	Message with plaintext multiplex-header.
	 * 
	 * @return			Message with encrypted multiplex-header.
	 */
	private byte[] encryptInterMixHeader(byte[] message) {
		
		if (message.length < INTER_MIX_BLOCK_SIZE) {
			
			byte[] padding = 
				new byte[INTER_MIX_BLOCK_SIZE - message.length];
			
			Arrays.fill(padding, (byte)120);
			message = Util.mergeArrays(message, padding);
			
		}
		
		byte[] blockForEncryption = 
			interMixEncryptCipherWithNextMix.update(	message, 
														0, 
														INTER_MIX_BLOCK_SIZE
														);
		
		return Util.mergeArrays(blockForEncryption, 
							 	Arrays.copyOfRange(	message, 
							 						INTER_MIX_BLOCK_SIZE, 
													message.length)
													);
		
	}
	
	
	/**
	 * Receives a cryptographic key, that is used to encrypt multiplex-headers 
	 * (between this mix an the next mix) from the next mix and sets up suiting 
	 * <code>Cipher</code>s.
	 * <p>
	 * Note: Authentication NOT implemented!
	 */
	private void setUpInterMixCiphers() {
		
		// get interMixKeyWithNextMix
		LOGGER.fine(	"(MIX" +POSITION_OF_MIX_IN_CASCADE +" IOH) "
						+"Waiting for data from next mix."
						);
		
		try {
			
			interMixKeyWithNextMix = 
				(SecretKey) externalInformationPort.
					getInformation(	NEXT_MIX_ADDRESS,
									NEXT_MIX_INFO_PORT, 
									Information.INTER_MIX_KEY
									);
			
			interMixIVWithNextMix = 
				(IvParameterSpec) externalInformationPort.
					getInformation(	NEXT_MIX_ADDRESS,
									NEXT_MIX_INFO_PORT, 
									Information.INTER_MIX_IV
									);
			
		} catch (InformationRetrieveException e) {
			
			LOGGER.severe(	"(MIX" +POSITION_OF_MIX_IN_CASCADE +" IOH) "
							+"Couldn't retrieve inter mix key!"
							);
			
			System.exit(1);
			
		}
		
		LOGGER.fine(	"(MIX" +POSITION_OF_MIX_IN_CASCADE +" IOH) "
						+"Data from next mix received."
						);
		
		try {
			
			this.interMixEncryptCipherWithNextMix = 
				Cipher.getInstance(
					getProperty("INTER_MIX_CRYPTOGRAPHY_ALGORITHM"),
					getProperty("CRYPTO_PROVIDER")
					);
					
			this.interMixEncryptCipherWithNextMix.init(	
				Cipher.ENCRYPT_MODE,
				interMixKeyWithNextMix,
				interMixIVWithNextMix
				);
				
			this.interMixDecryptCipherWithNextMix = Cipher.getInstance(
				getProperty("INTER_MIX_CRYPTOGRAPHY_ALGORITHM"),
				getProperty("CRYPTO_PROVIDER")
				);
					
			this.interMixDecryptCipherWithNextMix.init(	
				Cipher.DECRYPT_MODE,
				interMixKeyWithNextMix,
				interMixIVWithNextMix
				);
			
		} catch (Exception e) {
			
			LOGGER.severe(	"(MIX" +POSITION_OF_MIX_IN_CASCADE +" IOH) " 
							+"Couldn't set up inter mix ciphers!" 
							+e.getMessage()
							);
			
			System.exit(1);
			
		}
		
	}

	
	/**
	 * Resets the <code>Cipher</code>s used to encrypt data between this mix 
	 * and the next one. Must be called when connection was lost, since  
	 * <code>Cipher</code>s might be out of sync in that case.
	 */
	private void resetInterMixCiphers() {
		
		try {
			
			this.interMixEncryptCipherWithNextMix = 
				Cipher.getInstance(
					getProperty("INTER_MIX_CRYPTOGRAPHY_ALGORITHM"),
					getProperty("CRYPTO_PROVIDER")
					);
					
			this.interMixEncryptCipherWithNextMix.init(	
				Cipher.ENCRYPT_MODE,
				interMixKeyWithNextMix,
				interMixIVWithNextMix
				);
				
			this.interMixDecryptCipherWithNextMix = Cipher.getInstance(
				getProperty("INTER_MIX_CRYPTOGRAPHY_ALGORITHM"),
				getProperty("CRYPTO_PROVIDER")
				);
					
			this.interMixDecryptCipherWithNextMix.init(	
				Cipher.DECRYPT_MODE,
				interMixKeyWithNextMix,
				interMixIVWithNextMix
				);
			
		} catch (Exception e) {
			
			LOGGER.severe(	"(MIX" +POSITION_OF_MIX_IN_CASCADE +" IOH) " 
							+"Couldn't set up inter mix ciphers!" 
							+e.getMessage()
							);
			
			System.exit(1);
			
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
