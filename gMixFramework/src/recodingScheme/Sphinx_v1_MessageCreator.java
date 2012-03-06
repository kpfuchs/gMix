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

package recodingScheme;


import java.security.SecureRandom;
import java.util.Arrays;

import recodingScheme.Sphinx_v1.Config;
import message.MixMessage;
import message.Reply;
import message.Request;
import userDatabase.User;
import framework.ClientImplementation;
import framework.SubImplementation;
import framework.Util;


public class Sphinx_v1_MessageCreator extends SubImplementation implements MessageCreator {

	private Config config;
	private Sphinx_v1 master;
	private SecureRandom secureRandom;
	
	
	@Override
	public void constructor() {
		if (mix != null) {
			this.master = (Sphinx_v1)owner;
			this.config = master.getConfig();
		} else {
			Sphinx_v1 c = new Sphinx_v1(settings, infoService, client);
			config = c.new Config();
		}
		try {
			this.secureRandom = SecureRandom.getInstance(config.PRNG_ALGORITHM);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	@Override
	public void setOwner(ClientImplementation owner) {
		super.setOwner(owner);
	}
	
	
	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}

	
	@Override
	public void begin() {
		// TODO Auto-generated method stub
		
	}

	
	@Override
	public Request createMessage(byte[] payload) {
		return createMessage(payload, null);
	}

	
	@Override
	public Request createMessage(byte[] payload, User owner) {
		if (payload == null) {
			payload = new byte[0];
			System.out.println(mix +" creating dummy");
			System.out.println(mix +" config.numberOfMixes: " +config.NUMBER_OF_MIXES); 
		}
		if (payload.length > config.MAX_PAYLOAD_SIZE)
			throw new RuntimeException("can't send more than " +config.MAX_PAYLOAD_SIZE +" bytes in one message"); 
		
		Sphinx_v1_EncryptedMessage em = new Sphinx_v1_EncryptedMessage();
		try {
			Sphinx_v1_MixHeader header = new Sphinx_v1_MixHeader(config, secureRandom);
			byte[] addressForLastMix = new byte[config.SECURITY_PARAMETER_SIZE];
			secureRandom.nextBytes(addressForLastMix);
			addressForLastMix[0] = Sphinx_v1_MixHeader.SPECIAL_DEST_PREFIX;
			em.alpBetaGam = header.createHeader(addressForLastMix);

			byte[] body = Util.concatArrays(Sphinx_v1.ZERO16, payload);
			body = Sphinx_v1.padBody(body, config);
			assert body.length == config.DELTA_SIZE;
			
			byte[][] deltas = new byte[config.NUMBER_OF_MIXES][];
			
			// delta v-1
			byte[] delta = Sphinx_v1.pi(Sphinx_v1.hashPi(header.getSecret(config.NUMBER_OF_MIXES-1), config.SECURITY_PARAMETER_SIZE), body, config.SECURITY_PARAMETER_SIZE);
			deltas[config.NUMBER_OF_MIXES-1] = delta;
			
			// deltas for 0<=i<v-1
			for (int i=config.NUMBER_OF_MIXES-2; i>=0; i--) {
				delta = Sphinx_v1.pi(Sphinx_v1.hashPi(header.getSecret(i), config.SECURITY_PARAMETER_SIZE), delta, config.SECURITY_PARAMETER_SIZE);
				deltas[i] = delta;
			}
			
			em.delta = deltas[0];
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (owner != null)
			return MixMessage.getInstanceRequest(em.toByteArray(config), owner, settings);
		else
			return MixMessage.getInstanceRequest(em.toByteArray(config), settings);

	}

	
	@Override
	public int getMaxPayloadForNextMessage() {
		return config.MAX_PAYLOAD_SIZE;
	}
	

	@Override
	public int getMaxPayloadForNextReply(User user) {
		return config.MAX_PAYLOAD_SIZE;
	}

	
	private Sphinx_v1_ReplyData createReplyData(byte[] replyId) throws Exception {
		Sphinx_v1_ReplyData replyData = new Sphinx_v1_ReplyData();
		replyData.replyId = replyId;

		Sphinx_v1_MixHeader header = new Sphinx_v1_MixHeader(config, secureRandom);
		replyData.alpBetaGam = header.createHeader(Sphinx_v1.generateReplyAddress(config.id, replyId, config));
		replyData.ktilde =  new byte[config.SECURITY_PARAMETER_SIZE];
		secureRandom.nextBytes(replyData.ktilde);
		replyData.keytuples = new byte[config.NUMBER_OF_MIXES][];
		for (int i=0; i<config.NUMBER_OF_MIXES; i++) {
			try {
				replyData.keytuples[i] = Sphinx_v1.hashPi(header.getSecret(i), config.SECURITY_PARAMETER_SIZE);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		replyData.idMix0 = config.mixIdsSphinx[0];
		return replyData;
	}

	
	// "nym" is the pseudonym, the reply data is accessible under on the nymserver
	public void publishNym(byte[] nym) {
		try {
			Sphinx_v1_ReplyData replyData = createReplyData(Sphinx_v1.generateReplyID(config));
			config.replyDataTable.put(new String(replyData.replyId, "UTF-8"), replyData);
			byte[] idMix0 = replyData.idMix0;
			byte[][] alpBetaGam = replyData.alpBetaGam;
			byte[] ktilde = replyData.ktilde;
			infoService.postValue(new String(nym, "UTF-8"), Util.concatArrays(new byte[][] {idMix0, alpBetaGam[0], alpBetaGam[1], alpBetaGam[2], ktilde}));  // for testing; this should be done via an anonymous channel of course...
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	@Override
	public byte[] extractPayload(Reply reply) {
		byte[] replyId = Arrays.copyOfRange(reply.getByteMessage(), 0, 8);
		byte[] message = Arrays.copyOfRange(reply.getByteMessage(), 8, reply.getByteMessage().length);
		Sphinx_v1_ReplyData replyData = config.replyDataTable.get(replyId);
		if (replyData == null) {
			System.out.println("Unreadable reply message received");
			return null;
		} else {
			try {
				for (int i=replyData.keytuples.length-1; i>=0; i--) {
					message = Sphinx_v1.pi(replyData.keytuples[i], message, config.SECURITY_PARAMETER_SIZE);
				}
				message = Sphinx_v1.pii(replyData.ktilde, message, config.SECURITY_PARAMETER_SIZE);
				message = Sphinx_v1.unpadBody(message, config);
				byte[] payload = Arrays.copyOfRange(message, config.SECURITY_PARAMETER_SIZE, message.length);
				if (Arrays.equals(Arrays.copyOf(message, config.SECURITY_PARAMETER_SIZE), Sphinx_v1.ZERO16)) {
					//System.out.println(new String(payload) + " received by " + Util.md5(clientId));
					return payload;
				} else {
					System.err.println("Corrupted message received by");
					return null;
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("could not decrypt reply"); 
			}
		}
	}

	
	@Override
	public Reply createReply(byte[] payload, User owner) {
		throw new RuntimeException("not supported"); 
	}

}
