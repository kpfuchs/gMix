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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Vector;

import evaluation.traceParser.engine.Protocol;
import framework.core.util.Util;


public class ERFpacket {
	
	public enum ERFtype {
		TYPE_LEGACY,// 0
		TYPE_HDLC_POS,
		TYPE_ETH,
		TYPE_ATM,
		TYPE_AAL5,
		TYPE_MC_HDLC,
		TYPE_MC_RAW,
		TYPE_MC_ATM,
		TYPE_MC_RAW_CHANNEL,
		TYPE_MC_AAL5,
		TYPE_COLOR_HDLC_POS, // 10
		TYPE_COLOR_ETH,
		TYPE_MC_AAL2,
		TYPE_IP_COUNTER,
		TYPE_TCP_FLOW_COUNTER,
		TYPE_DSM_COLOR_HDLC_POS,
		TYPE_DSM_COLOR_ETH,
		TYPE_COLOR_MC_HDLC_POS,
		TYPE_AAL2,
		TYPE_COLOR_HASH_POS,
		TYPE_COLOR_HASH_ETH, // 20
		TYPE_INFINIBAND,
		TYPE_IPV4,
		TYPE_IPV6,
		TYPE_RAW_LINK,
		NOT_DEFINED1,NOT_DEFINED2,NOT_DEFINED3,NOT_DEFINED4,NOT_DEFINED5,NOT_DEFINED6,NOT_DEFINED7, // 25-31
		RESERVED1,RESERVED2,RESERVED3,RESERVED4,RESERVED5,RESERVED6,RESERVED7,RESERVED8,RESERVED9,RESERVED10,RESERVED11,RESERVED,RESERVED12,RESERVED13,RESERVED14,RESERVED15, // 32-47
		TYPE_PAD // 48
	};	
	
	public enum ExtensionHeaderType {
		RESERVED0, // 0
		RESERVED1,
		RESERVED2,
		CLASSIFICATION,
		INTERCEPT_ID,
		RAW_LINK // 5
	}
	
	public enum CaptureInterface {
		INTERFACE_0_OR_A,
		INTERFACE_1_OR_B,
		INTERFACE_2_OR_C,
		INTERFACE_3_OR_D,
	}
		
	private final static double MAX_UNSIGNED_INT_VALUE = Math.pow(2, 32);
	
	
	public static byte[] readERFpacket(InputStream inputStream) throws IOException {
		byte[] erfHeader = new byte[16];
		byte[] result;
		erfHeader = Util.forceRead(inputStream, erfHeader);
		if (erfHeader == null) // eof
			return null;
		int packetLength = getRlen(erfHeader);
		byte[] erfPayload = new byte[packetLength - erfHeader.length];
		if (erfPayload.length > 0) {
			erfPayload = Util.forceRead(inputStream, erfPayload);
			if (erfPayload == null) // eof
				return null;
			result = Util.concatArrays(erfHeader, erfPayload);
		} else {
			result = erfHeader;
		}
		if (ERFpacket.isRxError(result) || ERFpacket.isDsError(result)) {
			//System.out.println("WARNING: skipping malformed packet"); 
			return readERFpacket(inputStream);
		} else {
			return result;
		}
	}
	
	
	public static byte[] getERFtimestampRaw(byte[] packet) {
		return Arrays.copyOfRange(packet, 0, 8);
	}
	
	
	public static double getERFtimestampFractionalSeconds(byte[] packet) {
		byte[] partialSecondsRaw = Arrays.copyOfRange(packet, 0, 4);
		return (double)Util.unsignedIntToLong(Util.reverse(partialSecondsRaw))/MAX_UNSIGNED_INT_VALUE;
	}
	
	
	public static long getERFtimestampFractionalMilliSeconds(byte[] packet) {
		byte[] partialSecondsRaw = Arrays.copyOfRange(packet, 0, 4);
		return Math.round(((double)Util.unsignedIntToLong(Util.reverse(partialSecondsRaw))/MAX_UNSIGNED_INT_VALUE) * 1000d); // read ERF 64-bit timestamp: (yes, the timestamp is little-endian while the rest is not...)
	}
	
	// unix time (ms passed since midnight, January 1, 1970 UTC; same format as returned by "System.currentTimeMillis()")
	public static long getERFtimestamp(byte[] packet) {
		long ms = getERFtimestampFractionalMilliSeconds(packet);
		long rest = Util.unsignedIntToLong(Util.reverse(Arrays.copyOfRange(packet, 4, 8)));
		return (rest * 1000) + ms;
	}
	
	
	public static Calendar getERFtimestampAsCalendar(byte[] packet) {
		Calendar result = Calendar.getInstance();
		result.setTimeInMillis(getERFtimestamp(packet));
		return result;
	}
	
	
	public static boolean isExtensionHeadrPresent(byte[] packet) {
		return Util.getBitAt(7, packet[8]);
	}
	
	
	public static ERFtype getERFtype(byte[] packet) {
		byte b = packet[8];
		b = Util.setBitAt(7, false, b); // bit 7 is a flag and not part of the extension header type identifier
		return ERFtype.values()[Util.unsignedByteToShort(b)];
	}
	
	
	public static Protocol getERFtypeAsProtocol(byte[] packet) {
		return erfTypeToProtocol(getERFtype(packet));
	}
	
	
	public static Protocol erfTypeToProtocol(ERFtype erfType) {
		if (erfType == ERFtype.TYPE_COLOR_ETH)
			return Protocol.Ethernet;
		else if (erfType == ERFtype.TYPE_DSM_COLOR_ETH)
			return Protocol.Ethernet;
		else if (erfType == ERFtype.TYPE_ETH)
			return Protocol.Ethernet;
		else if (erfType == ERFtype.TYPE_IPV4)
			return Protocol.IPv4;
		else if (erfType == ERFtype.TYPE_IPV6)
			return Protocol.IPv6;
		else
			return Protocol.UNKNOWN;
	}
	

	public static CaptureInterface getCaptureInterface(byte[] packet) {
		byte captureInterface = 0;
		captureInterface = Util.setBitAt(0, Util.getBitAt(0, packet[9]), captureInterface);
		captureInterface = Util.setBitAt(1, Util.getBitAt(1, packet[9]), captureInterface);
		return CaptureInterface.values()[captureInterface];
	}
	
	
	/**
	 * "When set, packets shorter than the snap length are not padded and rlen resembles wlen."
	 * @param packet
	 * @return
	 */
	public static boolean isVaryingLengthRecord(byte[] packet) {
		return Util.getBitAt(2, packet[9]); 
	}
	
	
	/**
	 * "truncation is depreciated and this bit is unlikely to be set in an ERF record."; indicate "insufficient buffer space" (when true: "wlen is still correct for the packet on the wire" and "rlen is still correct for the resulting record. But, rlen is shorter than expected from snap length or wlen values.")
	 * @param packet
	 * @return
	 */
	public static boolean isTruncatedRecord(byte[] packet) {
		return Util.getBitAt(3, packet[9]); 
	}
	
	
	/**
	 * "An error in the received data. Present on the wire"
	 * @param packet
	 * @return
	 */
	public static boolean isRxError(byte[] packet) {
		return Util.getBitAt(4, packet[9]); 
	}
	
	
	/**
	 * "An internal error generated inside the card annotator. Not present on the wire."
	 * @param packet
	 * @return
	 */
	public static boolean isDsError(byte[] packet) {
		return Util.getBitAt(5, packet[9]); 
	}
	
	
	/**
	 * "Record length in bytes. Total length of the record transferred over the PCI bus to storage. The timestamp of the next ERF record starts exactly rlen bytes after the start of the timestamp of the current ERF record."
	 * @param packet
	 * @return
	 */
	public static int getRlen(byte[] packet) {
		return Util.unsignedShortToInt(Arrays.copyOfRange(packet, 10, 12));
	}
	
	
	/**
	 * "Depending upon the ERF type this 16 bit field is either a loss counter of color field. The loss counter records the number of packets lost between the DAG card and the stream buffer due to overloading on the PCI bus. The loss is recorded between the current record and the previous record captured on the same stream/interface. The color field is explained under the appropriate type details."
	 * @param packet
	 * @return
	 */
	public static int getLossCounter(byte[] packet) {
		ERFtype type = getERFtype(packet);
		if (	type == ERFtype.TYPE_COLOR_ETH ||
				type == ERFtype.TYPE_COLOR_HASH_ETH ||
				type == ERFtype.TYPE_COLOR_HASH_POS ||
				type == ERFtype.TYPE_COLOR_HDLC_POS ||
				type == ERFtype.TYPE_COLOR_MC_HDLC_POS ||
				type == ERFtype.TYPE_DSM_COLOR_ETH ||
				type == ERFtype.TYPE_DSM_COLOR_HDLC_POS)
			throw new RuntimeException("cannot extract the loss counter from a clored packet; color extensions not implemented"); 
		return Util.unsignedShortToInt(Arrays.copyOfRange(packet, 12, 14));
	}
	
	
	/**
	 * "Wire length. Packet length "on the wire" including some protocol overhead. The exact interpretation of this quantity depends on physical medium. This may contain padding."
	* @param packet
	 * @return
	 */
	public static int getWlen(byte[] packet) {
		return Util.unsignedShortToInt(Arrays.copyOfRange(packet, 14, 16));
	}
	
	
	public static byte[][] getExtensionHeaders(byte[] packet) {
		if (!isExtensionHeadrPresent(packet)) {
			return null;
		} else {
			int extensionHeaderPointer = 16;
			Vector<byte[]> headers = new Vector<byte[]>();
			while (true) {
				boolean furtherHeadersPresent = Util.getBitAt(7, packet[extensionHeaderPointer]);
				headers.add(Arrays.copyOfRange(packet, extensionHeaderPointer, extensionHeaderPointer+8));
				extensionHeaderPointer += 8; // note that all headers currently specified ("EDM11-01v8 ERF Types": CLASSIFICATION, INTERCEPT_ID and INTERCEPT_ID) are 8 bytes long. -> no need for individual parsing
				if (!furtherHeadersPresent)
					break;
			}
			byte[][] result = new byte[headers.size()][];
			for (int i=0; i<headers.size(); i++)
				result[i] = headers.get(i);
			return result;
		}
	}
	
	
	public static int getLengthOfExtensionHeaders(byte[] packet) {
		if (!isExtensionHeadrPresent(packet)) {
			return 0;
		} else {
			int haeders = 1;
			int extensionHeaderPointer = 16;
			while (true) {
				boolean furtherHeadersPresent = Util.getBitAt(7, packet[extensionHeaderPointer]);
				extensionHeaderPointer += 8; // note that all headers currently specified ("EDM11-01v8 ERF Types": CLASSIFICATION, INTERCEPT_ID and INTERCEPT_ID) are 8 bytes long. -> no need for individual parsing
				haeders++;
				if (!furtherHeadersPresent)
					break;
			}
			return haeders;
		}
	}
	
	
	/*	example code on how to read the extension header contents:		
		// ----------------------------
		// read extension headers (start)
		// ----------------------------
		int lengthOfExtensionHeaders = 0; // in byte
		if (extensionHeaderPresent) {
			while (true) {
				byte extHeader = read();
				boolean furtherExtensionHeaderPresent = Util.getBitAt(7, extHeader);
				System.out.println("furtherExtensionHeaderPresent: " +furtherExtensionHeaderPresent); 
				type = Util.setBitAt(7, false, extHeader); // bit 7 is a flag and not part of the extension header type
				int extensionHeaderType = Util.unsignedByteToShort(type);
				System.out.println("extension header type: " +erfType +" (" +EXTENSION_HEADER_TYPES[extensionHeaderType] +")"); 
				if (extensionHeaderType == 3) { // CLASSIFICATION ("Used to report filter and steering results. Used in conjunction with ERF 21. TYPE_INFINIBAND")
					System.out.println("reading CLASSIFICATION header"); 
					lengthOfExtensionHeaders += 8;
					byte next = read();
					boolean searchHit =  Util.getBitAt(7, next); // rest of bits are meaningful
					System.out.println("searchHit: " +searchHit); 
					boolean searchHitMultipleLocations =  Util.getBitAt(6, next); // "Search Hit Multiple Locations, lowest-numbered shown."
					System.out.println("searchHitMultipleLocations: " +searchHitMultipleLocations); 
					boolean reserved53 =  Util.getBitAt(5, next);
					boolean reserved52 =  Util.getBitAt(4, next);
					System.out.println("reserved53: " +reserved53); 
					System.out.println("reserved52: " +reserved52);
					boolean[] userBits = new boolean[16];
					int ctr = -1;
					userBits[ctr++] = Util.getBitAt(3, next);
					userBits[ctr++] = Util.getBitAt(2, next);
					userBits[ctr++] = Util.getBitAt(1, next);
					userBits[ctr++] = Util.getBitAt(0, next);
					byte furtherBits = read();
					for (int i=7; i>=0; i--)
						userBits[ctr++] = Util.getBitAt(i, furtherBits);
					byte rest = read();
					userBits[ctr++] = Util.getBitAt(7, rest);
					userBits[ctr++] = Util.getBitAt(6, rest);
					userBits[ctr++] = Util.getBitAt(5, rest);
					userBits[ctr++] = Util.getBitAt(4, rest);
					boolean reserved35 = Util.getBitAt(3, rest);
					System.out.println("reserved35: " +reserved35);
					boolean dropSteeringBit = Util.getBitAt(2, rest);// "Drop Steering Bit. May have Stream Steering bits set too."
					System.out.println("dropSteeringBit: " +dropSteeringBit);
					boolean streamSteeringBit33 = Util.getBitAt(1, rest);// "Stream Steering Bits. Binary encoded."
					boolean streamSteeringBit32 = Util.getBitAt(0, rest);// "Stream Steering Bits. Binary encoded."
					System.out.println("streamSteeringBit33: " +streamSteeringBit33); 
					System.out.println("streamSteeringBit32: " +streamSteeringBit32); 
					byte[] seqNumber = read(4);// "Sequence Number from Blackbird framer chip."
					System.out.println("sequence number: " +Util.toHex(seqNumber)); // TODO: decode (31:20 Reserved set to 0; 19:4 Tag (user classification(data)); 3: Reserved; 2: Drop Steering Bit. May have Stream Steering bits set too.; 1:0 Stream Steering Bits. Binary encoded.)
				
				} else if (extensionHeaderType == 4) { // INTERCEPT_ID ("ID attached to intercepted packet."; "Used to identify packet as associated with a unique ID.")
					System.out.println("reading INTERCEPT_ID header"); 
					lengthOfExtensionHeaders += 8;
					read(); // skip reserved byte
					int interceptId = Util.unsignedShortToInt(read(2)); // "Record length in bytes. Total length of the record transferred over the PCI bus to storage. The timestamp of the next ERF record starts exactly rlen bytes after the start of the timestamp of the current ERF record."
					System.out.println("interceptId: " +interceptId); 
					read(4); // skip reserved
				} else if (extensionHeaderType == 5) { // RAW_LINK ("￼Extra information for ERF 24. TYPE_RAW_LINK records"; "Used in Raw Capture image for SONET/SDH. Used with ERF 24. TYPE_RAW_LINK")
					System.out.println("reading RAW_LINK header"); 
					lengthOfExtensionHeaders += 8;
					byte moreFragAndReservedPt1 = read();
					boolean moreFragmentation = Util.getBitAt(7, moreFragAndReservedPt1);
					System.out.println("moreFragmentation: " +moreFragmentation);
					read(2);// skip reserved pt2
					int sequenceNumber = Util.unsignedShortToInt(read(2));
					System.out.println("sequenceNumber: " +sequenceNumber);
					int rawLink_rate = Util.unsignedByteToShort(read()); // As defined in the SONET control register.
					if (rawLink_rate == 0) 
						System.out.println("rawLink_rate: 0 (reserved)"); 
					else if (rawLink_rate == 1) 
						System.out.println("rawLink_rate: 1 (OC3)"); 
					else if (rawLink_rate == 2) 
						System.out.println("rawLink_rate: 2 (OC12)"); 
					else if (rawLink_rate == 3) 
						System.out.println("rawLink_rate: 3 (OC48)"); 
					else if (rawLink_rate == 4) 
						System.out.println("rawLink_rate: 4 (OC192)"); 
					else
						System.err.println("warning: read unkown rawLink_rate: " +rawLink_rate); 
					int rawLink_type = Util.unsignedByteToShort(read());
					if (rawLink_type == 0) 
						System.out.println("rawLink_type: 0 (SONET mode)"); 
					else if (rawLink_type == 1) 
						System.out.println("rawLink_type: 1 (SDH)"); 
					else
						System.out.println("reserved: " +rawLink_type); 
				} else {
					System.err.println("warning: found unknown or reserved extension header type: " +extensionHeaderType); 
				}

				if (!furtherExtensionHeaderPresent)
					break;
			}
		}
		*/
	
	
	public static byte[] getPayloadStat(byte[] packet) {
		assert ERFpacket.getERFtype(packet) == ERFtype.TYPE_ETH: "not supported ERFtype: " +ERFpacket.getERFtype(packet);
		int payloadLength = packet.length - 16 - getLengthOfExtensionHeaders(packet) - 2; // rlen - ERF header (16 bytes) - Extension headers (optional) - Padding (2 bytes)
		return Arrays.copyOfRange(packet, packet.length-payloadLength, packet.length);
	}
	
	
	public static int getPayloadLengthStat(byte[] packet) {
		assert ERFpacket.getERFtype(packet) == ERFtype.TYPE_ETH: "not supported ERFtype: " +ERFpacket.getERFtype(packet);
		return ERFpacket.getWlen(packet);
		}
	
	
	public static String toString(byte[] packet) {
		StringBuffer sb = new StringBuffer();
		sb.append("erf packet header: \n");
		sb.append(" erf timestamp: " +ERFpacket.getERFtimestampAsCalendar(packet).getTime() +" (+" +ERFpacket.getERFtimestampFractionalMilliSeconds(packet) +"ms)"+"\n");
		sb.append(" erf type: " +ERFpacket.getERFtype(packet) +"\n");
		sb.append(" extension header present: " +ERFpacket.isExtensionHeadrPresent(packet) +"\n");
		sb.append(" capture interface: " +ERFpacket.getCaptureInterface(packet) +"\n");
		sb.append(" varying record length: " +ERFpacket.isVaryingLengthRecord(packet) +"\n");
		sb.append(" truncated: " +ERFpacket.isTruncatedRecord(packet) +"\n");
		sb.append(" rx error: " +ERFpacket.isRxError(packet) +"\n");
		sb.append(" ds error: " +ERFpacket.isDsError(packet) +"\n");
		sb.append(" record length: " +ERFpacket.getRlen(packet) +"\n");
		sb.append(" loss counter: " +ERFpacket.getLossCounter(packet) +"\n");
		sb.append(" wire length: " +ERFpacket.getWlen(packet) +"\n");
		byte[] payload = ERFpacket.getPayloadStat(packet);
		if (payload == null)
			sb.append(" payload: none");
		else
			sb.append(" payload (" +payload.length +" bytes): " +Util.toHex(payload));
		return sb.toString();
	}

}
