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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.mix;

import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.util.concurrent.LinkedBlockingQueue;

import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.Connection;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.HttpInfo;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.HttpPartType;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.SynchronizedBuffer;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.helper.HttpParser;

/**
 * @author bash
 * 
 * This class represents the superclass for all communications from mix to the webbrowser or webserver.
 * It contains methods with is used by the subclasses EntryDataFromMix and ExitDataFromMix
 * 
 * The data chunks from the mix are added to a queue by the mix connection.
 * 
 */
public abstract class DataFromMix extends Thread {

    /**
     * Queue of connections which contains data
     */
    public LinkedBlockingQueue<Connection> writeableChunks;
    
    

    /**
     * Constructor
     * 
     * @param writeableChunks
     */
    public DataFromMix(LinkedBlockingQueue<Connection> writeableChunks) {

        this.writeableChunks = writeableChunks;
    }

    /**
     * Method for threadhandling
     * This method checks the blocking queue for connections with data from mix. 
     * If a connection receives data from the mix the handling by this class is started.
     * The called method depends on the expected message type stored in the connection object
     */
    @Override
    public void run() {
        while (true) {

            Connection connection = null;
            try {
                // System.out.println("DataFromMix: Wait for Connections");
            	
            	//Blocking queue
                connection = writeableChunks.take();
                connection.setInProgressReceive(true);
                // System.out.println("DataFromMix: receive Connection "
                // + connection.getId() + " Connection from Queue");
            } catch (InterruptedException e) {
                break;
            }
            // System.out.println("DataFromMix: receive Connection "
            // + connection.getId() + " Buffer with Payload");
            synchronized (connection) {

                connection.setMixMessageIncomplete(false);
                while (connection.isMixBuffer()) {
                    // connection.logger.logEntry(connection.getId(), "entry",
                    // HttpPartType.SocksAuth, 0, buffer.detemineSize(),
                    // buffer.getAllDataAsString());
                    HttpPartType type = connection.getStatusHTTPFromMix().getType();
                    System.out.println("DataFromMix: Connection "
                     + connection.getId() + " has Type " + type.toString());
                    // System.out.println(connection.getMixBuffer().getAllDataAsString());
                    
                    // Method call depending on message type
                    switch (type) {
                    case Header:
                        headerHandling(connection);
                        break;
                    case Body:
                        bodyHandling(connection);
                        break;
                    case BodyChunk:
                        bodyChunkHandling(connection);
                        break;
                    case SocksReply:
                        socksReplyHandling(connection);
                        break;
                    case SocksAuth:
                        socksAuthHandling(connection);
                        break;
                    case SocksRequest:
                        socksRequestHandling(connection);
                        break;
                    default:
                        System.out.println("Unknown Type!");
                        break;
                    }
                    if (connection.isMixMessageIncomplete()) {
                        break;
                    }
                }
                connection.setInProgressReceive(false);

            }
        }

    }

    /**
     * Method for sockshandling
     * 
     * @param message
     * @param connection
     */
    public SynchronizedBuffer socksRequestHandling(Connection connection) {
        SynchronizedBuffer buffer = connection.getMixBuffer();
        byte[] message = buffer.peekBuffer();
        int length = 0;
        if (message[3] == 0x01) {
            length = 10;
        } else if (message[3] == 0x02) {
            length = 6 + message[4];
        } else if (message[3] == 0x03) {
            length = 22;
        } else {
            System.out.println("ERROR in Socks parsing!");
        }
        byte[] payload = buffer.removeBytes(length);
        writeChunkToConnection(connection, payload);
        connection.getStatusHTTPFromMix().setType(HttpPartType.Header);
        return buffer;

    }

    /**
     * Method for sockshandling Not Implemented
     * 
     * @param connection
     */
    @Deprecated
    public SynchronizedBuffer socksAuthHandling(Connection connection) {
        System.err.println("ERROR: Not implemented Method DataFromMix 132");
        return null;

    }

    /**
     * Method for Sockshandling
     * 
     * @param connection
     */
    public SynchronizedBuffer socksReplyHandling(Connection connection) {
        int length = 0;
        SynchronizedBuffer buffer = connection.getMixBuffer();
        byte[] message = buffer.peekBuffer();
        if (message[3] == 0x01) {
            length = 10;
        } else if (message[3] == 0x02) {
            length = 6 + message[4];
        } else if (message[3] == 0x03) {
            length = 22;
        } else {
            System.out.println("ERROR in Socks parsing!");
        }
        byte[] payload = buffer.removeBytes(length);
        writeChunkToConnection(connection, payload);
        connection.getStatusHTTPFromMix().setType(HttpPartType.Header);
        return buffer;
    }

    /**
     * Method for Headerhandling Placeholder
     * 
     * @param connection
     */
    public abstract SynchronizedBuffer headerHandling(Connection connection);

    /**
     * Method for bodyhandling Placeholder for inherent class
     * 
     * @param connection
     */
    public abstract SynchronizedBuffer bodyHandling(Connection connection);

    /**
     * Method for bodychunkhandling Placeholder for inherent class
     * 
     * @param connection
     */
    public abstract SynchronizedBuffer bodyChunkHandling(Connection connection);

    /**
     * Method for Relaywriting of a body
     * This method contains all neccessary steps for every body passing this class
     * 
     * @param message
     * @param connection
     */
    public byte[] bodyRelayWrite(byte[] message, Connection connection, boolean writeOut) {
        int bufferLength = message.length;
        byte[] payload = message;
        if (writeOut) {
            connection.writeChunk(ByteBuffer.wrap(payload));
        }
        int remainingBodyLength = (connection.getStatusHTTPFromMix().getLength() - bufferLength);
        connection.getStatusHTTPFromMix().setLength(remainingBodyLength);
        if (remainingBodyLength <= 0 && connection.getStatusHTTPFromMix().isBody()) {
            connection.setStatusHTTPFromMix(new HttpInfo(HttpPartType.Header,  0));
        }
        return message;
    }

    /**
     * Methode to write a Message to a connection
     * 
     * @param payload
     */
    public void writeChunkToConnection(Connection connection, byte[] payload) {
        connection.writeChunk(ByteBuffer.wrap(payload));

    }

    /**
     * Helpermethod for header posthandling
     * 
     * It contains all necessary steps for header handling use by exit and entry
     * 
     * @param readableMessage
     * @param connection
     * @return the readable message as byte[]
     */
    public byte[] postHeaderHandling(byte[] readableMessage, Connection connection, boolean writeOut) {
        String headerString = new String(readableMessage);
        Hashtable<String, String> headerTable = HttpParser.parseHeader(headerString);
        connection.getStatusHTTPFromMix().setHeader(headerTable);
        String method;
        if (headerTable.containsKey("method")) {
            method = headerTable.get("method");
            connection.addMethodToQueue(method);
        } else {
            method = connection.returnMethodFromQueue();
        }
        int bodyType = HttpParser.determineBodyType(headerTable, method);
        // connection.setBodyImproveable(false);
        // int messageId = connection.peekPackageCounter();
        switch (bodyType) {
        case 0:
            connection.getStatusHTTPFromMix().setType(HttpPartType.Header);

            break;
        case 1:
            connection.getStatusHTTPFromMix().setType(HttpPartType.Body);
            int bodyLength = HttpParser.getBodyLengthFromHeader(headerTable);

            connection.getStatusHTTPFromMix().setLength(bodyLength);
            break;
        case 2:
            connection.getStatusHTTPFromMix().setLength(-1);
            connection.getStatusHTTPFromMix().setType(HttpPartType.BodyChunk);

            break;
        default:
            break;
        }
        // System.out.println("DataFromMix: Message ready to write out");
        if (writeOut) {
            // System.out.println(new String(readableMessage));
            writeChunkToConnection(connection, readableMessage);
        }
        // System.out.println("DataFromMix: Message written out on Connection "
        // + connection.getId());
        return readableMessage;
    }

}
