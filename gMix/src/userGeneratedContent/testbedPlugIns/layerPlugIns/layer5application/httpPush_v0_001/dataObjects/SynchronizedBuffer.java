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

 */
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * @author bash * SynchronizedBuffer.java
 * 
 *         This class represents a buffer for message handling.
 * 
 *         Incoming messages are added as byte[] to this buffer. The processing
 *         objects are able to remove messages from this buffer.
 * 
 *         The buffer contains a linked list of byte[].
 * 
 *         byte[] could be added to the buffer and removed if necessary.
 * 
 * 
 *         This buffer is threadsafe.
 * 
 */
public class SynchronizedBuffer {

    private LinkedList<byte[]> buffer;
    private int byteSize;

    /**
     * Constructor
     */
    public SynchronizedBuffer() {
        this.buffer = new LinkedList<byte[]>();
        this.byteSize = 0;

    }

    /**
     * Method to add a new element at the end of the List
     * 
     * @param message
     */
    public void addArrayToBuffer(byte[] message) {
        synchronized (buffer) {
            buffer.add(message);
            byteSize += message.length;
        }

    }

    /**
     * Method to get the first element of the queue without removing the element
     * from queue
     * 
     * @return byte[] from top of the list
     */
    public byte[] peekBuffer() {
        synchronized (buffer) {
            return buffer.peek();
        }

    }

    /**
     * This method allows to remove a designated amount of bytes from this
     * buffer
     * 
     * @param amount
     * @return `amount' bytes as byte[]
     */
    public byte[] removeBytes(int amount) {

        synchronized (buffer) {
            if (byteSize < amount) {
                return null;
            }
            byte[] result = new byte[amount];
            int pos = 0;
            byte[] buf = null;
            while (pos < amount) {
                buf = buffer.poll();
                System.arraycopy(buf, 0, result, pos, Math.min(amount - pos, buf.length));
                pos += buf.length;
            }
            if (pos > amount) {
                buffer.addFirst(Arrays.copyOfRange(buf, buf.length - (pos - amount), buf.length));
            }
            byteSize -= amount;
            return result;
        }
    }

    /**
     * This method allows to inspect, but not remove, a designated amount of
     * bytes from this buffer
     * 
     * @param amount
     * @return `amount' bytes as byte[]
     */
    public byte[] peekBytes(int amount) {

        synchronized (buffer) {
            if (byteSize < amount) {
                return null;
            }
            byte[] result = new byte[amount];
            int pos = 0;
            byte[] buf = null;
            int bufferPos = 0;
            while (pos < amount) {
                buf = buffer.get(bufferPos);
                System.arraycopy(buf, 0, result, pos, Math.min(amount - pos, buf.length));
                pos += buf.length;
                bufferPos += 1;
            }
            return result;
        }
    }

    /**
     * Removes the top of the list
     * 
     * @return byte[] from top of the list
     */
    public byte[] removeTopOfBuffer() {
        synchronized (buffer) {
            byteSize -= buffer.peek().length;
            return buffer.poll();

        }
    }

    /**
     * Return size of list
     * 
     * @return amount of byte[] contained in the buffer
     */
    public int getLength() {
        return buffer.size();
    }

    /**
     * Return the byte amount contained by the buffer
     * 
     * @return the byteSize
     */
    public int getByteSize() {
        return byteSize;
    }

    /**
     * Remove a header from top of the buffer
     * 
     * @return a header as string, null if there is no header on top of the
     *         buffer
     */
    public String getHeaderString() {
        synchronized (buffer) {
            int pos = 0;
            byte pre = 0;
            for (byte[] array : buffer) {
                for (int pos2 = 0; pos2 < array.length; pos2++) {
                    pos += 1;
                    if (pre == 13 && array[pos2] == 10) {

                        byte[] message = removeBytes(pos);
                        return new String(message, 0, pos);

                    }
                    pre = array[pos2];
                }
            }
            if (getByteSize() > 0) {
                int length = getByteSize();
                byte[] message = removeBytes(getByteSize());
                return new String(message, 0, length);
            }
            return null;
        }
    }

    /**
     * Method to return the length of a chunk from the first line of a chunk
     * 
     * @return chunk length in hex
     */
    public String getChunkHeaderLine() {
        byte pre = 0;
        int i = 0;
        for (byte[] array : buffer) {
            for (int pos2 = 0; pos2 < array.length; pos2++) {
                i += 1;
                if (pre == 13 && array[pos2] == 10) {

                    byte[] message = peekBytes(i);
                    return new String(message, 0, i);

                }
                pre = array[pos2];
            }
         
        }
        return null;

    }

    /**
     * Method for debugging purpose
     * 
     * @return complete buffer as String
     */
    public String getAllDataAsString() {
        String string = "readBytesSize: " + byteSize + "\r\n" + "queueSize: " + buffer.size() + "\r\n" + "DeterSize: "
                + getLength() + "\r\n";
        synchronized (buffer) {
            for (int i = 0; i < buffer.size(); i++) {
                string += "bufferpartlength: ";
                string += buffer.get(i).length;
                string += "\r\n";
            }
            for (int i = 0; i < buffer.size(); i++) {
                string += new String(buffer.get(i));

                string += "------------------";

            }
        }
        return string;
    }

}
