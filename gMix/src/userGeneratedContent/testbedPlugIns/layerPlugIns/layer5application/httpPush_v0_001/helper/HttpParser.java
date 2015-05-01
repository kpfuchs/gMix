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
/**
 * 
 */
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Set;

/**
 * @author bash
 * 
 * Simple HTTP parser
 * Contains all methods neccessary to parse a HTTP message,
 * especially the header 
 * 
 */
public final class HttpParser {

 


    /**
     * This method parses an http header as byte[] to hashtable.
     * The keys are the header field names, the values are the corresponding values 
     * @param header
     * @return Hashtable of the http header
     */
	public static Hashtable<String, String> parseHeader(String header) {
		Hashtable<String, String> headers = new Hashtable<String, String>();
		String line;
		int index;
		String headerField = null;
		Scanner scanner = new Scanner(header);

		line = scanner.nextLine();
		String[] requestLine = line.split(" ");
		if (requestLine[0].trim().contains("HTTP")) {
			headers.put("protocol", requestLine[0].trim());
			headers.put("reply-code", requestLine[1].trim());
			headers.put("reply-message", requestLine[2].trim());
		} else {
			headers.put("method", requestLine[0].trim());
			headers.put("uri", requestLine[1].trim());
			headers.put("protocol", requestLine[2].trim());
		}

		while (scanner.hasNextLine()) {
			line = scanner.nextLine();
			if (!line.isEmpty() && Character.isWhitespace(line.charAt(0))) {
				headers.put(headerField, headers.get(headerField).concat(line));

			} else {
				index = line.indexOf(':');
				if (index < 0) {
					break;
				} else {
					headerField = line.substring(0, index).toLowerCase();
					headers.put(headerField, line.substring(index + 1).trim());
				}
			}
		}
		scanner.close();
		return headers;

	}

	/**
	 * Returns the length of an http header from a byte[]
	 * @param message
	 * @return length of an http header
	 */
	public static short getHeaderLengthFromByteArray(byte[] message) {
		int i = 0;
		while (i < message.length - 3) {
			if (message[i] == 13 && message[i + 1] == 10
					&& message[i + 2] == 13 && message[i + 3] == 10) {
				i = i + 4;
				return (short) i;
			}
			i++;
		}
		return -1;
	}

	   /**
     * Returns the length of an http header from an already parsed header
     * @param message
     * @return length of an http header
     */
	public static int getBodyLengthFromHeader(Hashtable<String, String> header) {
		String lengthAsString = header.get("content-length");
		int length = Integer.parseInt(lengthAsString);
		return length;
	}

	/**
	 * Method to determine bodytype
	 * 
	 * @param header
	 * @return int: 0 no body; 1 Body; 2 chunked Body; 3 body till server close
	 */
	public static int determineBodyType(Hashtable<String, String> header,
			String method) {
		if (method.equals("HEAD")) {
			return 0;
		} else if (header.containsKey("reply-code")
				&& (header.get("reply-code").equals(204)
						|| header.get("reply-code").equals(304) || header.get(
						"reply-code").startsWith("1"))) {
			return 0;
		} else if (header.containsKey("transfer-encoding")
				&& header.get("transfer-encoding").contains("chunked")) {
			return 2;
		} else if (header.containsKey("content-length")) {
			return 1;
		} else {
			return -1;
		}

	}

	/**
	 * This method returns the first line of a chunk part
	 * 
	 * @param chunk
	 * @return First line of a Chunk as String
	 */
	@Deprecated
	public static String getFirstStringFromChunk(byte[] chunk) {
		int i = 0;
		while (i < chunk.length) {
			if (chunk[i] == 13) {
				if ((chunk.length >= i+2) && chunk[i + 1] == 10) {
					String headLine = new String(Arrays.copyOfRange(chunk, 0,
							i + 2));
					return headLine;
				}
			}
			i++;
		}
		return null;

	}

	/**
	 * Returns length of an http chunked body from the corresponding headline
	 * @param headline 
	 * @return length of the chunk
	 */
	public static int getBodyLengthFromHeadLine(String headline) {
		int i = headline.indexOf(";");
		String size;
		if (i != -1) {
			size = headline.substring(0, i);

		} else {
			size = headline.substring(0, headline.length() - 2);
		}

		return Integer.parseInt(size, 16);
	}

	/**
	 * Generates an http request header from header hashtable
	 * @param header
	 * @return http request as string
	 */
	public static String composeGetRequest(Hashtable<String, String> header) {

		Set<String> keylist = header.keySet();
		String request = header.get("method") + " " + header.get("uri") + " "
				+ header.get("protocol") + "\r\n";
		ArrayList<String> copyKey = new ArrayList<String>(keylist);
		copyKey.remove("method");
		copyKey.remove("uri");
		copyKey.remove("protocol");
		for (String key : copyKey) {
			request += key + ": " + header.get(key) + "\r\n";
		}
		request += "\r\n";
		return request;
	}

}
