/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2011  Karl-Peter Fuchs
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

package message;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import framework.LocalClassLoader;
import framework.Settings;
import userDatabase.User;


public abstract class MixMessage implements Message, ExternalMessage, Comparable<MixMessage> {
	
	private int identifier;
	private User owner;
	private byte[] byteMessage;
	private static int msgIdCounter = 0;
	
	public byte[] nextHopAddress;
	
	public int[] route;
	public byte[][] headers;

	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		
		try {
			//Get the System Classloader
	        ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
	        
	        //Get the URLs
	        URL[] urls = ((URLClassLoader)sysClassLoader).getURLs();

	        for(int i=0; i< urls.length; i++)
	        {
	            System.out.println(urls[i].getFile());
	        } 
	        
	        LocalClassLoader.instantiateMixRequest("Request.java", "message");
			/*System.get
			Enumeration e = ClassLoader.getSystemClassLoader().getResources("message.Reply");
			while(e.hasMoreElements())
				System.out.println(e.nextElement()); 
			System.out.println(); */
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		File verzeichnis = new File ("./");
		File[] dateien = verzeichnis.listFiles();
		for (File f: dateien)
			System.out.println(f); 
	} 
	
	
	public static Request getInstanceRequest(byte[] byteMesssage, User owner, Settings settings) {
		Request request = getInstanceRequest(byteMesssage, settings);
		request.setOwner(owner);
		return request;
	}
	
	
	public static Request getInstanceRequest(byte[] byteMesssage, Settings settings) {
		Request request = LocalClassLoader.instantiateMixRequest(settings);
		request.setByteMessage(byteMesssage);
		request.setIdentifier(msgIdCounter++);
		return request;
	}
	
	
	public static Request getInstanceRequest(Settings settings) {
		return LocalClassLoader.instantiateMixRequest(settings);
	}
	
	
	public static Reply getInstanceReply(byte[] byteMesssage, User owner, Settings settings) {
		Reply reply = LocalClassLoader.instantiateMixReply(settings);
		reply.setByteMessage(byteMesssage);
		reply.setOwner(owner);
		return reply;
	}
	
	
	public static Reply getInstanceReply(byte[] byteMesssage, Settings settings) {
		Reply reply = LocalClassLoader.instantiateMixReply(settings);
		reply.setByteMessage(byteMesssage);
		reply.setIdentifier(msgIdCounter++);
		return reply;
	}

	
	public static Reply getInstanceReply(Settings settings) {
		return LocalClassLoader.instantiateMixReply(settings);
	}
	
	
	@Override
	public int getIdentifier() {
		return identifier;
	}	
	
	
	@Override
	public void setIdentifier(int identifier) {
		this.identifier = identifier;
	}
	
	
	@Override
	public User getOwner() {
		return this.owner;
	}
	

	@Override
	public byte[] getByteMessage() {
		return this.byteMessage;
	}
	
	
	@Override
	public void setByteMessage(byte[] byteMessage) {
		this.byteMessage = byteMessage;
	}
	
	
	@Override
	public void setOwner(User owner) {
		this.owner = owner;
	}
	
	
	/**
	 * Implements the <code>Comparable</code> interface's <code>compareTo()
	 * </code> method. Compares this <code>MixMessage</code> with the specified 
	 * <code>MixMessage</code> for order (criterion: alphabetic order of this 
	 * <code>MixMessage</code>'s payload. Returns a negative integer, zero, or a 
	 * positive integer as this <code>MixMessage</code> is less than, equal to, or 
	 * greater than the specified <code>MixMessage</code>.
	 * 
	 * @param mixMessage	The <code>MixMessage</code> to be compared.
	 * 
	 * @return			-1, 0, or 1 as this <code>MixMessage</code> is less than, 
	 * 					equal to, or greater than the specified <code>MixMessage
	 * 					</code>.
	 * 
	 * @see #setPayloadRange(int, int)
	 */
	@Override
	public int compareTo(MixMessage mixMessage) {

		if (this.byteMessage.length < mixMessage.byteMessage.length)
			return -1;
		else if (this.byteMessage.length > mixMessage.byteMessage.length)
			return 1;	
		else { // both payloads have the same length
			for (int i=0; i<this.byteMessage.length; i++)
				if (this.byteMessage[i] < mixMessage.byteMessage[i])
					return -1;
				else if (this.byteMessage[i] > mixMessage.byteMessage[i])
					return 1;
			}
				
			// both payloads contain the same message
			return 0;
	}
	
}
