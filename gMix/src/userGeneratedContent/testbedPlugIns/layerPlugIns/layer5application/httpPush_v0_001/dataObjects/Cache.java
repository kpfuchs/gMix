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


import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import staticContent.framework.util.Util;

/**
 * @author bash * Cache.java
 * 
 *         This class represents the cache. It is needed for improvements with
 *         caching. The underlying construct is a hashtable. This hashtable
 *         contains the cacheentry and an identifier as string. There is also a
 *         capacity. There is no obligation to use a capacity but then the cache
 *         may grow without border.
 * 
 */
public class Cache {

    // Eventlist
    private Hashtable<String, LinkedList<Connection>> eventList;

    // Capacity
    private int size;
    private ConcurrentLinkedQueue<String> capacityQueue;

    private Hashtable<String, CacheEntry> cacheStorage;

    // private ConcurrentHashMap<Integer, Connection> connectionMap;

    /**
     * Standard constructor
     * 
     * @param ConcurrentHashMap
     *            with ConnectionId and connection
     */
    public Cache(int size, ConcurrentHashMap<Integer, Connection> connectionMap) {
        this.size = size;
        cacheStorage = new Hashtable<String, CacheEntry>();
        // this.connectionMap = connectionMap;
        this.capacityQueue = new ConcurrentLinkedQueue<String>();
        eventList = new Hashtable<String, LinkedList<Connection>>();

    }

    /**
     * Add event to eventlist
     * 
     * @param the
     *            identifier of the response (the request)
     * @param event
     *            from the connection
     */
    public void addEventToEventList(String identifier, Connection connection) {
        if (!eventList.containsKey(identifier)) {
            eventList.put(identifier, new LinkedList<Connection>());
        }
        eventList.get(identifier).add(connection);

    }

    /**
     * Methode wird von EntryDataToMix aufgerufen
     * 
     * @param connection
     */
    public void invokeEventsOfConnection(Connection connection, String uri) {
        Cache cache = connection.getCache();
        if (connection.getIdentifierQueue().size() == 0) {
            synchronized (eventList) {

                if (cache.containsEntry(uri) && cache.isMessageComplete(uri)) {
                    byte[] nachricht = cache.getCacheEntry(uri);

                    connection.writeChunk(ByteBuffer.wrap(nachricht));
                    // connection.writeChunk(ByteBuffer.wrap(cache.getCacheEntry(uri)));
                } else {
                    addEventToEventList(uri, connection);
                    connection.getIdentifierQueue().add(uri);
                }
            }
        } else {
            connection.getIdentifierQueue().add(uri);

        }

    }

    /**
     * Invoke the registered event for a given request
     * Only call if method complete
     * 
     * @param identifier
     *            from the request
     */
    public void invokeEvents(String identifier) {
        // Check if Event is registered for identifier
        synchronized (eventList) {

            if (!eventList.containsKey(identifier)) {
                return;
            }
            // iterate all registered events
            for (Connection connection : eventList.get(identifier)) {
                String uri = identifier;
                do {

                    connection.writeData(getCacheEntry(uri));
                    uri = connection.getIdentifierQueue().peek();
                } while (uri != null && isMessageComplete(uri));
                if (connection.getIdentifierQueue().size() > 0) {
                    addEventToEventList(uri, connection);
                }

            }
            eventList.remove(identifier);
        }
    }

    /**
     * Adds a new cacheentry to the cache
     * The new entry is a byte[]
     * 
     * @param identifier
     * @param cacheentry
     *            as byte[]
     */
    public void addNewCacheEntry(String identifier, byte[] cache) {
        CacheEntry value = new CacheEntry(false, false, cache);
        cacheStorage.put(identifier, value);
        capacityQueue.add(identifier);
        if (cacheStorage.size() >= size) {
            cacheStorage.remove(capacityQueue.poll());
        }

    }

    /**
     * Same as addNewCacheEntry but awaits an cacheentry as parameter
     * 
     * @param identifier
     * @param cache
     */
    public void addCacheEntry(String identifier, CacheEntry cache) {

        cacheStorage.put(identifier, cache);

        capacityQueue.add(identifier);
        if (cacheStorage.size() >= size) {
            cacheStorage.remove(capacityQueue.poll());
        }
    }

    /**
     * Append a body to an existing cacheentry
     * 
     * @param message
     */
    public void appendMessageToEntry(String identifier, byte[] message) {
        byte[] cacheEntry = cacheStorage.get(identifier).getCache();
        cacheEntry = Util.concatArrays(cacheEntry, message);
        cacheStorage.get(identifier).setCache(cacheEntry);
    }

    /**
     * Set the completeflag for an entry
     * 
     * @param identifier
     * @param isComplete
     */
    public void setMessageComplete(String identifier, boolean isComplete) {
        cacheStorage.get(identifier).setComplete(isComplete);
    }

    /**
     * Return if entry complete
     * 
     * @param identifier
     * @return true if a entry is complete, false otherwise
     */
    public boolean isMessageComplete(String identifier) {
        return cacheStorage.get(identifier).isComplete();
    }

    /**
     * Method to modify a cacheentry
     * This flag show if a connection requested an entry
     * 
     * @param identifier
     * @param connectionId
     */
    public void setBrowserRequest(String identifier, int connectionId) {
        try{
        cacheStorage.get(identifier).setBrowserRequest(true, connectionId);
        } catch (Exception e){
            
        }
    }

    /**
     * Method to modify a cacheentry
     * This flag shows if an entry is already requested by the exitnode
     * 
     * @param identifier
     */
    public void setWebRequest(String identifier) {
        cacheStorage.get(identifier).setWebRequest(true);
    }

    /**
     * Returns a cacheentry by its identifier
     * 
     * @param identifier
     * @return cacheentry or null if entry not exists
     */
    public byte[] getCacheEntry(String identifier) {
        if (cacheStorage.containsKey(identifier)) {
            return cacheStorage.get(identifier).getCache();
        } else {
            return null;
        }
    }

    /**
     * Method to check if entry exists
     * 
     * @param identifier
     * @return if entry already in the cache
     */
    public boolean containsEntry(String identifier) {
        return cacheStorage.containsKey(identifier);
    }

    /**
     * Shows if entry is requested by exitnode
     * 
     * @param identifier
     * @return true if requested, false otherwise
     */
    public boolean getWebRequestStatus(String identifier) {
        return cacheStorage.get(identifier).isWebRequest();

    }

    /**
     * Shows if entry is requested by a connection
     * 
     * @param identifier
     * @return true if requested, false otherwise
     */
    public boolean getBrowserRequestStatus(String identifier) {
        return cacheStorage.get(identifier).isBrowserRequest();

    }

    /**
     * Debug method, dumps complete cache
     * 
     * @return complete cache
     */
    public String getCompleteCache() {
        String returnString = "CacheSize: " + cacheStorage.size() + "\n" + "\n";
        Set<String> keys = cacheStorage.keySet();
        for (String key : keys) {
            returnString = returnString + key + ": " + cacheStorage.get(key) + " " + cacheStorage.get(key).isComplete()
                    + "\r\n";
        }

        return returnString;
    }

    /**
     * Debug method, dumps complete eventlist
     * 
     * @return complete eventlist
     */
    public String getCompleteEventList() {
        String returnString = "EventListSize: " + eventList.size() + "\n";
        Set<String> keys = eventList.keySet();
        for (String key : keys) {
            returnString = returnString + key + ": " + eventList.get(key) + "\r\n";
        }

        return returnString;
    }

}
