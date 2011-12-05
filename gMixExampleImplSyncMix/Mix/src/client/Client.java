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

package client;


import internalInformationPort.InternalInformationPortController;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.*;
import java.util.logging.Logger;

import exception.InformationRetrieveException;
import exception.MessageTooLongException;

import externalInformationPort.ExternalInformationPortController;
import externalInformationPort.Information;

import architectureInterface.ClientInterface;

import message.ChannelEstablishMessagePart;
import message.ChannelMessagePart;
import message.Payload;

import util.Util;


/**
 * The Client used to anonymize data (e. g. from a user's application) via a 
 * cascade of mixes. Follows the "everything is a stream" concept. See 
 * <code>architectureInterfaces.ClientInterface</code>. From a user 
 * perspective, this <code>Client</code> behaves quite equal to a socket 
 * (<code>java.net.Socket</code>) and therefore abstracts from the underlying 
 * technique.
 * 
 * @author Karl-Peter Fuchs
 */
public final class Client implements ClientInterface {
	
	/** 
	 * Reference on component <code>InternalInformationPort</code>. 
	 * Used to display and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = internalInformationPort.getLogger();
	
	/** 
	 * Address of the cascade (cascade's first mix) used to transmit messages. 
	 */
	private final InetAddress CASCADE_ADDRESS; 
	
	/** 
	 * Port number of the cascade (cascade's first mix) used to transmit 
	 * messages.
	 */
	private final int CASCADE_PORT;

	/** 
	 * Period of time, a client waits for the cascade to accept his connection 
	 * request in ms.
	 */
	private final int CASCADE_TIMEOUT;
	
	/** 
	 * Maximum amount of data (payload), that can be stored in a <code>
	 * ChannelEstablishMessage</code>.
	 */
	private final int MAX_CHANNEL_ESTABLISH_MESSAGE_LENGTH;
	
	/** 
	 * Maximum amount of data (payload), that can be stored in a <code>
	 * ChannelMessage</code>.
	 */
	private final int MAX_CHANNEL_MESSAGE_LENGTH;
	
	/**
	 * Period of time to wait between the receival of a <code>Reply</code> 
	 * (from the cascade) and the sending of a new <code>Request</code> for 
	 * the user (application) to react (= add new data to be sent). Note: If no 
	 * new data or a disconnect request has arrived during this period of time, 
	 * a dummy message is sent.
	 */
	private final long REACTION_TIME;
	
	/** Socket for mix communication. */
	private Socket mix;
	
	/** ClientOutputStream for client communication. */
	private ClientOutputStream clientOutputStream;
	
	/** ClientInputStream for client communication. */
	private ClientInputStream clientInputStream;
	
	/** MixOutputStream for mix communication. */
	private OutputStream mixOutputStream;
	
	/** MixInputStream for mix communication. */
	private InputStream mixInputStream;
	
	/** Buffer to store incoming data from the cascade. */
	private ByteBuffer receiveBuffer;
	
	/** 
	 * Object used to synchronize the <code>receiveBuffer</code>.
	 * <p>
	 * Note: This requires a special object, since the used 
	 * <code>ByteBuffer</code>-object may change during runtime (In Java, 
	 * <code>ByteBuffer</code>s can't be resized. Nevertheless, the buffer's 
	 * size must be adjusted in a few cases (when unexpected large 
	 * write-requests occur)). Therefore, a permanently available object must 
	 * be present.
	 * 
	 * @see #receiveBuffer
	 */
	private Object receiveBufferSynchronizer = new Object();
	
	/** Buffer to store data that shall be sent to the cascade. */
	private ByteBuffer sendBuffer;
	
	/** 
	 * Initial size of the <code>receiveBuffer</code>.
	 * 
	 * @see #receiveBuffer
	 */
	private final int RECEIVE_BUFFER_SIZE;
	
	/** 
	 * Initial size of the <code>sendBuffer</code>.
	 * 
	 * @see #sendBuffer
	 */
	private final int SEND_BUFFER_SIZE;
	
	/** Indicates whether a channel to the cascade is established or not. */
	private boolean isChannelEstablished = false;
	
	/** 
	 * Indicates whether the method <code>receiveDataFromCascade()</code> is 
	 * waiting for new data from the cascade (used for synchronization).
	 * 
	 * @see #receiveDataFromCascade(int)
	 */
	private boolean isReadMethodWaiting = false;
	
	/** 
	 * Indicates whether the method <code>receiveDataFromCascade()</code> shall 
	 * return all data received so far, no matter how much data it is waiting 
	 * for (since no further data can be expected from the cascade).
	 * 
	 * @see #receiveDataFromCascade(int)
	 */
	private boolean isReadMethodReturnForced = false;
	
	/** 
	 * Indicates whether the method <code>putInSendBuffer()</code> is 
	 * waiting for the <code>CascadeInputOutputHandler</code> to send the 
	 * data it gave to it (used for synchronization).
	 * 
	 * @see #putInSendBuffer(byte[])
	 */
	private boolean isWriteMethodWaiting = false;
	
	/** 
	 * Indicates whether the user (application) called <code>disconnect()
	 * </code> and therefore wants to release his/its connection to the 
	 * cascade.
	 * 
	 * @see #disconnect()
	 */
	private boolean isDisconnectRequested = false;
	
	/** 
	 * Indicates whether the user (application) has already put data in the 
	 * <code>OutputStream</code> (used to start the ). 
	 * 
	 * called <code>disconnect()
	 * </code> and therefore wants to release his/its connection to the 
	 * <code>CascadeInputOutputHandler</code> at the right time.
	 * 
	 * @see #disconnect()
	 */
	private boolean isFirstWriteDone = false;
	
	/**
	 * Used to perform cryptographic operations (decrypting, encrypting, 
	 * generating session keys, MACs... ).
	 */
	private Cryptography cryptography;
	
	/**
	 * Used to generate message the mixes are capable of processing.
	 */
	private MessageGenerator messageGenerator;
	
	
	/**
	 * Creates a new <code>Client</code> which uses the bypassed keys to 
	 * encrypt messages.
	 * 
	 * @param publicKeysOfMixes			Public keys of the cascade's mixes 
	 * 									(used to encrypt messages).
	 */
	public Client(Key[] publicKeysOfMixes) {
		/* 
		 * Reads and saves values from property file and instantiates Cipher 
		 * and KeyGenerator objects for later use.
		 */

		InetAddress cascadeAddress = null;
		
		try {
			
			cascadeAddress = 
				InetAddress.getByName(getProperty("CASCADE_ADDRESS"));
			
		} catch (UnknownHostException e) {
			
			LOGGER.severe(	"Invalid \"CASCADE_ADDRESS\" in property file! "
							+e.getMessage()
							);
			
			System.exit(1);
			
		}
		
		this.CASCADE_ADDRESS = cascadeAddress;
		
		this.RECEIVE_BUFFER_SIZE = 
			new Integer(getProperty("RECEIVE_BUFFER_SIZE"));
		
		this.SEND_BUFFER_SIZE = new Integer(getProperty("SEND_BUFFER_SIZE"));
		
		this.REACTION_TIME = 
			new Long(getProperty("REACTION_TIME"));
		
		this.MAX_CHANNEL_ESTABLISH_MESSAGE_LENGTH = 
			ChannelEstablishMessagePart.PAYLOAD.getBasicLength()
			- 
			Payload.getHeaderLength();
		
		this.MAX_CHANNEL_MESSAGE_LENGTH = 
			ChannelMessagePart.PAYLOAD.getBasicLength()
			- 
			Payload.getHeaderLength();
		
		this.CASCADE_PORT = new Integer(getProperty("CASCADE_PORT"));
		this.CASCADE_TIMEOUT = new Integer(getProperty("CASCADE_TIMEOUT"));
		
		this.cryptography = new Cryptography(publicKeysOfMixes);
		
		this.messageGenerator = 
			new MessageGenerator(publicKeysOfMixes.length, cryptography);
		
		this.sendBuffer = ByteBuffer.allocateDirect(SEND_BUFFER_SIZE);
		this.receiveBuffer = ByteBuffer.allocateDirect(RECEIVE_BUFFER_SIZE);
		this.clientOutputStream = new ClientOutputStream(this);
		this.clientInputStream = new ClientInputStream(this);
		
	}
	
	
	/**
	 * Creates a new <code>Client</code> which receives the keys (to encrypt 
	 * messages) from the cascade's mixes automatically.
	 */
	public Client() {
		
		this(getPublicKeys());

	}
	
	
	/**
	 * Retrieves the public keys of all mixes in the cascade (address of first 
	 * mix must be specified in property file) for later encryption.
	 * 
	 * @return	The public keys of all mixes in the cascade.
	 */
	private static PublicKey[] getPublicKeys() {
		
		PublicKey[] publicKeys = null;
		
		try {
			
			ExternalInformationPortController externalInformationPort = 
				new ExternalInformationPortController();
			
			externalInformationPort.initialize();
			
			InetAddress informationProviderAddress = 
				InetAddress.getByName(
						internalInformationPort.getProperty("CASCADE_ADDRESS")
						);
			
			int informationProviderPort = 
				new Integer(
						internalInformationPort.getProperty("CASCADE_INFO_PORT")
						);
			
			
			publicKeys = 
				(PublicKey[]) externalInformationPort.getInformationFromAll(
									informationProviderAddress,
									informationProviderPort,
									Information.PUBLIC_KEY
									);
			
		} catch (InformationRetrieveException e) {
			
			LOGGER.severe(e.getMessage());
			System.exit(1);
			
		} catch (UnknownHostException e) {
			
			LOGGER.severe(e.getMessage());
			System.exit(1);
			
		}
		
		return publicKeys;
		
	}
	
	
	/**
	 * Returns an <code>InputStream</code> that can be used to receive data
	 * anonymously. 
	 * 
	 * @return 	<code>InputStream</code> that can be used to receive data
	 * 			anonymously. 
	 * 
	 * @throws IOException I/O problem occurred.
	 */
	public ClientInputStream getInputStream() {
		
		return this.clientInputStream;
		
	}
	
	
	/**
	 * Returns an <code>OutputStream</code> that can be used to send data
	 * anonymously. 
	 * 
	 * @return 	<code>OutputStream</code> that can be used to send data
	 * 			anonymously. 
	 * 
	 * @throws IOException I/O problem occurred.
	 */
	public ClientOutputStream getOutputStream() {
		
		return this.clientOutputStream;
		
	}
	
	
	/**
	 * Connects client to the mix cascade, specified in property file.
	 * 
	 * @throws IOException I/O problem occurred.
	 */
	public void connect() throws IOException {
		
		isDisconnectRequested = false;
		
		mix = new Socket();
		
		SocketAddress socketAddress = 
			new InetSocketAddress(CASCADE_ADDRESS, CASCADE_PORT);

		mix.connect(socketAddress, CASCADE_TIMEOUT);
		mixOutputStream = mix.getOutputStream();
		mixInputStream = mix.getInputStream();
	
	}


	/**
	 * Disconnects client from mix cascade.
	 * 
	 * @throws IOException I/O problem occurred.
	 */
	public void disconnect() throws IOException {
		
		if (isDisconnectRequested) {
			
			throw new IOException("Connection already disconnected!");
			
		} else {
			
			isDisconnectRequested = true;
			
		}

	}

	
	/**
	 * Adds the bypassed data to the <code>sendBuffer</code> (from where it 
	 * gets transmitted to the mix cascade by <code>CascadeInputOutputHandler
	 * </code>). Blocks until all data is written (if data doesn't fit in 
	 * buffer).
	 * 
	 * @param data	Data to be put in <code>sendBuffer</code>.
	 */
	protected void putInSendBuffer(byte[] data) {
		
		if (data.length > 0) {
			
			synchronized (sendBuffer) {
				
				if (sendBuffer.remaining() < data.length) { // not enough space

					// use "data"-array as ByteBuffer
					ByteBuffer tempBuffer = ByteBuffer.wrap(data);
					tempBuffer.position(data.length);
					tempBuffer.flip();
					
					while (true) {
						
						int freeSpace = sendBuffer.remaining();
						int neededSpace = tempBuffer.remaining();
						
						if (freeSpace < neededSpace) {	// not enough space 
														// -> write fragment
							
							byte[] fragment = new byte[freeSpace];
							tempBuffer.get(fragment);
							sendBuffer.put(fragment);
							
							if (!isFirstWriteDone) {
								
								isFirstWriteDone = true;
								new CascadeInputOutputHandler();
								
							} 
							
							// wait for free space
							isWriteMethodWaiting = true;
							
							while(sendBuffer.remaining() == 0) {
								
								try {
									
									sendBuffer.wait();
									
								} catch (InterruptedException e) {

									LOGGER.fine(e.getMessage());
									continue;
									
								}
								
							}
							
							isWriteMethodWaiting = false;
							
						} else { // enough space -> last fragment
							
							byte[] lastFragment = new byte[neededSpace];
							tempBuffer.get(lastFragment);
							sendBuffer.put(lastFragment);
							break;
							
						}
						
					}
					
				} else { // enough space
					
					sendBuffer.put(data);
					
					if (!isFirstWriteDone) {
						
						isFirstWriteDone = true;
						new CascadeInputOutputHandler();
						
					} 
					
				}
				
			}
	
		}

	}
	
	
	/**
	 * Sends the bypassed data to the mix cascade. Called by <code>
	 * CascadeInputOutputHandler</code>.
	 * 
	 * @param dataToSend Data to be sent.
	 */
	private void sendMessage(byte[] dataToSend) {

		try {
			
			mixOutputStream.write(dataToSend);
			mixOutputStream.flush();
			
		} catch (IOException e) {

			LOGGER.severe(e.getMessage());
			System.exit(1);
	
		}
		
	}
	
	
	/**
	 * Tries to read <code>amoutOfDataNeeded</code> bytes from the mix cascade. 
	 * Blocks until <code>amoutOfDataNeeded</code> bytes are available, or 
	 * no further data can be expected from the corresponding communication 
	 * partner.
	 * 
	 * @param amoutOfDataNeeded	Number of bytes that shall be read.
	 * 
	 * @return					Read data.
	 */
	protected byte[] receiveDataFromCascade(int amoutOfDataNeeded) {
		
		byte[] result;
		int amountOfDataAvailable;
		
		if (amoutOfDataNeeded > 0) {
			
			synchronized (receiveBufferSynchronizer) {
				
				amountOfDataAvailable = receiveBuffer.position();
				
				if (amountOfDataAvailable >= amoutOfDataNeeded) { 
					// enough data in buffer

					// remove and return the requested data
					result = new byte[amoutOfDataNeeded];
					receiveBuffer.flip();
					receiveBuffer.get(result);
					receiveBuffer.compact();
					return result;
					
				} else { // not enough data in buffer
					
					// wait until enough data is available
					isReadMethodWaiting = true;
					
					while(receiveBuffer.position() < amoutOfDataNeeded) {
						
						try {
							
							receiveBufferSynchronizer.wait();
							
							if (isReadMethodReturnForced) {
								
								break;
								
							}
							
						} catch (InterruptedException e) {
							
							LOGGER.fine(e.getMessage());
							continue;
							
						}
						
					}
					
					isReadMethodWaiting = false;
					
					if (isReadMethodReturnForced) {
						// not enough data available, but return is forced 
						// (since no further data can be expected)
						
						isReadMethodReturnForced = false;
						result = new byte[receiveBuffer.position()];
						
						
						
					} else {	// enough data is available
								// -> remove and return the requested data
						
						result = new byte[amoutOfDataNeeded];
						
					}
					
					receiveBuffer.flip();
					receiveBuffer.get(result);
					receiveBuffer.compact();
					
					return result;
					
				}
				
			}
			
		} else {
			
			return new byte[0];
			
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
	 * Internal class used to communicate with the mix cascade. Makes sure 
	 * messages are sent synchronously (request and reply alternately). 
	 * Otherwise the cascade would drop this client. Synchronized with <code>
	 * Client</code> via <code>sendBuffer</code> and <code>receiveBuffer
	 * </code>.
	 * 
	 * @author Karl-Peter Fuchs
	 *
	 */
	private final class CascadeInputOutputHandler extends Thread {
		
		
		/**
		 * Indicates if the last message, that was received from the cascade, 
		 * was a dummy. Used as indicator when "no further data can be expected 
		 * from the corresponding communication partner".
		 * 
		 * @see #receiveReply()
		 */
		private boolean lastMessageWasDummy = false;
		
		
		/**
		 * Constructs a new <code>CascadeInputOutputHandler</code> and calls 
		 * <code>start()</code>.
		 */
		private CascadeInputOutputHandler() {
			
			start();
			
		}
		
		
		/**
		 * Communicates with the mix cascade. Makes sure messages are sent 
		 * synchronously (request and reply alternately). Otherwise the cascade 
		 * would drop this client. Synchronized with <code>Client</code> via 
		 * <code>sendBuffer</code> and <code>receiveBuffer</code>.
		 */
		@Override
		public void run() {
			
			while (true) {
				
				sendMixMessage();
				receiveReply();
				
				// give user(-application) time to answer his communication 
				// partner's reply (just received)
				try {
					
					sleep(REACTION_TIME);
					
				} catch (InterruptedException e) {

					LOGGER.warning(e.getMessage());
					continue;
					
				}
				
				if (isDisconnectRequested) {
					
					closeConnection();
					break;
					
				}

			}
			
		}

		
		/**
		 * Sends a message to the mix cascade. The message can be of one of the 
		 * following types (depending on connection status (disconnected or 
		 * connected) and the amount of data available in <code>sendBuffer
		 * </code>): <code>ChannelEstablishMessage</code>, <code>ChannelMessage
		 * </code>, <code>ChannelReleaseMessage</code> or <code>DummyMessage
		 * </code> (= <code>ChannelMessage</code> without payload (=only 
		 * padding)). Blocks until message is sent.
		 */
		private void sendMixMessage() {
			
			synchronized (sendBuffer) {
				
				int availableSpaceInMixPacket;
				int dataInBuffer;
				byte[] dataToSend;
				
				if (sendBuffer.position() != 0) { // data available to send
					
					if (!isChannelEstablished) { 
						// channel not yet established -> establish channel
						
						availableSpaceInMixPacket = 
							MAX_CHANNEL_ESTABLISH_MESSAGE_LENGTH; 
						
						dataInBuffer = sendBuffer.position();
						
						if (dataInBuffer < availableSpaceInMixPacket) { 
							// packet won't be filled completely
						
							dataToSend = new byte[dataInBuffer];
							
						} else { // packet will be filled completely
							
							dataToSend = new byte[availableSpaceInMixPacket];
							
						}
						
						sendBuffer.flip();
						sendBuffer.get(dataToSend);
						
						try {
							
							dataToSend = 
								messageGenerator.
									generateChannelEstablishMessage(dataToSend);
							
							sendMessage(dataToSend);// blocks until data is sent
							
						} catch (MessageTooLongException e) {
							
							LOGGER.severe(e.getMessage());
							System.exit(1);
							
						}
						
						isChannelEstablished = true;
						sendBuffer.compact();
						lastMessageWasDummy = false;
						
					} else {	// channel already established -> send data on 
								// existing channel
						
						availableSpaceInMixPacket = 
							MAX_CHANNEL_MESSAGE_LENGTH;
						
						dataInBuffer = sendBuffer.position();
						
						if (dataInBuffer < availableSpaceInMixPacket) { 
							// packet won't be filled completely
						
							dataToSend = new byte[dataInBuffer];
							
						} else { // packet will be filled completely
							
							dataToSend = new byte[availableSpaceInMixPacket];
							
						}
						
						sendBuffer.flip();
						sendBuffer.get(dataToSend);
						
						try {
							
							dataToSend = 
								messageGenerator.
									generateChannelMessage(dataToSend);
							
							sendMessage(dataToSend);// blocks until data is sent
							
						} catch (MessageTooLongException e) {
							
							LOGGER.severe(e.getMessage());
							System.exit(1);
							
						}
						
						sendBuffer.compact();
						lastMessageWasDummy = false;
						
					}
					
					if (isWriteMethodWaiting) {
						
						sendBuffer.notify();
						
					}
					
				} else { // no data available to send -> send dummy message
					
					try {
						
						dataToSend = 
							messageGenerator.
								generateChannelMessage(new byte[0]);
						
						sendMessage(dataToSend); // blocks until data is sent
						
					} catch (MessageTooLongException e) {
						
						LOGGER.severe(e.getMessage());
						System.exit(1);
						
					}
							
					lastMessageWasDummy = true;

				}
				
			}
			
		}
		

		/**
		 * Receives a message (<code>Reply</code>) from the mix cascade and 
		 * saves its payload in the <code>receiveBuffer</code>, from where it's 
		 * available for the user's <code>InputStream</code>. Blocks until 
		 * message is received and written to buffer.
		 */
		private void receiveReply() {

			byte[] lengthOfReplyAsArray = new byte[4]; // length header
			
			int lengthOfReply;	// indicates how long the cascades's  
								// (dynamic) reply will be
			
			byte[] encryptedReply;
			byte[] decryptedReply;
			byte[] decryptedReplyWithoutPadding;
			Payload payload = null;
			
			try {	// Read Reply, decrypt Reply and receive payload.
				
				lengthOfReplyAsArray = 
					Util.forceRead(mixInputStream, lengthOfReplyAsArray);
				
				lengthOfReply = Util.byteArrayToInt(lengthOfReplyAsArray);
				encryptedReply = new byte[lengthOfReply];
				encryptedReply = Util.forceRead(mixInputStream, encryptedReply);
				decryptedReply = cryptography.decryptReply(encryptedReply);
				payload = new Payload(decryptedReply);
				
			} catch (IOException e) {
				
				LOGGER.severe(e.getMessage());
				System.exit(1);
				
			}
			
			decryptedReplyWithoutPadding = payload.getMessage();
			
			// save received data to receiveBuffer
			synchronized (receiveBufferSynchronizer) {
				
				if (decryptedReplyWithoutPadding.length != 0) {
					
					// enlarge buffer if necessary
					if (	decryptedReplyWithoutPadding.length 
							> 
							receiveBuffer.remaining()
							) { // enlarge buffer
						
						byte[] dataFromOldBuffer = 
							new byte[receiveBuffer.position()];
						
						receiveBuffer.flip();
						receiveBuffer.get(dataFromOldBuffer);
						
						int newLength = 
							dataFromOldBuffer.length 
							+ 
							decryptedReplyWithoutPadding.length;
						
						receiveBuffer = 
							ByteBuffer.allocateDirect(newLength);
						
						receiveBuffer.put(dataFromOldBuffer);
							
					}
					
					receiveBuffer.put(decryptedReplyWithoutPadding);
					
				} else if (lastMessageWasDummy && isReadMethodWaiting) {
					// no further data can be expected
					// -> force read()-method to return
					
					isReadMethodReturnForced = true;
					
				}
				
				if (isReadMethodWaiting) {
					
					receiveBufferSynchronizer.notify();
					
				}
				
			}
			
		}

		
		/**
		 * Releases connection to mix cascade.
		 */
		private void closeConnection() {

			try {
				
				clientOutputStream.close();
				clientInputStream.close();
				mix.close();
				
			} catch (IOException e) {
				
				LOGGER.fine(e.getMessage());
				
			}
			
		}
	
	}

}