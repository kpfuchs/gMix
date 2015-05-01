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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer2recodingScheme.encDNS_v0_001;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

import staticContent.framework.EncDnsClient;
import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer1NetworkClient;
import staticContent.framework.interfaces.Layer2RecodingSchemeClient;
import staticContent.framework.interfaces.Layer3OutputStrategyClient;
import staticContent.framework.interfaces.Layer4TransportClient;
import staticContent.framework.message.Reply;
import staticContent.framework.message.Request;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.encDNS_v0_001.EncDnsReply;


public class ClientPlugIn extends Implementation implements Layer2RecodingSchemeClient {


	@Override
	public void constructor() {
		// nothing to do here
	}
	

	@Override
	public void initialize() {
		// nothing to do here
	}
	

	@Override
	public void begin() {
		// nothing to do here
	}
	
	
	@Override
	public void setReferences(
			Layer1NetworkClient layer1,
			Layer2RecodingSchemeClient layer2, 
			Layer3OutputStrategyClient layer3,
			Layer4TransportClient layer4) {
		assert layer2 == this;
	}
	

	@Override
	public Request applyLayeredEncryption(Request request) {
		byte[] encMsg;
        if(EncDnsClient.encryption) {
            // If encryption is enabled, encrypt message
             encMsg = encryptQuery(request.getByteMessage());
        } else {
            // Else just use the unencrypted message (copy)
            encMsg = Arrays.copyOf(request.getByteMessage(), request.getByteMessage().length);
        }
        if (encMsg == null) {
            if(EncDnsClient.verbosity >= 1) 
                System.out.println("Received empty response");
            return null;
        }
        request.setByteMessage(encMsg);
        return request;
	}

	
    /**
     * Encrypts a standard DNS query.
     * @param rcvDNS standard DNS query
     * @return encrypted EncDNS message containing the standard DNS query
     */
    private byte[] encryptQuery(byte[] rcvDNS) {
        // Generate client nonce: 8 byte timer+4 byte random value
        byte[] n = new byte[EncDnsClient.libSodium.NONCEBYTES];
        ByteBuffer timeBuffer = ByteBuffer.allocate(8);
        timeBuffer.putLong(System.nanoTime());
        byte[] time = timeBuffer.array();
        byte[] random = new byte[(EncDnsClient.libSodium.NONCEBYTES/2)-time.length];
        EncDnsClient.rand.nextBytes(random);
        System.arraycopy(time, 0, n, 0, time.length);
        System.arraycopy(random, 0, n, time.length, random.length);
        
        long ctime = System.currentTimeMillis();
        byte[] cbox = EncDnsClient.libSodium.makeCryptoBoxAfternm(rcvDNS, EncDnsClient.k, n);
        if(EncDnsClient.verbosity >= 1)
            System.out.println("query encryption time " + (System.currentTimeMillis()-ctime) + " ms");
        
        byte[] encQuery = buildEncQuery(cbox, n);
        return encQuery;
    }
    
    /**
     * Constructs an EncDNS message containing a specified cryptobox.
     * @param cbox cryptobox to include in message
     * @param nonce nonce used for encryption
     * @return EncDNS message containing the specified cryptobox and nonce
     */
    private byte[] buildEncQuery(byte[] cbox, byte[] nonce) {
        // TODO This uses 65 kB of memory even if the resultig query is a lot
        // smaller ... :-(
        ByteBuffer buf = ByteBuffer.allocate(EncDnsClient.MAX_MSG_SIZE);

        // ------ CBOX IN HOSTNAME ------
        // --- DNS HEADER ---
        // randomized ID
        byte[] id = new byte[2];
        EncDnsClient.rand.nextBytes(id);
        buf.put(id);
        buf.put((byte) 1); // QR=0 (Query), OpCode=0000 (Query), AA=0, TC=0, RD=1
        buf.put((byte) 0); // RA,Z,AD,CD,RCode=0
        buf.putShort((short) 1); // question count = 1
        buf.putShort((short) 0); // answer count = 0
        buf.putShort((short) 0); // authority count = 0
        buf.putShort((short) 1); // additional count = 1

        // --- DNS QUESTION SECTION ---
        // cbox must be split into labels of length<=63
        int cpos = 0;
        // We have to transmit: magic string, the client's public key, the client's part of the nonce and the cryptobox itself
        byte[] cryptoStuff = ArrayUtils.addAll(EncDnsClient.magicString, ArrayUtils.addAll(ArrayUtils.addAll(EncDnsClient.pk, Arrays.copyOfRange(nonce, 0, (EncDnsClient.libSodium.NONCEBYTES/2))), cbox));
        while (true) {
            if ((cpos + 63) < cryptoStuff.length) {
                // remaining part of cbox > 63 byte -> add first 63 bytes as label
                buf.put((byte)63);
                buf.put(cryptoStuff, cpos, 63);
                cpos += 63;
            } else {
                // remaining part of cbox <= 63 byte -> add as last label
                short remainingBytes = (short) (cryptoStuff.length - cpos);
                buf.put((byte)remainingBytes);
                buf.put(cryptoStuff, cpos, remainingBytes);
                break;
            }
        }

        buf.put(EncDnsClient.remoteNS); // put the remote recursive nameserver's EncDNS zone name at the end of the question name
        buf.putShort((short) 16); // TYPE=TXT(16)
        buf.putShort((short) 1); // CLASS=IN(1)

        // --- DNS ADDITIONAL SECTION - EDNS0 OPT PSEUDO-RR ---
        buf.put((byte) 0); // Domain name = root
        buf.putShort((short) 41); // RR TYPE=OPT(41)
        buf.putShort((short) EncDnsClient.MAX_MSG_SIZE); // sender max UDP payload
        buf.put((byte) 0); // RCode extension=0
        buf.put((byte) 0); // EDNS version=0
        buf.put((byte) 0); // DNSSEC OK=0, Z=0
        buf.put((byte) 0); // Z=0
        buf.putShort((short) 0); // RDATA length

        // convert to byte[]
        byte[] out = new byte[buf.position()];
        buf.position(0);
        buf.get(out);

        return out;
    }
    
	@Override
	public Reply extractPayload(Reply replyMessage) {
		EncDnsReply reply = (EncDnsReply)replyMessage;
        byte[] ansReply = reply.getByteMessage();
        byte[] decReply;

        if (ansReply != null) {
            byte[] qid = reply.id;
            byte[] rid = Arrays.copyOfRange(ansReply, 0, 2);
            // Compare query and response ID
            if (Arrays.equals(qid, rid)) {
                if(EncDnsClient.encryption) {
                    // If encryption enabled, we will have to decrypt the response
                     decReply = decryptResponse(ansReply);
                } else {
                    // Otherwise we can just use the response as is
                    decReply = Arrays.copyOf(ansReply, ansReply.length);
                }
            } else {
                // If the received message is not an EncDNS message or if it
                // does not match the query, a server failure will be 
                // reported to the stub resolver and the port is closed.
                // The correct response might still be received later, but 
                // keeping the port open would allow an attacker to try 
                // multiple forged responses for one query whereas closing
                // the port only gives him a single chance at the cost of
                // having to re-query if a stray package arrived at the port.
                decReply = EncDNSHelper.generateServfail(reply.id);
            }

            if (decReply == null) {
                // If the reply did not contain a decryptable answer,
                // generate a SERVFAIL message to send to the stub
                decReply = EncDNSHelper.generateServfail(reply.id);
            }
        } else {
            // If the response is empty, a server failure will be reported
            // back to the stub resolver.
            decReply = EncDNSHelper.generateServfail(reply.id);
        }
        reply.setByteMessage(decReply);
        return reply;
	}
	
	
    /**
     * Decrypt a response received from the local recursive nameserver
     * @param encResponse response received from the local recursive nameserver
     * @return decrypted standard DNS message
     */
    private byte[] decryptResponse(byte[] encResponse) {
        if((encResponse[3]&0x0f)!=0) { // if RCODE!=0 (i.e. if an error occurred)
            return null;
        }
        
        // find the position of the question name's end in the message
        int qNameEnd = EncDNSHelper.findQuestionNameEnd(encResponse);
        
        // This is a bit of cheating, but we know that the remote proxy should
        // send only one answer - if there are additional questions or answers
        // in the message, something went wrong (probably at the local recursive
        // nameserver)...
        
        // glue together <character-string>s of split messages
        int txtlen = (((int) encResponse[qNameEnd + 15]) << 8) | (encResponse[qNameEnd+16]&0xff);
        ArrayList<Byte> cryptList = new ArrayList<Byte>();
        for (int mpos = 17; mpos < (17+txtlen);) {
            int cslen = encResponse[qNameEnd+mpos]&0xff; // get length of character-string (and correct sign)
            mpos++;
            for (int i = 0; i < cslen; i++) {
                cryptList.add(encResponse[qNameEnd+mpos]);
                mpos++;
            }
        }

        byte[] cryptoStuff = ArrayUtils.toPrimitive(cryptList.toArray(new Byte[0]));
        byte[] n = Arrays.copyOfRange(cryptoStuff, 0, EncDnsClient.libSodium.NONCEBYTES); // nonce
        byte[] cbox = Arrays.copyOfRange(cryptoStuff, EncDnsClient.libSodium.NONCEBYTES, cryptoStuff.length); // cryptobox
        
        long time = System.currentTimeMillis();
        // decrypt the message
        byte[] decM = EncDnsClient.libSodium.openCryptoBoxAfternm(cbox, EncDnsClient.k, n);
        if(EncDnsClient.verbosity >= 1) 
            System.out.println("response decryption time " + (System.currentTimeMillis()-time) + " ms");
        return decM;
    }
    
	
	@Override
	public int getMaxPayloadForNextMessage() {
		return EncDnsClient.MAX_MSG_SIZE;
	}

	
	@Override
	public int getMaxPayloadForNextReply() {
		return EncDnsClient.MAX_MSG_SIZE;
	}

}