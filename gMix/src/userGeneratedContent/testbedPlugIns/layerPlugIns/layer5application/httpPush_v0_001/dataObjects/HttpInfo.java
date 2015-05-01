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
/**
 * HttpInfo.java

 */
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects;

import java.util.Hashtable;

/**
 * @author bash
 *  * 
 * This class represents the information of an HTTP package part.
 * It contains the type of the part (see HttpPartType), the header and the length.
 */
public class HttpInfo {

    /**
     * Type of the part
     */
	private HttpPartType type;
	/**
	 * Length of the part
	 */
	private int length;
	private boolean bodyImprovable = false;
	private boolean headerComplete;
	private boolean lastChunk;
	
	/**
	 * Corresponding header of the part
	 */
	private Hashtable<String, String> header;

	/**
	 * Constructor
	 */
	public HttpInfo() {
		this.header = new Hashtable<String,String>();
		headerComplete = false;
		lastChunk = false;
	}

	/**
	 * Constructor
	 * @param type
	 * @param length
	 */
	public HttpInfo(HttpPartType type, int length) {
		this.header = new Hashtable<String,String>();
		this.length = length;
		this.type = type;
		headerComplete = false;
	}

	/**
	 * @return the header
	 */
	public Hashtable<String, String> getHeader() {
		return header;
	}

	/**
	 * @param header the header to set
	 */
	public void setHeader(Hashtable<String, String> header) {
		this.header = header;
	}
	
	/**
	 * Add a header line to the header hashtable
	 * @param key
	 * @param value
	 */
	public void addHeaderlineToHeader(String key, String value){
		this.header.put(key, value);
	}

	/**
	 * @return the length
	 */
	public int getLength() {
		return length;
	}

	/**
	 * @return the type
	 */
	public HttpPartType getType() {
		return type;
	}

	/**
	 * Check if part is from type body not chunked encoded
	 * @return true if http part is a body, false otherwise 
	 */
	public boolean isBody() {
		return type == HttpPartType.Body ? true : false;
	}

	   /**
     * Check if part is from type body chunked encoded
     * @return true if http part is a body chunked encoded, false otherwise 
     */
	public boolean isBodyChunk() {
		return type == HttpPartType.BodyChunk ? true : false;
	}

	/**
	 * @return the bodyImprovable
	 */
	public boolean isBodyImprovable() {
		return bodyImprovable;
	}


	/**
	 * Check if part is header
	 * @return true if part is header, false otherwise
	 */
	public boolean isHeader() {
		return type == HttpPartType.Header ? true : false;
	}
	
    /**
     * Check if part is socks authentification message
     * @return true if part is SocksAuth, false otherwise
     */
	public boolean isSocksAuth() {
		return type == HttpPartType.SocksAuth ? true : false;
	}
	
    /**
     * Check if part is socks Reply message
     * @return true if part is SocksReply, false otherwise
     */
	public boolean isSocksReply() {
		return type == HttpPartType.SocksReply ? true : false;
	}

    /**
     * Check if part is socks request message
     * @return true if part is SocksRequest, false otherwise
     */
	public boolean isSocksRequest() {
		return type == HttpPartType.SocksRequest ? true : false;
	}

	/**
	 * @param bodyImprovable
	 *            the bodyImprovable to set
	 */
	public void setBodyImprovable(boolean bodyImprovable) {
		this.bodyImprovable = bodyImprovable;
	}

	/**
	 * @param length
	 *            the length to set
	 */
	public void setLength(int length) {
		this.length = length;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(HttpPartType type) {
		this.type = type;
	}

	/**
	 * @return the headerComplete
	 */
	public boolean isHeaderComplete() {
		return headerComplete;
	}

	/**
	 * @param headerComplete the headerComplete to set
	 */
	public void setHeaderComplete(boolean headerComplete) {
		this.headerComplete = headerComplete;
	}

    /**
     * @return the lastChunk
     */
    public boolean isLastChunk() {
        return lastChunk;
    }

    /**
     * @param lastChunk the lastChunk to set
     */
    public void setLastChunk(boolean lastChunk) {
        this.lastChunk = lastChunk;
    }

}
