/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2012  Karl-Peter Fuchs
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
package plugIns.layer3outputStrategy.waitForReply_v0_001;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer1NetworkClient;
import framework.core.interfaces.Layer2RecodingSchemeClient;
import framework.core.interfaces.Layer3OutputStrategyClient;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.routing.MixList;
import framework.core.routing.RoutingMode;
import framework.core.util.Util;


public class ClientPlugIn extends Implementation implements Layer3OutputStrategyClient {

	private Layer1NetworkClient layer1;
	private Layer2RecodingSchemeClient layer2;
	private AtomicBoolean isStopped = new AtomicBoolean(false);
	private ArrayBlockingQueue<Request> requestQueue;
	private ArrayBlockingQueue<Reply> replyQueue;
	private RequestReplyThread requestReplyThread;
	private long delay;
	private int availableReplyPayload = 0;
	private int availableReplies = 0;
	private Object replySynchronizer = new Object();
	private MixList route;
	
	
	@Override
	public void constructor() {
		this.requestQueue = new ArrayBlockingQueue<Request>(settings.getPropertyAsInt("WAIT_FOR_REPLY_REQUEST_QUEUE_SIZE"));
		this.replyQueue = new ArrayBlockingQueue<Reply>(settings.getPropertyAsInt("WAIT_FOR_REPLY_REPLY_QUEUE_SIZE"));
		this.delay = TimeUnit.MICROSECONDS.toNanos(settings.getPropertyAsLong("WAIT_FOR_REPLY_DELAY"));
	}

	
	@Override
	public void initialize() {
		
	}

	
	@Override
	public void begin() {
		
	}

	
	@Override
	public void setReferences(
			Layer1NetworkClient layer1,
			Layer2RecodingSchemeClient layer2, 
			Layer3OutputStrategyClient layer3) {
		this.layer2 = layer2;
		this.layer1 = layer1;
		assert this == layer3;
	}
	
	
	@Override
	public void connect() {
		this.isStopped.set(false);
		if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING)
			this.route = anonNode.mixList.getRandomRoute(anonNode.FREE_ROUTE_LENGTH);
		this.requestReplyThread = new RequestReplyThread();
		this.requestReplyThread.start();
		this.layer1.connect();
	}

	
	@Override
	public void connect(int destPseudonym) {
		this.isStopped.set(false);
		if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) {
			this.route = anonNode.mixList.getRandomRoute(anonNode.FREE_ROUTE_LENGTH, destPseudonym);
			if (anonNode.DISPLAY_ROUTE_INFO)
				System.out.println(""+anonNode +" generated random route: " +this.route); 
			this.requestReplyThread = new RequestReplyThread();
			this.requestReplyThread.start();
			this.layer1.connect(this.route);
		} else {
			this.requestReplyThread = new RequestReplyThread();
			this.requestReplyThread.start();
			this.layer1.connect();
		}
	}
	
	
	@Override
	public void disconnect() {
		this.isStopped.set(true);
		this.layer1.disconnect();
	}

	
	private void forcePutInQueue(Request request) {
		try {
			requestQueue.put(request);
		} catch (InterruptedException e) {
			e.printStackTrace();
			forcePutInQueue(request);
		}
	}
	
	
	private void forcePutInQueue(Reply reply) {
		try {
			replyQueue.put(reply);
		} catch (InterruptedException e) {
			e.printStackTrace();
			forcePutInQueue(reply);
		}
	}
	
	
	@Override
	public void sendMessage(Request request) {
		if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_SOURCE_ROUTING) {
			if (!anonNode.IS_CONNECTION_BASED) { // new route for every message
				if (request.destinationPseudonym == Util.NOT_SET)
					this.route = anonNode.mixList.getRandomRoute(anonNode.FREE_ROUTE_LENGTH);
				else
					this.route = anonNode.mixList.getRandomRoute(anonNode.FREE_ROUTE_LENGTH, request.destinationPseudonym);
			} 
			request.destinationPseudonym = this.route.mixIDs[route.mixIDs.length-1];
			request.nextHopAddress = this.route.mixIDs[0];
			request.route = this.route.mixIDs;
		}
		request = layer2.applyLayeredEncryption(request);
		forcePutInQueue(request);
	}
	
	
	@Override
	public Reply receiveReply() {
		Reply reply;
		try {
			reply = replyQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return receiveReply();
		}
		synchronized (replySynchronizer) { // lazy-set information about available data
			availableReplyPayload -= reply.getByteMessage().length;
			availableReplies--;
		}
		return reply;
	}


	@Override
	public int getMaxSizeOfNextRequest() {
		return layer2.getMaxPayloadForNextMessage();
	}


	@Override
	public int getMaxSizeOfNextReply() {
		return layer2.getMaxPayloadForNextReply();
	}
	
	
	private class RequestReplyThread extends Thread {
		
		@Override
		public void run() {
			while (true) {
				// send request
				if (requestQueue.size() == 0) { // send dummy
					Request dummy = MixMessage.getInstanceRequest(new byte[0]);
					sendMessage(dummy);
				} else {
					try {
						sendMessage(requestQueue.take());
					} catch (InterruptedException e) {
						e.printStackTrace();
						continue;
					}
				}
				
				Reply reply = layer1.receiveReply();
				reply = layer2.extractPayload(reply);
				forcePutInQueue(reply);
				synchronized (replySynchronizer) { // lazy-set information about available data
					availableReplyPayload += reply.getByteMessage().length;
					availableReplies++;
				}
				
				if (isStopped.get() == true)
					break;
				
				// give layer 5 time to answer the last reply
				if (delay > 0)
					try {
						Util.sleepNanos(delay);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
			}
		}
	}


	@Override
	public int availableReplies() {
		synchronized (replySynchronizer) {
			return availableReplies;
		}
	}


	@Override
	public int availableReplyPayload() {
		synchronized (replySynchronizer) {
			return availableReplyPayload;
		}
	}
	
}
