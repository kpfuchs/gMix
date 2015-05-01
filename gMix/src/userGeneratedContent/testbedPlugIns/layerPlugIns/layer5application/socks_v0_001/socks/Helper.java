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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.socks_v0_001.socks;

import java.io.IOException;
import java.io.InputStream;

public class Helper {

    /**
     * Reads from InputStream until n zero bytes found or the end of the data is reached.
     * 
     * @param in	InputStream
     * @param n		number of zero bytes in a row
     * @return		read data
     * @throws IOException
     */
    public static byte[] forceReadUntilNzeroBytesOrEnd(InputStream in, int n) throws IOException
    {
	byte[] bufResult = new byte[65535]; // max size for udp packet
	byte read = (byte) -1;
	int countZeroBytes = 0;
	int countWrite = 0;
	int countRead = 0;
	int usefulBytes = 0;
	int bytesOnStream = in.available();
	System.out.println("Util: " + bytesOnStream);

	while (bytesOnStream == 0)
	{
	    bytesOnStream = in.available();
	}

	while (true)
	{
	    read = (byte) in.read();
	    countRead++;

	    if (read == 0x00)
	    {
		countZeroBytes++;
		if (countZeroBytes == n)
		{
		    usefulBytes = countWrite - (n - 1);
		    break;
		} else
		{
		    bufResult[countWrite] = read;
		    countWrite++;
		}
	    } else if (countRead == bytesOnStream) // there is nothing more to read
	    {
		bufResult[countWrite] = read;
		countWrite++;
		usefulBytes = countWrite;
		break;
	    } else
	    {
		countZeroBytes = 0;
		bufResult[countWrite] = read;
		countWrite++;
		System.out.println("bufResult[" + countWrite + "]: " + bufResult[countWrite]);
	    }
	}

	byte[] result = new byte[usefulBytes];
	for (int i = 0; i < usefulBytes; i++)
	{
	    result[i] = bufResult[i];
	}
	return result;
    }
}
