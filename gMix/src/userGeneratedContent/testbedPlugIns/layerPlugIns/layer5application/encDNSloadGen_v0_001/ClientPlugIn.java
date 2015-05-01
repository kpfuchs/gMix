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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.encDNSloadGen_v0_001;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.ArrayUtils;

import staticContent.framework.EncDnsClient;
import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer1NetworkClient;
import staticContent.framework.interfaces.Layer2RecodingSchemeClient;
import staticContent.framework.interfaces.Layer5ApplicationClient;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer2recodingScheme.encDNS_v0_001.EncDNSHelper;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.encDNS_v0_001.EncDnsRequest;


/**
 * local proxy for encdns (EncDNS-Proxy); stub resolver is supposed to 
 * communicate with this component
 *
 * ---
 *
 * This is the EncDNS client proxy (also known as local proxy). It should be
 * executed on the client computer or at least be connected to it via a
 * trustworthy connection. The client proxy will encrypt standard DNS requests
 * sent to it by a stub resolver and pass the encrypted request on to the local
 * recursive nameserver. It will also decrypt the encrypted response received 
 * from the local recursive nameserver and pass the decrypted standard DNS 
 * response on to the stub resolver.
 * 
 * This implementation currently supports UDP only.
 */
public class ClientPlugIn extends Implementation implements Layer5ApplicationClient {

	/** maximum DNS message size */
    public static final int MAX_MSG_SIZE = 65535;
    
    private EncDnsClient owner;
    private Layer2RecodingSchemeClient layer2;
    private Layer1NetworkClient layer1; 
	
    private SecureRandom _rnd;
    
    private long SEND_INTERVAL_IN_NS;
    private int THREADS;
    private boolean AFAP = false;
    
    private final long SLEEP_PRECISION = TimeUnit.MILLISECONDS.toNanos(2); // TODO: determine for current machine
	private final long INIT_TIME = System.nanoTime();
    
    private byte[] _qname;
    private short _qtype;
    private short _qclass;
    private int _qNum;
    
    
	@Override
	public void constructor() {
		System.out.println("EncDNS load generator started"); 
		this.owner = EncDnsClient.getInstance();
		if (settings.getPropertyAsInt("EDNS_LG_MSG_PER_SEC") == -1)
			AFAP = true;
		else
			this.SEND_INTERVAL_IN_NS = (int)Math.ceil(1000000000d/settings.getPropertyAsDouble("EDNS_LG_MSG_PER_SEC"));
		this.THREADS = settings.getPropertyAsInt("EDNS_LG_THREADS");
		this._qclass = settings.getPropertyAsShort("EDNS_LG_Q_CLASS");
		this._qname = EncDNSHelper.parseZoneNameString(settings.getProperty("EDNS_LG_Q_NAME"));
		this._qtype = (short)settings.getPropertyAsInt("EDNS_LG_Q_TYPE"); // This must be parsed as an Integer as otherwise values having a highest-value bit of 1 would be considered out of range
        if(this._qtype>65535)
        	throw new RuntimeException("EDNS_LG_Q_TYPE must be <= 65535!");
        this._qNum = 0;  
        this._rnd = new SecureRandom();
        StatisticsRecorder.init(settings.getPropertyAsInt("EDNS_LG_DISPLAY_STAT_PERIOD"), THREADS);
	}


	@Override
	public void initialize() {
		this.layer2 = (Layer2RecodingSchemeClient) owner.recodingLayerClient.getImplementation();
		this.layer1 = (Layer1NetworkClient) owner.networkLayerClient.getImplementation();
	}


	@Override
	public void begin() {
		for (int i=0; i<THREADS; i++) 
            new SenderThread().start();
		// TODO: start display-stat-thread
	}


	public class SenderThread extends Thread {

		long nextOutput;
		final int threadId = StatisticsRecorder.getThreadId();
		final long sendInterval = Math.round(((double)SEND_INTERVAL_IN_NS)*((double)THREADS));
		
		@Override
		public void run() {
			if (AFAP) {
				while (true) {
					EncDnsRequest request = new EncDnsRequest(null, 0, getQueryBytes());
			        request = (EncDnsRequest) layer2.applyLayeredEncryption(request); // skip layer 3 + 4...
			        if (request == null || request.getByteMessage() == null)
			        	continue;
					layer1.sendMessage(request);
					StatisticsRecorder.messageSent(threadId);
				}
			} else {
				nextOutput = now() + sendInterval;
				while (true) {
					long now = now();
					if (now < nextOutput) // we must wait
						sleepNanos(nextOutput - now);
					nextOutput += sendInterval;
					EncDnsRequest request = new EncDnsRequest(null, 0, getQueryBytes());
			        request = (EncDnsRequest) layer2.applyLayeredEncryption(request); // skip layer 3 + 4...
			        if (request == null || request.getByteMessage() == null)
			        	continue;
					layer1.sendMessage(request);
					StatisticsRecorder.messageSent(threadId);
				}
			}
		}
	}
	
	
    /**
     * Creates a new query message with a unique prefix.
     * @return query message
     */
    public byte[] getQueryBytes() {
    	byte[] label1length = {36};
    	byte[] label1 = ("a123456789a123456789a123456789a12345".getBytes());
    	
    	byte[] prefix = new byte[19];
    	prefix[0] = 18;
    	prefix[1] = 'a';
    	prefix[2] = '1';
    	prefix[3] = '2';
    	prefix[4] = '3';
    	prefix[5] = '4';
    	prefix[6] = '5';
    	prefix[7] = '6';
    	prefix[8] = '7';
    	prefix[9] = '8';
    	prefix[10] = '9';
    	prefix[11] = 'w';
    	String numstr = String.format("%08d", _qNum);
    	if(_qNum>=99999999) {
    		_qNum = 0;
    	} else {
    		_qNum++;
    	}
    	for(int i = 12; i < prefix.length; i++) {
    		prefix[i] = (byte)numstr.charAt(i-12);
    	}
    	byte[] qname = ArrayUtils.addAll(prefix, label1length);
    	qname = ArrayUtils.addAll(qname, label1);
    	//qname = ArrayUtils.addAll(qname, label1length);
    	//qname = ArrayUtils.addAll(qname, label1);
    	qname = ArrayUtils.addAll(qname, _qname);
    	//byte[] qname = ArrayUtils.addAll(prefix, _qname);
    	byte[] query = buildQuery(qname, _qtype, _qclass);
        return query;
    }
    
    
    /**
     * Builds the query message
     * @param qName query name
     * @param type query type
     * @param qClass query class
     * @return query message
     */
    private byte[] buildQuery(byte[] qName, short type, short qClass) {
        ByteBuffer buf = ByteBuffer.allocate(MAX_MSG_SIZE);

        // --- DNS HEADER ---
        // Choose random ID
        byte[] id = new byte[2];
        _rnd.nextBytes(id);
        buf.put(id);
        buf.put((byte) 1); // QR=0 (Query), OpCode=0000 (Query), AA=0, TC=0, RD=1
        buf.put((byte) 0); // RA,Z,AD,CD,RCode=0
        buf.putShort((short) 1); // question count = 1
        buf.putShort((short) 0); // answer count = 0
        buf.putShort((short) 0); // authority count = 0
        buf.putShort((short) 0); // additional count = 0

        // --- DNS QUESTION SECTION ---
        buf.put(qName); // NAME
        buf.putShort(type); // TYPE
        buf.putShort(qClass); // CLASS

        // convert to byte[]
        byte[] out = new byte[buf.position()];
        buf.position(0);
        buf.get(out);

        return out;
    }
    
    
	private long now() {
		return System.nanoTime() - INIT_TIME;
	}
	
    
	private void sleepNanos(long nanoDuration) {
		// see http://andy-malakov.blogspot.de/2010/06/alternative-to-threadsleep.html
		final long end = now() + nanoDuration;
		long timeLeft = nanoDuration;
		try {
			do {
				if (timeLeft > SLEEP_PRECISION)
					Thread.sleep(1);
				else
					Thread.yield();
				timeLeft = end - now();
				if (Thread.interrupted())
					throw new InterruptedException();
			} while (timeLeft > 0/* && !interruptSleep*/);
		} catch (InterruptedException e) {
			if (timeLeft > 0/* && !interruptSleep*/)
				sleepNanos(end - now());
		}
	}
}
