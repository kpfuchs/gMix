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
package framework.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;


/**
 * Data structure that can be used to send/receive messages via network without 
 * having to deal with message lengths. The addChunk(byte[]) method can be used 
 * on the receiver side to add message chunks received via network. The 
 * received parts will be recombined to the same message as intended by the 
 * sender.
 * Will create an Overhead of 4 bytes (length header).
 */
public class FragmentedMessage {
	
	private ByteBuffer headerCache = ByteBuffer.allocate(4); // length header
	private ByteBuffer messageCache = null;
	
	private final static String BAD_FRAGMENT_MESSAGE = 
			"bad fragment. some possible reasons: " +
			"\nthe method toSendableMessage(message) was not called on the" +
			"sender side" +
			"\nmessage bytes were changed during transfer (faulty " +
			"implementation of underying communication channel or none " +
			"reliable in-order channel)" +
			"\nthe bypassed byte array does not belong to this message";
	
	
	/**
	 * Use on receiver side only. On sender side, use the static method 
	 * toSendableMessage(message).
	 */
	public FragmentedMessage() {
		
	}
	
	
	/**
	 * Use this method on the sender side to prepare am message for sending via 
	 * network.
	 * @return
	 */
	public static byte[] toSendableMessage(byte[] message) {
		assert message != null;
		assert message.length != 0;
		// add length header:
		return Util.concatArrays(Util.intToByteArray(message.length), message);
	}
	
	
	/**
	 * Use this method on receiver side. reads a (possibly) fragmented message 
	 * from the given InputStream. blocks until the message is fully received 
	 * or an IOException occurs.
	 * if you don't want a blocking method, create a FragmentedMessage 
	 * instance and add fragments (addFragment(fragment)) until the message is
	 * fully received (check with method isFullyReceived()).
	 * @param inputStream
	 * @return the message read or null if EOF
	 */
	public static byte[] forceReadMessage(InputStream inputStream) throws IOException {
		int length = Util.forceReadInt(inputStream);
		if (length < 0)
			throw new RuntimeException(BAD_FRAGMENT_MESSAGE);
		return Util.forceRead(inputStream, length);
	}
	
	
	/**
	 * Use this method on the sender side to send a fragmentable message via 
	 * the bypassed OutputStream. Blocks until the message is fully transmitted 
	 * to the OutputStream.
	 * Do NOT use toSendableMessage(message) on the bypassed byte array before 
	 * sending.
	 * Does NOT call flush() after writing.
	 * 
	 * @param inputStream 
	 * @return 
	 */
	public static void writeMessage(OutputStream outputStream, byte[] message) throws IOException {
		if (message == null)
			throw new RuntimeException("bypassed message == null"); 
		if (outputStream == null)
			throw new RuntimeException("bypassed outputStream == null"); 
		if (message.length == 0)
			throw new RuntimeException("bypassed message is of length 0. can't send it via network"); 
		outputStream.write(Util.intToByteArray(message.length));
		outputStream.write(message);
	}
	
			
	/**
	 * Use this method on receiver side to add fragments until the message is
	 * received completely (see isFullyReceived()).
	 * @param fragment the message fragment received via network
	 * @return bytes of the bypassed array that were not needed to create the 
	 * FragmentedMessage (may belong to the next fragment), or null if all 
	 * bytes were added to this FragmentedMessage.
	 */
	public byte[] addFragment(byte[] fragment) {
		if (headerCache != null) { // header not yet read (completely)
			if (headerCache.hasRemaining()) { 
				if (fragment.length < headerCache.remaining()) { // not enough data to read header completely
					headerCache.put(fragment);
					return null;
				} else if (fragment.length == headerCache.remaining()) { // exactly enough data to read header completely
					headerCache.put(fragment);
					int length = Util.byteArrayToInt(headerCache.array());
					headerCache = null;
					if (length < 0)
						throw new RuntimeException(BAD_FRAGMENT_MESSAGE);
					messageCache = ByteBuffer.allocate(length);
					return null;
				} else if (fragment.length > headerCache.remaining()) { // more data available than needed to read header completely
					byte[][] splitted = Util.split(headerCache.remaining(), fragment);
					headerCache.put(splitted[0]);
					fragment = splitted[1]; // will be used by the code below this if statement
					int length = Util.byteArrayToInt(headerCache.array());
					headerCache = null;
					if (length < 0)
						throw new RuntimeException(BAD_FRAGMENT_MESSAGE);
					messageCache = ByteBuffer.allocate(length);
				}
			}
		}
		// Note: at this point we know that the header is read completely and that "fragment" contains data (see code above)
		
		// try to read rest of message:
		if (fragment.length <= messageCache.remaining()) { // the whole fragment belongs to this message (not enough or exactly enough data to read message completely)
			messageCache.put(fragment);
			return null;
		} else { // only a fraction of the fragment belongs to this message (more data available than needed to read message completely)
			byte[][] splitted = Util.split(messageCache.remaining(), fragment);
			messageCache.put(splitted[0]);
			return splitted[1]; // return not needed bytes (may belong to next fragmented message)
		}
	}
	
	
	public boolean isFullyReceived() {
		if (messageCache != null && messageCache.remaining() == 0)
			return true;
		else
			return false;
	}
	
	
	public byte[] getRawMessage() {
		if (!isFullyReceived())
			throw new RuntimeException("the message is not yet fully received! check the result of isFullyReceived() before calling getRawMessage()"); 
		return messageCache.array();
	}
	
}
