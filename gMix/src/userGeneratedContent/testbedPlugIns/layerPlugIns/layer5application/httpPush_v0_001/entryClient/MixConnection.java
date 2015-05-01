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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import staticContent.framework.socket.socketInterfaces.StreamAnonSocket;
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
 * When this class receives data from the mix it adds the data chunk to a queue. 
 * 
 */
public class MixConnection extends Thread implements MixWriteInterface {


	private StreamAnonSocket mixSocket;
    private OutputStream toMixStream;
    private InputStream fromMixStream;
    private ConcurrentHashMap<Integer, Connection> connectionMap;
    private LinkedBlockingQueue<Connection> writeableConnections;

    
    private volatile boolean blinker;
    /**
	 * 
	 */
    public MixConnection() {
    }

    
    /**
     * Constructor
     * 
     * @param mixAdress
     * @param port
     * @param connectionMap
     * @param writeableConnections
     */
    public MixConnection(StreamAnonSocket anonSocket, ConcurrentHashMap<Integer, Connection> connectionMap,
            LinkedBlockingQueue<Connection> writeableConnections) {
    	this.mixSocket = anonSocket;
    	try {
			this.fromMixStream = mixSocket.getInputStream();
			this.toMixStream = mixSocket.getOutputStream();
		} catch (IOException e) {
			throw new RuntimeException("could not open connection to mix: " +e.getMessage()); 
		}
    	this.blinker = true;
        this.connectionMap = connectionMap;
        this.writeableConnections = writeableConnections;

    }
    
    /**
     * Stops the connections with the mix
     */
    public void stopConnection() {
        blinker = false;
    }

    /**
     * Threadmethod
     * 
     * This method checks if the mixnetwork sends data to the client and adds the chunks to the queue
     */
    @Override
    public void run() {
        while (blinker) {
            {
                int id;
                int len;
                byte[] message;
                try {
                    id = Util.forceReadInt(fromMixStream); // read id
                    len = Util.forceReadInt(fromMixStream); // read length
                    message = Util.forceRead(fromMixStream, len);

                    addChunkToBufferQueue(id, message);
                    // }
                } catch (IOException e) { // connection do multiplexer lost
                    System.out.println("Lost connection to mix!");
                    e.printStackTrace();
                    try {
                        toMixStream.close();

                        fromMixStream.close();
                        mixSocket.disconnect();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    break;

                }

            }

        }
        try {
            toMixStream.close();

            fromMixStream.close();
            mixSocket.disconnect();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    /**
     * This helperclass add a datachunk to the queue for the EntryDataFromMix
     * @param id
     * @param payload
     */
    private void addChunkToBufferQueue(int id, byte[] payload) {

        try {
            Connection connection = connectionMap.get(id);

            connection.addElementToMixBuffer(payload);
            synchronized (connection) {

                if (!writeableConnections.contains(connection) && !connection.isInProgressReceive()) {

                    writeableConnections.add(connection);

                }
            }
        } catch (Exception e) {
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

}
