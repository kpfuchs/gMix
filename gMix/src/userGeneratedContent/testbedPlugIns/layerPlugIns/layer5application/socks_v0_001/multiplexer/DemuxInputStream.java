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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.socks_v0_001.multiplexer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class DemuxInputStream extends InputStream
{

    protected ByteArrayOutputStream data;
    protected volatile boolean isClosed;

    public DemuxInputStream(ByteArrayOutputStream data)
    {
	this.data = data;
    }

    @Override
    public int read() throws IOException
    {
	synchronized (data)
	{
	    if (isClosed)
		return -1;
	    while (data.size() == 0)
	    { // wait for data if necessary
		try
		{
		    data.wait();
		} catch (InterruptedException e)
		{
		    e.printStackTrace();
		    continue;
		}
	    }
	    byte[] availableData = data.toByteArray();
	    data.reset();
	    byte result = availableData[0];
	    if (availableData.length > 1) // TODO: inefficient...
		data.write(availableData, 1, availableData.length - 1);
	    return result;
	}
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException
    {
	synchronized (data)
	{
	    if (isClosed)
		return -1;
	    if (b == null)
	    {
		throw new NullPointerException();
	    } else if (off < 0 || len < 0 || len > b.length - off)
	    {
		throw new IndexOutOfBoundsException();
	    } else if (len == 0)
	    {
		return 0;
	    }
	    while (data.size() == 0)
	    { // wait for data if necessary
		try
		{
		    data.wait();
		} catch (InterruptedException e)
		{
		    e.printStackTrace();
		    continue;
		}
	    }
	    byte[] availableData = data.toByteArray();
	    data.reset();
	    int toCopy = Math.min(availableData.length, len);
	    System.arraycopy(availableData, 0, b, off, toCopy);
	    if (toCopy < availableData.length) // TODO: inefficient...
		data.write(availableData, toCopy, availableData.length - toCopy);
	    return toCopy;
	}
    }

    public int available() throws IOException
    {
	synchronized (data)
	{
	    if (isClosed)
		return -1;
	    return data.size();
	}
    }

    public void close() throws IOException
    {
	synchronized (data)
	{
	    isClosed = true;
	}
    }

}