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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.encDNS_v0_001;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Random;

import org.xbill.DNS.Cache;
import org.xbill.DNS.Credibility;
import org.xbill.DNS.DClass;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.SetResponse;

import staticContent.framework.EncDnsServer;
import staticContent.framework.config.Settings;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.dnsProxy_v0_001.DNSUtils;


public class LocalResolver {

	private static String[] dnsServers;
	
	/** Flag indicating if all DNS queries shall get an A record of 127.0.0.1 instead of 
	 * really resolving them through the DNS server */
	private final boolean RESOLVE_ALL_DNS_TO_LOCALHOST;

	/** The resolver object to use for resolving the DNS queries */
	private static ExtendedResolver res;

	/** Random number generator for performing the DNS lookups with a random DNS server */
	private static final Random r = new SecureRandom();
	
	
	/** Local DNS cache that answers queries if the records are cached */
	private Cache dnsCache = new Cache(DClass.IN);

	//private final int RESOLVER_TIMEOUT;
	private boolean SIMULATE_RESOLVE_PROCESS;
	
	
	public LocalResolver(boolean resolveLocal/*, int timeout*/, Settings settings) {
		this.RESOLVE_ALL_DNS_TO_LOCALHOST = resolveLocal;
		this.SIMULATE_RESOLVE_PROCESS = settings.getPropertyAsBoolean("SIMULATE_RESOLVE_PROCESS");
		String dnsServerString = settings.getProperty("DNS_SERVERS");
		dnsServerString = dnsServerString.replaceAll(" ", "");
		LocalResolver.dnsServers = dnsServerString.split(",");
		//this.RESOLVER_TIMEOUT = timeout;
		/*this.DNSP_DEBUG = settings.getPropertyAsBoolean("DNSP_DEBUG");
		this.RESOLVE_ALL_DNS_TO_LOCALHOST = settings.getPropertyAsBoolean("RESOLVE_ALL_DNS_TO_LOCALHOST");
		this.RESOLVER_TIMEOUT = settings.getPropertyAsInt("RESOLVER_TIMEOUT");*/
		try {
			res = new ExtendedResolver(dnsServers);
			res.setLoadBalance(true);
			//res.setTimeout(RESOLVER_TIMEOUT);
			res.setRetries(0);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			throw new RuntimeException("could not set up resolver"); 
		}
	}
	
	
	public byte[] resolve(byte[] request) throws IOException {
		if (SIMULATE_RESOLVE_PROCESS) {
			// Open a socket on a new port
            DatagramSocket udpSock = null;
            int port;
            while (udpSock == null) {
                try {
                    port = 1024 + EncDnsServer.rnd.nextInt(64512);
                    udpSock = new DatagramSocket(port);
                } catch (SocketException e) {
                    // do nothing as this is being handled by the while loop
                }
            }
            // Send the decrypted query to the remote recursive nameserver
            DatagramPacket sendPacket = new DatagramPacket(request, request.length, udpSock.getLocalAddress(), udpSock.getLocalPort());
            udpSock.send(sendPacket);
            // Wait for a response
            byte[] rcvbytes = new byte[EncDnsServer.MAX_MSG_SIZE];
            DatagramPacket rcvPkt = new DatagramPacket(rcvbytes, rcvbytes.length);
            udpSock.setSoTimeout(EncDnsServer.timeout); // Make sure we do not wait for ages...
            udpSock.receive(rcvPkt);
            udpSock.close();
            byte[] rcvDNS = new byte[rcvPkt.getLength()];
            System.arraycopy(rcvPkt.getData(), rcvPkt.getOffset(), rcvDNS, 0, rcvPkt.getLength());
            if(EncDnsServer.verbosity >= 1) {
                System.out.println("Received reply from aNS");
            }
            return DNSUtils.resolveDNSQueryToLocalhost(new Message(request).toWire());
		} else {
			byte[] data;
			Message dnsQuery = new Message(request);
			SetResponse cached;
			synchronized(dnsCache) {
				cached = dnsCache.lookupRecords(dnsQuery.getQuestion().getName(), dnsQuery.getQuestion().getType(), Credibility.NONAUTH_ANSWER);
			}
			if (cached.isSuccessful() && !RESOLVE_ALL_DNS_TO_LOCALHOST) {
				Record[] r = DNSUtils.processSetResponse(cached);
				for (Record rec : r){
					dnsQuery.addRecord(rec, Section.ANSWER);
				}
				dnsQuery.getHeader().setFlag(Flags.RA);
				dnsQuery.getHeader().setFlag(Flags.QR);
				data = dnsQuery.toWire();
			} else if(RESOLVE_ALL_DNS_TO_LOCALHOST){
				data = DNSUtils.resolveDNSQueryToLocalhost(dnsQuery.toWire());					
			} else {
				Message dnsReply = res.getResolver(r.nextInt(dnsServers.length)).send(dnsQuery);
				data = dnsReply.toWire();					
			}
			return data;
		}
	}

}
