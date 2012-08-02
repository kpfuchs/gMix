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
package framework.core.socket.socketInterfaces;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import framework.core.message.MixMessage;


public class ConcurrentCapacityAwareMixMessageQueue<E extends MixMessage> {
	
	private int capacity;
	private int messageCounter = 0;
	private int byteCounter = 0;
	private ArrayBlockingQueue<E> queue;
	private ReentrantLock lock;
    private Condition notEmpty;
    private Condition notFull;
	
    
	public ConcurrentCapacityAwareMixMessageQueue(int capacity) {
		this.capacity = capacity;
		this.queue = new ArrayBlockingQueue<E>(capacity);
		this.lock = new ReentrantLock(false);
		this.notEmpty = lock.newCondition();
		this.notFull =  lock.newCondition();
	}
	
	
	 public void add(E message) throws InterruptedException {
	        if (message == null) throw new NullPointerException();
	        lock.lockInterruptibly();
	        try {
	            try {
	                while (messageCounter == capacity)
	                    notFull.await();
	            } catch (InterruptedException e) {
	                notFull.signal();
	                throw e;
	            }
	            queue.put(message);
	            messageCounter++;
	            byteCounter += message.getByteMessage().length;
	            notEmpty.signal();
	        } finally {
	            lock.unlock();
	        }
	 }
	 

	 public E get() throws InterruptedException {
		 lock.lockInterruptibly();
		 try {
			 try {
				 while(messageCounter == 0)
					 notEmpty.await();
			 } catch (InterruptedException e) {
				 notEmpty.signal(); // propagate
				 throw e;
			 }
			 final ArrayBlockingQueue<E> queue = this.queue;
			 E result = queue.take();
			 messageCounter--;
			 byteCounter -= result.getByteMessage().length;
			 notFull.signal();
			 return result;
		 } finally {
			 lock.unlock();
		 }
	 }
     
	 
	 public E peek() {
		 lock.lock();
		 try {
			 final ArrayBlockingQueue<E> queue = this.queue;
			 return (messageCounter == 0) ? null : queue.peek();
		 } finally {
			 lock.unlock();
		 }
	 }
	 
	 
	 public int messagesAvailable() {
		 lock.lock();
		 try {
			 return messageCounter;
		 } finally {
			 lock.unlock();
		 }
	 }
	 
	 
	 public int bytesAvailable() {
		 lock.lock();
		 try {
			 return byteCounter;
		 } finally {
			 lock.unlock();
		 }
	 }
}
