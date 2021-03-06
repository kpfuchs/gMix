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
package staticContent.evaluation.traceParser.engine.fileReader;

import java.io.IOException;
import java.io.InputStream;

import staticContent.evaluation.traceParser.engine.dataStructure.Packet;


public abstract class PacketReader {

	protected InputStream sourceTraceFile;

	
	public PacketReader(InputStream sourceTraceFile) {
		this.sourceTraceFile = sourceTraceFile;
	}
	
	
	public abstract Packet readPacket() throws IOException;
	public abstract Packet readPacket(Packet reusePacket) throws IOException;
	public abstract long getTotalBytesRead();
	
}
