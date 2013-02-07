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
package evaluation.traceParser.engine;

import evaluation.traceParser.engine.protocolHandler.DNShandler;
import evaluation.traceParser.engine.protocolHandler.ERFhandler;
import evaluation.traceParser.engine.protocolHandler.EthernetHandler;
import evaluation.traceParser.engine.protocolHandler.IPv4Handler;
import evaluation.traceParser.engine.protocolHandler.IPv6Handler;
import evaluation.traceParser.engine.protocolHandler.PCAPhandler;
import evaluation.traceParser.engine.protocolHandler.ProtocolHandler;
import evaluation.traceParser.engine.protocolHandler.TCPhandler;
import evaluation.traceParser.engine.protocolHandler.UDPhandler;


public enum Protocol {
	
	/* 
	 * WARNING:
	 * ONLY ADD NEW ENTRIES TO THE BOTTOM OF THIS ENUM, otherwise it will not 
	 * be possible to read old traces (entries are serialized by id, not name).
	 * IF YOU CHANGE THE LIST (this also includes changing a handler from null 
	 * to an instance), INCREMENT THE VERSION NUMBER BELOW (the constant 
	 * named "VERSION"), otherwise the seed mechanism to recreate synthetic 
	 * traces may produce unexpected results
	 */
	UNKNOWN(-1, null),
	GMP(-1, null),	// gMix packet
	GMF(-1, null),	// gMix flow
	GMT(-1, null),	// gMix transaction
	PM(-1, null),	// packMime-HTTP
	ERF(0, new ERFhandler()),
	PCAP(0, new PCAPhandler()),
	Ethernet(2, new EthernetHandler()),
	ARP(2, null),
	IPv4(2, new IPv4Handler()),
	IPv6(2, new IPv6Handler()),
	TCP(3, new TCPhandler()),
	UDP(3, new UDPhandler()),
	ICMP(3, null),
	ESP(3, null),
	DCCP(3, null),
	RDP(3, null),
	XTP(3, null),
	DDP(3, null),
	RSVP(3, null),
	IPv6_ICMP(3, null),
	TTP(3, null),
	IPIP(3, null),
	GMTP(3, null),
	SCTP(3, null),
	UDPlite(3, null),
	HTTP(4, null),
	HTTPS(4, null),
	FTP(4, null),
	SFTP(4, null),
	FTPS(4, null),
	SSH(4, null),
	TELNET(4, null),
	TELNETS(4, null),
	LMTP(4, null),
	SMTP(4, null),
	SMTPS(4, null),
	POP2(4, null),
	POP3(4, null),
	POP3S(4, null),
	MTP(4, null),
	IMAP(4, null),
	IMAPS(4, null),
	IMAP3(4, null),
	SOCKS(4, null),
	IPSEC(4, null),
	DNS(4, new DNShandler()),
	NETBIOS(4, null),
	LDAP(4, null),
	RIP(4, null),
	RPC(4, null),
	SMB(4, null),
	DHCP(4, null),
	RTSP(4, null),
	IPP(4, null),
	PPTP(4, null),
	RTP(4, null),
	SIP(4, null),
	SIPS(4, null),
	MDNS(4, null),
	VNC(4, null);

	public static final String VERSION = "01";
	private int layer;
	private ProtocolHandler protocolHandler;
	
		
	private Protocol(int layer, ProtocolHandler protocolHandler) {
		this.layer = layer;
		this.protocolHandler = protocolHandler;
	}
		
		
	public int getLayer() {
		return this.layer;
	}
		
		
	public ProtocolHandler getProtocolHandler() {
		return this.protocolHandler;
	}
		
		
	public static Protocol getProtocol(int id) {
		Protocol[] protocolArray = Protocol.values();
		return (id >= protocolArray.length || id < 0) ? Protocol.UNKNOWN : protocolArray[id];
	}


	public static Protocol portToProtocol(int port) {
		switch (port) {
		case 80: return Protocol.HTTP;
		case 8080: return Protocol.HTTP;
		case 443: return Protocol.HTTPS;
		case 20: return Protocol.FTP;
		case 21: return Protocol.FTP;
		case 989: return Protocol.FTPS;
		case 990: return Protocol.FTPS;
		case 22: return Protocol.SSH;
		case 23: return Protocol.TELNET;
		case 24: return Protocol.LMTP;
		case 25: return Protocol.SMTP;
		case 465: return Protocol.SMTPS;
		case 109: return Protocol.POP2;
		case 110: return Protocol.POP3;
		case 995: return Protocol.POP3S;
		case 57: return Protocol.MTP;
		case 143: return Protocol.IMAP;
		case 993: return Protocol.IMAPS;
		case 220: return Protocol.IMAP3;
		case 992: return Protocol.SMTP;
		case 1080: return Protocol.SOCKS;
		case 1293: return Protocol.IPSEC;
		case 53: return Protocol.DNS;
		case 5353: return Protocol.MDNS;
		case 137: return Protocol.NETBIOS;
		case 138: return Protocol.NETBIOS;
		case 139: return Protocol.NETBIOS;
		case 389: return Protocol.LDAP;
		case 520: return Protocol.RIP;
		case 530: return Protocol.RPC;
		case 445: return Protocol.SMB;
		case 546: return Protocol.DHCP;
		case 547: return Protocol.DHCP;
		case 554: return Protocol.RTSP;
		case 631: return Protocol.IPP;
		case 1723: return Protocol.PPTP;
		case 5004: return Protocol.RTP;
		case 5005: return Protocol.RTP;
		case 5060: return Protocol.SIP;
		case 5061: return Protocol.SIPS;
		case 5900: return Protocol.VNC;
		default: return Protocol.UNKNOWN;
		}
	}
	
	
}
