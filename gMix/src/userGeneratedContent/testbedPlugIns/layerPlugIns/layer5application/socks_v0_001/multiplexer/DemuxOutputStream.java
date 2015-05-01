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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import staticContent.framework.util.Util;


public class DemuxOutputStream extends OutputStream {

	private OutputStream jointOsToMultiplexer;
	private int multiplexId;
	private volatile boolean isClosed = false;
	
	
	public DemuxOutputStream(OutputStream jointOsToMultiplexer, int multiplexId) {
		this.jointOsToMultiplexer = jointOsToMultiplexer;
		this.multiplexId = multiplexId;
	}
	

	@Override
	public void write(int b) throws IOException {
		synchronized (jointOsToMultiplexer) {
			jointOsToMultiplexer.write(formMultimplexMessage(new byte[]{(byte)b}));
		}
	}
	

    public void write(byte b[], int off, int len) throws IOException {
    	synchronized (jointOsToMultiplexer) {
    		jointOsToMultiplexer.write(formMultimplexMessage(Arrays.copyOfRange(b, off, off + len)));
    	}
    }
    
    
    public void flush() throws IOException {
    	synchronized (jointOsToMultiplexer) {
 			jointOsToMultiplexer.flush();
 		}
    }

    
    public void close() throws IOException {
    	synchronized (jointOsToMultiplexer) {
    		if (!isClosed) {
    			isClosed = true;
    			// send DISCONNECT message (or acknowledge DISCONNECT)
    			jointOsToMultiplexer.write(Util.concatArrays(new byte[][] {Util.intToByteArray(multiplexId), Util.shortToByteArray(-1)}));
     			jointOsToMultiplexer.flush();
    		}
 		}
    }
    
    
    private byte[] formMultimplexMessage(byte[] payload) {
    	return Util.concatArrays(new byte[][] {Util.intToByteArray(multiplexId), Util.shortToByteArray(payload.length), payload});
    	
    }
    
}
