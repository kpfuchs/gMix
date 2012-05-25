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

import java.util.Arrays;
import recodingScheme.Sphinx_v1.Config;
import message.Reply;
import message.Request;
import framework.SubImplementation;
import framework.Util;


public class Sphinx_v1_Recoder extends SubImplementation implements Recoder {

	private Config config;
	private Sphinx_v1 master;
	
	
	@Override
	public void constructor() {
		this.master = (Sphinx_v1)owner;
		this.config = master.getConfig();
	}

	
	@Override
	public void initialize() {
		
	}

	
	@Override
	public void begin() {
		
	}

	
	@Override
	public Request recodeMessage(Request message) {
		try {
			Sphinx_v1_EncryptedMessage msg = new Sphinx_v1_EncryptedMessage(message.getByteMessage(), config);
			// System.out.println("processing at " + new String(Hex.encode(_name)));
			if (msg.alpBetaGam[0].length != 32) {
				System.out.println("alpha is not an element of ECC group curve25519");
				return null;
			}
			byte[] sharedSecret = Sphinx_v1.genSharedSecret(msg.alpBetaGam[0], config.privateKey);
			byte[] tag = Sphinx_v1.hashTau(sharedSecret);

			if (config.PERFORM_REPLY_DETECTION) {
				if (config.replayDetection.isReplay(tag)) {
					System.out.println("the shared secret was already seen");
					return null;
				}
			}

			if (!Arrays.equals(msg.alpBetaGam[2], Sphinx_v1.mu(Sphinx_v1.hashMu(sharedSecret, config.SECURITY_PARAMETER_SIZE), msg.alpBetaGam[1], config.SECURITY_PARAMETER_SIZE))) {
				System.out.println("MAC mismatch");
				return null;
			}

			byte[] B = Sphinx_v1.xor(Util.concatArrays(msg.alpBetaGam[1], Sphinx_v1.ZERO32), Sphinx_v1.rho(Sphinx_v1.hashRho(sharedSecret, config.SECURITY_PARAMETER_SIZE), config.NUMBER_OF_MIXES, config.SECURITY_PARAMETER_SIZE));
			
			if (B[0] == Sphinx_v1_MixHeader.MIX_PREFIX) { // this mix is not the final hop -> forward message to next hop
				
				byte[] nextMixId = Arrays.copyOf(B, config.SECURITY_PARAMETER_SIZE);
				//byte[] remaining = Arrays.copyOfRange(B, Config._k, B.length);
				byte[] blindingFactor = Sphinx_v1.hashB(msg.alpBetaGam[0], sharedSecret);
				msg.alpBetaGam[0] = Sphinx_v1.genSharedSecret(msg.alpBetaGam[0], blindingFactor);
				msg.alpBetaGam[2] = Arrays.copyOfRange(B, config.SECURITY_PARAMETER_SIZE, 2 * config.SECURITY_PARAMETER_SIZE);
				msg.alpBetaGam[1] = Arrays.copyOfRange(B, config.SECURITY_PARAMETER_SIZE * 2, B.length);
				msg.delta = Sphinx_v1.pii(Sphinx_v1.hashPi(sharedSecret, config.SECURITY_PARAMETER_SIZE), msg.delta, config.SECURITY_PARAMETER_SIZE);
				//System.out.println("this mix is not the final hop -> forward message to next hop"); 
				message.nextHopAddress = config.getGlobalMixIdFor(nextMixId);
				message.setByteMessage(msg.toByteArray(config));
				return message;
			
			} else if (B[0] == Sphinx_v1_MixHeader.SPECIAL_DEST_PREFIX) { // this mix is the final hop; message is a request
				
				//System.out.println("this mix is the final hop; message is a request"); 
				msg.delta = Sphinx_v1.pii(Sphinx_v1.hashPi(sharedSecret, config.SECURITY_PARAMETER_SIZE), msg.delta, config.SECURITY_PARAMETER_SIZE);
				msg.delta = Sphinx_v1.unpadBody(msg.delta, config);
				byte[] payload = Arrays.copyOfRange(msg.delta, config.SECURITY_PARAMETER_SIZE, msg.delta.length);
				//System.out.println("payload received by last mix: " +Util.md5(payload)); 
				message.setByteMessage(payload);
				return message;
			
			} else if (B[0] == Sphinx_v1_MixHeader.CLIENT_PREFIX) { // this mix is the final hop; message is a reply
				
				//System.out.println("this mix is the final hop; message is a reply"); 
				byte[] addressData = Arrays.copyOf(B, config.SECURITY_PARAMETER_SIZE);
				byte[] clientId = Sphinx_v1.extractClientId(addressData, config);
				byte[] replyId = Sphinx_v1.extractReplyId(addressData, config);
				msg.delta = Sphinx_v1.pii(Sphinx_v1.hashPi(sharedSecret, config.SECURITY_PARAMETER_SIZE), msg.delta, config.SECURITY_PARAMETER_SIZE);
				message.nextHopAddress = infoService.getValue(new String(clientId, "UTF-8"));
				message.setByteMessage(Util.concatArrays(replyId, msg.delta));
				return message;
			
			} else {
				System.err.println("received unknown id"); 
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	
	}

	
	@Override
	public Reply recodeReply(Reply message) {
		throw new RuntimeException("not supported"); 
	}

}
