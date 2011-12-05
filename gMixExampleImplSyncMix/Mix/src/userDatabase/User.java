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

package userDatabase;


import internalInformationPort.InternalInformationPortController;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import message.Payload;

import networkClock.NetworkClockController;


/**
 * Data structure used to store user-specific data (for example identifiers, 
 * session keys or buffers).
 * 
 * @author Karl-Peter Fuchs
 *
 */
public final class User {
	
	/** 
	 * Reference on component <code>InternalInformationPort</code>. 
	 * Used to display and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = internalInformationPort.getLogger();
	
	/** 
	 * Maximum size of a <code>Request</code> in byte (used to initialize 
	 * buffers).
	 */
	private static int MAX_REQUEST_LENGTH = 
		new Integer(
			internalInformationPort.getProperty(
				"MAX_REQUEST_LENGTH")
		);
	
	/** 
	 * Maximum size of a <code>Reply</code> in byte (used to initialize 
	 * buffers).
	 */
	private static int MAX_REPLY_LENGTH = 
		new Integer(
			internalInformationPort.getProperty(
				"MAX_REPLY_LENGTH")
		);
	
	/** 
	 * Random number generator used to generate 
	 * <code>IDENTIFIER_FOR_NEXT_MIX</code>.
	 * 
	 * @see #IDENTIFIER_FOR_NEXT_MIX
	 */
	private static SecureRandom secureRandom = new SecureRandom();
	
	/** Reference on component <code>NetworkClock</code>. */
	private static NetworkClockController clock = new NetworkClockController();
	
	/** A (local) identifier for this <code>User</code>. */
	private final Integer IDENTIFIER;
	
	/** 
	 * An identifier for this <code>User</code>, used by the next mix of the 
	 * cascade. Note: Different identifiers must be used to prevent 
	 * linkability.
	 */
	private final Integer IDENTIFIER_FOR_NEXT_MIX;
	
	/** Timestamp of this <code>User</code>'s last activity. */
	private long lastActivity;
	
	/** 
	 * Indicates whether a channel (used to transmit 
	 * <code>ChannelMessage</code>s) has been establish for/by this 
	 * <code>User</code> or not.
	 */
	private boolean isChannelEstablished = false;
	
	/** 
	 * <code>ByteBuffer</code> used to cache fragments of a message received 
	 * from the <code>User</code>'s <code>Client</code>. Needed for 
	 * non-blocking I/O.
	 * 
	 * @see inputOutputHandler.ClientConnectionHandler
	 */
	private ByteBuffer clientReadBuffer;
	
	/** 
	 * <code>ByteBuffer</code> used to cache messages that shall be sent to 
	 * this <code>User</code>'s <code>Client</code>.Needed for non-blocking I/O.
	 * 
	 * @see inputOutputHandler.ClientConnectionHandler
	 */
	private ByteBuffer clientWriteBuffer;
	
	/** 
	 * <code>ByteBuffer</code> used to cache data received from proxy server. 
	 * Needed for non-blocking I/O.
	 */
	private ByteBuffer proxyReadBuffer;
	
	/** 
	 * <code>ByteBuffer</code> used to cache messages that shall be sent to 
	 * this <code>User</code>'s <code>Client</code>. Needed for non-blocking 
	 * I/O.
	 * 
	 * @see inputOutputHandler.ClientConnectionHandler
	 */
	private ByteBuffer proxyWriteBuffer;
	
	/** <code>SecretKey</code> used for message authentication codes. */
	private SecretKey macKey;
	
	/** 
	 * Reference on <code>User</code>'s <code>SocketChannel</code>. Needed for 
	 * non-blocking I/O.
	 */
	private SocketChannel socketChannel;
	
	/** 
	 * Indicates whether a <code>Request</code> of this <code>User</code> is 
	 * already in batch or not (used to prevent flooding attacks).
	 */
	private boolean hasMessageInCurrentBatch = false;
	
	/** 
	 * Indicates whether a <code>Reply</code> for this <code>User</code> is 
	 * already in batch or not (used to prevent flooding attacks).
	 */
	private boolean hasMessageInCurrentReplyBatch = false;
	
	/**
	 * <code>Cipher</code> used to encrypt this <code>User</code>'s 
	 * <code>Reply</code>ies.
	 */
	private Cipher encryptCipher;
	
	/**
	 * <code>Cipher</code> used to decrypt this <code>User</code>'s 
	 * <code>Request</code>s (<code>ChannelMessage</code>s and symmetric part 
	 * of <code>ChannelEstablishMessage</code>s).
	 */
	private Cipher decryptCipher;

	/**
	 * Indicates whether <code>putInProxyReadBuffer(byte[])</code> is waiting 
	 * for free space in <code>proxyReadBuffer</code> or not (used for 
	 * synchronization).
	 */
	private boolean isPutInProxyReadBufferWaiting = false;
	

	/**
	 * Creates a new <code>User</code> using the bypassed identifier.
	 * 
	 * @param identifier 	A (local) identifier for this <code>User</code> 
	 * 						used to distinguish between <code>User</code>s.
	 *
	 */
	public User(int identifier) {
		
		this.IDENTIFIER = identifier;
		this.IDENTIFIER_FOR_NEXT_MIX = Math.abs(secureRandom.nextInt());
		this.lastActivity = clock.getTime();
	
	}
	
	
	/**
	 * Returns this <code>User</code>s (local) identifier (used to distinguish 
	 * between <code>User</code>s).
	 * 
	 * @return	This <code>User</code>s (local) identifier.
	 */
	public Integer getIdentifier() {
		
		setTimestampOfLastActivity();
		return this.IDENTIFIER;
		
	}
	
	
	/**
	 * Returns this <code>User</code>'s identifier, used by the next mix of the 
	 * cascade (used to distinguish between <code>User</code>s). Note: 
	 * Different identifiers must be used to prevent linkability.
	 * 
	 * @return	This <code>User</code>s identifier, used by the next mix of the 
	 * 			cascade
	 */
	public Integer getIdentifierForNextMix() {
		
		return this.IDENTIFIER_FOR_NEXT_MIX;
		
	}
	
	
	/**
	 * Returns whether a channel (used to transmit 
	 * <code>ChannelMessage</code>s) has been establish for/by this 
	 * <code>User</code> or not.
	 * 
	 * @return 	Whether a channel (used to transmit 
	 * 			<code>ChannelMessage</code>s) has been establish for/by this 
	 * 			<code>User</code> or not.
	 */
	public boolean getIsChannelEstablished() {
		
		return this.isChannelEstablished;
		
	}
	
	
	/**
	 * Sets variable <code>isChannelEstablished</code> to the bypassed value 
	 * (indicates whether a channel (used to transmit 
	 * <code>ChannelMessage</code>s) has been establish for/by this 
	 * <code>User</code> or not).
	 * 
	 * @param newValue	Value <code>isChannelEstablished</code> variable shall 
	 * 					be set to.
	 */
	public void setIsChannelEstablished(boolean newValue) {
		
		this.isChannelEstablished = newValue;
		
	}
	
	
	/**
	 * Indicates whether this <code>User</code>'s last activity took place 
	 * during the last <code>timeout</code> ms or not (used to detect outdated 
	 * <code>User</code>s).
	 * 
	 * @param timeout	Period of time to be taken in account.
	 * 
	 * @return			Whether this <code>User</code>'s last activity took 
	 * 					place during the last <code>timeout</code> ms or not.
	 */
	public boolean isStillValid(long timeout) {
		
		return ((clock.getTime() - lastActivity) >= timeout) ? false : true;
		
	}
	
	
	/**
	 * Returns a timestamp of this <code>User</code>'s last activity.
	 * 
	 * @return	Timestamp for this <code>User</code>'s last activity.
	 */
	public long getTimestampOfLastActivity() {
		
		return this.lastActivity;
		
	}
	
	
	/**
	 * Must be called when <code>User</code> sends a <code>Message</code> 
	 * (Used to detect inactive <code>User</code>s).
	 */
	public void setTimestampOfLastActivity() {
		
		this.lastActivity = clock.getTime();
		
	}

	
	/**
	 * Returns this <code>User</code>'s <code>encryptCipher</code>, used to 
	 * encrypt <code>Reply</code>ies.
	 * 
	 * @return	This <code>User</code>'s <code>encryptCipher</code>, used to 
	 * 			encrypt <code>Reply</code>ies.
	 */
	public Cipher getEncryptCipher() {
		
		return this.encryptCipher;
		
	}
	
	
	/**
	 * Makes this <code>User</code> use the bypassed <code>Cipher</code> for 
	 * encrypting <code>Reply</code>ies.
	 * 
	 * @param encryptCipher	<code>Cipher</code>, that shall be used for  
	 * 						encrypting <code>Reply</code>ies.
	 */
	public void setEncryptCipher(Cipher encryptCipher) {
		
		this.encryptCipher = encryptCipher;
		
	}
	
	
	/**
	 * Returns this <code>User</code>'s <code>decryptCipher</code>, used to 
	 * decrypt <code>Request</code>s.
	 * 
	 * @return	This <code>User</code>'s <code>decryptCipher</code>, used to 
	 * 			decrypt <code>Request</code>s.
	 */
	public Cipher getDecryptCipher() {
		
		return this.decryptCipher;
		
	}
	
	
	/**
	 * Makes this <code>User</code> use the bypassed <code>Cipher</code> for 
	 * decrypting <code>Request</code>s
	 * 
	 * @param decryptCipher	<code>Cipher</code>, that shall be used for  
	 * 						decrypting <code>Request</code>s
	 */
	public void setDecryptCipher(Cipher decryptCipher) {
		
		this.decryptCipher = decryptCipher;
		
	}
	
	
	/**
	 * Returns the <code>SecretKey</code>, used for validating message 
	 * authentication codes by this <code>User</code>.
	 * 
	 * @return	<code>SecretKey</code>, used for validating message 
	 * 			authentication codes by this <code>User</code>.
	 */
	public SecretKey getMacKey() {
		
		return this.macKey;
		
	}
	
	
	/**
	 * Makes this <code>User</code> use the bypassed <code>SecretKey</code> for 
	 * validating message authentication codes.
	 * 
	 * @param newMacKey	<code>SecretKey</code>, that shall be used for 
	 * 					validating message authentication codes.
	 */
	public void setMacKey(SecretKey newMacKey) {
		
		this.macKey = newMacKey;
		
	}
	

	/**
	 * Returns whether a <code>Request</code> of this <code>User</code> is 
	 * already in batch or not (used to prevent flooding attacks).
	 * 
	 * @return	Whether a <code>Request</code> of this <code>User</code> is 
	 * 			already in batch or not (used to prevent flooding attacks).
	 */
	public boolean getHasMessageInCurrentBatch() {
		
		return this.hasMessageInCurrentBatch;
		
	}
	
	
	/**
	 * Used to indicate, whether a <code>Request</code> of this 
	 * <code>User</code> is already in batch or not (used to prevent flooding 
	 * attacks).
	 * 
	 * @param newValue	Whether a <code>Request</code> of this 
	 * 					<code>User</code> is already in batch or not.
	 */
	public void setHasMessageInCurrentBatch(boolean newValue) {
		
		this.hasMessageInCurrentBatch = newValue;
		
	}
	
	
	/**
	 * Returns whether a <code>Reply</code> of this <code>User</code> is 
	 * already in batch or not (used to prevent flooding attacks).
	 * 
	 * @return	Whether a <code>Reply</code> of this <code>User</code> is 
	 * 			already in batch or not (used to prevent flooding attacks).
	 */
	public boolean getHasMessageInCurrentReplyBatch() {
		
		return this.hasMessageInCurrentReplyBatch;
		
	}
	
	
	/**
	 * Used to indicate, whether a <code>Reply</code> for this 
	 * <code>User</code> is already in batch or not (used to prevent flooding 
	 * attacks).
	 * 
	 * @param newValue	Whether a <code>Reply</code> for this 
	 * 					<code>User</code> is already in batch or not.
	 */
	public void setHasMessageInCurrentReplyBatch(boolean newValue) {
		
		this.hasMessageInCurrentReplyBatch = newValue;
		
	}
	
	
	/**
	 * Returns this <code>User</code>'s <code>ByteBuffer</code> used for 
	 * caching fragments of a message received from the corresponding 
	 * <code>Client</code>. Needed for non-blocking I/O.
	 * 
	 * @return	This <code>User</code>'s <code>ByteBuffer</code> used for 
	 * 			caching fragments of a message received from the corresponding 
	 * 			<code>Client</code>.
	 */
	public ByteBuffer getClientReadBuffer() {
		
		return this.clientReadBuffer;
		
	}
	
	
	/**
	 * Returns this <code>User</code>'s <code>ByteBuffer</code> used for 
	 * caching messages that shall be sent to the corresponding 
	 * <code>Client</code>. Needed for non-blocking I/O.
	 * 
	 * @return	This <code>User</code>'s <code>ByteBuffer</code> used for 
	 * 			caching messages that shall be sent to the corresponding 
	 * 			<code>Client</code>.
	 */
	public ByteBuffer getClientWriteBuffer() {
		
		return this.clientWriteBuffer;
		
	}
	
	
	/** 
	 * Returns this <code>User</code>'s <code>SocketChannel</code>. Needed for 
	 * non-blocking I/O.
	 * 
	 * @return This <code>User</code>'s <code>SocketChannel</code>.
	 */
	public SocketChannel getSocketChannel() {
		
		return this.socketChannel;
		
	}

	
	/**
	 * Used to add data received from proxy server for this <code>User</code>. 
	 * Puts the bypassed data in this <code>User</code>'s 
	 * <code>proxyReadBuffer</code>, from where it will be taken to generate 
	 * <code>Reply</code>ies.
	 * <p>
	 * Blocks until all data is written.
	 * 
	 * @param data Data for this <code>User</code>, received from proxy server.
	 */
	public void putInProxyReadBuffer(byte[] data) {

		synchronized (proxyReadBuffer) {

			if (proxyReadBuffer.remaining() < data.length) { // not enough space

				// use "data"-array as ByteBuffer
				ByteBuffer tempBuffer = ByteBuffer.wrap(data);
				tempBuffer.position(data.length);
				tempBuffer.flip();
				
				while (true) {
					
					int freeSpace = proxyReadBuffer.remaining();
					int neededSpace = tempBuffer.remaining();
					
					if (freeSpace < neededSpace) {	// not enough space 
													// -> write fragment
						
						byte[] fragment = new byte[freeSpace];
						tempBuffer.get(fragment);
						proxyReadBuffer.put(fragment);
						
						
						// wait for free space
						isPutInProxyReadBufferWaiting = true;
						
						while(proxyReadBuffer.remaining() == 0) {
							
							try {
								
								proxyReadBuffer.wait();
								
							} catch (InterruptedException e) {

								LOGGER.fine(e.getMessage());
								continue;
								
							}
							
						}
						
						isPutInProxyReadBufferWaiting = false;
						
					} else { // enough space -> last fragment
						
						byte[] lastFragment = new byte[neededSpace];
						tempBuffer.get(lastFragment);
						proxyReadBuffer.put(lastFragment);
						break;
						
					}
					
				}
				
			} else { // enough space
				
				proxyReadBuffer.put(data);
				
			}

		}

	}

	
	/**
	 * Used to read data from <code>proxyReadBuffer</code>, which contains 
	 * data received from proxy server for this <code>User</code>.
	 * <p>
	 * Reads as much bytes as available (until <code>limit</code>).
	 * 
	 * @param limit	Maximum number of bytes to be returned.
	 * 
	 * @return		The requested data.
	 */
	public byte[] getFromProxyReadBuffer(int limit) {

		synchronized (proxyReadBuffer) {

			int numberOfBytesToBeRead = 
				(proxyReadBuffer.position() > limit) 
				? limit
				: proxyReadBuffer.position();

			byte[] result = new byte[numberOfBytesToBeRead];

			proxyReadBuffer.flip();
			proxyReadBuffer.get(result);
			proxyReadBuffer.compact();
			
			if (isPutInProxyReadBufferWaiting) {
				
				proxyReadBuffer.notify();
				
			}
			
			return result;

		}

	}

	
	/**
	 * Returns the number of bytes currently available in 
	 * <code>proxyReadBuffer</code>, which contains data received from proxy 
	 * server for this <code>User</code>.
	 * <p>
	 * Assures that AT LEAST <code>@return</code> bytes can be read from 
	 * <code>proxyReadBuffer</code> until next call of 
	 * <code>getFromProxyReadBuffer(int)</code>.
	 * 
	 * @return	Number of bytes currently available in 
	 * 			<code>proxyReadBuffer</code>
	 */
	public int availableDataInProxyReadBuffer() {

		synchronized (proxyReadBuffer) {

			return proxyReadBuffer.position();

		}

	}

	
	/**
	 * Used to add data, that shall be sent to proxy server. Puts the bypassed 
	 * data in this <code>User</code>'s <code>proxyWriteBuffer</code>, from 
	 * where it will be sent to the corresponding proxy server.
	 * <p>
	 * Blocks until all data is written.
	 * 
	 * @param data	Data, that shall be sent to proxy server.
	 */
	public void putInProxyWriteBuffer(byte[] data) {

		synchronized (proxyWriteBuffer) {

			try {
				
				proxyWriteBuffer.put(data);
				
			} catch (BufferOverflowException e) {
				// receiver/proxy is too slow
				
				LOGGER.info(	"Receiver of channel " +IDENTIFIER 
								+" doesn't receive data fast enough!"
								);
				
			}

		}
		// TODO: remove
		//System.out.println("at end of cascade: " +new String(data)); 
		
		// TODO: remove (=ECHO)
		putInProxyReadBuffer(getFromProxyWriteBuffer(data.length));
		
	}
	
	
	/**
	 * Used to read data from <code>proxyWriteBuffer</code>, which contains 
	 * data that shall be sent to the proxy server. 
	 * <p>
	 * Reads as much bytes as available (until <code>limit</code>).
	 * 
	 * @param limit	Maximum number of bytes to be returned.
	 * 
	 * @return		The requested data.
	 */
	public byte[] getFromProxyWriteBuffer(int limit) {

		synchronized (proxyWriteBuffer) {

			int numberOfBytesToBeRead = 
				(proxyWriteBuffer.position() > limit) 
				? limit
				: proxyWriteBuffer.position();

			byte[] result = new byte[numberOfBytesToBeRead];

			proxyWriteBuffer.flip();
			proxyWriteBuffer.get(result);
			proxyWriteBuffer.compact();
			
			return result;

		}

	}

	
	/**
	 * Returns the number of bytes currently available in 
	 * <code>proxyWriteBuffer</code>, which contains data that shall be sent to 
	 * the proxy server. 
	 * <p>
	 * Assures that AT LEAST <code>@return</code> bytes can be read from 
	 * <code>proxyWriteBuffer</code> until next call of 
	 * <code>getFromProxyWriteBuffer(int)</code>.
	 * 
	 * @return	Number of bytes currently available in 
	 * 			<code>proxyWriteBuffer</code>
	 */
	public int availableDataInProxyWriteBuffer() {

		synchronized (proxyWriteBuffer) {

			return proxyWriteBuffer.position();

		}

	}
	
	
	/**
	 * Allocates space for the buffers used to communicate with this 
	 * <code>User</code>'s corresponding <code>Client</code> (needed for 
	 * non-blocking I/O).
	 * <p>
	 * Used by first mix of cascade only.
	 * 
	 * @param socketChannel	<code>SocketChannel</code> to read/write data 
	 * 						from/to.
	 */
	public void initializeClientBuffers(SocketChannel socketChannel) {
		
		this.socketChannel = socketChannel;
		
		this.clientReadBuffer = 
			ByteBuffer.allocateDirect(MAX_REPLY_LENGTH);
		
		this.clientWriteBuffer = 
			ByteBuffer.allocateDirect(MAX_REQUEST_LENGTH);
		
	}
	
	
	/**
	 * Allocates space for the buffers used to communicate with this 
	 * <code>User</code>'s corresponding <code>proxy server</code> (needed for 
	 * non-blocking I/O).
	 * <p>
	 * Used by last mix of cascade only.
	 */
	public void initializeProxyBuffers() {
		
		int bufferCapacity = 
			new Integer(internalInformationPort.getProperty(
					"PROXY_BUFFER_SIZE")
				)
			-
			Payload.getHeaderLength();

		this.proxyReadBuffer = ByteBuffer.allocateDirect(bufferCapacity);
		this.proxyWriteBuffer = ByteBuffer.allocateDirect(bufferCapacity);
		
	}
	
	
	/**
	 * Returns a simple String representation of this class.
	 * 
	 * @return	A simple String representation of this class.
	 * 
	 */
	@Override
	public String toString() {
		
		String output = "";
		output += "Identifier: " +IDENTIFIER +"\n";
		output += "Identifier for next mix: " +IDENTIFIER_FOR_NEXT_MIX +"\n";
		output += "Channel established: " +isChannelEstablished +"\n";

		return output;
		
	}

}