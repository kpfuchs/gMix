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
package evaluation.simulator.core.event;


public class Event implements Comparable<Event> {
	
	private long executionTime;
	private long sequenceNumber; // used to preserver order for events with same execution time
	private EventExecutor target;
	private SimulationEvent eventType;
	//private NetworkMessage networkMessage;
	private Object attachment;
	private boolean isCanceled = false;
	private boolean wasExecuted = false;
	
	
	/*public Event(EventExecutor target, int executionTime, SimulationEvent eventType, NetworkMessage networkMessage, Object attachment) {
		
		this.target = target;
		this.executionTime = executionTime;
		this.eventType = eventType;
		//this.networkMessage = networkMessage;
		this.attachment = attachment;
		
	}*/
	
	/*public Event(EventExecutor target, int executionTime, SimulationEvent eventType, NetworkMessage networkMessage) {
		
		this.target = target;
		this.executionTime = executionTime;
		this.eventType = eventType;
		this.networkMessage = networkMessage;
		
	}*/

	public Event(EventExecutor target, long executionTime, SimulationEvent eventType) {
		
		this.target = target;
		this.executionTime = executionTime;
		this.eventType = eventType;
		
	}
	
	public Event(EventExecutor target, long executionTime, SimulationEvent eventType, Object attachment) {
		
		this.target = target;
		this.executionTime = executionTime;
		this.eventType = eventType;;
		this.attachment = attachment;
		
	}
	
	
	public Event reuse(EventExecutor target, long executionTime, SimulationEvent eventType) {
		
		this.target = target;
		this.executionTime = executionTime;
		this.eventType = eventType;
		//this.attachment = null; // TODO: null or not?
		
		return this;
		
	}


	public Event reuse(EventExecutor target, long executionTime, SimulationEvent eventType, Object attachment) {
		
		this.target = target;
		this.executionTime = executionTime;
		this.eventType = eventType;;
		this.attachment = attachment;
		
		return this;
	}
	

	/**
	 * Implements the <code>Comparable</code> interface's <code>compareTo()
	 * </code> method. Compares this <code>Event</code> with the specified 
	 * <code>Event</code> for order (criterion: alphabetic order of this 
	 * <code>Event</code>'s <code>executionTime</code>. Returns a negative 
	 * integer, zero, or a positive integer as this <code>Event</code> is 
	 * less than, equal to, or greater than the specified <code>Event</code>.
	 * 
	 * @param event		The <code>Event</code> to be compared.
	 * 
	 * @return			-1, 0, or 1 as this <code>Event</code> is less than, 
	 * 					equal to, or greater than the specified <code>Event
	 * 					</code>.
	 */
	@Override
	public int compareTo(Event e) {
		int res = comp(this.executionTime, e.executionTime);
		return res == 0 ? comp(this.sequenceNumber, e.sequenceNumber) : res;
	}

	
	private int comp(long x, long y) {
		return (x < y) ? -1 : ((x == y) ? 0 : 1);
	}
	
	
	public void setSequenceNumber(long sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	
	public long getSequenceNumber() {
		return sequenceNumber;
	}

	/**
	 * @return the executionTime
	 */
	public long getExecutionTime() {
		return executionTime;
	}


	/**
	 * @param executionTime the executionTime to set
	 */
	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
	}


	/**
	 * @return the target
	 */
	public EventExecutor getTarget() {
		return target;
	}


	/**
	 * @param target the target to set
	 */
	public void setTarget(EventExecutor target) {
		this.target = target;
	}


	/**
	 * @return the eventType
	 */
	public SimulationEvent getEventType() {
		return eventType;
	}


	/**
	 * @param eventType the eventType to set
	 */
	public void setEventType(SimulationEvent eventType) {
		this.eventType = eventType;
	}


	/**
	 * @return the attachment
	 */
	public Object getAttachment() {
		return attachment;
	}


	/**
	 * @param attachement the attachment to set
	 */
	public void setAttachment(Object attachment) {
		this.attachment = attachment;
	}

	/**
	 * @return the networkMessage
	 */
	/*public NetworkMessage getNetworkMessage() {
		return networkMessage;
	}*/

	/**
	 * @param networkMessage the networkMessage to set
	 */
	/*public void setNetworkMessage(NetworkMessage networkMessage) {
		this.networkMessage = networkMessage;
	}*/
	
	
	@Override
	public String toString() {
		return eventType.toString();
	}


	public void cancel() {
		this.isCanceled = true;
	}
	
	
	public boolean isCanceled() {
		return this.isCanceled;
	}

	
	public boolean wasExecuted() {
		return wasExecuted;
	}

	
	public void setExecuted() {
		this.wasExecuted = true;
	}
	
}
