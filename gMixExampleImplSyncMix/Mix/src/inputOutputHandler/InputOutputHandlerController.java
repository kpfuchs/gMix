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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import architectureInterface.InputOutputHandlerInterface;

import outputStrategy.OutputStrategyController;

import message.Reply;
import message.ReplyMessage;
import message.Request;

import externalInformationPort.ExternalInformationPortController;

import userDatabase.User;
import userDatabase.UserDatabaseController;


/**
 * Controller class of component <code>InputOutputHandler</code>. Implements  
 * the architecture interface <code>InputOutputHandlerInterface</code>.
 * <p>
 * Handles communication with clients, other mixes and receivers.
 * <p>
 * Waits for new connections from clients or other mixes and accepts messages 
 * using the classes <code>ClientConnectionHandler</code>, 
 * <code>PreviousMixConnectionHandler</code>, 
 * <code>NextMixConnectionHandler</code> and 
 * <code>ProxyConnectionHandler</code> and stores them in a 
 * <code>ConcurrentLinkedQueue</code>.
 * <p>
 * The process of accepting connections and receiving messages works 
 * in parallel to the mix operations. Therefore, the mix is capable of taking 
 * messages at any time (unless it is overloaded).
 * <p>
 * Sends (already mixed) messages to their destination using the same classes 
 * mentioned above. The mixed messages are stored in a 
 * <code>ConcurrentLinkedQueue</code>. The process of sending messages works in 
 * parallel to the mix operations. Therefore, the mix is capable of mixing new 
 * messages while sending the old ones.
 * <p>
 * This class is thread-safe.
 * 
 * @author Karl-Peter Fuchs
 */
public final class InputOutputHandlerController implements 
		InputOutputHandlerInterface {
	
	/** 
	 * Reference on component <code>InternalInformationPort</code>. 
	 * Used to display and/or log data and read general settings.
	 */
	private static InternalInformationPortController internalInformationPort = 
		new InternalInformationPortController();
	
	/** Logger used to log and display information. */
	private final static Logger LOGGER = internalInformationPort.getLogger();
	
	/** 
	 * Number of further mixes between the mix this <code>InputOutputHandler
	 * </code> belongs to and the receiver. 
	 */
	protected final int NUMBER_OF_FURTHER_HOPS; 
	
	/** 
	 * Number of previous mixes between the mix this <code>InputOutputHandler
	 * </code> belongs to and the sender. 
	 */
	protected final int NUMBER_OF_PREVIOUS_HOPS;
	
	/** 
	 * The mix' position in the cascade this object belongs to. "1" means 
	 * "first mix", "2" means "a middle mix" and "3" means "last mix" of 
	 * cascade.
	 */
	protected final int POSITION_OF_MIX_IN_CASCADE;
	
	/** Indicates whether this mix is the first of the cascade or not. */
	protected final boolean IS_FIRST;
	
	/** Indicates whether this mix is the lost of the cascade or not. */
	protected final boolean IS_LAST;
	
	/**
	 * A <code>ConcurrentLinkedQueue</code>, that stores <code>Request</code>s 
	 * until they are processed.
	 */
	private ConcurrentLinkedQueue<Request> requestInputQueue = 
		new ConcurrentLinkedQueue<Request>();
	
	/**
	 * A <code>ConcurrentLinkedQueue</code>, that stores already processed 
	 * <code>Request</code>s until they are sent (to the next mix or server).
	 */
	private ConcurrentLinkedQueue<Request> requestOutputQueue = 
		new ConcurrentLinkedQueue<Request>();
	
	/**
	 * A <code>ConcurrentLinkedQueue</code>, that stores <code>Reply</code>ies 
	 * until they are processed.
	 */
	private ConcurrentLinkedQueue<Reply> replyInputQueue = 
		new ConcurrentLinkedQueue<Reply>();
	
	/**
	 * A <code>ConcurrentLinkedQueue</code>, that stores already processed 
	 * <code>Reply</code>ies until they are sent (to the previous mix or 
	 * client).
	 */
	private ConcurrentLinkedQueue<Reply> replyOutputQueue = 
		new ConcurrentLinkedQueue<Reply>();
	
	/** 
	 * Reference on <code>ClientConnectionHandler</code>. Used to exchange 
	 * <code>Message</code>s with <code>Client</code>s.
	 */
	private ClientConnectionHandler clientConnectionHandler;
	
	/** 
	 * Reference on <code>PreviousMixConnectionHandler</code>. Used to exchange 
	 * <code>Message</code>s with previous <code>Mix</code>.
	 */
	private PreviousMixConnectionHandler previousMixConnectionHandler;
	
	/**  
	 * Reference on <code>ReplyDecision</code>. Used to determine reply size by 
	 * last mix of cascade.
	 */
	private ReplyDecision replyDecision;
	
	/**  Reference on component <code>OutputStrategy</code>. */
	private OutputStrategyController outputStrategy;
	
	/**  Reference on component <code>ExternalInformationPort</code>. */
	private ExternalInformationPortController externalInformationPort;
	
	/**  Reference on component <code>UserDatabase</code>. */
	private UserDatabaseController userDatabase;
	
	
	/**
	 * Creates a new <code>InputOutputHandler</code> component that handles 
	 * communication with clients, other mixes and proxies.
	 * <p>
	 * Component can't be used before calling <code>initialize()</code>.
	 * 
	 * @see #initialize(	UserDatabaseController, 
	 * 						OutputStrategyController, 
	 * 						ExternalInformationPortController)
	 */
	public InputOutputHandlerController() {
		
		// load values (from property file)
		this.NUMBER_OF_FURTHER_HOPS = 
			new Integer(getProperty("NUMBER_OF_FURTHER_MIXES"));
			
		this.NUMBER_OF_PREVIOUS_HOPS = 
			new Integer(getProperty("NUMBER_OF_PREVIOUS_MIXES"));
			
		this.POSITION_OF_MIX_IN_CASCADE = 
			new Integer(getProperty("POSITION_OF_MIX_IN_CASCADE"));
			
		this.IS_FIRST = (NUMBER_OF_PREVIOUS_HOPS == 0) ? true : false;
		this.IS_LAST = (NUMBER_OF_FURTHER_HOPS == 0) ? true : false;
		
		
		
	}
	
	
	/**
	 * Initialization method for this component. Must be called before using an 
	 * instance of this class for anything but dispensing references on the 
	 * instance itself.
	 * 
	 * @param userDatabase		Reference on component 
	 * 							<code>UserDatabase</code>.
	 * @param outputStrategy	Reference on component 
	 * 							<code>OutputStrategy</code>.
	 * @param eip				Reference on component 
	 * 							<code>ExternalInformationPort</code>.
	 */
	public void initialize(	UserDatabaseController userDatabase, 
							OutputStrategyController outputStrategy,
							ExternalInformationPortController eip
							) {
		
		this.userDatabase = userDatabase;
		this.outputStrategy = outputStrategy;
		this.externalInformationPort = eip;
		
		this.replyDecision = new ReplyDecision(this);
		logInformation();
		
	}
	
	
	/**
	 * Makes component listen for connections/messages on communication 
	 * channels.
	 */
	public void acceptConnections() {
		
		// start connection handlers
		if (IS_FIRST && IS_LAST) { // single mix
			
			clientConnectionHandler = 
				new ClientConnectionHandler(this, userDatabase, outputStrategy);
			
			new ProxyConnectionHandler(this);
			
			clientConnectionHandler.acceptConnections();
			
		} else if (NUMBER_OF_PREVIOUS_HOPS == 0) { // first mix of cascade
			
			clientConnectionHandler = 
				new ClientConnectionHandler(this, userDatabase, outputStrategy);
			
			new NextMixConnectionHandler(	this, 
											userDatabase, 
											externalInformationPort
											);
			
			clientConnectionHandler.acceptConnections();
			
		} else if (NUMBER_OF_FURTHER_HOPS == 0) { // last mix of cascade
			
			previousMixConnectionHandler = 
				new PreviousMixConnectionHandler(	this, 
													userDatabase, 
													outputStrategy,
													externalInformationPort
													);
			
			new ProxyConnectionHandler(this);
			
			previousMixConnectionHandler.acceptConnection();
			
		} else { // "middle" mix of cascade (not last and not first)
			
			previousMixConnectionHandler = 
				new PreviousMixConnectionHandler(	this, 
													userDatabase, 
													outputStrategy,
													externalInformationPort
													);
			
			new NextMixConnectionHandler(	this, 
											userDatabase, 
											externalInformationPort
											);
			
			previousMixConnectionHandler.acceptConnection();
			
		}
		
	}
	
	
	/**
	 * Tries to generate an <code>InetAddress</code> from the bypassed host 
	 * name. If an exception occurs, the program exits!
	 * 
	 * @param hostName Host name to generate <code>InetAddress</code> from.
	 * @return 	<code>InetAddress</code> representation of the bypassed host 
	 * 			name.
	 */
	protected static InetAddress tryToGenerateInetAddress(String hostName) {
		
		try {
			
			return InetAddress.getByName(hostName);
			
		} catch (UnknownHostException e) {
			
			LOGGER.severe(	"(MIX IOH) Invalid address: " +hostName 
							+e.getMessage()
							);
			
			System.exit(1);
			return null;
			
		}
		
	}
	
	
	/**
	 * Logs/displays information about this mix.
	 */
	private void logInformation() {
		
		if (IS_FIRST) {
			
			LOGGER.config(	"(MIX" +POSITION_OF_MIX_IN_CASCADE+") Type "
							+"of mix: First mix"
							);
			
		} else if (IS_LAST) {
			
			LOGGER.config(	"(MIX" +POSITION_OF_MIX_IN_CASCADE+") Type "
							+"of mix: Last mix"
							);
			
		} else {
			
			LOGGER.config(	"(MIX" +POSITION_OF_MIX_IN_CASCADE+") Type "
							+"of mix: Middle mix"
							);
			
		}
		
		LOGGER.config(	"(MIX" +POSITION_OF_MIX_IN_CASCADE+") Number of "
						+"previous hops: " +NUMBER_OF_PREVIOUS_HOPS
						);
		
		LOGGER.config(	"(MIX" +POSITION_OF_MIX_IN_CASCADE +") Number "
						+"of further hops: " +NUMBER_OF_FURTHER_HOPS
						);
		
		
	}
	
	
	/**
	 * Adds the bypassed (already mixed) <code>Request</code> to the 
	 * <code>requestOutputQueue</code> (from where it will be sent to its 
	 * destination).
	 * <p>
	 * Returns immediately (asynchronous behavior), the process of sending 
	 * itself may be deferred (e. g. if communication channel is busy).
	 * <p>
	 * Assures order (queuing strategy) and is thread-safe.
	 * <p>
	 * Used by component <code>OutputStrategy</code>.
	 * 
	 * @param request 	Already processed message, that shall be sent to the 
	 * 					next communication partner.
	 * 
	 * @see #getProcessedRequest()
	 * @see #addRequests(Request[])
	 */
	public void addRequest(Request request) {
		
		synchronized (requestOutputQueue) {

			requestOutputQueue.add(request);
			
			// notify waiting "getRequest()" about the newly received message
			requestOutputQueue.notify();			
			
		}
	
	}
	
	
	/**
	 * Adds all the bypassed (already mixed) <code>Request</code>s to the 
	 * <code>requestOutputQueue</code> (from where they will be sent to their 
	 * destination).
	 * <p>
	 * Returns immediately (asynchronous behavior), the process of sending 
	 * itself may be deferred (e. g. if communication channel is busy).
	 * <p>
	 * Assures order (queuing strategy) and is thread-safe.
	 * <p>
	 * Used by component <code>OutputStrategy</code>.
	 * 
	 * @param requests 	Already processed messages, that shall be sent to the 
	 * 					next communication partner.
	 * 
	 * @see #getProcessedRequest()
	 * @see #addRequest(Request)
	 */
	public void addRequests(Request[] requests) {
		
		for (Request request: requests) {
			
			addRequest(request);
			
		}
		
		if (IS_LAST) {
			
			User[] users = new User[requests.length];
			
			for (int i=0; i<requests.length; i++) {
				
				users[i] = requests[i].getChannel();
				
			}
			
			replyDecision.manageReplyProcess(users);
			
		}
		
	}


	/**
	 * Returns an (already mixed) <code>Request</code> from the 
	 * <code>requestOutputQueue</code>. If no requests are available, this 
	 * method blocks until a new <code>Request</code> arrives.
	 * <p>
	 * Assures order (queuing strategy) and is thread-safe.
	 * 
	 * @return 	An (already mixed) <code>Request</code>.
	 */
	protected Request getProcessedRequest() {
		
		synchronized (requestOutputQueue) {
			
			Request request = null;
			
			while ((request = requestOutputQueue.poll()) == null) {
					
				try {
						
					// wait for new messages in the requestOutputQueue
					// = wait for notification at "addRequest()"
					requestOutputQueue.wait();
						
				} catch (InterruptedException e) {
						
					LOGGER.severe(e.getMessage());
					continue;
						
				}
					
			}
			
			LOGGER.fine(	"(MIX" 
							+POSITION_OF_MIX_IN_CASCADE 
							+" IOH) Putting out request "
							+request.getChannelID() + ":" 
							+request.getMessageID() + "."
							);
	
			return request;

		}

	}
	

	/**
	 * Adds the bypassed (just received) <code>Request</code> to the 
	 * <code>requestInputQueue</code> (from where it will be taken by 
	 * component <code>MessageProcessor</code> via <code>getRequest()</code>).
	 * <p>
	 * Assures order (queuing strategy) and is thread-safe.
	 * 
	 * @param request 	Just received message, that shall be added.
	 */
	protected void addUnprocessedRequest(Request request) {
		 
		synchronized (requestInputQueue) {
			
			requestInputQueue.add(request);
			
			// notify waiting "getRequest()" about the newly received message
			requestInputQueue.notify();
	
		}
		
	}
	
	
	/**
	 * Returns a <code>Request</code> (previously received, unprocessed) from a 
	 * communication partner (e. g. client or other mix). If no 
	 * <code>Request</code>s are available, this method blocks until a new 
	 * <code>Request</code> arrives.
	 * <p>
	 * Assures order (queuing strategy) and is thread-safe.
	 * 
	 * @return 	A (previously received, unprocessed) <code>Request</code>.
	 */
	public Request getRequest() {
		
		synchronized (requestInputQueue) {
			
			Request request = null;
			
			// as long as the forwardInputQueue is empty
			while ((request = requestInputQueue.poll()) == null) {
					
				try {
						
					// wait for new messages in the requestInputQueue
					// = wait for notification at "addRequest()"
					requestInputQueue.wait();
						
				} catch (InterruptedException e) {

					LOGGER.severe(e.getMessage());
					continue;
						
				}
					
			}
			
			return request;

		}
		
	}

	
	/**
	 * Adds the bypassed (already mixed) <code>Reply</code> to the 
	 * <code>replyOutputQueue</code> (from where it will be sent to its 
	 * destination).
	 * <p>
	 * Returns immediately (asynchronous behavior), the process of sending 
	 * itself may be deferred (e. g. if communication channel is busy).
	 * <p>
	 * Assures order (queuing strategy) and is thread-safe.
	 * <p>
	 * Used by component <code>OutputStrategy</code>.
	 * 
	 * @param reply 	Already processed message, that shall be sent to the 
	 * 					next communication partner.
	 * 
	 * @see #getProcessedReply()
	 * @see #addReplies(Reply[])
	 */
	public void addReply(Reply reply) {
		
		synchronized (replyOutputQueue) {

			replyOutputQueue.add(reply);
			
			// notify waiting "getReply()" about the newly received message
			replyOutputQueue.notify();			
			
		}
	
	}
	
	
	/**
	 * Adds all the bypassed (already mixed) <code>Reply</code>ies to the 
	 * <code>replyOutputQueue</code> (from where they will be sent to their 
	 * destination).
	 * <p>
	 * Returns immediately (asynchronous behavior), the process of sending 
	 * itself may be deferred (e. g. if communication channel is busy).
	 * <p>
	 * Assures order (queuing strategy) and is thread-safe.
	 * <p>
	 * Used by component <code>OutputStrategy</code>.
	 * 
	 * @param replies 	Already processed messages, that shall be sent to the 
	 * 					next communication partner.
	 * 
	 * @see #getProcessedReply()
	 * @see #addReply(Reply)
	 */
	public void addReplies(Reply[] replies) {
		
		for (Reply reply: replies) {
			
			addReply(reply);
			
		}
		
	}
	

	/**
	 * Returns an (already mixed) <code>Reply</code> from the 
	 * <code>replyOutputQueue</code>. If no replies are available, this 
	 * method blocks until a new <code>Reply</code> arrives.
	 * <p>
	 * Assures order (queuing strategy) and is thread-safe.
	 * 
	 * @return 	An (already mixed) <code>Reply</code>.
	 */
	protected Reply getProcessedReply() {
		
		synchronized (replyOutputQueue) {
			
			Reply reply = null;
			
			// as long as the replyOutputQueue is empty
			while ((reply = replyOutputQueue.poll()) == null) {
					
				try {
						
					// wait for new messages in the replyOutputQueue
					// = wait for notification at "addReply()"
					replyOutputQueue.wait();
						
				} catch (InterruptedException e) {
						
					LOGGER.severe(e.getMessage());
					continue;
						
				}
					
			}
			
			LOGGER.fine(	"(MIX" +POSITION_OF_MIX_IN_CASCADE
							+" IOH) Putting out reply on channel "
							+reply.getChannelID()
							);

			return reply;

		}

	}
	

	/**
	 * Adds the bypassed (just received) <code>Reply</code> to the 
	 * <code>replyInputQueue</code> (from where it will be taken by 
	 * component <code>MessageProcessor</code> via <code>getReply()</code>).
	 * <p>
	 * Assures order (queuing strategy) and is thread-safe.
	 * 
	 * @param message 	Message just received, that shall be added.
	 */
	protected void addUnprocessedReply(ReplyMessage message) {
		
		synchronized (replyInputQueue) {
			
			replyInputQueue.add(message);
			
			// notify waiting "getReply()" about the newly received message
			replyInputQueue.notify();
	
		}
		
	}
	
	
	/**
	 * Returns a <code>Reply</code> (previously received, unprocessed) from a 
	 * communication partner (e. g. proxy or other mix). If no 
	 * <code>Reply</code>ies are available, this method blocks until a new 
	 * <code>Reply</code> arrives.
	 * <p>
	 * Assures order (queuing strategy) and is thread-safe.
	 * 
	 * @return 	A (previously received, unprocessed) <code>Reply</code>.
	 */
	public Reply getReply() {
		
		synchronized (replyInputQueue) {
			
			Reply reply = null;
			
			// as long as the replyInputQueue is empty
			while ((reply = (Reply) replyInputQueue.poll()) == null) {
					
				try {
						
					// wait for new messages in the replyInputQueue
					// = wait for notification at "addReply()"
					replyInputQueue.wait();
						
				} catch (InterruptedException e) {

					LOGGER.severe(e.getMessage());
					continue;
						
				}
					
			}
			
			return reply;

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
