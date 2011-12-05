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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Logger;

import outputStrategy.OutputStrategyController;

import message.ChannelEstablishMessage;
import message.ChannelEstablishMessagePart;
import message.ChannelReleaseMessage;
import message.ChannelMessage;
import message.ChannelMessagePart;
import message.Reply;

import userDatabase.User;
import userDatabase.UserDatabaseController;

import util.Util;

import exception.UserAlreadyExistingException;
import exception.UnknownUserException;


/**
 * Handles communication with <code>Client</code>s. Accepts connections, 
 * receives <code>Requests</code> and sends <code>Reply</code>ies. 
 * <code>Requests</code> are put in the 
 * <code>InputOutputHandlerController</code>'s <code>requestInputQueue</code> 
 * (see <code>InputOutputHandlerController.addUnprocessedRequest()</code>).
 * <code>Reply</code>ies are taken from the 
 * <code>InputOutputHandlerController</code>'s <code>replyOutputQueue</code> 
 * (see <code>InputOutputHandlerController.getProcessedReply()</code>).
 * <p>
 * Adds/removes user to/from <code>UserDatabase</code>.
 * <p>
 * Uses non-blocking I/O.
 * 
 * @author Karl-Peter Fuchs
 */
final class ClientConnectionHandler extends Thread {
	
	/** 
	 * Reference on component <code>InternalInformationPort</code>. 
	 * Used to display and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = internalInformationPort.getLogger();
	
	/** Port number this <code>ClientConnectionHandler</code> runs on. */
	private final int PORT; 
	
	/** 
	 * Address this <code>ClientConnectionHandler</code>'s socket shall be 
	 * bound to.
	 */
	private final InetAddress BIND_ADDRESS;
	
	/** 
	 * The maximum queue length for incoming connection indications. If a 
	 * connection indication arrives when the queue is full, the connection is 
	 * refused.
	 */
	private final int BACKLOG; 
	
	/** 
	 * Maximum amount of time an <code>accept</code> (of a client's connection) 
	 * may take before being canceled in ms.
	 */
	private final int SO_TIMEOUT;
	
	/** 
	 * Maximum number of connections (=connected <code>Client</code>s).
	 */
	private final int MAX_CONNECTIONS;
	
	/** 
	 * The mix' position in the cascade this object belongs to. "1" means 
	 * "first mix", "2" means "a middle mix" and "3" means "last mix" of 
	 * cascade.
	 */
	private final int POSITION_OF_MIX_IN_CASCADE;
	
	/** 
	 * Number of further mixes between the mix this <code>InputOutputHandler
	 * </code> belongs to and the receiver. 
	 */
	private final int NUMBER_OF_FURTHER_HOPS; 
	
	/**  
	 * Reference on <code>InputOutputHandlerController</code> (Used to add 
	 * messages).
	 */
	private InputOutputHandlerController inputOutputHandler;
	
	/**  
	 * Reference on component <code>UserDatabase</code> (Used to add/remove
	 * <code>User</code>)s.
	 */
	private UserDatabaseController userDatabase;
	
	/** 
	 * Reference on component <code>OutputStrategy</code> (Used to send 
	 * <code>InternalMessage</code>s).
	 */
	private OutputStrategyController outputStrategy;
	
	/** List of <code>User</code>s with data ready to be sent. */
	private LinkedList<User> writeRequests =  new LinkedList<User>();
	
	/** 
	 * Random number generator used to generate <code>User</code> identifiers.
	 */
	private SecureRandom secureRandom = new SecureRandom();
	
	/** Number of <code>Client</code>s currently connected. */
	private Integer numberOfActiveConnections = 0;
	
	/** <code>Selector</code> used for non-blocking I/O. */
	private Selector selector = null;
	
	/** <code>ServerSocketChannel</code> used for accepting connections. */
	private ServerSocketChannel serverSocketChannel;
	
	
	/**
	 * Constructs a new <code>ClientConnectionHandler</code> which accepts 
	 * connections, receives <code>Requests</code> and sends 
	 * <code>Reply</code>ies. <code>Requests</code> are put in the 
	 * <code>InputOutputHandlerController</code>'s 
	 * <code>requestInputQueue</code> 
	 * (see <code>InputOutputHandlerController.addUnprocessedRequest()</code>).
	 * <code>Reply</code>ies are taken from the 
	 * <code>InputOutputHandlerController</code>'s <code>replyOutputQueue</code> 
	 * (see <code>InputOutputHandlerController.getProcessedReply()</code>).
	 * <p>
	 * Adds/removes user to/from <code>UserDatabase</code>.
	 * <p>
	 * Uses non-blocking I/O.
	 * 
	 * @param inputOutputHandler	Reference on 
	 * 								<code>InputOutputHandlerController</code> 
	 * 								(Used to add messages).
	 * @param userDatabase			Reference on component 
	 * 								<code>UserDatabase</code> (Used to 
	 * 								add/remove <code>User</code>)s.
	 * @param outputStrategy		Reference on component 
	 * 								<code>OutputStrategy</code> (Used to send 
	 * 								<code>InternalMessage</code>s).
	 */
	protected ClientConnectionHandler(
			InputOutputHandlerController inputOutputHandler,
			UserDatabaseController userDatabase,
			OutputStrategyController outputStrategy
			) {
		
		this.inputOutputHandler = inputOutputHandler;
		this.userDatabase = userDatabase;
		this.outputStrategy = outputStrategy;
			
		// load values (from property file)
		this.BIND_ADDRESS = 
			InputOutputHandlerController.tryToGenerateInetAddress(
					getProperty("BIND_ADDRESS")
					);
			
		this.PORT = new Integer(getProperty("PORT")); 
		this.BACKLOG = new Integer(getProperty("BACKLOG"));
		this.SO_TIMEOUT = new Integer(getProperty("SO_TIMEOUT"));
		
		this.MAX_CONNECTIONS = 
			new Integer(getProperty("MAX_CONNECTIONS"));
		
		this.POSITION_OF_MIX_IN_CASCADE = 
			inputOutputHandler.POSITION_OF_MIX_IN_CASCADE;
		
		this.NUMBER_OF_FURTHER_HOPS = 
			inputOutputHandler.NUMBER_OF_FURTHER_HOPS;
			
	}
	
	
	/**
	 * Makes this <code>ClientConnectionHandler</code> wait for connections, 
	 * <code>Request</code>s and <code>Reply</code>ies.
	 */
	protected void acceptConnections() {
		
		// generate and bind serverSocketChannel
		try {
			
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.configureBlocking(false);
			ServerSocket serverSocket = serverSocketChannel.socket();
			
			InetSocketAddress endpoint = 
				new InetSocketAddress(BIND_ADDRESS, PORT);
			
			serverSocket.bind(endpoint, BACKLOG);
			
			serverSocket.setSoTimeout(SO_TIMEOUT);
			
			
			// generate selector
			selector = Selector.open(); 
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
			
			LOGGER.info(	"(MIX" +POSITION_OF_MIX_IN_CASCADE +" IOH) " 
							+"Listening on port " +PORT
							);
			
			LOGGER.config(	"(MIX" +POSITION_OF_MIX_IN_CASCADE +") Bound to: " 
							+BIND_ADDRESS +":" +PORT
							);
			
		} catch (IOException e) {

			LOGGER.severe(	"(MIX" 
							+inputOutputHandler.POSITION_OF_MIX_IN_CASCADE 
							+" IOH) Couldn't bind socket to port " +PORT +"!" 
							+e.getMessage()
							);
	
			System.exit(1);
	
		}
		
		start();

		new Thread(
					
			new Runnable() {
								
				public void run() {

					initializeReplyProcess();
						
				}
								
			}
					
		).start();
		
	}
	
	
	/**
	 * Waits for <code>Reply</code>ies ready to be sent (to 
	 * <code>Client</code>s). Ready <code>Reply</code>ies are put in the 
	 * suiting <code>User</code>'s <code>buffer</code>. Afterwards the 
	 * <code>selector</code> is notified about the data being ready to 
	 * send (non-blocking I/O).
	 * 
	 * @see #writeRequests
	 * @see #selector
	 */
	private void initializeReplyProcess() {
		
		User channelWhosReplyIsReady;
		Reply reply;
		ByteBuffer channelWriteBuffer;
			
		while (true) {
				
			reply = inputOutputHandler.getProcessedReply();
			channelWhosReplyIsReady = reply.getChannel();
			
			channelWriteBuffer = 
				channelWhosReplyIsReady.getClientWriteBuffer();
			
			if (	channelWriteBuffer.remaining() 
					<
					(reply.getByteMessage().length + 4)
					) {	// "+4" since length-header must be submitted as 
						// well
				
				LOGGER.fine(	"User "
								+channelWhosReplyIsReady.getIdentifier()
								+" doesn't receive messages fast enough!"
								);
				
			} else {
				
				// generate header for reply
				int lengthOfReply = reply.getByteMessage().length;
				channelWriteBuffer.put(Util.intToByteArray(lengthOfReply));
				channelWriteBuffer.put(reply.getByteMessage());
				
				synchronized(writeRequests) {
					
					writeRequests.add(channelWhosReplyIsReady);
					
				}
				
				// wake up selector so it can send the replies
				selector.wakeup();
				
			}

		}
		
	}
	
	
	/**
	 * Handles read, write and accept events (non-blocking I/O).
	 */
	@Override
	public void run() {
		
		while (true) { // handle read, write and accept events

			try {
					
				registerWriteRequests();
				
				// wait for event(s)
				selector.select();
					
				// retrieve keys
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> selectedKeys = keys.iterator();
					
				SelectionKey key;
					
				// for each key
				while (selectedKeys.hasNext()) {
						
					key = (SelectionKey)selectedKeys.next();
					selectedKeys.remove();
		
					if (key.isAcceptable()) {
						
						handleAcceptRequest();
						continue;
					
					} else if (key.isReadable()) {
						
						handleReadRequest(key);
						continue;
							
					} else if (key.isWritable()) {
						
						handleWriteRequest(key);
							
					}
						
				}
					
			} catch (IOException e) {
					
				LOGGER.fine(e.getMessage());
				continue;
						
			}
				
		}

	} 
	
	
	/**
	 * Registers all write requests from <code>writeRequests</code> with 
	 * <code>selector</code>.
	 * 
	 * @see #writeRequests
	 * @see #selector
	 */
	private void registerWriteRequests() {
		
		synchronized(writeRequests) {
		        	
	        Iterator<User> writeRequestIterator = 
	        	writeRequests.iterator();
	        	
	        while (writeRequestIterator.hasNext()) { 
	        	// for each channelWriteBuffer with reply ready: 
	        		
	        	User channel= writeRequestIterator.next();
	        	
	        	SelectionKey selectionkey = 
	        		channel.getSocketChannel().keyFor(selector);
	        	
	        	// register writeRequest in selector
	        	selectionkey.interestOps(SelectionKey.OP_WRITE);
		        	
	        }
		        	
	        // delete old writeRequests
	        writeRequests.clear();
	        
		}
		
	}
	
	
	/**
	 * Handles an accept request. Accepts connections until the maximum number 
	 * of connections is reached (see <code>numberOfActiveConnections</code>, 
	 * <code>MAX_CONNECTIONS</code>). Generates <code>User</code> objects and 
	 * adds them to the <code>UserDatabase</code> (if connection accepted).
	 * 
	 * @throws IOException If an I/O error occurres.
	 */
	private void handleAcceptRequest() throws IOException {
		
		if (	numberOfActiveConnections 
				< 
				MAX_CONNECTIONS) {
				
			SocketChannel client = serverSocketChannel.accept();
			User channel = null;
			numberOfActiveConnections++;
			client.configureBlocking(false);
			
			while (true) {
				
				try {
					
					channel = new User(Math.abs(secureRandom.nextInt()));
					channel.initializeClientBuffers(client);
					userDatabase.addUser(channel);
					
				} catch (UserAlreadyExistingException e) {

					continue;

				}
				
				break;
				
			}
			
			if (inputOutputHandler.IS_LAST) { // single mix
					
				channel.initializeProxyBuffers();
					
			}
			
			int channelIdentifier = channel.getIdentifier();
					
			client.register(	selector, 
								SelectionKey.OP_READ, 
								channelIdentifier
								);
					
			LOGGER.fine(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
							+" IOH) Accept granted: " +client
							+", ID: " +channelIdentifier
							);
				
		} else {
				
			LOGGER.fine(	"(MIX" 
							+POSITION_OF_MIX_IN_CASCADE 
							+" IOH) Accept denied!"
							);
				
		}
		
	}
	
	
	/**
	 * Handles a read request. Writes received data to the suiting 
	 * <code>User</code>'s buffer. If a buffer contains a whole message, a 
	 * <code>Message</code> object is created and passed to the 
	 * <code>InputOutputHandlerController</code> (which provides it to the 
	 * <code>MessageProcessor</code>).
	 * 
	 * @param key	Token representing the registration of a 
	 * 				<code>SelectableChannel</code> with the 
	 * 				<code>selector</code>. 
	 * 
	 * @throws IOException If an I/O error occurres.
	 * 
	 * @see #selector
	 */
	private void handleReadRequest(SelectionKey key) throws IOException {
		
		int channelIdentifier = (Integer)key.attachment();
		SocketChannel client = (SocketChannel)key.channel();
		User channel = null;
		
		try {
			
			channel = userDatabase.getUser(channelIdentifier);
			
		} catch (UnknownUserException e) {
			
			LOGGER.severe(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
							+" IOH) Internal error: \"key.attachment()\" "
							+"points on invalid channel! \n"
							+e.getMessage()
							);
			
			System.exit(1);
			
		}
		
		ByteBuffer buffer = channel.getClientReadBuffer();
		channel.setTimestampOfLastActivity();
		
		if (	buffer.position() == 0 
				&& 
				channel.getHasMessageInCurrentBatch()
				) {
			// user is trying to send a message, although he has already a
			// message in the current batch
			// => ignore his request until next output
			
			LOGGER.fine(	"(MIX" +POSITION_OF_MIX_IN_CASCADE 
							+" IOH) User " +channelIdentifier
							+" is trying to add multiple messages to one "
							+"batch!"
							);
			
			key.cancel();
			return;
			
		}
		
		if (channel.getIsChannelEstablished()) {
				
			buffer.limit(
					ChannelMessagePart.getMessageLength(
						NUMBER_OF_FURTHER_HOPS)
						);
				
		} else {
				
			buffer.limit(
					ChannelEstablishMessagePart.getMessageLength(
						NUMBER_OF_FURTHER_HOPS)
						);
			
		}
			
		try {
				
			int bytesRead = client.read(buffer);
				
			if (bytesRead == -1) {
					
				LOGGER.fine(	"(MIX"+POSITION_OF_MIX_IN_CASCADE 
								+" IOH) Client disconnected."
								);
					
				key.cancel();
				client.close();
					
				if (POSITION_OF_MIX_IN_CASCADE != 3) { 
					// not last mix of cascade
						
					outputStrategy.addRequest(
						new ChannelReleaseMessage(channel)
						);	
						
				}
					
				numberOfActiveConnections--;
				
				try {
					
					userDatabase.removeUser(channelIdentifier);
					
				} catch (UnknownUserException e) {

					LOGGER.fine(e.getMessage());
					return;
					
				}
				
				return;
					
			}
				
		} catch (IOException e) {
				
			LOGGER.fine(	"(MIX" 
							+POSITION_OF_MIX_IN_CASCADE 
							+" IOH) Connection to " 
							+"client lost."
							);
				
			key.cancel();
			client.close();
			
			if (POSITION_OF_MIX_IN_CASCADE != 3) { 
				// not last mix of cascade
					
				outputStrategy.addRequest(
						new ChannelReleaseMessage(channel)
					);	
					
			}
				
			numberOfActiveConnections--;
			
			try {
				
				userDatabase.removeUser(channelIdentifier);
				
			} catch (UnknownUserException ex) {

				LOGGER.fine(ex.getMessage());
				return;
				
			}
			
			return;
				
		}
			
		if(buffer.position() < buffer.limit()) {
				
			// message receival not complete yet
				
		} else { // message received completely
				
			buffer.flip();
			byte[] byteMessage = new byte[buffer.limit()];
			buffer.get(byteMessage);
				
			if (!channel.getIsChannelEstablished()) {
				
				inputOutputHandler.addUnprocessedRequest(
						new ChannelEstablishMessage(byteMessage, 
													channel, 
													NUMBER_OF_FURTHER_HOPS
													)
												);
				
			} else { // channel is established
					
				inputOutputHandler.addUnprocessedRequest(
						new ChannelMessage(
							byteMessage, 
							channel,
							NUMBER_OF_FURTHER_HOPS
							)
					);
					
			}
				
			buffer.clear();
				
		}
		
	}
	
	
	/**
	 * Handles a write request. Writes data from a <code>User</code>'s buffer 
	 * to the suiting <code>SocketChannel</code>.
	 * 
	 * @param key	Token representing the registration of a 
	 * 				<code>SelectableChannel</code> with the 
	 * 				<code>selector</code>. 
	 * 
	 * @throws IOException If an I/O error occurres.
	 */
	private void handleWriteRequest(SelectionKey key) throws IOException {
		
		int channelIdentifier = (Integer)key.attachment();
		SocketChannel client = (SocketChannel)key.channel();
		User channel;
		
		try {
			
			channel = userDatabase.getUser(channelIdentifier);
			
		} catch (UnknownUserException e) {

			throw new IOException(e.getMessage());
			
		}
		
		ByteBuffer buffer = channel.getClientWriteBuffer();
		
		buffer.flip();
		
		client.write(buffer);
		
		if (buffer.remaining() > 0) {
			// couldn't write all data at once
				
			// continue writing later
			return;
				
		} else { // all data written
				
			buffer.clear();
			// signalize interest in new data from client
			key.interestOps(SelectionKey.OP_READ);
				
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