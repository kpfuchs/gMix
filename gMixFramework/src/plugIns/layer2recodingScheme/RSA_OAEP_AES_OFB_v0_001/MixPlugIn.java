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
package plugIns.layer2recodingScheme.RSA_OAEP_AES_OFB_v0_001;

import java.util.Arrays;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer2RecodingSchemeMix;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.userDatabase.User;


public class MixPlugIn extends Implementation implements Layer2RecodingSchemeMix {

	private RSA_OAEP_AES_OFB_Config config;
	private RequestThread[] requestThreads;
	private ReplyThread[] replyThreads;
	private RSA_OAEP_AES_OFB messageCreator;
	
	
	@Override
	public void constructor() {
		
		this.config = new RSA_OAEP_AES_OFB_Config(anonNode, false);
		this.messageCreator = new RSA_OAEP_AES_OFB(anonNode, config);
		this.requestThreads = new RequestThread[config.NUMBER_OF_THREADS];
		if (anonNode.IS_DUPLEX)
			replyThreads = new ReplyThread[config.NUMBER_OF_THREADS];
		for (int i=0; i<config.NUMBER_OF_THREADS; i++) {
			RSA_OAEP_AES_OFB recodingScheme = new RSA_OAEP_AES_OFB(anonNode, config);
			requestThreads[i] = new RequestThread(recodingScheme);
			if (anonNode.IS_DUPLEX)
				replyThreads[i] = new ReplyThread(recodingScheme);
		}
	}

	
	@Override
	public void initialize() {
		config.loadPlubicKeysOfOtherMixes();
		if (!anonNode.IS_LAST_MIX) {
			config.publicKeysOfMixes = Arrays.copyOfRange(config.publicKeysOfMixes, (anonNode.PUBLIC_PSEUDONYM +1), config.publicKeysOfMixes.length); // TODO
		}
		config.numberOfMixes = config.publicKeysOfMixes.length;
		for (int i=0; i<requestThreads.length; i++) {
			requestThreads[i].recodingScheme.initAsRecoder();
			if (anonNode.IS_DUPLEX)
				replyThreads[i].recodingScheme.initAsRecoder();
		}
	}
	

	@Override
	public void begin() {
		for (int i=0; i<requestThreads.length; i++) {
			requestThreads[i].start();
			if (anonNode.IS_DUPLEX)
				replyThreads[i].start();
		}
	}

	
	@Override
	public int getMaxSizeOfNextReply() {
		return anonNode.MAX_PAYLOAD;
	}

	
	@Override
	public int getMaxSizeOfNextRequest() {
		return anonNode.MAX_PAYLOAD;
	}

	
	@Override
	public Request generateDummy(int[] route, User user) {
		throw new RuntimeException("not supported");
	}

	
	@Override
	public Request generateDummy(User user) {
		if (anonNode.IS_LAST_MIX)
			return null;
		Request request = MixMessage.getInstanceRequest(new byte[0]);
		request.setOwner(user);
		request = messageCreator.applyLayeredEncryption(request);
		return request;
	}
	

	@Override
	public Reply generateDummyReply(int[] route, User user) {
		throw new RuntimeException("not supported");
	}

	
	@Override
	public Reply generateDummyReply(User user) {
		throw new RuntimeException("not supported");
	}

	
	class RequestThread extends Thread {
		
		RSA_OAEP_AES_OFB recodingScheme;
		
		public RequestThread(RSA_OAEP_AES_OFB recodingScheme) {
			this.recodingScheme = recodingScheme;
		}

		
		@Override
		public void run() {
			while (true) { // process messages
				Request[] requests = anonNode.getFromRequestInputQueue();
				for (int i=0; i<requests.length; i++) {
					requests[i] = recodingScheme.recodeMessage(requests[i]);
					if (requests[i] != null)
						outputStrategyLayerMix.addRequest(requests[i]);
				}
			}	
		}
	}
	
	
	class ReplyThread extends Thread {
		
		RSA_OAEP_AES_OFB recodingScheme;
		
		public ReplyThread(RSA_OAEP_AES_OFB recodingScheme) {
			this.recodingScheme = recodingScheme;
		}
		
		
		@Override
		public void run() {
			while (true) { // process messages
				Reply[] replies = anonNode.getFromReplyInputQueue();
				for (Reply reply:replies) {
					reply = recodingScheme.recodeReply(reply);
					if (reply != null)
						outputStrategyLayerMix.addReply(reply);
				}
			}
		}
	}
}
