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
 * 
 */
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.entryClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import staticContent.framework.clock.Clock;
import staticContent.framework.config.Settings;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.Cache;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.Connection;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.HttpInfo;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.HttpPartType;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.mix.ConnectionPoolInterface;

/**
 * @author bash
 * 
 * This class holds all connections with the webbrowser
 * 
 * The ConnectionPool holds a Socket and instantiates a new connection object if the webbrowser established a new connection-
 * The new connection is added to the connection hashtable to identify a connection by the id. 
 * 
 * If the webbrowser sends data on the stream the Pool added the receiving connection to a Queue. 
 * From this queue EntryDataToMix class could take the connection and exercise on the data.
 * 
 * 
 */
public class ApplicationConnectionPool extends Thread implements ConnectionPoolInterface {

    private Cache cache;
    private LinkedBlockingQueue<Connection> readableConnections;
    private SecureRandom randomGen = new SecureRandom();
    private int connectionCounter;
    private ConcurrentHashMap<Integer, Connection> connectionMap;
    private Clock clock;
    private Selector selector = null;
    private ServerSocketChannel serverChannel = null;
    private Settings settings;


    /**
	 * Constructor
	 */

    public ApplicationConnectionPool(int port, int bufferSize, LinkedBlockingQueue<Connection> readableConnections,
            ConcurrentHashMap<Integer, Connection> connectionMap, Settings settings) {
   //     this.entryClient = entryClient;
 //       this.port = port;
  //      this.bufferSize = bufferSize;
        this.readableConnections = readableConnections;
        this.connectionMap = connectionMap;
        clock = new Clock(settings);
        this.settings = settings;
        connectionCounter = 0;
        // Open ServerSocket NIO
        try {
            selector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);

            ServerSocket ss = serverChannel.socket();
            InetSocketAddress address = new InetSocketAddress(port);
            ss.bind(address);
            System.err.println("listening on 127.0.0.1:" +port +" for connections"); 

          //  System.out.println("Waiting for Connection on Port " + port);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
             serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (ClosedChannelException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        cache = new Cache(settings.getPropertyAsInt("HP_CACHE_SIZE"), connectionMap);

    }

    public Selector getSelector() {
        return selector;
    }

    /**
     * Thread method 
     * 
     * This method handles the incoming connection and instatiates a new connection.
     * It also adds a connection to the queue if a webbrowser sends data.
     */
    @Override
    public void run() {

        while (true) {
       
            try {
                 selector.select();
            } catch (IOException e) {
                continue;
            }
            synchronized (this) {
                // Synchronized is necessary to prevent a race condition
                // with the registerConnection Method

                // Set<SelectionKey> selectedKeys = ;
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                //Wait for event on socket
                while (it.hasNext()) {
                    SelectionKey key = (SelectionKey) it.next();
                    //True if new Connection is established
                    if (key.isAcceptable()) {
                        Connection newConnection = null;
                        // Accept the new connection
                        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                        SocketChannel sc = null;
                        try {
                            sc = ssc.accept();
                            sc.configureBlocking(false);
                            int connectionId = connectionCounter;
                            connectionCounter += 1;
                            // int connectionId = randomGen.nextInt();
                            newConnection = new Connection(sc, connectionId, clock, settings, this, cache);
                            newConnection.setStatusHTTPFromApp(new HttpInfo(HttpPartType.SocksAuth, 0));
                            newConnection.setStatusHTTPFromMix(new HttpInfo(HttpPartType.Header, 0));
                            // registerSocket(sc, newConnection,
                            // SelectionKey.OP_READ);
                            sc.register(selector, SelectionKey.OP_READ, newConnection);

                            connectionMap.put(connectionId, newConnection);

                      //      System.out.println("Got connection from " + sc);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            System.err.println("Connection could not be established");

                        }
                        it.remove();
                        
                     //True if connection contains data from Webbrowser
                    } else if (key.isReadable()) {
                        Connection connection = (Connection) key.attachment();
                        key.cancel();
                        it.remove();
                        this.readableConnections.add(connection);

                    }

                }
            }

        }

    }

    /**
     * Method to register a readflag for the underlying connection
     * 
     * @return SelectionKey (null if fails)
     * @throws IOException
     */
    public SelectionKey registerSocket(SocketChannel serverSocket, Connection connection, int event) throws IOException {
        SelectionKey newKey = null;
        synchronized (this) {
            selector.wakeup();
            selector.selectNow();
            newKey = serverSocket.register(selector, event, connection);
            return newKey;

        }

    }

    /**
     * @return the connectionMap
     */
    public ConcurrentHashMap<Integer, Connection> getConnectionMap() {
        return connectionMap;
    }

    /**
     * @param connectionMap the connectionMap to set
     */
    public void setConnectionMap(ConcurrentHashMap<Integer, Connection> connectionMap) {
        this.connectionMap = connectionMap;
    }


}
