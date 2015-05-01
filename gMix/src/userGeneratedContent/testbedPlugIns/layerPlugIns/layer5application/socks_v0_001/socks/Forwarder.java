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
import java.io.OutputStream;

import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.socks_v0_001.Config;

/**
 * Just forward data.
 * Reads from an InputStream and writes to an OutputStream.
 * 
 * @author Haseloff, Schmitz, Sprotte
 *
 */
public class Forwarder extends Thread
{

    private byte[] buffer;
    private InputStream from;
    private OutputStream to;
    private Config config;
    

    /**
     * Construtor.
     * 
     * @param from	InputStream should be read from
     * @param to	OuputStream should be write to
     */
    public Forwarder(InputStream from, OutputStream to, Config config)
    {
	this.from = from;
	this.to = to;
	this.buffer = new byte[config.BUFFER_SIZE];
	this.config = config;
    }

    public void close()
    {
	try
	{
	    from.close();
	    to.close();
	} catch (IOException e)
	{
	}
    }

    @Override
    public void run()
    {
	while (true)
	{
	    int readBytes;
	    try
	    {
		readBytes = from.read(buffer);
		if (readBytes < 1)
		{ // TODO
		    close();
		    break;
		}
	    } catch (IOException e)
	    {
		if (config.DEBUG)
		    e.printStackTrace();
		close();
		break;
	    }
	    try
	    {
		if (readBytes > 0)
		{
		    to.write(buffer, 0, readBytes);
		    to.flush();
		}
	    } catch (IOException e)
	    {
		if (config.DEBUG)
		    e.printStackTrace();
		close();
		break;
	    }
	}
    }

}
