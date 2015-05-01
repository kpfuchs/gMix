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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer4transport.orderPreservingForwarder_v0_001;


public class QueueElement<T> implements Comparable<QueueElement<T>> {

	private int sequenceNumber = -1;
	private T queuedMessage;
	
	
	public QueueElement(int sequenceNumber, T message) {
		this.sequenceNumber = sequenceNumber;
		this.queuedMessage = message;
	}
	
	
	/*public QueueElement(T message) {
		assert message.length > 4;
		this.queuedMessage = message;
		byte[][] splitted = Util.split(4, message);
		this.sequenceNumber = Util.byteArrayToInt(splitted[0]);
		this.queuedMessage = splitted[1];
	}*/
	
	
	public int getSequenceNumber() {
		return this.sequenceNumber;
	}
	
	
	public T getMessage() {
		return this.queuedMessage;
	}


	@Override
	public int compareTo(QueueElement<T> o) {
		return sequenceNumber - o.getSequenceNumber();
	}

}
