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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import keyGenerator.KeyGeneratorController;

import outputStrategy.OutputStrategyController;

import message.BatchSizeMessage;
import message.ChannelEstablishMessage;
import message.ChannelEstablishMessagePart;
import message.ChannelReleaseMessage;
import message.ChannelMessage;
import message.ChannelMessagePart;
import message.Reply;
import message.ReplyMessage;

import userDatabase.User;
import userDatabase.UserDatabaseController;

import util.Util;

import exception.UserAlreadyExistingException;
import exception.UnknownUserException;

import externalInformationPort.ExternalInformationPortController;


/**
 * Handles communication with a previous mix (in the same cascade as this one). 
 * Waits for the previous mix to establish a permanent (TCP) connection, which 
 * is used to transmit the messages (<code>Request</code>s and 
 * <code>Reply</code>ies) of all users connected to the cascade's fist mix 
 * (multiplex channel). 
 * (De-)multiplexes <code>Request</code>s/<code>Reply</code>ies using an 
 * (encrypted) header (see addInterMixHeader()).
 * <p>
 * Note: Authentication is NOT implemented!
 * 
 * @author Karl-Peter Fuchs
 */
final class PreviousMixConnectionHandler extends Thread {
	
	/** 
	 * Reference on component <code>InternalInformationPort</code>. 
	 * Used to display and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = internalInformationPort.getLogger();
	
	/** Port number this <code>PreviousMixConnectionHandler</code> runs on. */
	private final int PORT; 
	
	/** 
	 * Address this <code>PreviousMixConnectionHandler</code>'s socket shall be 
	 * bound to.
	 */
	private final InetAddress BIND_ADDRESS;
	
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
	
	/** Address of the previous mix in the cascade. */
	private final InetAddress PREVIOUS_MIX_ADDRESS;
	
	/** Port number of the previous mix in the cascade. */
	private final int PREVIOUS_MIX_PORT;
	
	/** 
	 * Number of further mixes between the mix this 
	 * <code>PreviousMixConnectionHandler</code> belongs to and the receiver. 
	 */
	private final int NUMBER_OF_FURTHER_HOPS;
	
	/** Indicates whether this mix is the lost of the cascade or not. */
	private final boolean IS_LAST;
	
	/** Socket for communicating with previous mix. */
	private Socket previousMixSocket;
	
	/** InputStream for communicating with previous mix. */
	private InputStream previousMixInputStream;
	
	/** OutputStream for communicating with previous mix. */
	private OutputStream previousMixOutputStream;
	
	/** Cipher for encrypting multiplex-headers of replies. */
	private Cipher interMixEncryptCipherWithPreviousMix;
	
	/** Cipher for decrypting multiplex-headers of requests. */
	private Cipher interMixDecryptCipherWithPreviousMix;
	
	/** Key used to encrypt data between this mix and its predecessor. */
	private SecretKey interMixKeyWithPreviousMix = null;
	
	/** 
	 * Initialization vector used to encrypt data between this mix and its
	 * predecessor.
	 */
	private IvParameterSpec interMixIVWithPreviousMix = null;
	
	/** 
	 * Indicates that method <code>handleReplies()</code> wants to send 
	 * replies (to the previous mix), but the connection is lost (Used for 
	 * synchronization, when connection is established again).
	 * 
	 * @see #handleReplies()
	 * @see #waitForIncomingConnection()
	 */
	private AtomicBoolean handleRepliesIsWaitingForConnection = 
		new AtomicBoolean(false);
	
	/** ServerSocket used to accept the previous mix' connection attempt. */
	private ServerSocket serverSocket;
	
	/** 
	 * Reference on <code>InputOutputHandlerController()</code> (Used to get 
	 * processed replies and add unprocessed requests).
	 * 
	 * @see InputOutputHandlerController#addUnprocessedRequest(message.Request)
	 * @see InputOutputHandlerController#getProcessedReply()
	 */
	private InputOutputHandlerController inputOutputHandler;
	
	/** 
	 * Reference on component <code>UserDatabase</code> (Used to add/remove 
	 * users).
	 */
	private UserDatabaseController userDatabase;
	
	/** 
	 * Reference on component <code>OutputStrategy</code> (Used to add 
	 * <code>ChannelReleaseMessages</code>).
	 */
	private OutputStrategyController outputStrategy;
	
	/** 
	 * Reference on component <code>ExternalInformationPort</code> (Used to 
	 * provide the cryptographic key used to encrypt/decrypt multiplex-headers 
	 * to the previous mix. 
	 */
	private ExternalInformationPortController externalInformationPort;
	
	
	/**
	 * Constructs a new <code>PreviousMixConnectionHandler</code> which handles 
	 * communication with a previous mix (in the same cascade as this one). 
	 * Waits for the previous mix to establish a permanent (TCP) connection, 
	 * which is used to transmit the messages (<code>Request</code>s and 
	 * <code>Reply</code>ies) of all users connected to the cascade's fist mix 
	 * (multiplex channel). (De-)multiplexes 
	 * <code>Request</code>s/<code>Reply</code>ies using an (encrypted) header 
	 * (see addInterMixHeader()).
	 * <p>
	 * <code>Requests</code> are put in the 
	 * <code>InputOutputHandlerController</code>'s 
	 * <code>requestInputQueue</code> 
	 * (see <code>InputOutputHandlerController.addUnprocessedRequest()</code>).
	 * <code>Reply</code>ies are taken from the 
	 * <code>InputOutputHandlerController</code>'s <code>replyOutputQueue</code> 
	 * (see <code>InputOutputHandlerController.getProcessedReply()</code>).
	 * <p>
	 * Adds/removes user to/from <code>UserDatabase</code>.
	 * <p>
	 * Note: Authentication is NOT implemented!
	 * 
	 * @param inputOutputHandler	Reference on 
	 * 								<code>InputOutputHandlerController</code> 
	 * 								(Used to add messages).
	 * @param userDatabase			Reference on component 
	 * 								<code>UserDatabase</code> (Used to 
	 * 								add/remove <code>User</code>)s.
	 * @param outputStrategy		Reference on component 
	 * 								<code>OutputStrategy</code> (Used to add 
	 * 								<code>ChannelReleaseMessages</code>).
	 * @param externalInformationPort	Reference on component 
	 * 									<code>ExternalInformationPort</code> 
	 * 									(Used to provide the cryptographic key 
	 * 									used to encrypt/decrypt 
	 * 									multiplex-headers to the previous mix. 
	 */
	protected PreviousMixConnectionHandler(
			InputOutputHandlerController inputOutputHandler,
			UserDatabaseController userDatabase,
			OutputStrategyController outputStrategy,
			ExternalInformationPortController externalInformationPort
			) {
		
		this.BIND_ADDRESS = 
			InputOutputHandlerController.tryToGenerateInetAddress(
					getProperty("BIND_ADDRESS")
					);
			
		this.PORT = new Integer(getProperty("PORT")); 
		
		this.PREVIOUS_MIX_ADDRESS = 
			InputOutputHandlerController.tryToGenerateInetAddress(
					getProperty("PREVIOUS_MIX_ADDRESS")
					);
		
		this.PREVIOUS_MIX_PORT = 
				new Integer(getProperty("PREVIOUS_MIX_PORT"));
		
		this.POSITION_OF_MIX_IN_CASCADE = 
			new Integer(getProperty("POSITION_OF_MIX_IN_CASCADE"));
		
		this.INTER_MIX_BLOCK_SIZE = 
			new Integer(getProperty("INTER_MIX_BLOCK_SIZE"));
		
		this.NUMBER_OF_FURTHER_HOPS = 
			new Integer(getProperty("NUMBER_OF_FURTHER_MIXES"));
		
		this.IS_LAST = inputOutputHandler.IS_LAST;
		
		this.inputOutputHandler = inputOutputHandler;
		this.userDatabase = userDatabase;
		this.outputStrategy = outputStrategy;
		this.externalInformationPort = externalInformationPort;
		
		LOGGER.config(	"(MIX" +POSITION_OF_MIX_IN_CASCADE +") "
						+"Address of previous mix: " 
						+PREVIOUS_MIX_ADDRESS +":" +PREVIOUS_MIX_PORT
						);
		
		setUpInterMixCiphers();
				
	}
	
	
	/**
	 * Makes this <code>PreviousMixConnectionHandler</code> wait for 
	 * its predecessor to connect.
	 */
	protected void acceptConnection() {
		
		try {
			
			serverSocket = new ServerSocket(PORT, 1, BIND_ADDRESS);
				
			LOGGER.info(	"(MIX" +POSITION_OF_MIX_IN_CASCADE +" IOH) " 
							+"Listening on port " +PORT
							);
			
			LOGGER.config(	"(MIX" +POSITION_OF_MIX_IN_CASCADE +") Bound to: " 
							+BIND_ADDRESS +":" +PORT
							);
			
		} catch (IOException e) {
				
			LOGGER.severe(	"(MIX" +POSITION_OF_MIX_IN_CASCADE +" IOH) " 
							+"Couldn't bind socket to port " +PORT +"!" 
							+e.getMessage()
							);
			
			System.exit(1);
			
		}
		
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
	 * Waits for <code>Reply</code>ies ready to be sent (to the previous mix).
	 */
	private void handleReplies() {
		
		waitForConnection();
		
		while (true) { // send replies to previous mix
			
			// wait for next message
			Reply reply = inputOutputHandler.getProcessedReply();
			
			byte[] messageToSend = addInterMixHeader(reply);
			
			// send message
			try {
				
				byte[] lengthHeader = 
					Util.intToByteArray(messageToSend.length);
				
				previousMixOutputStream.write(lengthHeader);
				previousMixOutputStream.write(messageToSend);
				previousMixOutputStream.flush();
					
			} catch (IOException e) {
					
				LOGGER.warning(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
								+" IOH) Connection to previous mix (" 
								+PREVIOUS_MIX_ADDRESS 
								+":" 
								+PREVIOUS_MIX_PORT 
								+") lost."
								);
					
				waitForConnection();
				continue;
				
			}
			
		}
		
	}

	
	/**
	 * Waits for the previous mix to establish a connection.
	 */
	private void waitForConnection() {
		
		synchronized (handleRepliesIsWaitingForConnection) {
			
			if (	previousMixSocket == null 
					|| 
					!previousMixSocket.isConnected()
					) {
				
				// wait for connection
				handleRepliesIsWaitingForConnection.set(true);
				
				while (	previousMixSocket == null 
						|| 
						!previousMixSocket.isConnected()
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
	 * Adds an (encrypted) header to a <code>Reply</code> containing 
	 * an identifier for the suiting channel (the identifier is unique for any 
	 * neighboured mixes).
	 * 
	 * @param reply	<code>Reply</code> without header.
	 * 
	 * @return <code>Reply</code> with header.
	 */
	private byte[] addInterMixHeader(Reply reply) {
		
		byte[] messageToSend = reply.getByteMessage();
		byte[] interMixHeader = new byte[5];
		
		byte[] channelIdentifier = 
			Util.intToByteArray(reply.getChannelID());
		
		System.arraycopy(channelIdentifier, 0, interMixHeader, 1, 4);
		interMixHeader[0] = ReplyMessage.IDENTIFIER;
		
		messageToSend = 
			Util.mergeArrays(interMixHeader, messageToSend);
			
		// encrypt block with interMixHeader
		interMixHeader = 
			interMixEncryptCipherWithPreviousMix.update(messageToSend, 
														0, 
														INTER_MIX_BLOCK_SIZE
														);
		
		return Util.mergeArrays(interMixHeader, 
								Arrays.copyOfRange(	messageToSend, 
													INTER_MIX_BLOCK_SIZE, 
													messageToSend.length
													)
					);
		
	}


	/**
	 * Receives messages from previous mix and passes them to the 
	 * <code>InputOutputHandler</code>.
	 * 
	 * @see InputOutputHandlerController#addUnprocessedRequest(message.Request)
	 */
	@Override
	public void run() {
		
		waitForIncomingConnection();
		
		while (true) { // receive messages from previous mix
			
			try {
					
				User user = null;
				byte[] blockWithHeader = new byte[INTER_MIX_BLOCK_SIZE];
				blockWithHeader = Util.forceRead(previousMixInputStream, blockWithHeader);
				// decrypt blockWithHeader
				blockWithHeader = decryptBlockWithHeader(blockWithHeader);
				
				int channelIdentifier = 
					Util.byteArrayToInt(Arrays.copyOfRange(blockWithHeader, 1, 5));
				
				byte messageIdentifier = blockWithHeader[0];
				
				if (	messageIdentifier 
						== 
						ChannelEstablishMessage.IDENTIFIER
						) {
					
					User newChannel;
					
					try {
						
						newChannel = createNewUser(channelIdentifier);
						
					} catch (UserAlreadyExistingException e) {
						// skip this message
						
						LOGGER.severe(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
										+" IOH) Received a "
										+"ChannelEstablishMessage from "
										+"previous mix with a channel "
										+"indentifier already in use!"
										);
						
						skipChannelEstablishMessage();
						continue;
						
					} 
					
					ChannelEstablishMessage cem = 
						readChannelEstablishMessage(	blockWithHeader, 
														newChannel
														);
						
					inputOutputHandler.addUnprocessedRequest(cem);
					
				} else if (	messageIdentifier 
							== 
							ChannelMessage.IDENTIFIER
							) {
					
					// get channel
					try {
						
						user = userDatabase.getUser(channelIdentifier);
						
					} catch (UnknownUserException e) {

						LOGGER.severe(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
										+" IOH) Received a "
										+"ChannelMessage from "
										+" previous mix with an unknown "
										+"channel indentifier!"
										);
				
						skipChannelMessage();
						continue;
				
					}
					
					if (!isAllowedToSendChannelMessage(user)) {
						
						LOGGER.severe(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
										+" IOH) Previous mix tried to send"
										+"multiple messages on a channel "
										+"for one batch!!"
										);
						
						skipChannelMessage();
						continue;
						
					} else {
						
						user.setTimestampOfLastActivity();
						
						ChannelMessage fm = 
							readChannelMessage(blockWithHeader, user);
						
						inputOutputHandler.addUnprocessedRequest(fm);
						
					}
					
				} else if (	messageIdentifier 
							== 
							ChannelReleaseMessage.IDENTIFIER
							) {
					
					if(!IS_LAST) {
						
						try {
							
							user = userDatabase.getUser(channelIdentifier);
							
							ChannelReleaseMessage channelReleaseMessage = 
								new ChannelReleaseMessage(user);
							
							userDatabase.removeUser(channelIdentifier);
							
							outputStrategy.
								addRequest(channelReleaseMessage);
							
							
							
						} catch (UnknownUserException e) {
							
							LOGGER.severe(	"(MIX" 
											+POSITION_OF_MIX_IN_CASCADE 
											+" IOH) Received a "
											+"ChannelReleaseMessage from "
											+"previous mix with an unknown "
											+"channel indentifier!"
											);

							continue;
							
						}
						
					} else { // last mix of cascade (or single mix)
						
						try {
							
							userDatabase.removeUser(channelIdentifier);
							
						} catch (UnknownUserException e) {

							LOGGER.severe(	"(MIX" 
											+POSITION_OF_MIX_IN_CASCADE 
											+" IOH) Received a "
											+"ChannelReleaseMessage from "
											+"previous mix with an unknown "
											+"channel indentifier!"
											);

							continue;
					
						}
						
					}
					
				} else if (	messageIdentifier 
							== 
							BatchSizeMessage.IDENTIFIER
							) {
					
					byte[] sizeAsArray = 
						Arrays.copyOfRange(blockWithHeader, 1, 5);
					
					outputStrategy.setBatchSize(
							Util.byteArrayToInt(sizeAsArray)
							);
					
				} else { // invalid identifier
					
					LOGGER.severe(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
									+" IOH) Received invalid message "
									+"header!"
									);
					
					continue;
					
				}
							
			} catch (IOException e) { // connection is lost
							
					LOGGER.warning(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
									+" IOH) Connection from previous mix"
									+" (" 
									+PREVIOUS_MIX_ADDRESS 
									+":" 
									+PREVIOUS_MIX_PORT 
									+") lost."
									);

					waitForIncomingConnection();
					continue;
					
			}
			
		}

	}
	
	
	/**
	 * Waits for the previous mix to connect. Called when connection to 
	 * previous mix is lost.
	 */
	private void waitForIncomingConnection() {
		
		if (previousMixSocket != null) { // connection lost => reset ciphers
			
			resetInterMixCiphers();
			
		}
		
		LOGGER.info(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
						+" IOH) Waiting for Connection of previous mix..."
						);
		
		while (true) { 	// wait for previous mix to reestablish 
						// connection
		
			try {
						
				synchronized (handleRepliesIsWaitingForConnection) {
					
					previousMixSocket = serverSocket.accept();
					previousMixSocket.setKeepAlive(true);
					previousMixInputStream = previousMixSocket.getInputStream();
					
					previousMixOutputStream = 
						previousMixSocket.getOutputStream();
							
					LOGGER.info(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
									+" IOH) Connection established."
									);
					
					if (handleRepliesIsWaitingForConnection.get()) {
						
						handleRepliesIsWaitingForConnection.notify();
						
					}
					
					break;
					
				}
						
			} catch (IOException e) {
					
				LOGGER.warning(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
								+" IOH) Connection try from" 
								+"previous mix failed."
								);
				
				continue; // wait again
					
			}
	
		}
		
	}
	
	
	/**
	 * Decrypts multiplex-header.
	 * 
	 * @param blockWithHeader	Block of the <code>Request</code> containing 
	 * 							the multiplex-header.
	 * 
	 * @return					Decrypted multiplex-header.
	 */
	private byte[] decryptBlockWithHeader(byte[] blockWithHeader) {
		
		return interMixDecryptCipherWithPreviousMix.update(blockWithHeader);
		
	}
	
	
	/**
	 * Creates a new <code>User</code> with the bypassed identifier.
	 * 
	 * @param userIdentifier	Used to identify to which user/channel a 
	 * 							message belongs.
	 * 
	 * @return					New <code>User</code>.
	 * 
	 * @throws IOException		If an I/O error occurres.
	 * @throws UserAlreadyExistingException	If a <code>User</code> with the 
	 * 										specified identifier is already 
	 * 										existing (-> attack or collision).
	 */
	private User createNewUser(int userIdentifier) throws 
			IOException, UserAlreadyExistingException {
		
		if (userDatabase.isExistingUser(userIdentifier)) {
			// channel with this identifier already existent
			
			LOGGER.info("(MIX" +POSITION_OF_MIX_IN_CASCADE 
						+" IOH) Received "
						+"ChannelEstablishMessage for existing "
						+"channel: " +userIdentifier +"!"
						);
			
			skipChannelEstablishMessage();
			return null;
			
		} else {
			
			// create new channel
			User newChannel = new User(userIdentifier);
			
			if (IS_LAST) { // last mix of cascade
				
				newChannel.initializeProxyBuffers();
				
			}
			
			userDatabase.addUser(newChannel);
			return newChannel;
			
		}
		
	}
	
	
	/**
	 * Skips a <code>ChannelEstablishMessage</code> on the communication 
	 * channel.
	 * 
	 * @throws IOException	If an I/O error occurres.
	 */
	private void skipChannelEstablishMessage() throws IOException {
		
		int restOfMessageLength = 
			ChannelEstablishMessagePart.getMessageLength(
					NUMBER_OF_FURTHER_HOPS
					)
			- 
			(INTER_MIX_BLOCK_SIZE - 5);
			// "-(INTER_MIX_BLOCK_SIZE - 5)" because the first 
			// "(INTER_MIX_BLOCK_SIZE - 5)" bytes of the message are 
			// already read (they are located in 
			// "blockWithHeader[5-INTER_MIX_BLOCK_SIZE]")
		
		previousMixInputStream.skip(restOfMessageLength);
		
	}
	
	
	/**
	 * Skips a <code>ChannelMessage</code> on the communication channel.
	 * 
	 * @throws IOException	If an I/O error occurres.
	 */
	private void skipChannelMessage() throws IOException {

		int restOfMessageLength = 
			ChannelMessagePart.getMessageLength(
					NUMBER_OF_FURTHER_HOPS
					)
			- 
			(INTER_MIX_BLOCK_SIZE - 5);
			// "-(INTER_MIX_BLOCK_SIZE - 5)" because the first 
			// "(INTER_MIX_BLOCK_SIZE - 5)" bytes of the message are 
			// already read (they are located in 
			// "blockWithHeader[5-INTER_MIX_BLOCK_SIZE]")
		
		previousMixInputStream.skip(restOfMessageLength);
		
	}


	/**
	 * Reads a <code>ChannelEstablishMessage</code> from the previous mix, 
	 * generates a <code>ChannelEstablishMessage</code> object and returns it.
	 * 
	 * @param blockWithHeader	First part of the message (already received 
	 * 							since overlapped by the block including the 
	 * 							multiplex-header).
	 * @param user				<code>User</code> the returned message belongs 
	 * 							to.
	 * 
	 * @return					The received 
	 * 							<code>ChannelEstablishMessage</code>;
	 * 
	 * @throws IOException		If an I/O error occurres.
	 */
	private ChannelEstablishMessage readChannelEstablishMessage(
			byte[] blockWithHeader, 
			User user) throws IOException {
		
		byte[] firstPartOfMessage = 
			Arrays.copyOfRange(blockWithHeader, 5, INTER_MIX_BLOCK_SIZE);
		
		int restOfMessageLength = 
			ChannelEstablishMessagePart.getMessageLength(
					NUMBER_OF_FURTHER_HOPS
					)
			- 
			(INTER_MIX_BLOCK_SIZE - 5);
			// "-(INTER_MIX_BLOCK_SIZE - 5)" because the first 
			// "(INTER_MIX_BLOCK_SIZE - 5)" bytes of the message are 
			// already read (they are located in 
			// "blockWithHeader[5-INTER_MIX_BLOCK_SIZE]")
		
		byte[] restOfMessage = new byte[restOfMessageLength];
		restOfMessage = Util.forceRead(previousMixInputStream, restOfMessage);
			
		byte[] completeMessage = 
			Util.mergeArrays(firstPartOfMessage, restOfMessage);
			
		return new ChannelEstablishMessage(	completeMessage, 
											user, 
											NUMBER_OF_FURTHER_HOPS
											);
			
	}
	
	
	/**
	 * Reads a <code>ChannelMessage</code> from the previous mix, generates a 
	 * <code>ChannelMessage</code> object and returns it.
	 * 
	 * @param blockWithHeader	First part of the message (already received 
	 * 							since overlapped by the block including the 
	 * 							multiplex-header).
	 * @param user				<code>User</code> the returned message belongs 
	 * 							to.
	 * 
	 * @return					The received <code>ChannelMessage</code>;
	 * 
	 * @throws IOException		If an I/O error occurres.
	 */
	private ChannelMessage readChannelMessage(
			byte[] blockWithHeader, 
			User user) throws IOException {
		
		
		byte[] firstPartOfMessage = 
			Arrays.copyOfRange(blockWithHeader, 5, INTER_MIX_BLOCK_SIZE);
		
		int restOfMessageLength = 
			ChannelMessagePart.getMessageLength(
					NUMBER_OF_FURTHER_HOPS
					)
			- 
			(INTER_MIX_BLOCK_SIZE - 5);
			// "-(INTER_MIX_BLOCK_SIZE - 5)" because the first 
			// "(INTER_MIX_BLOCK_SIZE - 5)" bytes of the message are 
			// already read (they are located in 
			// "blockWithHeader[5-INTER_MIX_BLOCK_SIZE]")
			
		byte[] restOfMessage = new byte[restOfMessageLength];
		restOfMessage = Util.forceRead(previousMixInputStream, restOfMessage);
			
		byte[] completeMessage = 
			Util.mergeArrays(firstPartOfMessage, restOfMessage);
		
		return new ChannelMessage(	completeMessage, 
									user, 
									NUMBER_OF_FURTHER_HOPS
									);

	}
	
	
	/**
	 * Indicates whether the bypassed user is allowed to send a message or not.
	 * 
	 * @param user	Reference on <code>User</code> who's authorization shall be 
	 * 				checked.
	 * 
	 * @return		Whether the bypassed user is allowed to send a message or 
	 * 				not.
	 */
	private boolean isAllowedToSendChannelMessage(User user) {
		
		if (user == null) { // no suiting channel
			
			LOGGER.fine(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
							+" IOH) Received message for "
							+"unknown channelID!"
							);
			
			return false;
			
		} else if (user.getHasMessageInCurrentReplyBatch()) {
			// next Mix is trying to send multiple messages on 
			// one channel!
			
			LOGGER.severe(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
							+" IOH) Previous Mix is trying to "
							+"send multiple messages on one " +
							"channel!"
							);
			
			return false;
			
		} else {
			
			return true;
			
		}

	}
	
	
	/**
	 * Generates a cryptographic key, that is used to encrypt multiplex-headers 
	 * between this mix an its predecessor and sets up suiting 
	 * <code>Cipher</code>s.
	 */
	private void setUpInterMixCiphers() {
		
		KeyGeneratorController keyGenerator = new KeyGeneratorController();
		
		// generate key for inter-mix-communication (with previous mix)
		interMixKeyWithPreviousMix = getInterMixKey(keyGenerator);
		interMixIVWithPreviousMix = getInterMixIV(keyGenerator);
		
		externalInformationPort.setInterMixKeyWithPreviousMix(
				interMixKeyWithPreviousMix
				);
		
		externalInformationPort.setInterMixIVWithPreviousMix(
				interMixIVWithPreviousMix
				);
		
		try {
			
			this.interMixEncryptCipherWithPreviousMix = 
				Cipher.getInstance(
					getProperty("INTER_MIX_CRYPTOGRAPHY_ALGORITHM"),
					getProperty("CRYPTO_PROVIDER")
					);
			
			this.interMixEncryptCipherWithPreviousMix.init(	
				Cipher.ENCRYPT_MODE,
				interMixKeyWithPreviousMix,
				interMixIVWithPreviousMix
				);
			
			this.interMixDecryptCipherWithPreviousMix = Cipher.getInstance(
				getProperty("INTER_MIX_CRYPTOGRAPHY_ALGORITHM"),
				getProperty("CRYPTO_PROVIDER")
				);
			
			this.interMixDecryptCipherWithPreviousMix.init(	
				Cipher.DECRYPT_MODE,
				interMixKeyWithPreviousMix,
				interMixIVWithPreviousMix
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
	 * and its predecessor. Must be called when connection was lost, since  
	 * <code>Cipher</code>s might be out of sync in that case.
	 */
	private void resetInterMixCiphers() {
		
		try {
			
			this.interMixEncryptCipherWithPreviousMix = 
				Cipher.getInstance(
					getProperty("INTER_MIX_CRYPTOGRAPHY_ALGORITHM"),
					getProperty("CRYPTO_PROVIDER")
					);
					
			this.interMixEncryptCipherWithPreviousMix.init(	
				Cipher.ENCRYPT_MODE,
				interMixKeyWithPreviousMix,
				interMixIVWithPreviousMix
				);
				
			this.interMixDecryptCipherWithPreviousMix = Cipher.getInstance(
				getProperty("INTER_MIX_CRYPTOGRAPHY_ALGORITHM"),
				getProperty("CRYPTO_PROVIDER")
				);
					
			this.interMixDecryptCipherWithPreviousMix.init(	
				Cipher.DECRYPT_MODE,
				interMixKeyWithPreviousMix,
				interMixIVWithPreviousMix
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
	 * Generates an initialization vector, that can be used to encrypt 
	 * multiplex-headers between this mix an its predecessor.
	 * 
	 * @param keyGenerator				Reference on component 
	 * 									<code>KeyGenerator</code> (Used to 
	 * 									generate the initialization vector).
	 * 
	 * @return							An initialization vector, that can be 
	 * 									used to encrypt multiplex-headers 
	 * 									between this mix an its predecessor.
	 */
	private IvParameterSpec getInterMixIV(KeyGeneratorController keyGenerator) {

		IvParameterSpec iv = null;
		
		int positionOfMixInCascade = 
			new Integer(internalInformationPort.
					getProperty("POSITION_OF_MIX_IN_CASCADE")
				);
		
		
		if (positionOfMixInCascade != 1) {
			
			iv = (IvParameterSpec)
				 keyGenerator.generateKey(KeyGeneratorController.INTER_MIX_IV);
			
		}
		
		return iv;
		
	}


	/**
	 * Generates a cryptographic key, that can be used to encrypt 
	 * multiplex-headers between this mix an its predecessor.
	 * 
	 * @param keyGenerator				Reference on component 
	 * 									<code>KeyGenerator</code> (Used to 
	 * 									generate the cryptographic key).
	 * 
	 * @return							A cryptographic key, that can be 
	 * 									used to encrypt multiplex-headers 
	 * 									between this mix an its predecessor.
	 */
	private SecretKey getInterMixKey(KeyGeneratorController keyGenerator) {
		
		SecretKey key = null;
		
		int positionOfMixInCascade = 
			new Integer(internalInformationPort.
					getProperty("POSITION_OF_MIX_IN_CASCADE")
				);
		
		
		if (positionOfMixInCascade != 1) {
			
			key =	(SecretKey)
					keyGenerator.generateKey(
							KeyGeneratorController.INTER_MIX_KEY
							);
			
		}
			
		return key;
		
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
