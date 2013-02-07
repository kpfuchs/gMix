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

import java.util.Arrays;
import java.util.zip.CRC32;

import framework.core.util.Util;


//format:
//0-5:		dest mac
//6-11:	src mac
//12-13:	type field (or vlan-tag)
//14-17:	payload or vlan field
//18-:		payload
//pad + crc: ignored
public class EthernetFrame {

	public static final int ETHER_TYPE_IP_V4 = 0x0800;
	public static final int ETHER_TYPE_IP_V6 = 0x86DD;
	public static final int ETHER_TYPE_ARP = 0x0806;
	public static final int ETHER_TYPE_VLAN = 0x8100;
	public static final int ETHER_TYPE_WOL = 0x0842;
	
	
	public static byte[] getEtherTypeRaw(byte[] frame) {
		byte[] type = Arrays.copyOfRange(frame, 12, 14);
		int etherType = Util.unsignedShortToInt(type);
		if (etherType != ETHER_TYPE_VLAN)
			return type;
		else
			return Arrays.copyOfRange(frame, 16, 18);
	}
	
	
	public static int getEtherType(byte[] frame) {
		return Util.unsignedShortToInt(getEtherTypeRaw(frame));
	}
	
	
	public static String getEtherTypeAsString(byte[] frame) {
		return etherTypeToString(getEtherType(frame));
	}
	
	
	public static boolean isVLAN(byte[] frame) {
		byte[] type = Arrays.copyOfRange(frame, 12, 14);
		return Util.unsignedShortToInt(type) == ETHER_TYPE_VLAN ? true : false;
	}
	
	
	public static byte[] getVLANtag(byte[] frame) {
		if (!isVLAN(frame))
			throw new RuntimeException("this is not a VLAN frame"); 
		return Arrays.copyOfRange(frame, 14, 16);
	}
	

	public static boolean isIEEE_802_3_Frame(byte[] frame) {
		return (getEtherType(frame) <= 1500) ? true : false;
	}
	
	
	public static boolean isEthernet2Frame(byte[] frame) {
		return (getEtherType(frame) > 1500) ? true : false;
	}
	
	
	/**
	 * in case of an ethernet II frame, the result MAY contain padding bytes.
	 * in case of an IEEE 802.3 frame, the result will NEVER contain padding.
	 * @param frame
	 * @return
	 */
	public static byte[] getPayloadStat(byte[] frame) { 
		int etherType = getEtherType(frame);
		int payloadLength;
		if (etherType <= 1500) { // IEEE 802.3 frame
			payloadLength = etherType;
			int startOffset = isVLAN(frame) ? 18 : 14;
			return (payloadLength < 1) ? null : Arrays.copyOfRange(frame, startOffset, startOffset + payloadLength);
		} else { // ethernet II frame
			payloadLength = isVLAN(frame) ? frame.length - 18 : frame.length - 14; 
			int crcLength = (frame.length < 64) ? 0 : 4; //  (frame.length < 64) indicates a truncated packet; 4 is the normal length of the crc field
			return (payloadLength < 1) ? null : Arrays.copyOfRange(frame, frame.length - payloadLength, frame.length -crcLength);
		}
	}

	
	public static int getLengthStat(byte[] frame) { 
		int etherType = getEtherType(frame);
		if (etherType <= 1500) { // IEEE 802.3 frame
			return etherType;
		} else { // ethernet II frame
			return frame.length;
		}
	}
	
	
	public static int getHeaderLengthStat(byte[] frame) { 
		return isVLAN(frame) ? 18 : 14;
	}
	
	
	public static int getPayloadLengthStat(byte[] frame) { 
		int etherType = getEtherType(frame);
		int payloadLength;
		if (etherType <= 1500) { // IEEE 802.3 frame
			return etherType;
		} else { // ethernet II frame
			payloadLength = isVLAN(frame) ? frame.length - 18 : frame.length - 14; 
			int crcLength = (frame.length < 64) ? 0 : 4; //  (frame.length < 64) indicates a truncated packet; 4 is the normal length of the crc field
			return payloadLength - crcLength;
		}
	}
	
	
	public static byte[] calculateCRC(byte[] frame) {
		CRC32 crcImpl = new CRC32();
		crcImpl.update(Arrays.copyOf(frame, frame.length - 4));
		return Arrays.copyOfRange(Util.longToByteArray(crcImpl.getValue()), 0, 4);
	}
	
	
	public static byte[] getCRC(byte[] frame) {
		return Arrays.copyOfRange(frame, frame.length-4, frame.length);
	}
	
	
	public static boolean isCRCcorrect(byte[] frame) {
		return Arrays.equals(getCRC(frame), calculateCRC(frame));
	}
	
	
	public static byte[] getDstMac(byte[] frame) {
		return Arrays.copyOfRange(frame, 0, 6);
	}
	
	
	public static String getDstMacAsString(byte[] frame) {
		return Util.toHex(getDstMac(frame));
	}
	
	
	public static byte[] getSrcMac(byte[] frame) {
		return Arrays.copyOfRange(frame, 6, 12);
	}
	
	public static String getSrcMacAsString(byte[] frame) {
		return Util.toHex(getSrcMac(frame));
	}

	
	public static boolean getIsIP(byte[] frame) {
		int etherType = getEtherType(frame);
		return etherType == ETHER_TYPE_IP_V4 || etherType == ETHER_TYPE_IP_V6;
	}
	
	
	public static boolean getIsIPv4(byte[] frame) {
		return getEtherType(frame) == ETHER_TYPE_IP_V4;
	}
	
	
	public static boolean getIsIPv6(byte[] frame) {
		return getEtherType(frame) == ETHER_TYPE_IP_V6;
	}
	
	public static boolean getIsARP(byte[] frame) {
		return getEtherType(frame) == ETHER_TYPE_ARP;
	}
	
	
	public static boolean getIsWOL(byte[] frame) {
		return getEtherType(frame) == ETHER_TYPE_WOL;
	}


	public static String etherTypeToString(int etherType) {
		switch (etherType) {
			case ETHER_TYPE_IP_V4:
				return "ETHER_TYPE_IP_V4";
			case ETHER_TYPE_IP_V6:
				return "ETHER_TYPE_IP_V6";
			case ETHER_TYPE_ARP:
				return "ETHER_TYPE_JUMBO_FRAMES";
			case ETHER_TYPE_VLAN:
				return  "ETHER_TYPE_VLAN";
			case ETHER_TYPE_WOL:
				return  "ETHER_TYPE_WOL";
			default:
				System.err.println("warning: unknown ether type: " +etherType);
				return ""+etherType;
		}
	}

	
	public static String toString(byte[] packet) {
		StringBuffer sb = new StringBuffer();
		sb.append("ethernet frame header: \n");
		sb.append(" source mac: " +EthernetFrame.getSrcMacAsString(packet) +"\n");
		sb.append(" destination mac: " +EthernetFrame.getDstMacAsString(packet) +"\n");
		sb.append(" type: " +EthernetFrame.getEtherTypeAsString(packet) +"\n");
		sb.append(" is IEEE 802.3 Frame: " +EthernetFrame.isIEEE_802_3_Frame(packet) +"\n");
		sb.append(" is Ethernet II Frame: " +EthernetFrame.isEthernet2Frame(packet) +"\n");
		sb.append(" crc correct: " +EthernetFrame.isCRCcorrect(packet) +"\n");
		boolean isVlan = EthernetFrame.isVLAN(packet);
		sb.append(" is vlan: " +isVlan +"\n");
		if (isVlan)
			sb.append("\n vlan-tag: " +Util.toHex(EthernetFrame.getVLANtag(packet)) +"\n");
		byte[] payload = EthernetFrame.getPayloadStat(packet);
		if (payload == null)
			sb.append(" payload: none");
		else
			sb.append(" payload (" +payload.length +" bytes): " +Util.toHex(payload));
		return sb.toString();
	}
	
}
