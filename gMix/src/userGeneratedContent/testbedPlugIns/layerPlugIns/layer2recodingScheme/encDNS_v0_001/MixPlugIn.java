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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.ArrayUtils;

import staticContent.framework.EncDnsServer;
import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer2RecodingSchemeMix;
import staticContent.framework.message.Reply;
import staticContent.framework.message.Request;
import staticContent.framework.userDatabase.User;
import staticContent.framework.util.Util;

import com.github.encdns.LibSodiumWrapper;


/**
 * This is the EncDNS server proxy (also known as remote proxy). It should be
 * executed on the same computer as the remote recursive nameserver or at least 
 * be connected to it via a trustworthy connection. The server proxy will 
 * decrypt encrypted EncDNS queries it receives and pass the decrypted
 * standard DNS query on to the remote recursive nameserver. It will also
 * encrypt the standard DNS responses received from the remote recursive
 * nameserver and pass the EncDNS response on to the local recursive nameserver.
 */
public class MixPlugIn extends Implementation implements Layer2RecodingSchemeMix {

	/** size of cache for intermediate shared secrets */
    public int CACHESIZE;
	/** maximum number of threads */
    private int _maxThreads;
	
    
	private byte[] _magicString;
	private LibSodiumWrapper _libSodium;
    private byte[] _zoneName;
    private Map<EncDNSKey, byte[]> _cache;
    private boolean _encryption;
    private byte[] _sk;
    
    private static boolean displayByteThroughput;
    private static boolean displayPacketThroughput;
    private static boolean displayThreadStatus;
    private static int displayThreadInterval;
    private static AtomicInteger decryptingThreads = new AtomicInteger(0);
    private static AtomicInteger resolvingThreads = new AtomicInteger(0);
    private static AtomicInteger encryptingThreads = new AtomicInteger(0);
    private static AtomicInteger sendingThreads = new AtomicInteger(0);
    private static AtomicInteger idleThreads = new AtomicInteger(0);
    private static int threads;
    
    private userGeneratedContent.testbedPlugIns.layerPlugIns.layer1network.encDNS_v0_001.MixPlugIn layer1;
    private DatagramSocket layer1socket;
	private userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.encDNS_v0_001.MixPlugIn layer5;
    
	
	@Override
	public void constructor() {
		_encryption = EncDnsServer.encryption;
		if (_encryption)
			_libSodium = new LibSodiumWrapper();
        CACHESIZE = EncDnsServer.cachesize;
        if (CACHESIZE == 0)
        	System.out.println("key-caching is disabled"); 
        _zoneName = EncDNSHelper.parseZoneNameString(EncDnsServer.zoneurl);

        _magicString = new byte[]{0x20, 0x45, 0x5e};
        _cache = Collections.synchronizedMap(new SharedSecretCache(CACHESIZE));
        _maxThreads = EncDnsServer.maxThreads;
        threads = _maxThreads;
        
        if (_encryption) {
        	//_pk = EncDNSHelper.readByteArrayFromFile(pkPath);
        	_sk = EncDNSHelper.readByteArrayFromFile(EncDnsServer.skPath);
        	if (Util.toHex(_sk).equals("59A79183CC11D725C64DF8005E784B8ABE6908F053E1AC77BB022DA250D36049"))
        		System.err.println("WARNING: you are using a publicly known test key for this EncDNS Server; never use this key to transmit sensitive data!"); 
        }
        
        displayThreadInterval = EncDnsServer.displayThreadStatusInt;
        displayThreadStatus = EncDnsServer.displayThreadStatusBool;
        displayByteThroughput = EncDnsServer.displayThroughputBool;
        displayPacketThroughput = EncDnsServer.displayThroughputBool;
        
        if (displayThreadStatus) {
        	new Thread(
        			new Runnable() {
        				public void run() {
        					while (true) {
        						try {
									Thread.sleep(displayThreadInterval);
								} catch (InterruptedException e) {
									e.printStackTrace();
									continue;
								}
        						String threadStatus = "\nThreadStatus (of "+threads +" threads):\n";
        						threadStatus += "  idle threads: " +idleThreads.get() +"\n";
        						threadStatus += "  decrypting threads: " +decryptingThreads.get() +"\n";
        						threadStatus += "  resolvingThreads threads: " +resolvingThreads.get() +"\n";
        						threadStatus += "  encryptingThreads threads: " +encryptingThreads.get() +"\n";
        						System.out.println(threadStatus); 
        					} 
        				}
        			}
        		).start(); 
        	}
        
        if (EncDnsServer.displayThroughputBool) {
        	StatisticsRecorder.init(EncDnsServer.displayThroughputBool, EncDnsServer.displayThroughputBool, displayThreadInterval, threads);
        }
	}

	
	@Override
	public void initialize() {
		}
	

	@Override
	public void begin() {
		this.layer1 = ((userGeneratedContent.testbedPlugIns.layerPlugIns.layer1network.encDNS_v0_001.MixPlugIn)networkLayerMix.getImplementation());
		this.layer1socket = layer1.getSocket();
		this.layer5 = ((userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.encDNS_v0_001.MixPlugIn)applicationLayerMix.getImplementations()[0]);
		for (int i=0; i<_maxThreads; i++) {
        	new Thread(new ServerMessageHandlerThread()).start();
        }
	}

	
	@Override
	public int getMaxSizeOfNextReply() {
		return EncDnsServer.MAX_MSG_SIZE;
	}

	
	@Override
	public int getMaxSizeOfNextRequest() {
		return EncDnsServer.MAX_MSG_SIZE;
	}
	
	
    /**
     * This thread will send a query to the remote recursive nameserver, wait 
     * for a response and pass the decrypted response on to the local recursive
     * nameserver.
     * 
     * This is encapsulated in a thread to allow parallelized processing of
     * queries.
     */
    private class ServerMessageHandlerThread implements Runnable {
        //private DatagramPacket _rcvPkt;
        private byte[] _k;
        private int threadId;

        
        /**
         * Constructor for ServerMessageHandlerThread
         * @param rcvPkt (hopefully) an EncDNS query received from a loccal
         * recursive nameserver
         */
        ServerMessageHandlerThread(/*DatagramPacket rcvPkt*/) {
        	this.threadId = StatisticsRecorder.getThreadId();
        }
        

        public void run() {
        	/*if(EncDnsServer.verbosity >= 1) {
                System.out.println("ServerMessageHandler started");
            }*/
        	while (true) {
        		byte[] rcvPkt = new byte[EncDnsServer.MAX_MSG_SIZE];
        		DatagramPacket _rcvPkt = new DatagramPacket(rcvPkt, rcvPkt.length);
        		if (displayThreadStatus) {
        			sendingThreads.decrementAndGet();
        			idleThreads.incrementAndGet();
            	}
        		// reiceive:
                synchronized (layer1) {
                	try {
                		layer1socket.receive(_rcvPkt);
						if (displayPacketThroughput || displayByteThroughput)
							StatisticsRecorder.addRequestThroughputRecord(_rcvPkt.getLength(), threadId);
					} catch (IOException e) {
						if(EncDnsServer.verbosity >= 1) {
							System.err.println("Failed to receive message:");
                            e.printStackTrace();
			            }
						continue;
					}
                }
                if(EncDnsServer.verbosity >= 1) {
                    System.out.println("Received message");
                }
                if (displayThreadStatus) {
                	idleThreads.decrementAndGet();
                	decryptingThreads.incrementAndGet();
            	}
             // Copy received query into a byte[]
                byte[] rcvDNS = new byte[_rcvPkt.getLength()];
                System.arraycopy(_rcvPkt.getData(), _rcvPkt.getOffset(), rcvDNS, 0,
                        _rcvPkt.getLength());
                
                int qNameEnd = EncDNSHelper.findQuestionNameEnd(rcvDNS);
                
                byte[] decMsg;
                if(_encryption) {
                    // If encryption is enabled, decrypt the message
                    decMsg = decryptQuery(rcvDNS, qNameEnd);
                } else {
                    // else just pass the unencrypted message on
                    decMsg = Arrays.copyOf(rcvDNS, rcvDNS.length);
                }
                if (decMsg == null) {
                    // If the decrypted message is null, decryption failed (probably due to a manipulated message)
                    if(EncDnsServer.verbosity >= 1) {
                        System.out.println("Decryption of EncDNS message failed.");
                    }
                    continue;
                }

                if (displayThreadStatus) {
                	decryptingThreads.decrementAndGet();
                	resolvingThreads.incrementAndGet();
            	}
                
                byte[] ansReply = layer5.sendQueryAndListenForReply(decMsg);

                if (displayThreadStatus) {
                	resolvingThreads.decrementAndGet();
                	encryptingThreads.incrementAndGet();
            	}
                byte[] qID = Arrays.copyOfRange(decMsg, 0, 2);
                byte[] rID = (ansReply == null) ? null : Arrays.copyOfRange(ansReply, 0, 2);
                byte[] encReply;
                // Check for empty responses and compare query and response IDs
                if (ansReply != null && Arrays.equals(qID, rID)) {
                    if(_encryption) {
                        // If encryption is enabled, encrypt the response
                        encReply = encryptResponse(rcvDNS, qNameEnd, ansReply, _k);
                    } else {
                        // Else just pass the unencrypted response on
                        encReply = Arrays.copyOf(ansReply, ansReply.length);
                    }
                } else {
                    // If we received an empty response or the response ID does not
                    // match the query ID, something went wrong -> generate a
                    // SERVFAIL message
                    if(_encryption) {
                        encReply = encryptResponse(rcvDNS, qNameEnd, EncDNSHelper.generateServfail(rcvDNS), _k);
                    } else {
                        encReply = EncDNSHelper.generateServfail(rcvDNS);
                    }
                }
                
                if (displayThreadStatus) {
                	encryptingThreads.decrementAndGet();
                	sendingThreads.incrementAndGet();
        		}
                
             // Send response to local recursive nameserver
                DatagramPacket sendPacket = new DatagramPacket(encReply, encReply.length, _rcvPkt.getAddress(), _rcvPkt.getPort());
                
                //synchronized (_udpSock53) {
                	try {
                		if (displayPacketThroughput || displayByteThroughput)
							StatisticsRecorder.addReplyThroughputRecord(encReply.length, threadId);
                		layer1socket.send(sendPacket);
                    } catch (IOException e) {
                        if(EncDnsServer.verbosity >= 1) {
                            System.err.println("Failed to send message:");
                            System.err.println(e);
                        }
                    }
                //}
        	} 
        }
        
        
        /**
         * Decrypts a query.
         * @param rcv encrypted query
         * @param qNameEnd position of the question name's end
         * @return the decrypted query
         */
        private byte[] decryptQuery(byte[] rcv, int qNameEnd) {
            // expected start position of EncDNS zone name
            int msgZoneNameStart = qNameEnd + 1 - _zoneName.length;
            // check whether the EncDNS zone name in the query is correct
            byte[] rcvZoneName = Arrays.copyOfRange(rcv, msgZoneNameStart, qNameEnd + 1);
            if ((msgZoneNameStart > rcv.length) || (!Arrays.equals(_zoneName, rcvZoneName))) {
                if(EncDnsServer.verbosity >= 1) {
                    System.err.println("DNS message was not destined for this server! " +new String(rcvZoneName) +" != " +new String(_zoneName));
                }
                return null;
            }

            // check for the magic string
            if (rcv[12] < _magicString.length || (!Arrays.equals(_magicString, Arrays.copyOfRange(rcv, 13, 13 + _magicString.length)))) {
                if(EncDnsServer.verbosity >= 1) {
                    System.err.println("Message not in EncDNS format!");
                    System.err.println(rcv[12] +" <? " +_magicString.length); 
                    System.err.println(Util.toHex(_magicString) +" ==? " +Util.toHex(Arrays.copyOfRange(rcv, 13, 13 + _magicString.length))); 
                }
                return null;
            }

            // copy the crypto information from the query
            ArrayList<Byte> cryptList = new ArrayList<Byte>();
            for (int mpos = 12; mpos < msgZoneNameStart;) {
                int llen = rcv[mpos];
                mpos++;
                for (int i = 0; i < llen; i++) {
                    cryptList.add(rcv[mpos]);
                    mpos++;
                }
            }
            byte[] cryptoStuff = ArrayUtils.toPrimitive(cryptList.toArray(new Byte[0]));
            
            // split cryptographic information
            int pkAndNonceEnd = _magicString.length + _libSodium.PKBYTES + (_libSodium.NONCEBYTES / 2);
            byte[] rpk = Arrays.copyOfRange(cryptoStuff, _magicString.length, _magicString.length + _libSodium.PKBYTES);
            byte[] n = new byte[_libSodium.NONCEBYTES];
            System.arraycopy(cryptoStuff, _magicString.length + _libSodium.PKBYTES, n, 0, _libSodium.NONCEBYTES / 2);
            byte[] cbox = Arrays.copyOfRange(cryptoStuff, pkAndNonceEnd, cryptoStuff.length);

            // check whether we've got the corresponding intermediate shared
            // secret in the cache
            EncDNSKey rpkobj = new EncDNSKey(rpk);
            _k = _cache.get(rpkobj);
            if (_k == null) {
                if(EncDnsServer.verbosity >= 1) {
                    System.out.println("Key not found in cache. Calculating intermediate shared secret...");
                }
                _k = _libSodium.cryptoBoxBeforenm(rpk, _sk);
                _cache.put(rpkobj, _k);
            } else {
                if(EncDnsServer.verbosity >= 1) {
                    System.out.println("Key found in cache.");
                }
            }

            // decrypt and return the query
            byte[] decCbox = _libSodium.openCryptoBoxAfternm(cbox, _k, n);
            return decCbox;
        }
    
    }

    
    /**
     * Encrypts a standard DNS response.
     * @param encQuery EncDNS query
     * @param qNameEnd position of the question name's end
     * @param response standard DNS response to encrypt
     * @param k intermediate shared secret to use for encryption
     * @return the encrypted EndDNS response containing the specified standard DNS response
     */
    private byte[] encryptResponse(byte[] encQuery, int qNameEnd, byte[] response, byte[] k) {
        // TODO This uses 65 kB of memory even if the resultig message is a lot
        // smaller ... :-(
        ByteBuffer buf = ByteBuffer.allocate(EncDnsServer.MAX_MSG_SIZE);

        // --- DNS HEADER ---
        // ID must match the query ID
        byte[] id = Arrays.copyOfRange(encQuery, 0, 2);
        buf.put(id);
        
        buf.put((byte) 0x81); // QR=1 (Response), OpCode=0000 (Standard), AA=0, TC=0, RD=1
        buf.put((byte) 0); // RA=0, Z,AD,CD=0, RCode=0000 (no error)
        buf.putShort((short) 1); // question count = 1
        buf.putShort((short) 1); // answer count = 1
        buf.putShort((short) 0); // authority count = 0
        buf.putShort((short) 0); // additional count = 0

        // --- DNS QUESTION SECTION ---
        // copy the question from the query
        buf.put(Arrays.copyOfRange(encQuery, 12, qNameEnd + 5));

        // --- DNS ANSWER SECTION ---
        buf.put((byte) 0xc0); // c00c is the address of the question sections name
        buf.put((byte) 0x0c);
        buf.putShort((short) 16); // TYPE=TXT
        buf.putShort((short) 1); // CLASS = IN
        buf.putInt(0); // TTL = 0

        // copy client nonce
        byte[] nonce = new byte[_libSodium.NONCEBYTES];
        System.arraycopy(encQuery, 13 + _magicString.length + _libSodium.PKBYTES, nonce, 0, _libSodium.NONCEBYTES / 2);
        
        // Generate and add server nonce: 8 byte timer+4 byte random value
        byte[] sn = new byte[_libSodium.NONCEBYTES/2];
        ByteBuffer timeBuffer = ByteBuffer.allocate(8);
        timeBuffer.putLong(System.nanoTime());
        byte[] time = timeBuffer.array();
        byte[] random = new byte[(_libSodium.NONCEBYTES/2)-time.length];
        EncDnsServer.rnd.nextBytes(random);
        System.arraycopy(time, 0, sn, 0, time.length);
        System.arraycopy(random, 0, sn, time.length, random.length);
        
        System.arraycopy(sn, 0, nonce, _libSodium.NONCEBYTES/2, _libSodium.NONCEBYTES/2);
        
        // encrypt the message
        byte[] cbox = _libSodium.makeCryptoBoxAfternm(response, k, nonce);
        
        byte[] cryptoStuff = ArrayUtils.addAll(nonce, cbox);
        
        // The contents of a TXT RR must be split into <character-string>s
        // of a length of less than 256 bytes. Each of these has a length byte,
        // so we'll need space for those in the RR.
        short txtlen = (short) (cryptoStuff.length + (cryptoStuff.length / 255) + 1);
        buf.putShort(txtlen);

        // Put nonce and cbox into TXT RR.
        int cpos = 0;
        while (true) {
            if ((cpos + 255) < cryptoStuff.length) {
                // remaining part > 255 byte -> add 255-byte <character-string>
                buf.put((byte) 255);
                buf.put(cryptoStuff, cpos, 255);
                cpos += 255;
            } else {
                // remaining part of cbox <= 63 byte -> add remaining part
                short remainingBytes = (short) (cryptoStuff.length - cpos);
                buf.put((byte) remainingBytes);
                buf.put(cryptoStuff, cpos, remainingBytes);
                break;
            }
        }

        // TODO We may want to copy the payload size from the request. As the
        // value is fixed in the current client implementation, we'll just
        // assume the fixed value here. Also, there is currently no check
        // to prevent messages exceeding this size to be sent.
        // --- DNS ADDITIONAL SECTION - OPT PSEUDO-RR ---
        buf.put((byte) 0); // Domain name = root
        buf.putShort((short) 41); // RR TYPE=OPT(41)
        buf.putShort((short) EncDnsServer.MAX_MSG_SIZE); // sender max UDP payload=65535
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
	public Request generateDummy(int[] route, User user) {
		throw new RuntimeException("not supported");
	}

	
	@Override
	public Request generateDummy(User user) {
		throw new RuntimeException("not supported");
	}
	

	@Override
	public Reply generateDummyReply(int[] route, User user) {
		throw new RuntimeException("not supported");
	}

	
	@Override
	public Reply generateDummyReply(User user) {
		throw new RuntimeException("not supported");
	}
}
