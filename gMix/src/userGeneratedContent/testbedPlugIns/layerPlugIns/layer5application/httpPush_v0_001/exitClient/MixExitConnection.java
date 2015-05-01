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
 * This Class provides the connection to the mix network
 */
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.exitClient;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import staticContent.framework.socket.socketInterfaces.StreamAnonSocketMix;
import staticContent.framework.util.Util;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.Connection;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.mix.MixWriteInterface;

/**
 * @author bash
 * 
 * This class handles the connection to the mix on the entry side
 * 
 * The connection over the mix is a multiplexed connection.
 * This class handles this connection.
 * 
 * To handle the multiplexed data the first four byte represent the id of the received connection. 
 * This id is a converted integer.
 * 
 * When this class receives data from mix, it adds the data chunk to a queue. 
 * 
 * 
 */
public class MixExitConnection extends Thread implements MixWriteInterface {

    private StreamAnonSocketMix mixConnection;
    private OutputStream toMixStream;
    private InputStream fromMixStream;
    private ConcurrentHashMap<Integer, Connection> connectionMap;
    private ExitConnectionPool connectionPool;
    private LinkedBlockingQueue<Connection> writeableConnections;

    private volatile boolean blinker;
    /**
	 * 
	 */
    public MixExitConnection(StreamAnonSocketMix mixConnection, ConcurrentHashMap<Integer, Connection> connectionMap,
            ExitConnectionPool connectionPool, LinkedBlockingQueue<Connection> writeableConnections) {
        blinker = true;
        this.connectionMap = connectionMap;
        this.writeableConnections = writeableConnections;
        this.connectionPool = connectionPool;
        this.mixConnection = mixConnection;
        this.fromMixStream = mixConnection.getInputStream();
        this.toMixStream = mixConnection.getOutputStream();
    }

    /**
     * Threadmethod
     * 
     * This method checks if the mixnetwork sends data to the client and adds the chunks to the queue
     */
    @Override
    public void run() {
        while (blinker) {

            int id;
            int len;
            byte[] message;

            try {
                id = Util.forceReadInt(fromMixStream);
                len = Util.forceReadInt(fromMixStream);
                message = Util.forceRead(fromMixStream, len);

                if (!connectionMap.containsKey(id)) {
                    // System.out.println("mix: generate new connection to internet");
                    connectionPool.generateConnectionFromSocks(id, message);
                } else {
                    addChunkToBufferQueue(id, message);

                }

            } catch (IOException e) {
                System.out.println("Lost connection to mix!");
                e.printStackTrace();
                
                try {
                    mixConnection.disconnect();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                // System.out.println("mix: entry closed connection!");
                break;

            }

        }
    }

    /**
     * This helperclass add a datachunk to the queue for the EntryDataFromMix
     * @param id
     * @param payload
     */
    private void addChunkToBufferQueue(int id, byte[] payload) {
        if (connectionMap.containsKey(id)) {
            Connection connection = connectionMap.get(id);
            connection.addElementToMixBuffer(payload);
            synchronized (connection) {

                if (!writeableConnections.contains(connection) && !connection.isInProgressSend()) {
                    writeableConnections.add(connection);
                }
            }

        } else {
            System.out.println("Unknown Connection");
        }

    }

    /**
     * Method to write data to mixnetwork
     */
    @Override
    public void writeChunk(byte[] message) {

        synchronized (toMixStream) {
            try {
                toMixStream.write(message);
                toMixStream.flush();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }
    
    /**
     * Method to stop the Mixconnection
     */
    public void stopConnection() {
        blinker = false;
    }

}
