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
import framework.core.util.Util;


/**
 * Can only read the first 12 bytes of a DNS header ("last" supported field: "Total Additional RRs") yet.
 */
public class DNSpacket {

	public enum OPcode {
		QUERY,
		IQUERY,
		STATUS,
		UNKNOWN,
		NOTIFY,
		UPDATE,
		RESERVED6,
		RESERVED7,
		RESERVED8,
		RESERVED9,
		RESERVED10,
		RESERVED11,
		RESERVED12,
		RESERVED13,
		RESERVED14,
		RESERVED15,
	};
	
	
	public enum ReturnCode {
		NO_ERROR, // 0
		FORMAT_ERROR,
		SERVER_FAILURE,
		NAME_ERROR,
		NOT_IMPLEMENTED,
		REFUSED,
		YX_DOMAIN,
		YXRR_SET,
		NXRR_SET,
		NOT_ZONE, // 10
		UNKNOWN_11,
		UNKNOWN_12,
		UNKNOWN_13,
		UNKNOWN_14,
		UNKNOWN_15,
		BADVERS_BADSIG,
		BADKEY,
		BADTIME,
		BADMODE,
		BADNAME, // 20
		BADALG,
		BADTRUNC, 
		UNKNOWN // 23 - 65535
	};
	
	
	
	public static OPcode getOPcodeByUnsignedByteID(byte code) {
		return OPcode.values()[Util.unsignedByteToShort(code)];
	}
	
	
	public static OPcode getOPcodeByID(int code) {
		return OPcode.values()[code];
	}
	
	
	public static ReturnCode getReturnCodeByUnsignedByteID(byte code) {
		int index = Util.unsignedByteToShort(code);
		if (index > 22)
			return ReturnCode.UNKNOWN;
		else
			return ReturnCode.values()[index];
	}
	
	
	public static ReturnCode getReturnCodeByID(int code) {
		if (code > 22)
			return ReturnCode.UNKNOWN;
		else
			return ReturnCode.values()[code];
	}
	
	
	public static int getTransactionId(byte[] packet) {
		return Util.unsignedShortToInt(Arrays.copyOfRange(packet, 0, 2));
	}
	
	
	public static byte[] getTransactionIdRaw(byte[] packet) {
		return Arrays.copyOfRange(packet, 0, 2);
	}
	
	
	public static String getTransactionIdAsString(byte[] packet) {
		return Util.toHex(Arrays.copyOfRange(packet, 0, 2));
	}
	
	
	public static boolean isRequest(byte[] packet) {
		return !Util.getBitAt(7, packet[2]);
	}
	
	
	public static boolean isQuery(byte[] packet) {
		return isRequest(packet);
	}
	
	
	public static boolean isReply(byte[] packet) {
		return Util.getBitAt(7, packet[2]);
	}
	
	
	public static boolean isResponse(byte[] packet) {
		return isReply(packet);
	}
	
	
	public static int getOPcodeAsInt(byte[] packet) {
		byte result = 0;
		result = Util.setBitAt(3, Util.getBitAt(6, packet[2]), result);
		result = Util.setBitAt(2, Util.getBitAt(5, packet[2]), result);
		result = Util.setBitAt(1, Util.getBitAt(4, packet[2]), result);
		result = Util.setBitAt(0, Util.getBitAt(3, packet[2]), result);
		return Util.unsignedByteToShort(result);
	}
	
	
	public static OPcode getOPcode(byte[] packet) {
		return getOPcodeByID(getOPcodeAsInt(packet));
	}
	
	
	public static boolean isAuthoritativeAnswer(byte[] packet) {
		return Util.getBitAt(2, packet[2]);
	}
	
	
	public static boolean isTruncated(byte[] packet) {
		return Util.getBitAt(1, packet[2]);
	}
	
	
	public static boolean isRecursionDesired(byte[] packet) {
		return Util.getBitAt(0, packet[2]);
	}
	
	
	public static boolean isRecursionAvailable(byte[] packet) {
		return Util.getBitAt(7, packet[3]);
	}
	
	
	public static boolean isZ(byte[] packet) {
		return Util.getBitAt(6, packet[3]);
	}
	
	
	public static boolean isAuthenticatedData(byte[] packet) {
		return Util.getBitAt(5, packet[3]);
	}
	
	
	public static boolean isCheckingDisabled(byte[] packet) {
		return Util.getBitAt(4, packet[3]);
	}
	
	
	public static boolean isNoneAuthenticatedDataAcceptable(byte[] packet) {
		return isCheckingDisabled(packet);
	}
	
	
	public static ReturnCode getReturnCode(byte[] packet) {
		return getReturnCodeByID(packet[3] & (0x0f));
	}
	
	
	public static int getTotalQuestions(byte[] packet) {
		return Util.unsignedShortToInt(Arrays.copyOfRange(packet, 4, 6));
	}
	
	
	public static int getTotalAnswerRRs(byte[] packet) {
		return Util.unsignedShortToInt(Arrays.copyOfRange(packet, 6, 8));
	}
	
	
	public static int getTotalAuthorityRRs(byte[] packet) {
		return Util.unsignedShortToInt(Arrays.copyOfRange(packet, 8, 10));
	}
	
	
	public static int getTotalAdditionalRRs(byte[] packet) {
		return Util.unsignedShortToInt(Arrays.copyOfRange(packet, 10, 12));
	}
	
	
	public static String toString(byte[] packet) {
		StringBuffer sb = new StringBuffer();
		sb.append("DNS header: \n");
		sb.append(" transaction ID: " +DNSpacket.getTransactionId(packet) +"\n");
		boolean isRequest = DNSpacket.isRequest(packet);
		sb.append(" is query (request): " +isRequest +"\n");
		sb.append(" is resonse (reply): " +DNSpacket.isReply(packet) +"\n");
		sb.append(" OP code: " +DNSpacket.getOPcode(packet) +"\n");
		if (isRequest) {
			sb.append(" is truncated: " +DNSpacket.isTruncated(packet) +"\n");
			sb.append(" is recursion desired: " +DNSpacket.isRecursionDesired(packet) +"\n");
			sb.append(" is none authenticated data accepatble: " +DNSpacket.isNoneAuthenticatedDataAcceptable(packet) +"\n");
			sb.append(" total questions: " +DNSpacket.getTotalQuestions(packet) +"\n");
			sb.append(" total answer RRs: " +DNSpacket.getTotalAnswerRRs(packet) +"\n");
			sb.append(" total authority RRs: " +DNSpacket.getTotalAuthorityRRs(packet) +"\n");
			sb.append(" total additional RRs: " +DNSpacket.getTotalAdditionalRRs(packet));
		} else { // reply
			sb.append(" is authoritative answer: " +DNSpacket.isAuthoritativeAnswer(packet) +"\n");
			sb.append(" is truncated: " +DNSpacket.isTruncated(packet) +"\n");
			sb.append(" is recursion desired: " +DNSpacket.isRecursionDesired(packet) +"\n");
			sb.append(" is recursion available: " +DNSpacket.isRecursionAvailable(packet) +"\n");
			sb.append(" is Z: " +DNSpacket.isZ(packet) +"\n");
			sb.append(" is authenticated data: " +DNSpacket.isAuthenticatedData(packet) +"\n");
			sb.append(" is none authenticated data accepatble: " +DNSpacket.isNoneAuthenticatedDataAcceptable(packet) +"\n");
			sb.append(" return code: " +DNSpacket.getReturnCode(packet) +"\n");
			sb.append(" total questions: " +DNSpacket.getTotalQuestions(packet) +"\n");
			sb.append(" total answer RRs: " +DNSpacket.getTotalAnswerRRs(packet) +"\n");
			sb.append(" total authority RRs: " +DNSpacket.getTotalAuthorityRRs(packet) +"\n");
			sb.append(" total additional RRs: " +DNSpacket.getTotalAdditionalRRs(packet));
		}
		return sb.toString();
	}
}
