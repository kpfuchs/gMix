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
package plugIns.layer2recodingScheme.sphinx_v0_001;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer2RecodingSchemeMix;
import framework.core.message.MixMessage;
import framework.core.message.Reply;
import framework.core.message.Request;
import framework.core.routing.RoutingMode;
import framework.core.userDatabase.User;


public class MixPlugIn extends Implementation implements Layer2RecodingSchemeMix {

	private Sphinx_Config config;
	private RequestThread[] requestThreads;
	private Sphinx messageCreator;
	
	
	@Override
	public void constructor() {
		if (anonNode.ROUTING_MODE == RoutingMode.FREE_ROUTE_DYNAMIC_ROUTING)
			throw new RuntimeException("not supported");
		this.config = new Sphinx_Config(anonNode, false);
		this.messageCreator = new Sphinx(anonNode, config);
		this.requestThreads = new RequestThread[config.NUMBER_OF_THREADS];
		//if (anonNode.IS_DUPLEX)
		//	replyThreads = new ReplyThread[config.NUMBER_OF_THREADS];
		for (int i=0; i<config.NUMBER_OF_THREADS; i++) {
			Sphinx recodingScheme = new Sphinx(anonNode, config);
			requestThreads[i] = new RequestThread(recodingScheme);
			//if (anonNode.IS_DUPLEX)
			//	replyThreads[i] = new ReplyThread(recodingScheme);
		}
	}

	
	@Override
	public void initialize() {
		config.loadPlubicKeysOfMixes(anonNode);
		/*if (!anonNode.IS_LAST_MIX) {
			config.publicKeysOfMixes = Arrays.copyOfRange(config.publicKeysOfMixes, (anonNode.PUBLIC_PSEUDONYM +1), config.publicKeysOfMixes.length); // TODO
			config.mixIdsSphinx = Arrays.copyOfRange(config.mixIdsSphinx, (anonNode.PUBLIC_PSEUDONYM +1), config.mixIdsSphinx.length); // TODO
		}
		config.NUMBER_OF_MIXES = config.publicKeysOfMixes.length;*/
		for (int i=0; i<requestThreads.length; i++) {
			requestThreads[i].recodingScheme.initAsRecoder();
		}
	}
	

	@Override
	public void begin() {
		for (int i=0; i<requestThreads.length; i++) {
			requestThreads[i].start();
			//if (anonNode.IS_DUPLEX)
			//	replyThreads[i].start();
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
		throw new RuntimeException("not supported"); // TODO
		/*Request request = MixMessage.getInstanceRequest(new byte[0], settings);
		request.setOwner(user);
		request.route = route;
		return request;*/
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
		/*Reply reply = MixMessage.getInstanceReply(new byte[0], settings);
		reply.setOwner(user);
		reply.route = route;
		return reply;*/
	}

	
	@Override
	public Reply generateDummyReply(User user) {
		throw new RuntimeException("not supported");
		/*Reply reply = MixMessage.getInstanceReply(new byte[0], settings);
		reply.setOwner(user);
		return reply;*/
	}

	
	class RequestThread extends Thread {
		
		Sphinx recodingScheme;
		
		public RequestThread(Sphinx recodingScheme) {
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

}
