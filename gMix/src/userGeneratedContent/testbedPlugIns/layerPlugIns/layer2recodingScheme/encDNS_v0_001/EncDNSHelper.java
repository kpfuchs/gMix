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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer2recodingScheme.encDNS_v0_001;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;

/**
 * This class provides methods which are used by other EncDNS classes.
 * 
 * @author Jens Lindemann
 */
public abstract class EncDNSHelper {
    /**
     * This method finds the end of the question name in a standard DNS message
     * and returns its position.
     * @param dnsmsg message to analyse
     * @return position of the question name's end
     */
     public static int findQuestionNameEnd(byte[] dnsmsg) {
        int qNameEnd = 12; // question name starts at byte 12 of a DNS message
        // read the length of a label and jump to the next length byte until
        // we hit the terminating 0-length label
        while(dnsmsg[qNameEnd] > 0) {
            qNameEnd += dnsmsg[qNameEnd] + 1;
        }
        return qNameEnd;
    }
     
     /**
      * Generates a standard DNS response signaling a server failure (SERVFAIL).
      * @param query query for which a SERVFAIL response should be generated
      * @return SERVFAIL response corresponding to query
      */
     public static byte[] generateServfail(byte[] query) {
        ByteBuffer buf = ByteBuffer.allocate(12);
        
        // --- DNS HEADER ---
        byte[] id = Arrays.copyOfRange(query, 0, 2);
        buf.put(id);
        buf.put((byte) 0x81); // QR=1 (response), OpCode=0000 (std. query), AA=0, TC=0, RD=1
        buf.put((byte) 0x82); // RA=1, Z,AD,CD=0, RCode=2 (SERVFAIL)
        buf.putShort((short) 0); // question count = 0
        buf.putShort((short) 0); // answer count = 0
        buf.putShort((short) 0); // authority count = 0
        buf.putShort((short) 0); // additional count = 0

        // convert to byte[]
        byte[] out = new byte[buf.position()];
        buf.position(0);
        buf.get(out);

        return out;
     }
     
     /**
      * Converts a zone name passed as a String into a form which can be used in DNS
      * messages (i.e. convert it to a byte[] and replace dots by label lengths).
      * @param znstr zone name String to convert
      * @return DNS representation of zone name
      */
     public static byte[] parseZoneNameString(String znstr) {
         ArrayList<Byte> al = new ArrayList<Byte>();
         
         byte llen = 0;
         // As we have to prepend the length to each label, it is easier to 
         // start at the end of the String.
         for(int i = znstr.length()-1; i >= 0; i--) {
             char c = znstr.charAt(i);
             if(c == '.') {
                 al.add(0, llen);
                 llen = 0;
             } else {
                 // A zone name is terminated by a 0-length label representing
                 // the root zone. Even if there is no . at the end of the 
                 // String representation (which is quite common), we'll have
                 // to add this label.
                 if (i == (znstr.length()-1)) {
                    al.add((byte)0);
                 }
                 
                 al.add(0, (byte)c);
                 llen++;
             } 
         }
         
         // There is no . at the beginning of a zone name, but we still have to
         // add the first label length.
         al.add(0, llen);
         
         byte[] urlarray = ArrayUtils.toPrimitive(al.toArray(new Byte[0]));
         return urlarray;
     }
     
     /**
      * Reads a file and puts its contents in a byte[].
      * WARNING: This will only read files of length<=Integer.MAX_VALUE
      * and return null for longer files.
      * @param filePath path of file to read
      * @return byte[] containing file contents
      */
     public static byte[] readByteArrayFromFile(String filePath) {
        try {
            File f = new File(filePath);
            if(f.length()>=Integer.MAX_VALUE) {
                System.err.println("Could not read file " + filePath + ": too large.");
                return null; // We don't really need to read larger files and a byte[] can't be longer anyway...
            }
            int flen = (int)f.length();
            byte[] array = new byte[flen];
           DataInputStream is = new DataInputStream(new FileInputStream(filePath));
           is.readFully(array);
           return array;
        } catch(FileNotFoundException e) {
            System.err.println("File could not be found: " + filePath);
            return null;
        } catch(IOException e) {
            System.err.println("Error when writing to file " + filePath);
            return null;
        }
     }
     
     /**
      * Checks whether the operating system is Windows.
      * @return true if running on Windows, false otherwise.
      */
     public static boolean osIsWindows() {
         String osname = System.getProperty("os.name", "generic").toLowerCase();
         return osname.startsWith("windows");
     }
     
     /**
      * Checks whether the operating system is Linux.
      * @return true if running on Linux, false otherwise.
      */
     public static boolean osIsLinux() {
         String osname = System.getProperty("os.name", "generic").toLowerCase();
         return osname.startsWith("linux");
     }
     
     /**
      * Checks whether the operating system is Mac OS.
      * @return true if running on Mac OS, false otherwise.
      */
     public static boolean osIsMacOS() {
         String osname = System.getProperty("os.name", "generic").toLowerCase();
         return osname.startsWith("mac");
     }
}
