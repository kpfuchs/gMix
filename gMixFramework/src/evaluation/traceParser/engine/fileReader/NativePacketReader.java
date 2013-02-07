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
package evaluation.traceParser.engine.fileReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import evaluation.traceParser.engine.dataStructure.Packet;


public class NativePacketReader extends PacketReader {

	private BufferedReader reader;
	private int read = 0;
	
	
	public NativePacketReader(InputStream sourceTraceFile) {
		super(sourceTraceFile);
		this.reader = new BufferedReader(new InputStreamReader(sourceTraceFile));
		
	}


	@Override
	public Packet readPacket() throws IOException {
		String line = reader.readLine();
		if (line == null)
			return null;
		read += line.length() + 1; // assume single "\n"
		return new Packet(line);
	}



	@Override
	public Packet readPacket(Packet reusePacket) throws IOException {
		String line = reader.readLine();
		if (line == null)
			return null;
		read += line.length() + 1; // assume single "\n"
		reusePacket.init(line);
		return reusePacket;
	}


	@Override
	public long getTotalBytesRead() {
		return read;
	}
	
}
