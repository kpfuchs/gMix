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

 */
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects;

/**
 * @author bash
 *  * CacheEntry.java
 * 
 * This class represents an entry of the cache.
 * It contains the message as byte[], the connectionId of the requesting connection.
 * It also contains three flags 
 * webRequest indicates if an entry is requested by the exitnode
 * browserRequest shows if an entry is requested by a connection
 * isComplete indicates if an entry is completed
 *
 */
public class CacheEntry {

	private boolean webRequest;
	private boolean browserRequest;
	private boolean isComplete;
	private byte[] cache;
	private int connectionId;
	
	
	/**
	 * Constructor
	 * @param connectionId
	 * @param browserRequest
	 */
	public CacheEntry(int connectionId, boolean browserRequest){
		this.connectionId = connectionId;
		this.browserRequest = browserRequest;
		this.isComplete = false;
	}
	
	/**
	 * Constructor
	 * @param webRequest
	 * @param browserRequest
	 */
	public CacheEntry(boolean webRequest, boolean browserRequest){
		this.webRequest = webRequest;
		this.browserRequest = browserRequest;
		this.isComplete = false;
	}
	
	/**
	 * Constructor 
	 * @param webRequest
	 * @param browserRequest
	 * @param cache
	 */
	public CacheEntry(boolean webRequest, boolean browserRequest, byte[] cache){
		this.webRequest = webRequest;
		this.browserRequest = browserRequest;
		this.cache = cache;
		this.isComplete = false;
	}


	/**
	 * @return the webRequest
	 */
	public boolean isWebRequest() {
		return webRequest;
	}

	/**
	 * @return the connectionId
	 */
	public int getConnectionId() {
		return connectionId;
	}

	/**
	 * @param connectionId the connectionId to set
	 */
	public void setConnectionId(int connectionId) {
		this.connectionId = connectionId;
	}

	/**
	 * @param webRequest the webRequest to set
	 */
	public void setWebRequest(boolean webRequest) {
		this.webRequest = webRequest;
	}

	/**
	 * @return the browserRequest
	 */
	public boolean isBrowserRequest() {
		return browserRequest;
	}

	/**
	 * @param browserRequest the browserRequest to set
	 * @param connectionId where the request came from
	 */
	public void setBrowserRequest(boolean browserRequest, int connectionId) {
		this.browserRequest = browserRequest;
		this.connectionId = connectionId;
	}
	
	
	

	/**
	 * @return the cache
	 */
	public byte[] getCache() {
		return cache;
	}

	/**
	 * @param cache the cache to set
	 */
	public void setCache(byte[] cache) {
		this.cache = cache;
	}

    /**
     * @return the isComplete
     */
    public boolean isComplete() {
        return isComplete;
    }

    /**
     * @param isComplete the isComplete to set
     */
    public void setComplete(boolean isComplete) {
        this.isComplete = isComplete;
    }

    /**
     * Debug method
     * @return an entry as String
     */
    public String dumpEntry() {
        String returnValue = new String(cache) + "\r\n" + isComplete;
        return returnValue;
    }
    
}
