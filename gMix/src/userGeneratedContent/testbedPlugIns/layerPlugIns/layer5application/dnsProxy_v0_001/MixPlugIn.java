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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.dnsProxy_v0_001;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.xbill.DNS.Cache;
import org.xbill.DNS.Credibility;
import org.xbill.DNS.DClass;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.SetResponse;

import staticContent.framework.controller.Implementation;
import staticContent.framework.interfaces.Layer5ApplicationMix;
import staticContent.framework.routing.RoutingMode;
import staticContent.framework.socket.socketInterfaces.AnonMessage;
import staticContent.framework.socket.socketInterfaces.DatagramAnonServerSocket;
import staticContent.framework.socket.socketInterfaces.AnonSocketOptions.CommunicationDirection;
import staticContent.framework.socket.socketInterfaces.NoneBlockingAnonSocketOptions.IO_Mode;
import staticContent.framework.util.Util;


public class MixPlugIn extends Implementation implements Layer5ApplicationMix {

	private DatagramAnonServerSocket socket = null;
	
	/** Flag indicating if all DNS queries shall get an A record of 127.0.0.1 instead of 
	 * really resolving them through the DNS server */
	private boolean RESOLVE_ALL_DNS_TO_LOCALHOST;

	/** The resolver object to use for resolving the DNS queries */
	private static ExtendedResolver res;

	/** Random number generator for performing the DNS lookups with a random DNS server */
	private static final Random r = new SecureRandom();

	/** Terminating sequence used in batched DNS messages to signal end of a message part */
	//private static final byte[] dnsTerminator = new byte[]{(byte)0xfa,(byte)0xfb,(byte)0xfc,(byte)0xfd,(byte)0xfe};
	
	/** Local DNS cache that answers queries if the records are cached */
	private Cache dnsCache = new Cache(DClass.IN);

	private int RESOLVER_TIMEOUT;

	/** The DNS servers used for resolving incoming queries */
	private static final String[] dnsServers = new String[]{// TODO add this to the property file
		"8.8.4.4"//,
		/*"193.101.111.10",
		"131.188.3.72",
		"208.67.222.222",
		"85.214.73.63",
		"194.150.168.168",
		"208.67.222.222",
		"156.154.70.1"*/
	};
	/*private static final String[] dnsServers = new String[]{
	"8.8.8.8", // Google
	"8.8.4.4", // Google
	"213.73.91.35", // dnscache.berlin.ccc.de
	"85.214.73.63", // anonymisierungsdienst.foebud.org
	//"204.152.184.76", // f.6to4-servers.net - Slow
	"194.150.168.168", // dns.as250.net
	"80.237.196.2", // dnsc1.dtfh.de
	"194.95.202.198", // omni.digital.udk-berlin.de
    "67.138.54.100", // ScrubIT
    "207.225.209.66", //ScrubIT
	"208.67.222.222", //OpenDNS
	"208.67.220.220", // OpenDNS
    "156.154.70.1", // DNS Advantage
	"156.154.71.1", // DNS Advantage
	"4.2.2.1", // vnsc-pri.sys.gtei.net
	"4.2.2.2", // vnsc-bak.sys.gtei.net
	"4.2.2.3", // vnsc-lc.sys.gtei.net
	"4.2.2.4", // vnsc-pri-dsl.genuity.net
	"4.2.2.5", // vnsc-bak-dsl.genuity.net
	"4.2.2.6" // vnsc-lc-dsl.genuity.net
};*/
	
	private Executor exec;

	private boolean DNSP_DEBUG;
	 
	
	@Override
	public void constructor() {
		this.DNSP_DEBUG = settings.getPropertyAsBoolean("DNSP_DEBUG");
		this.RESOLVE_ALL_DNS_TO_LOCALHOST = settings.getPropertyAsBoolean("RESOLVE_ALL_DNS_TO_LOCALHOST");
		this.RESOLVER_TIMEOUT = settings.getPropertyAsInt("RESOLVER_TIMEOUT");
		this.exec = Executors.newFixedThreadPool(settings.getPropertyAsInt("RESOLVER_THREADS"));
	}

	
	@Override
	public void initialize() {
		this.socket = super.anonNode.createDatagramServerSocket(
				settings.getPropertyAsInt("INTERNAL_MIX_PORT"),
				CommunicationDirection.DUPLEX,
				IO_Mode.BLOCKING,
				false,
				false,
				super.anonNode.ROUTING_MODE != RoutingMode.GLOBAL_ROUTING
			);
	}

	
	@Override
	public void begin() {
		setUpResolver();
		new RequestReceiverThread().start(); // TODO: use observer-pattern
	}

	
	/**
	 * The {@code ExtendedResolver} is instantiated with the 
	 * DNS servers from the <code>dnsServers</code> array.
	 * Load balancing will be performed when resolving.
	 * (Probably does not apply to asynchronous resolving)
	 * 
	 */
	private void setUpResolver() {
		try {
			res = new ExtendedResolver(dnsServers);
			res.setLoadBalance(true);
			res.setTimeout(RESOLVER_TIMEOUT);
			res.setRetries(0);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			throw new RuntimeException("could not set up resolver"); 
		}
	}

	
	private class RequestReceiverThread extends Thread {
		
		/**
		 * Waits for processed <code>Requests</code> and puts their payload in the 
		 * suiting <code>User</code>'s proxy buffer.
		 * 
		 * @see de.ur.sec.dnsmix.userDatabase.User#putInProxyWriteBuffer(byte[])
		 */
		@Override
		public void run() {
			while (true) {
				AnonMessage datagram = socket.receiveMessage();
				
				if(DNSP_DEBUG)
                    System.out.println("mix received mix message (" +Util.toHex(datagram.getByteMessage()) +")");
				
				// the raw byte[] message could contain multiple DNS queries
				Payload payload = new Payload(datagram.getMaxReplySize());
				payload.setMessage(datagram.getByteMessage());

				// now we have a list that can either be size 1 or > 1 if the raw byte message contained more messages
				List<byte[]> dnsQueriesWithID = payload.getMessages(0);
				
				for (byte[] dnsQueryWithID : dnsQueriesWithID) {
					exec.execute(new DNSResolver(datagram, dnsQueryWithID));
				}
			}
		}
		
	}
	
	
	class DNSResolver implements Runnable {
	
		private byte[] msgID;
		private byte[] queryInWire;
		
		//private User channel;
		private AnonMessage datagram;
		
		
		DNSResolver(AnonMessage datagram, byte[] dnsQueryWithMsgID){
			//this.channel = datagram.getUser();
			this.msgID = Arrays.copyOfRange(dnsQueryWithMsgID, 0, 4);
			this.queryInWire = Arrays.copyOfRange(dnsQueryWithMsgID, 4, dnsQueryWithMsgID.length);
			if(DNSP_DEBUG)
				System.out.println(" mix unpacked query: " + Util.byteArrayToInt(msgID) +" (" +Util.toHex(queryInWire) +")");
			this.datagram = datagram;
		}

		@Override
		public void run() {

			Message dnsQuery;
			Message dnsReply;
			
			// TODO check DNS cache
			
								
			try {
				dnsQuery = new Message(queryInWire);
				
				byte[] data;
				
				SetResponse cached = dnsCache.lookupRecords(dnsQuery.getQuestion().getName(), dnsQuery.getQuestion().getType(), Credibility.NONAUTH_ANSWER);
				if (cached.isSuccessful() && !RESOLVE_ALL_DNS_TO_LOCALHOST) {
					Record[] r = DNSUtils.processSetResponse(cached);
					for (Record rec : r){
						dnsQuery.addRecord(rec, Section.ANSWER);
					}

					dnsQuery.getHeader().setFlag(Flags.RA);
					dnsQuery.getHeader().setFlag(Flags.QR);
					
					data = dnsQuery.toWire();
				} else if(RESOLVE_ALL_DNS_TO_LOCALHOST){
					
					data = DNSUtils.mergeArrays(msgID, DNSUtils.resolveDNSQueryToLocalhost(dnsQuery.toWire()));					

				} else {
					
					dnsReply = res.getResolver(r.nextInt(dnsServers.length)).send(dnsQuery);
					
					data = DNSUtils.mergeArrays(msgID, dnsReply.toWire());
										
				}
				if ((data.length + Payload.getHeaderLength()) > datagram.getMaxReplySize()) {
					System.err.println("warning: reply too big for mix message; discarting it"); 
					return;
				}
				
				Payload replyPayload = new Payload(
							new byte[data.length + Payload.getHeaderLength()],
							datagram.getMaxReplySize()
						);
					
				replyPayload.setMessage(data);
				datagram.setByteMessage(replyPayload.getBytePayloadWithLengthHeader());
				if(DNSP_DEBUG)
					System.out.println(" mix sending reply-mix-message (" +Util.toHex(replyPayload.getBytePayloadWithLengthHeader()) +")");
				socket.sendMessage(datagram);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
									
		}
		
	}

}
