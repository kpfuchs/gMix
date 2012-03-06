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

import inputOutputHandler.communicationHandler.CommunicationHandlerManager;

import java.util.concurrent.LinkedBlockingQueue;

import framework.Implementation;
import framework.LocalClassLoader;
import message.Reply;
import message.Request;


public class BasicInputOutputHandler extends Implementation implements InputOutputHandler, InputOutputHandlerInternal {

	private LinkedBlockingQueue<Request> requestInputQueue;
	private LinkedBlockingQueue<Request> requestOutputQueue;
	private LinkedBlockingQueue<Reply> replyInputQueue;
	private LinkedBlockingQueue<Reply> replyOutputQueue;
	
	
	@Override
	public void constructor() {
		int maxMessagesInQueue = settings.getPropertyAsInt("MAX_MESSAGES_IN_QUEUE");
		this.requestInputQueue = new LinkedBlockingQueue<Request>(maxMessagesInQueue);
		this.requestOutputQueue = new LinkedBlockingQueue<Request>(maxMessagesInQueue);
		this.replyInputQueue = new LinkedBlockingQueue<Reply>(maxMessagesInQueue);
		this.replyOutputQueue = new LinkedBlockingQueue<Reply>(maxMessagesInQueue);
		LocalClassLoader.instantiateSubImplementation(
					this.getClass().getPackage().getName() + ".communicationHandler", 
					settings.getProperty("COMMUNICATION_HANDLER_MANAGER"), 
					this, 
					CommunicationHandlerManager.class
					);
	}
	
	
	@Override
	public void initialize() {
		
	}

	
	@Override
	public void begin() {
	}
	
	
	@Override
	public void addRequest(Request request) {
		assert request != null;
		try {
			requestOutputQueue.put(request);
		} catch (InterruptedException e) {
			e.printStackTrace();
			addRequest(request);
		}
	}

	
	@Override
	public void addRequests(Request[] requests) {
		for (Request request: requests)
			addRequest(request);
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
	@Override
	public Request getProcessedRequest() {	
		while (true) {
			try {
				return requestOutputQueue.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
				continue;
			}
		}
	}
	
	
	@Override
	public Request peekProcessedRequest() {
		return requestOutputQueue.peek();
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
	@Override
	public void addUnprocessedRequest(Request request) {
		assert request != null;
		try {
			requestInputQueue.put(request);
		} catch (InterruptedException e) {
			e.printStackTrace();
			addUnprocessedRequest(request);
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
	@Override
	public Request getRequest() {
		while (true) {
			try {
				return requestInputQueue.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
				continue;
			}
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
	@Override
	public void addReply(Reply reply) { 
		assert reply != null;
		assert reply.getByteMessage() != null; // TODO remove
		try {
			replyOutputQueue.put(reply);
		} catch (InterruptedException e) {
			e.printStackTrace();
			addReply(reply);
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
	@Override
	public void addReplies(Reply[] replies) {
		for (Reply reply: replies)
			addReply(reply);
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
	@Override
	public Reply getProcessedReply() {
		while (true) {
			try {
				return replyOutputQueue.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
				continue;
			}
		}
	}
	

	@Override
	public Reply peekProcessedReply() {
		return replyOutputQueue.peek();
	}
	
	
	/**
	 * Adds the bypassed (just received) <code>Reply</code> to the 
	 * <code>replyInputQueue</code> (from where it will be taken by 
	 * component <code>MessageProcessor</code> via <code>getReply()</code>).
	 * <p>
	 * Assures order (queuing strategy) and is thread-safe.
	 * 
	 * @param message 	MixMessage just received, that shall be added.
	 */
	@Override
	public void addUnprocessedReply(Reply reply) {
		assert reply != null;
		assert reply.getByteMessage() != null; // TODO remove
		try {
			replyInputQueue.put(reply);
		} catch (InterruptedException e) {
			e.printStackTrace();
			addUnprocessedReply(reply);
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
	@Override
	public Reply getReply() {
		while (true) {
			try {
				return replyInputQueue.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
				continue;
			}
		}
	}
	
	
	// TODO: reomve - for testing only
	/*private void simulateDistantProxy() {
		
		// TODO:
		// receive requests and send them back immediately (=echo)
		/*new Thread( // simple testroutine
				new Runnable() {
					public void run() {
						while (true) {
							getProcessedRequest();
							System.out.println("echo"); 
							addUnprocessedReply(new Reply());
						}
					}
				}
			).start(); 
	
		
		
	}
	
	
	// TODO: remove - for testing only
	private void simulateClient() {
		
		//int clients = 10;
		
		// send requests:
		/*for (int i=0; i<clients; i++)
			new Thread( // simple test routine
				new Runnable() {
					int sendingrate = 100; //ms
					Client c = new Client();
					public void run() {
						while (true) {
							try {Thread.sleep(sendingrate);} catch (InterruptedException e) {e.printStackTrace();}
							System.out.println("sending message"); 
							addUnprocessedRequest(MixMessage.getInstanceRequest(c.generateMessage()));
						}
					}
				}
			).start(); 
		*/
		
		// TODO:
		// receive replies:
		/*for (int i=0; i<clients; i++)
			new Thread( // simple test routine
				new Runnable() {
					public void run() {
						while (true) {
							getReply();
							System.out.println("message received"); 
						}
					}
				}
			).start(); 
		
	}*/

	
	@Override
	public int remainingUnprocessedRequestCapacity() {
		return requestInputQueue.remainingCapacity();
	}


	@Override
	public int remainingUnprocessedReplyCapacity() {
		return replyInputQueue.remainingCapacity();
	}

}
