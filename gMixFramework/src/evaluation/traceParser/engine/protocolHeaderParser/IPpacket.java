/*
 * gMix open source project _ https://svs.informatik.uni_hamburg.de/gmix/
 * Copyright (C) 2012  Karl_Peter Fuchs
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
package evaluation.traceParser.engine.protocolHeaderParser;

import java.net.InetAddress;

import framework.core.util.Util;


public class IPpacket {

	public enum IPprotocol {
		IPv6_HOPOPT, // 0
		ICMP,
		IGMP,
		GGP,
		IPv4,
		ST,
		TCP,
		CBT,
		EGP,
		IGP,
		BBN_RCC_MON, // 10
		NVP_II,
		PUP,
		ARGUS,
		EMCON,
		XNET,
		CHAOS,
		UDP,
		MUX,
		DCN_MEAS,
		HMP, // 20
		PRM,
		XNS_IDP,
		TRUNK_1,
		TRUNK_2,
		LEAF_1,
		LEAF_2,
		RDP,
		IRTP,
		ISO_TP4, 
		NETBLT, // 30
		MFE_NSP,
		MERIT_INP,
		DCCP,
		_3PC,
		IDPR,
		XTP,
		DDP,
		IDPR_CMTP,
		TPplusPlus,
		IL, // 40
		IPv6,
		SDRP,
		IPv6_Route,
		IPv6_Frag,
		IDRP,
		RSVP,
		GRE,
		MHRP,
		BNA,
		ESP, // 50
		IPv6_AH,
		I_NLSP,
		SWIPE,
		NARP,
		MOBILE,
		TLSP,
		SKIP,
		IPv6_ICMP,
		IPv6_NoNxt,
		IPv6_Dst_Opts, // 60
		AnyHostInternalProtocol,
		CFTP,
		AnyLocalNetwork,
		SAT_EXPAK,
		KRYPTOLAN,
		RVD,
		IPPC,
		AnyDistributedFileSystem,
		SAT_MON,
		VISA, // 70
		IPCV,
		CPNX,
		CPHB,
		WSN,
		PVP,
		BR_SAT_MON,
		SUN_ND,
		WB_MON,
		WB_EXPAK,
		ISO_IP, // 80
		VMTP,
		SECURE_VMTP,
		VINES,
		TTP,	
		NSFNET_IGP,
		DGP,
		TCF,
		EIGRP,
		OSPF,
		Sprite_RPC, // 90
		LARP,
		MTP,
		AX_25,
		IPIP,
		MICP,
		SCC_SP,
		ETHERIP,
		ENCAP,
		AnyPrivateEncryptionScheme,
		GMTP, // 100
		IFMP,
		PNNI,
		PIM,
		ARIS,
		SCPS,
		QNX,
		A_N,
		IPComp,
		SNP,
		Compaq_Peer, // 110
		IPX_in_IP,
		VRRP,
		PGM,
		AnyZeroHopProtocol,
		L2TP,
		DDX,
		IATP,
		STP,
		SRP,
		UTI, // 120
		SMP,
		SM,
		PTP,
		IS_ISoverIPv4,
		FIRE,
		CRTP,
		CRUDP,
		SSCOPMCE,
		IPLT,
		SPS, // 130
		PIPE,
		SCTP,
		FC,
		RSVP_E2E_IGNORE,
		IPv6_MobilityHeader,
		UDPlite,
		MPLS_in_IP,
		manet,
		HIP,
		Shim6, // 140
		UNASSIGNED // 141 - 255
	};

	
	public static IPprotocol getIPprotocolNumberByByte(byte code) {
		int index = Util.unsignedByteToShort(code);
		if (index > 140)
			return IPprotocol.UNASSIGNED;
		else
			return IPprotocol.values()[index];
	}
	
	
	public static boolean isIPv6ExtensionHeader(int code) {
		if ( 	code == 0 || 
				code == 60 || 
				code == 43 || 
				code == 44 || 
				code == 51 || 
				code == 50 || 
				code == 60 || 
				code == 135)
			return true;
		else
			return false;
	}

	
	public static boolean isIPv6ExtensionHeader(IPprotocol code) {
		return isIPv6ExtensionHeader(code);
	}
	
	
	public static boolean isIPv4(byte[] packet) {
		return getVersion(packet) == 4;
	}
	
	
	public static boolean isIPv6(byte[] packet) {
		return getVersion(packet) == 6;
	}
	
	
	public static int getVersion(byte[] packet) {
		return (packet[0] & (0xf0)) >> 4;
	}
	
	
	
	public static int getHeaderLengthStat(byte[] packet) {
		if (isIPv4(packet))
			return IPv4Packet.getHeaderLengthStat(packet);
		else
			return IPv6Packet.getHeaderLengthStat(packet);
	}
	
	
	/**
	 * header length + payload length in byte
	 */
	public static int getTotalLength(byte[] packet) {
		if (isIPv4(packet))
			return IPv4Packet.getTotalLength(packet);
		else
			return IPv6Packet.getPayloadLengthIncludingExtensionHeaders(packet) + 40;
	}
	

	
	public static IPprotocol getProtocol(byte[] packet) {
		if (isIPv4(packet))
			return IPv4Packet.getProtocol(packet);
		else
			return IPv6Packet.getProtocol(packet);
	}
	
	
	public static int getProtocolAsInt(byte[] packet) {
		if (isIPv4(packet))
			return IPv4Packet.getProtocolAsInt(packet);
		else
			return IPv6Packet.getProtocolAsInt(packet);
	}
	
	
	public static String getProtocolAsHex(byte[] packet) {
		if (isIPv4(packet))
			return IPv4Packet.getProtocolAsHex(packet);
		else
			return IPv6Packet.getProtocolAsHex(packet);
	}
	

	public static String getSrcIPasString(byte[] packet) {
		if (isIPv4(packet))
			return IPv4Packet.getSrcIPasString(packet);
		else
			return IPv6Packet.getSrcIPasString(packet);
	}
	
	
	public static InetAddress getSrcIP(byte[] packet) {
		if (isIPv4(packet))
			return IPv4Packet.getSrcIP(packet);
		else
			return IPv6Packet.getSrcIP(packet);
	}
	
	
	public static String getDstIPasString(byte[] packet) {
		if (isIPv4(packet))
			return IPv4Packet.getDstIPasString(packet);
		else
			return IPv6Packet.getDstIPasString(packet);
	}
	
	
	public static InetAddress getDstIP(byte[] packet) {
		if (isIPv4(packet))
			return IPv4Packet.getDstIP(packet);
		else
			return IPv6Packet.getDstIP(packet);
	}
	
	
	public static boolean containsPayload(byte[] packet) {
		if (isIPv4(packet))
			return IPv4Packet.containsPayload(packet);
		else
			return IPv6Packet.containsPayload(packet);
	}
	
	
	public static byte[] getPayloadStat(byte[] packet) {
		if (isIPv4(packet))
			return IPv4Packet.getPayloadStat(packet);
		else
			return IPv6Packet.getPayloadStat(packet);
	}
	
	
	public static String toString(byte[] packet) {
		if (isIPv4(packet))
			return IPv4Packet.toString(packet);
		else
			return IPv6Packet.toString(packet);
	}
	
}
