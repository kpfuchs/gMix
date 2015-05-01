/*******************************************************************************
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2014  SVS
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
 *******************************************************************************/
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer3outputStrategy.waitForReply_v0_001;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer1NetworkClient;
import staticContent.framework.interfaces.Layer2RecodingSchemeClient;
import staticContent.framework.interfaces.Layer3OutputStrategyClient;
import staticContent.framework.interfaces.Layer4TransportClient;
import staticContent.framework.message.MixMessage;
import staticContent.framework.message.Reply;
import staticContent.framework.message.Request;
import staticContent.framework.message.ExternalMessage.DummyStatus;
import staticContent.framework.routing.MixList;
import staticContent.framework.routing.RoutingMode;
import staticContent.framework.util.Util;


public class ClientPlugIn extends Implementation implements Layer3OutputStrategyClient {

	private Layer1NetworkClient layer1;
	private Layer2RecodingSchemeClient layer2;
	private AtomicBoolean isStopped = new AtomicBoolean(false);
	private ArrayBlockingQueue<Request> requestQueue;
	private ArrayBlockingQueue<byte[]> replyQueue;
	private RequestReplyThread requestReplyThread;
	private long delay;
	private int availableReplyPayload = 0;
	private int availableReplies = 0;
	private Object replySynchronizer = new Object();
	private MixList route;
	private int dstPseudonym = Util.NOT_SET;
	
	
	// TODO: write new mix-side-plug-in, that generates replies on the last mix independent from layer 4 (i.e. independent from layer-4-write-operations)
	@Override
	public void constructor() {
		this.requestQueue = new ArrayBlockingQueue<Request>(settings.getPropertyAsInt("WAIT_FOR_REPLY_REQUEST_QUEUE_SIZE"));
		this.replyQueue = new ArrayBlockingQueue<byte[]>(settings.getPropertyAsInt("WAIT_FOR_REPLY_REPLY_QUEUE_SIZE"));
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
			Layer3OutputStrategyClient layer3,
			Layer4TransportClient layer4
			) {
		//this.layer4 = layer4;
		assert this == layer3;
		this.layer2 = layer2;
		this.layer1 = layer1;
	}
	
	
	@Override
	public void connect() {
		this.isStopped.set(false);
		if (anonNode.ROUTING_MODE == RoutingMode.SOURCE_ROUTING)
			this.route = sourceRoutingPlugInClient.choseRoute();
		this.layer1.connect();
		this.requestReplyThread = new RequestReplyThread();
		this.requestReplyThread.start();
	}

	
	@Override
	public void connect(int destPseudonym) {
		this.isStopped.set(false);
		if (anonNode.ROUTING_MODE == RoutingMode.SOURCE_ROUTING) {
			this.route = sourceRoutingPlugInClient.choseRoute(destPseudonym);
			if (anonNode.DISPLAY_ROUTE_INFO)
				System.out.println(""+anonNode +" generated random route: " +this.route); 
			this.layer1.connect(this.route);
			this.requestReplyThread = new RequestReplyThread();
			this.requestReplyThread.start();
		} else {
			this.layer1.connect();
			this.requestReplyThread = new RequestReplyThread();
			this.requestReplyThread.start();
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
	
	
	private void forcePutInQueue(byte[] replyBlock) {
		try {
			replyQueue.put(replyBlock);
		} catch (InterruptedException e) {
			e.printStackTrace();
			forcePutInQueue(replyBlock);
		}
	}
	
	
	@Override
	public void write(byte[] data) {
		if (data == null || data.length == 0)
			throw new RuntimeException("write(null) and write(byte[0]) are not allowed");
		Request request = generateRequest(data); // TODO: just store data as byte[] and create mix messages later (as in other plug-ins)
		forcePutInQueue(request);
	}
	
	
	private Request generateRequest(byte[] data) {
		Request request = MixMessage.getInstanceRequest(data);
		if (anonNode.ROUTING_MODE == RoutingMode.SOURCE_ROUTING) {
			if (!anonNode.IS_CONNECTION_BASED) { // new route for every message
				if (this.dstPseudonym == Util.NOT_SET)
					this.route = sourceRoutingPlugInClient.choseRoute();
				else
					this.route = sourceRoutingPlugInClient.choseRoute(request.destinationPseudonym);
			} 
			request.destinationPseudonym = this.route.mixIDs[route.mixIDs.length-1];
			request.nextHopAddress = this.route.mixIDs[0];
			request.route = this.route.mixIDs;
		}
		request = layer2.applyLayeredEncryption(request);
		return request;
	}


	@Override
	public void write(byte[] data, int destPseudonym) {
		this.dstPseudonym = destPseudonym;
		write(data);
	}
	
	
	@Override
	public byte[] receive() {
		byte[] reply;
		try {
			reply = replyQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return receive();
		}
		synchronized (replySynchronizer) { // lazy-set information about available data
			availableReplyPayload -= reply.length;
			availableReplies--;
		}
		return reply;
	}


	@Override
	public int getMaxSizeOfNextWrite() {
		return layer2.getMaxPayloadForNextMessage();
	}


	@Override
	public int getMaxSizeOfNextReceive() {
		return layer2.getMaxPayloadForNextReply();
	}
	
	
	private class RequestReplyThread extends Thread {
		
		@Override
		public void run() {
			while (true) {
				// send request
				if (requestQueue.size() == 0) { // send dummy
					Request dummy = generateRequest(null);
					layer1.sendMessage(dummy); 
				} else {
					try {
						layer1.sendMessage(requestQueue.take());
					} catch (InterruptedException e) {
						e.printStackTrace();
						continue;
					}
				}
				Reply reply = layer1.receiveReply();
				reply = layer2.extractPayload(reply);
				assert reply.getDummyStatus() != DummyStatus.UNKNOWN;
				if (reply.getDummyStatus() != DummyStatus.DUMMY) {
					assert reply.getByteMessage() != null && reply.getByteMessage().length != 0;
					forcePutInQueue(reply.getByteMessage());
					synchronized (replySynchronizer) { // lazy-set information about available data
						availableReplyPayload += reply.getByteMessage().length;
						availableReplies++;
					}
				}
				
				if (isStopped.get() == true)
					break;
				
				if (reply.getDummyStatus() != DummyStatus.DUMMY) {
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
	}


	@Override
	public int availableReplies() {
		synchronized (replySynchronizer) {
			return availableReplies;
		}
	}


	@Override
	public int availableReplyData() {
		synchronized (replySynchronizer) {
			return availableReplyPayload;
		}
	}
	
}
