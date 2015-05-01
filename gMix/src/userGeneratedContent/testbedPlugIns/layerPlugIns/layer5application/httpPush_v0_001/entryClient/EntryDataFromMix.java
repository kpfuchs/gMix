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

import java.util.Hashtable;
import java.util.concurrent.LinkedBlockingQueue;

import staticContent.framework.config.Settings;
import staticContent.framework.util.Util;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.Connection;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.HttpInfo;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.HttpPartType;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.SynchronizedBuffer;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.helper.HttpParser;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.improvement.EntryImprovement;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.mix.DataFromMix;

/**
 * @author bash
 * 
 * This class is a subclass from DataFromMix
 * It handles specific message transfer on the entryside to the mix. 
 * It holds methods adjusted for this purpose.
 * 
 *  It implements the abstract classes from the superclass
 * 
 */
public class EntryDataFromMix extends DataFromMix {

    private EntryImprovement improvement;

    /**
     * Constructor
     * @param writeableChunks
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     */
    public EntryDataFromMix(LinkedBlockingQueue<Connection> writeableChunks, Settings settings)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        super(writeableChunks);
        improvement = new EntryImprovement(settings.getProperty("HP_PLUGIN"));

       

    }

    /**
     * Method for Headerhandling
     * 
     * This class handles the entry specific part of the headerhandling
     * 
     * @param message
     * @param connection
     */
    public SynchronizedBuffer headerHandling(Connection connection) {
        SynchronizedBuffer buffer = connection.getMixBuffer();
        short result;
        byte[] hlength = buffer.removeBytes(2);

        result = Util.byteArrayToShort(hlength);
        byte[] message = buffer.removeBytes(result);

        byte[] readableMessage = improvement.unImproveHeader(message, connection);
        String headerString = new String(readableMessage);

        Hashtable<String, String> headerTable = HttpParser.parseHeader(headerString);
        String method;
        if (headerTable.containsKey("method")) {
            method = headerTable.get("method");
            connection.addMethodToQueue(method);
        } else {
            method = connection.peekMethodFromQueue();

        }
        int bodyType = HttpParser.determineBodyType(headerTable, method);
        if (bodyType != 0) {
            connection.getStatusHTTPFromMix().setBodyImprovable(
                    improvement.isBodyFromMixImproveable(headerTable, connection));
        }

        readableMessage = postHeaderHandling(readableMessage, connection, improvement.writeOut());
     
        if (connection.getStatusHTTPFromMix().isHeader()) {
            // connection.logger.logEntryResHead(connection.getId(),
            // connection.removeCounterFromList(), message.length,
            // "No Body");
        } else {
            // connection.logger.logEntryResHead(connection.getId(),
            // connection.peekPackageCounter(), message.length, connection
            // .getStatusHTTPFromMix().getType().toString());
        }
        return buffer;

    }

    /**
     * Method for bodyhandling
     * 
     * @param message
     * @param connection
     */
    public SynchronizedBuffer bodyHandling(Connection connection) {
        SynchronizedBuffer buffer = connection.getMixBuffer();
        byte[] mixMessage;
        if (connection.getStatusHTTPFromMix().isBodyImprovable()) {
            int bodyLength = connection.getStatusHTTPFromMix().getLength();
            if (bodyLength > buffer.getByteSize()) {
                writeableChunks.add(connection);
                return null;
            } else {

                // connection.logger.logEntryResBody(connection.getId(),
                // connection.removeCounterFromList(), bodyLength, null);
                mixMessage = buffer.removeBytes(bodyLength);
                mixMessage = improvement.unImproveBody(mixMessage, connection);
                mixMessage = bodyRelayWrite(mixMessage, connection, improvement.writeOut());

            }
        } else {
            // Relaywrite

            // connection.logger.logEntryResBody(connection.getId(),
            // connection.removeCounterFromList(), message.length,
            // null);
            int bodyLength = connection.getStatusHTTPFromMix().getLength();

            if (buffer.getByteSize() < bodyLength) {

                mixMessage = buffer.removeBytes(buffer.getByteSize());
                mixMessage = bodyRelayWrite(mixMessage, connection, improvement.writeOut());

            } else {
                mixMessage = buffer.removeBytes(bodyLength);
                mixMessage = bodyRelayWrite(mixMessage, connection, improvement.writeOut());
            }
        }
        return buffer;
    }

    /*
     * (non-Javadoc)
     * @see mix.DataFromMix#bodyChunkHandling(dataObjects.Connection)
     */
    public SynchronizedBuffer bodyChunkHandling(Connection connection) {
        byte[] mixMessage;
        SynchronizedBuffer buffer = connection.getMixBuffer();
        if (connection.getStatusHTTPFromMix().getLength() <= 0) {
            if (buffer.peekBuffer() == null) {
                connection.setMixMessageIncomplete(true);
                return buffer;
            }
            String headline = HttpParser.getFirstStringFromChunk(buffer.peekBuffer());
            if(headline == null){
                connection.setMixMessageIncomplete(true);
                return buffer;
            }
            int bodySize = 0;
            try {
            bodySize = HttpParser.getBodyLengthFromHeadLine(headline);
            } catch (Exception e){
                System.out.println(buffer.getAllDataAsString());
            }
            connection.getStatusHTTPFromMix().setLength((bodySize + headline.getBytes().length + 2));
            if (bodySize == 0) {
                connection.getStatusHTTPFromMix().setLastChunk(true);
               
            }
        }
        int bodyLength = connection.getStatusHTTPFromMix().getLength();
        if (connection.getStatusHTTPFromMix().isLastChunk()) {
            //
            if(bodyLength > buffer.getByteSize()){
                connection.setConnectionMessageIncomplete(true);
                return buffer;
            }
            mixMessage = buffer.removeBytes(bodyLength);

            mixMessage = improvement.unImproveBody(mixMessage, connection);
            connection.setStatusHTTPFromMix(new HttpInfo(HttpPartType.Header, 0));
            mixMessage = bodyRelayWrite(mixMessage, connection, improvement.writeOut());

            return buffer;

        
        }
        if (connection.getStatusHTTPFromMix().isBodyImprovable()) {

            if (bodyLength > buffer.getByteSize()) {
                //writeableChunks.add(connection);
                connection.setMixMessageIncomplete(true);
                return null;
            } else {

                mixMessage = buffer.removeBytes(bodyLength);
                mixMessage = improvement.unImproveBody(mixMessage, connection);

                mixMessage = bodyRelayWrite(mixMessage, connection, improvement.writeOut());

            }
        } else {
            // Relaywrite

            if (buffer.getByteSize() < bodyLength) {
                mixMessage = buffer.removeBytes(buffer.getByteSize());
            } else {
                mixMessage = buffer.removeBytes(bodyLength);
            }

            mixMessage = bodyRelayWrite(mixMessage, connection, improvement.writeOut());
        }
        return buffer;
    }

}
