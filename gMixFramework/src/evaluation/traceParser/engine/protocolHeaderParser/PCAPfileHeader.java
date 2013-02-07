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
package evaluation.traceParser.engine.protocolHeaderParser;

import java.io.IOException;
import java.io.InputStream;

import evaluation.traceParser.engine.Protocol;
import framework.core.util.Util;

// see http://wiki.wireshark.org/Development/LibpcapFileFormat
public class PCAPfileHeader {

	private final static long MAGIC_NUMBER = 0xA1B2C3D4l;
	private final static long MAGIC_NUMBER_SWAPPED = 0xD4C3B2A1l;
	private boolean isSwapped;
	private int majorVersionNr;
	private int minorVersionNr;
	private int timeZoneOffest; // not used in practice (always 0)
	private int timestampAccurycy; // not used in practice (always 0)
	private int snaplen;
	private LinkLayerHeaderType linkLayerHeaderType;
	
	
	public enum LinkLayerHeaderType {
		LINKTYPE_NULL, // 0
		LINKTYPE_ETHERNET,
		RESERVED2,
		LINKTYPE_AX25,
		RESERVED4,
		RESERVED5,
		LINKTYPE_IEEE802_5,
		LINKTYPE_ARCNET_BSD,
		LINKTYPE_SLIP,
		LINKTYPE_PPP,
		LINKTYPE_FDDI,
		RESERVED11,
		RESERVED12,
		RESERVED13,
		RESERVED14,
		RESERVED15,
		RESERVED16,
		RESERVED17,
		RESERVED18,
		RESERVED19,
		RESERVED20,
		RESERVED21,
		RESERVED22,
		RESERVED23,
		RESERVED24,
		RESERVED25,
		RESERVED26,
		RESERVED27,
		RESERVED28,
		RESERVED29,
		RESERVED30,
		RESERVED31,
		RESERVED32,
		RESERVED33,
		RESERVED34,
		RESERVED35,
		RESERVED36,
		RESERVED37,
		RESERVED38,
		RESERVED39,
		RESERVED40,
		RESERVED41,
		RESERVED42,
		RESERVED43,
		RESERVED44,
		RESERVED45,
		RESERVED46,
		RESERVED47,
		RESERVED48,
		RESERVED49,
		LINKTYPE_PPP_HDLC,
		LINKTYPE_PPP_ETHER,
		RESERVED52,
		RESERVED53,
		RESERVED54,
		RESERVED55,
		RESERVED56,
		RESERVED57,
		RESERVED58,
		RESERVED59,
		RESERVED60,
		RESERVED61,
		RESERVED62,
		RESERVED63,
		RESERVED64,
		RESERVED65,
		RESERVED66,
		RESERVED67,
		RESERVED68,
		RESERVED69,
		RESERVED70,
		RESERVED71,
		RESERVED72,
		RESERVED73,
		RESERVED74,
		RESERVED75,
		RESERVED76,
		RESERVED77,
		RESERVED78,
		RESERVED79,
		RESERVED80,
		RESERVED81,
		RESERVED82,
		RESERVED83,
		RESERVED84,
		RESERVED85,
		RESERVED86,
		RESERVED87,
		RESERVED88,
		RESERVED89,
		RESERVED90,
		RESERVED91,
		RESERVED92,
		RESERVED93,
		RESERVED94,
		RESERVED95,
		RESERVED96,
		RESERVED97,
		RESERVED98,
		RESERVED99,
		LINKTYPE_ATM_RFC1483,
		LINKTYPE_RAW,
		RESERVED102,
		RESERVED103,
		LINKTYPE_C_HDLC,
		LINKTYPE_IEEE802_11,
		RESERVED106,
		LINKTYPE_FRELAY,
		LINKTYPE_LOOP,
		RESERVED109,
		RESERVED110,
		RESERVED111,
		RESERVED112,
		LINKTYPE_LINUX_SLL,
		LINKTYPE_LTALK,
		RESERVED115,
		RESERVED116,
		LINKTYPE_PFLOG,
		RESERVED118,
		LINKTYPE_IEEE802_11_PRISM,
		RESERVED120,
		RESERVED121,
		LINKTYPE_IP_OVER_FC,
		LINKTYPE_SUNATM,
		RESERVED124,
		RESERVED125,
		RESERVED126,
		LINKTYPE_IEEE802_11_RADIOTAP,
		RESERVED128,
		LINKTYPE_ARCNET_LINUX,
		RESERVED130,
		RESERVED131,
		RESERVED132,
		RESERVED133,
		RESERVED134,
		RESERVED135,
		RESERVED136,
		RESERVED137,
		LINKTYPE_APPLE_IP_OVER_IEEE1394,
		LINKTYPE_MTP2_WITH_PHDR,
		LINKTYPE_MTP2,
		LINKTYPE_MTP3,
		LINKTYPE_SCCP,
		LINKTYPE_DOCSIS,
		LINKTYPE_LINUX_IRDA,
		RESERVED145,
		RESERVED146,
		LINKTYPE_USER0_LINKTYPE_USER15_147,
		LINKTYPE_USER0_LINKTYPE_USER15_148,
		LINKTYPE_USER0_LINKTYPE_USER15_149,
		LINKTYPE_USER0_LINKTYPE_USER15_150,
		LINKTYPE_USER0_LINKTYPE_USER15_151,
		LINKTYPE_USER0_LINKTYPE_USER15_152,
		LINKTYPE_USER0_LINKTYPE_USER15_153,
		LINKTYPE_USER0_LINKTYPE_USER15_154,
		LINKTYPE_USER0_LINKTYPE_USER15_155,
		LINKTYPE_USER0_LINKTYPE_USER15_156,
		LINKTYPE_USER0_LINKTYPE_USER15_157,
		LINKTYPE_USER0_LINKTYPE_USER15_158,
		LINKTYPE_USER0_LINKTYPE_USER15_159,
		LINKTYPE_USER0_LINKTYPE_USER15_160,
		LINKTYPE_USER0_LINKTYPE_USER15_161,
		LINKTYPE_USER0_LINKTYPE_USER15_162,
		LINKTYPE_IEEE802_11_AVS,
		RESERVED164,
		LINKTYPE_BACNET_MS_TP,
		LINKTYPE_PPP_PPPD,
		RESERVED167,
		RESERVED168,
		LINKTYPE_GPRS_LLC,
		RESERVED170,
		RESERVED171,
		RESERVED172,
		RESERVED173,
		RESERVED174,
		RESERVED175,
		RESERVED176,
		LINKTYPE_LINUX_LAPD,
		RESERVED178,
		RESERVED179,
		RESERVED180,
		RESERVED181,
		RESERVED182,
		RESERVED183,
		RESERVED184,
		RESERVED185,
		RESERVED186,
		LINKTYPE_BLUETOOTH_HCI_H4,
		RESERVED188,
		LINKTYPE_USB_LINUX,
		RESERVED190,
		RESERVED191,
		LINKTYPE_PPI,
		RESERVED193,
		RESERVED194,
		LINKTYPE_IEEE802_15_4,
		LINKTYPE_SITA,
		LINKTYPE_ERF,
		RESERVED198,
		RESERVED199,
		RESERVED200,
		LINKTYPE_BLUETOOTH_HCI_H4_WITH_PHDR,
		LINKTYPE_AX25_KISS,
		LINKTYPE_LAPD,
		LINKTYPE_PPP_WITH_DIR,
		LINKTYPE_C_HDLC_WITH_DIR,
		LINKTYPE_FRELAY_WITH_DIR,
		RESERVED207,
		RESERVED208,
		LINKTYPE_IPMB_LINUX,
		RESERVED210,
		RESERVED211,
		RESERVED212,
		RESERVED213,
		RESERVED214,
		LINKTYPE_IEEE802_15_4_NONASK_PHY,
		RESERVED216,
		RESERVED217,
		RESERVED218,
		RESERVED219,
		LINKTYPE_USB_LINUX_MMAPPED,
		RESERVED221,
		RESERVED222,
		RESERVED223,
		LINKTYPE_FC_2,
		LINKTYPE_FC_2_WITH_FRAME_DELIMS,
		LINKTYPE_IPNET,
		LINKTYPE_CAN_SOCKETCAN,
		LINKTYPE_IPV4,
		LINKTYPE_IPV6,
		LINKTYPE_IEEE802_15_4_NOFCS,
		LINKTYPE_DBUS,
		RESERVED232,
		RESERVED233,
		RESERVED234,
		LINKTYPE_DVB_CI,
		LINKTYPE_MUX27010,
		LINKTYPE_STANAG_5066_D_PDU,
		RESERVED238,
		LINKTYPE_NFLOG,
		LINKTYPE_NETANALYZER,
		LINKTYPE_NETANALYZER_TRANSPARENT,
		LINKTYPE_IPOIB,
		LINKTYPE_MPEG_2_TS,
		LINKTYPE_NG40,
		LINKTYPE_NFC_LLCP,
		RESERVED246,
		RESERVED247,
		RESERVED248,
		RESERVED249,
		RESERVED250,
		RESERVED251,
		RESERVED252,
		RESERVED253,
		RESERVED254,
		RESERVED255
	};
	
	
	private PCAPfileHeader() {
		
	}
	
	
	public static Protocol linkLayerHeaderTypeToProtocol(LinkLayerHeaderType type) {
		if (type == LinkLayerHeaderType.LINKTYPE_ETHERNET) {
			return Protocol.Ethernet;
		} else if (type == LinkLayerHeaderType.LINKTYPE_IPV4) {
			return Protocol.IPv4;
		} else if (type == LinkLayerHeaderType.LINKTYPE_IPV6) {
			return Protocol.IPv6;
		} else
			return Protocol.UNKNOWN;
	}
	
	
	public static PCAPfileHeader readHeader(InputStream inputStream) throws IOException {
		PCAPfileHeader pcapHeader = new PCAPfileHeader();
		long magicNumber = Util.unsignedIntToLong(Util.forceRead(inputStream, 4));
		if (magicNumber == MAGIC_NUMBER) {
			pcapHeader.isSwapped = false;
		} else if (magicNumber == MAGIC_NUMBER_SWAPPED) {
			pcapHeader.isSwapped = true;
		} else 
			throw new RuntimeException("no PCAP header found. doesn't seem to be a PCAP file"); 
		pcapHeader.majorVersionNr = Util.unsignedShortToInt(pcapHeader.forceRead(inputStream, 2));
		pcapHeader.minorVersionNr = Util.unsignedShortToInt(pcapHeader.forceRead(inputStream, 2));
		pcapHeader.timeZoneOffest = (int) Util.unsignedIntToLong(pcapHeader.forceRead(inputStream, 4));
		if (pcapHeader.timeZoneOffest != 0)
			System.err.println("warning: timeZoneOffest in pcap header not set to 0"); // should always be 0 according to http://wiki.wireshark.org/Development/LibpcapFileFormat
		pcapHeader.timestampAccurycy = (int) Util.unsignedIntToLong(pcapHeader.forceRead(inputStream, 4));
		if (pcapHeader.timestampAccurycy != 0)
			System.err.println("warning: timestampAccurycy in pcap header not set to 0"); // should always be 0 according to http://wiki.wireshark.org/Development/LibpcapFileFormat
		pcapHeader.snaplen = (int) Util.unsignedIntToLong(pcapHeader.forceRead(inputStream, 4));
		pcapHeader.linkLayerHeaderType = LinkLayerHeaderType.values()[(int) Util.unsignedIntToLong(pcapHeader.forceRead(inputStream, 4))];
		return pcapHeader;
	}
	
	
	public byte[] forceRead(InputStream inputStream, int length) throws IOException {
		byte[] data = Util.forceRead(inputStream, length);
		if (data == null) // EOF
			return null;
		if (isSwapped) 
			return Util.reverse(data);
		else
			return data;
	}
	
	
	public boolean isSwapped() {
		return isSwapped;
	}
	
	
	public int getMajorVersionNr() {
		return majorVersionNr;
	}
	
	
	public int getMinorVersionNr() {
		return minorVersionNr;
	}
	
	
	public long getTimeZoneOffest() {
		return timeZoneOffest;
	}
	
	
	public long getTimestampAccurycy() {
		return timestampAccurycy;
	}
	
	
	public long getSnaplen() {
		return snaplen;
	}
	
	
	public LinkLayerHeaderType getLinkLayerHeaderType() {
		return linkLayerHeaderType;
	}
	
	
	public Protocol getLinkLayerHeaderTypeAsProtocol() {
		return linkLayerHeaderTypeToProtocol(linkLayerHeaderType);
	}
	
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(" pcap header: \n");
		sb.append("  is swapped: " +isSwapped +"\n");
		sb.append("  version: " +majorVersionNr +"." +minorVersionNr +"\n");
		sb.append("  timeZoneOffest: " +timeZoneOffest +"\n");
		sb.append("  timestampAccurycy: " +timestampAccurycy +"\n");
		sb.append("  snaplen: " +snaplen +"\n");
		sb.append("  linkLayerHeaderType: " +linkLayerHeaderType +"\n");
		return sb.toString();
	}
	
}
