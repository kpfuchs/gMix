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
 * Connection.java
 * 

 */
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import staticContent.framework.clock.Clock;
import staticContent.framework.config.Settings;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.mix.ConnectionPoolInterface;

/**
 * @author bash
 * 
 *         * This class represent a connection.
 * 
 *         It contains all information that is necessary to handle a connection
 *         This class is very important for the whole framework.
 * 
 *         A connection represents a socket which connects the nodes to a
 *         webbrowser on entry side or to a webserver on exit side. Each
 *         connection has a unique id. It knows the type of the incoming and
 *         outgoing messages (see HttpPartType). Each direction is a
 *         synchronized buffer to buffer the message exchange.
 * 
 *         Each connection writes its own log. The directory of the log is
 *         specified in the properties file.
 * 
 */
public class Connection {

    /**
     * ID of the Connection
     */
    private int id;

    /**
     * Channel to Application via HTTP
     */
    private SocketChannel serverSocket;

    /**
     * Variable for the responding connectionpool. The connectionpool manages
     * the connections
     */
    public ConnectionPoolInterface selector;

    /**
     * Cache
     */
    private Cache cache;

    /**
     * Logger is used for logging of connection events
     */
    // public TimeLog logger;

    /**
     * The following three variables are in use for logging purposes. Do NOT
     * touch
     */
    private int packageCounter;
    private ConcurrentLinkedQueue<Integer> packageCounterQueue;
    private Integer packageCounterTail;

    /**
     * Statusnotation of the connection is used for the communication via HTTP
     * statusHTTPFromApp symbolized the incoming connection from webbrowser or
     * webserver
     * statusHTTPFromMix symbolized the incoming connection from Mix
     */
    private HttpInfo statusHTTPFromApp;
    private HttpInfo statusHTTPFromMix;

    /**
     * Buffers the message from mix or from connection to browser or webserver
     */
    private SynchronizedBuffer mixBuffer;
    private SynchronizedBuffer connectionBuffer;
    private byte[] headerBuffer;

    /**
     * sendBuffer buffer an incomplete header. Only in use if header incomplete
     */
    private String sendBuffer;

    /**
     * The mix connections checks following boolean to prevent double using of
     * the from two or more threads. Is true if a thread uses the connection
     */
    private boolean inProgressReceive;
    private boolean inProgressSend;

    /**
     * The following booleans show if a message in a buffer is incomplete and
     * further messages are required
     */
    private boolean mixMessageIncomplete;
    private boolean connectionMessageIncomplete;

    /**
     * The methodqueue contains the method of the sent request
     * Main reason: A HEAD-request has no body. This is the only possibility to detect it
     */
    private ConcurrentLinkedQueue<String> methodQueue;

    /**
     * Counter for requests
     * May used to check the order of the responses
     * 
     */
    public int requestId;

    /**
     * Variable to link a header to the body (see activeCacheImprovement)
     */
    public int actualRequestId;

    /**
     * Queue of requestIds
     */
    public ConcurrentLinkedQueue<Integer> requestQueue;

    /**
     * Same as above but contains the uri of the request
     */
    private LinkedList<String> identifierQueue;

    /**
     * Hashtable to maintain the correct order of messages from mix if
     * counterpart adds responses of its own
     * The key is the requestId
     * The value is a queue of uri in the correct order
     * 
     * Example
     * Two initialrequests
     *  RequestId 0 (index) and 1 (home)
     * 
     * { 0: [ index ] 1: [ home ] }
     * 
     * The counterpart generates two additional (img1 and img2) requests from
     * request 0
     * 
     * { 0: [ index, img1, img2 ] 1: [ home ] }
     * 
     */
    public Hashtable<Integer, Queue<String>> requestTable;

    /**
     * @param serverSocket
     * @param id
     * @param status
     */
    public Connection(SocketChannel serverSocket, int id, Clock clock, Settings settings,
            ConnectionPoolInterface selector, Cache cache) {
        this.mixBuffer = new SynchronizedBuffer();
        this.connectionBuffer = new SynchronizedBuffer();
        this.serverSocket = serverSocket;
        this.id = id;
        packageCounter = 0;
        this.selector = selector;
        packageCounterQueue = new ConcurrentLinkedQueue<Integer>();
        // logger = new TimeLog(settings.getProperty("LOG_PATH"), "connection" +
        // String.valueOf(id), clock);
        methodQueue = new ConcurrentLinkedQueue<String>();
        inProgressReceive = false;
        inProgressSend = false;
        packageCounterTail = 0;
        this.cache = cache;
        requestId = 0;
        requestQueue = new ConcurrentLinkedQueue<Integer>();
        identifierQueue = new LinkedList<String>();
        sendBuffer = "";
        this.requestTable = new Hashtable<Integer, Queue<String>>();

    }

    /**
     * Method to write Data to the connection
     * 
     * @param payload
     */
    public void writeChunk(ByteBuffer payload) {
        synchronized (serverSocket) {
            try {
                // System.out.println("Connection: Start writting Message on Socket");

                serverSocket.write(payload);
                while (payload.remaining() > 0) {
                    Thread.sleep(5);
                    serverSocket.write(payload);
                }
                // System.out.println("Connection: Message ( " +
                // payload.capacity() + " bytes) is written on Socket");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }

    /**
     * Writes message to connction
     * 
     * @param message
     */
    public void writeData(byte[] message) {
        // System.out.println("-------------------------------------------------------"+new
        // String(message));
        writeChunk(ByteBuffer.wrap(message));
        identifierQueue.poll();
    }

    /**
     * Adds a request to requestTable
     * 
     * @param id
     *            requestId
     * @param uri
     */
    public void addIdToTable(int id, String uri) {
        if (!requestTable.containsKey(id)) {
            requestTable.put(id, new LinkedList<String>());
        }
        requestTable.get(id).add(uri);
    }

    /**
     * Method to add the name of an HTTP method to a queue. Necessary to detect a
     * HEAD Request
     * 
     * @param method
     */
    public void addMethodToQueue(String method) {
        methodQueue.add(method);

    }

    /**
     * Returns the method of an HTTP Request 
     * Necessary to detect a HEAD Request
     * 
     * @return and remove method
     */
    public String returnMethodFromQueue() {
        return methodQueue.poll();
    }

    /**
     * Returns the method of an HTTP Request 
     * Necessary to detect a HEAD Request
     * 
     * @return method
     */
    public String peekMethodFromQueue() {
        return methodQueue.peek();
    }

    /**
     * Method to identify received Message
     * 
     * @return messageID
     */
    public int incrementPackageCounter() {
        packageCounterQueue.add(packageCounter);
        packageCounterTail = packageCounter;
        packageCounter += 1;
        return packageCounter - 1;
    }

    /**
     * Adds messageid to queue
     * 
     * @param messageId
     */
    public void addPackageCounter(int messageId) {
        packageCounterTail = messageId;
        packageCounterQueue.add(messageId);
    }

    /**
     * return messageid from queue but does not remove it
     * 
     * @return messageid from top of queue
     */
    public int peekPackageCounter() {
        return packageCounterQueue.peek();
    }

    /**
     * returns last element of packagequeue
     * 
     * @return last element of queue
     */
    public int inspectPackageCounterTail() {
        return packageCounterTail;
    }

    /**
     * Method to identify sendt Messages
     * 
     * @return messageID
     */
    public int removeCounterFromList() {

        return packageCounterQueue.poll();
    }

    /**
     * Add an element to buffer
     * 
     * @param payload
     */
    public void addElementToMixBuffer(byte[] payload) {
        mixBuffer.addArrayToBuffer(payload);
    }

    // Getter and Setter part

    /**
     * Checks if mixbuffer contains elements
     * 
     * @return amount of bytes
     */
    public boolean isMixBuffer() {
        if (mixBuffer.getByteSize() > 0) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * Checks if ConnectionBuffer contains bytes
     * 
     * @return true if the form the connection contains data, false otherwise
     */
    public boolean isConnectionBuffer() {
        if (connectionBuffer.getByteSize() > 0) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * Returns serverChannel
     * 
     * @return serverChannel
     */
    public SocketChannel getServerSocket() {
        return serverSocket;
    }

    /**
     * Returns the connection id
     * 
     * @return connectionID
     */
    public int getId() {
        return id;
    }

    /**
     * @return the cache
     */
    public Cache getCache() {
        return cache;
    }

    /**
     * @return the requestQueue
     */
    public LinkedList<String> getIdentifierQueue() {
        return identifierQueue;
    }

    /**
     * @param inProgressSend
     *            the inProgressSend to set
     */
    public void setInProgressSend(boolean inProgressSend) {
        this.inProgressSend = inProgressSend;
    }

    /**
     * @return the inProgressSend
     */
    public boolean isInProgressSend() {
        return inProgressSend;
    }

    /**
     * @param inProgressReceive
     *            the inProgressReceive to set
     */
    public void setInProgressReceive(boolean inProgressReceive) {
        this.inProgressReceive = inProgressReceive;
    }

    /**
     * @return the inProgressReceive
     */
    public boolean isInProgressReceive() {
        return inProgressReceive;
    }

    /**
     * @param statusHTTPFromMix
     *            the statusHTTPFromMix to set
     */
    public void setStatusHTTPFromMix(HttpInfo statusHTTPFromMix) {
        this.statusHTTPFromMix = statusHTTPFromMix;
    }

    /**
     * @return the statusHTTPFromMix
     */
    public HttpInfo getStatusHTTPFromMix() {
        return statusHTTPFromMix;
    }

    /**
     * @param statusHTTPFromApp
     *            the statusHTTPFromApp to set
     */
    public void setStatusHTTPFromApp(HttpInfo statusHTTPFromApp) {
        this.statusHTTPFromApp = statusHTTPFromApp;
    }

    /**
     * @return the statusHTTPFromApp
     */
    public HttpInfo getStatusHTTPFromApp() {
        return statusHTTPFromApp;
    }

    /**
     * @return the connectionBuffer
     */
    public SynchronizedBuffer getConnectionBuffer() {
        return connectionBuffer;
    }

    /**
     * get the buffer for mix messages
     * 
     * @return the synchronized buffer for messages from the mix
     */
    public SynchronizedBuffer getMixBuffer() {
        return mixBuffer;
    }

    /**
     * @return the improvementBuffer
     */
    public String getSendBuffer() {
        return sendBuffer;
    }

    /**
     * @param improvementBuffer
     *            the improvementBuffer to set
     */
    public void setSendBuffer(String improvementBuffer) {
        this.sendBuffer = improvementBuffer;
    }

    /**
     * @return the headerBuffer
     */
    public byte[] getHeaderBuffer() {
        return headerBuffer;
    }

    /**
     * @param headerBuffer
     *            the headerBuffer to set
     */
    public void setHeaderBuffer(byte[] headerBuffer) {
        this.headerBuffer = headerBuffer;
    }

    /**
     * @return the mixMessageIncomplete
     */
    public boolean isMixMessageIncomplete() {
        return mixMessageIncomplete;
    }

    /**
     * @param mixMessageIncomplete
     *            the mixMessageIncomplete to set
     */
    public void setMixMessageIncomplete(boolean mixMessageIncomplete) {
        this.mixMessageIncomplete = mixMessageIncomplete;
    }

    /**
     * @return the connectionMessageIncomplete
     */
    public boolean isConnectionMessageIncomplete() {
        return connectionMessageIncomplete;
    }

    /**
     * @param connectionMessageIncomplete
     *            the connectionMessageIncomplete to set
     */
    public void setConnectionMessageIncomplete(boolean connectionMessageIncomplete) {
        this.connectionMessageIncomplete = connectionMessageIncomplete;
    }

    /**
     * Debugmethod to dump the complete requesttable
     * 
     * @return the complete request table as string
     */
    public String dumpRequestTable() {
        String returnString = "TableSize: " + requestTable.size() + "\n" + "\n";
        Set<Integer> keys = requestTable.keySet();
        for (int key : keys) {
            returnString = returnString + key + ": " + "\r\n";
            for (String entry : requestTable.get(key)) {
                returnString = returnString + entry + "\r\n";
            }
        }

        return returnString;
    }

}
