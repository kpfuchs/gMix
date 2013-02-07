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

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import evaluation.traceParser.engine.dataStructure.Packet;
import evaluation.traceParser.engine.filter.PacketFilter;
import framework.core.util.Util;


public class PacketIterator implements Iterator<Packet> {

	private PacketFilter filter;
	private PacketSource packetSource;
	private Packet actualPacket;
	boolean wasHasNextCalled = false;
	
	
	public PacketIterator(PacketSource packetSource) {
		this(packetSource, null);
	}
	
	
	public PacketIterator(PacketSource packetSource, PacketFilter filter) {
		this.packetSource = packetSource;
		this.filter = filter;
		Util.displayWarningOnLowReservedMemory();
	}
	
	
	@Override
	public boolean hasNext() {
		try {
			this.actualPacket = packetSource.readPacket();
			if (filter != null && actualPacket != null && filter.newRecord(actualPacket) == null) { // filter filtered the packet
				return hasNext();
			}
		} catch (IOException e) {
			e.printStackTrace();
			if (filter != null)
				filter.finished();
			packetSource.close();
			return false;
		}
		wasHasNextCalled = true;
		if (actualPacket == null) {
			if (filter != null)
				filter.finished();
			packetSource.close();
			return false;
		} else {
			return true;
		}
	}
	

	@Override
	public Packet next() {
		if (!wasHasNextCalled)
			hasNext();
		wasHasNextCalled = false;
		if (actualPacket == null)
			throw new NoSuchElementException();
		else
			return actualPacket;
	}

	
	@Override
	public void remove() {
		throw new UnsupportedOperationException("this iterator refers to a read only trace file on the hdd.");
	}
	
	
	public void reset() {
		packetSource.reset();
		actualPacket = null;
		wasHasNextCalled = false;
	}

	
	public void close() {
		packetSource.close();
		actualPacket = null;
		wasHasNextCalled = false;
	}
}
