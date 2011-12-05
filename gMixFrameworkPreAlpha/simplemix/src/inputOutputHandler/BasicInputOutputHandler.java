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

import java.util.concurrent.ConcurrentLinkedQueue;
import framework.Implementation;
import message.Reply;
import message.Request;
import message.TestReply;
import message.TestRequest;
import architectureInterface.InputOutputHandlerInterface;


public class BasicInputOutputHandler extends Implementation implements InputOutputHandlerInterface {

	
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
	

	@Override
	public void constructor() {

	}
	
	
	@Override
	public boolean usesPropertyFile() {
		return false;
	}
	
	
	@Override
	public void initialize() {
		
	}

	@Override
	public void begin() {

		// TODO: reomve - for testing only
		simulateDistantProxy();
		simulateClient();

	}
	
	
	@Override
	public void addRequest(Request request) {
		
		synchronized (requestOutputQueue) {
			 
			requestOutputQueue.add(request);
			
			// notify waiting "getRequest()" about the newly received message
			requestOutputQueue.notify();			
			
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
	protected Request getProcessedRequest() {
		
		synchronized (requestOutputQueue) {
			
			Request request = null;
			
			while ((request = requestOutputQueue.poll()) == null) {
					
				try {
						
					// wait for new messages in the requestOutputQueue
					// = wait for notification at "addRequest()"
					requestOutputQueue.wait();
						
				} catch (InterruptedException e) {
					//controller.getInternalInformationPort...log	// TODO 
					continue;
						
				}
					
			}
	
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

					//controller.getInternalInformationPort...log	// TODO 
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
						
					//controller.getInternalInformationPort...log	// TODO 
					continue;
						
				}
					
			}
			
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
	protected void addUnprocessedReply(Reply message) {
		
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

					//controller.getInternalInformationPort...log	// TODO 
					continue;
						
				}
					
			}
			
			return reply;

		}
		
	}
	
	
	@Override
	public String[] getCompatibleImplementations() {
		return (new String[] {	"outputStrategy.BasicBatch",
								"outputStrategy.BasicPool",
								"inputOutputHandler.BasicInputOutputHandler",
								"keyGenerator.BasicKeyGenerator",
								"messageProcessor.BasicMessageProcessor",
								"externalInformationPort.BasicExternalInformationPort",
								"networkClock.BasicSystemTimeClock",
								"userDatabase.BasicUserDatabase",
								"message.BasicMessage"	
			});
	}
	
	
	// TODO: reomve - for testing only
	private void simulateDistantProxy() {
		
		// receive requests and send them back immediately (=echo)
		new Thread( // simple testroutine
				new Runnable() {
					public void run() {
						while (true) {
							getProcessedRequest();
							System.out.println("echo"); 
							addUnprocessedReply(new TestReply());
						}
					}
				}
			).start(); 
	
		
	}
	
	
	// TODO: reomve - for testing only
	private void simulateClient() {
		
		// send requests:
		new Thread( // simple testroutine
				new Runnable() {
					int sendingRate = 100; // ms
					public void run() {
						while (true) {
							try {Thread.sleep(sendingRate);} catch (InterruptedException e) {e.printStackTrace();}
							System.out.println("sending message"); 
							addUnprocessedRequest(new TestRequest());
						}
					}
				}
			).start(); 
		
		// receive replies:
		new Thread( // simple testroutine
				new Runnable() {
					public void run() {
						while (true) {
							getReply();
							System.out.println("message received"); 
						}
					}
				}
			).start(); 
		
	}

}
