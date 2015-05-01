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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.exitClient;

import java.util.Hashtable;
import java.util.concurrent.LinkedBlockingQueue;

import staticContent.framework.config.Settings;
import staticContent.framework.util.Util;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.Connection;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.HttpInfo;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.HttpPartType;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.SynchronizedBuffer;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.helper.HttpParser;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.improvement.ExitImprovement;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.mix.DataToMix;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.mix.MixWriteInterface;

/**
 * @author bash
 * 
 *   This class is a subclass from DataToMix
 *   It handles specific message
 *   transfer on the exitside from the mix to the webserver. It holds
 *   methods adjusted for this purpose.
 *
 *   It implements the abstract methods from the superclass
 * 
 * 
 */

public class ExitDataToMix extends DataToMix {

    private ExitImprovement improvement;

    /**
     * @param readableConnections
     * @param mixTunnel
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     */
    public ExitDataToMix(LinkedBlockingQueue<Connection> readableConnections, MixWriteInterface mixTunnel,
            Settings settings) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        super(readableConnections, mixTunnel, settings);
        improvement = new ExitImprovement(settings.getProperty("HP_PLUGIN"));

    }

    /*
     * (non-Javadoc)
     * @see mix.DataToMix#headerHandling(dataObjects.Connection)
     */
    public void headerHandling(Connection readableConnection) {
        SynchronizedBuffer buffer = readableConnection.getConnectionBuffer();
        
        String headerString = readableConnection.getSendBuffer();
        String headerLine;
        if(headerString.endsWith("\r")){
            headerLine = new String(buffer.removeBytes(1));
        } else {
         headerLine = buffer.getHeaderString();
        }
        while (headerLine != null) {

            headerString += headerLine;
            if (headerString.endsWith("\r\n\r\n")) {
                readableConnection.getStatusHTTPFromApp().setHeaderComplete(true);
                break;
            }
            
            headerLine = buffer.getHeaderString();
        }

        if (readableConnection.getStatusHTTPFromApp().isHeaderComplete()) {
            Hashtable<String, String> header = HttpParser.parseHeader(headerString);
            readableConnection.setSendBuffer("");
            readableConnection.getStatusHTTPFromApp().setHeader(header);
            byte[] mixMessage = headerString.getBytes();
            mixMessage = improvement.improveHeader(mixMessage, readableConnection);
            short hlength = (short) mixMessage.length;
         //   System.out.println("------" + headerString);
            readableConnection.getStatusHTTPFromApp().setBodyImprovable(
                    improvement.isBodyToMixImproveable(header, readableConnection));
            mixMessage = Util.concatArrays(Util.shortToByteArray(hlength), mixMessage);
            // Add method to method queue
            String method = readableConnection.returnMethodFromQueue();

            int bodyExistens = HttpParser.determineBodyType(header, method);

            switch (bodyExistens) {
            case 0:
                readableConnection.getStatusHTTPFromApp().setType(HttpPartType.Header);
                if (mixMessage != null) {
                    writeChunkToMix(readableConnection.getId(), mixMessage);
                }
                break;
            case 1:
                readableConnection.getStatusHTTPFromApp().setType(HttpPartType.Body);
                int bodyLength = HttpParser.getBodyLengthFromHeader(header);
                readableConnection.getStatusHTTPFromApp().setLength(bodyLength);
                if (mixMessage != null) {
                    readableConnection.setHeaderBuffer(mixMessage);
                }
                break;
            case 2:
                readableConnection.getStatusHTTPFromApp().setType(HttpPartType.BodyChunk);
                readableConnection.getStatusHTTPFromApp().setLength(-1);
                if (mixMessage.length > 500){
                    readableConnection.getStatusHTTPFromApp().setType(HttpPartType.Header);
                }
                if (mixMessage != null) {
                    readableConnection.setHeaderBuffer(mixMessage);
                }
                break;
            default:
                break;
            }
            // readableConnection.logger.logEntryReqHead(readableConnection.getId(),
            // readableConnection.incrementPackageCounter(), mixMessage.length,
            // readableConnection
            // .getStatusHTTPFromApp().getType().toString());


        } else {
            readableConnection.setSendBuffer(headerString);

        }

    }

    /*
     * (non-Javadoc)
     * @see mix.DataToMix#bodyHandling(dataObjects.Connection)
     */
    public void bodyHandling(Connection readableConnection) {
        byte[] mixMessage;
        readableConnection.getId();
        SynchronizedBuffer buffer = readableConnection.getConnectionBuffer();
        int bodyLength = readableConnection.getStatusHTTPFromApp().getLength();
        if (bodyLength >= buffer.getByteSize()) {
            mixMessage = buffer.removeBytes(buffer.getByteSize());
        } else {
            mixMessage = buffer.removeBytes(bodyLength);
        }

        boolean improveable = readableConnection.getStatusHTTPFromApp().isBodyImprovable();
        // readableConnection.logger.logEntryReqBody(readableConnection.getId(),
        // readableConnection.inspectPackageCounterTail(), mixMessage.length,
        // null);

        if (improveable) {
            mixMessage = improvement.improveBody(mixMessage, readableConnection);
            if (mixMessage != null) {
                bodyRelayWrite(mixMessage, readableConnection);
            }

        } else {
            bodyRelayWrite(mixMessage, readableConnection);

        }

    }

    /*
     * 
     * (non-Javadoc)
     * @see mix.DataToMix#bodyChunkHandling(dataObjects.Connection)
     */
    public void bodyChunkHandling(Connection connection) {
        byte[] mixMessage;
        SynchronizedBuffer buffer = connection.getConnectionBuffer();
        connection.getId();
        boolean improveable = connection.getStatusHTTPFromApp().isBodyImprovable();
        if ((connection.getStatusHTTPFromApp().getLength() <= 0) && (!connection.getStatusHTTPFromApp().isLastChunk())) {
            if (buffer.peekBuffer() == null) {
                return;
            }
           
            String headline = buffer.getChunkHeaderLine();
            if(headline== null){
                connection.setConnectionMessageIncomplete(true);
                return;
            }
            int bodySize = HttpParser.getBodyLengthFromHeadLine(headline);
            if(bodySize == 0){
                connection.getStatusHTTPFromApp().setLastChunk(true);
            }
            
            connection.getStatusHTTPFromApp().setLength((bodySize + headline.getBytes().length + 2));
            
        }
       

        int bodySize = connection.getStatusHTTPFromApp().getLength();
        
        if (connection.getStatusHTTPFromApp().isLastChunk()) {
            if(bodySize > buffer.getByteSize()){
                connection.setConnectionMessageIncomplete(true);
                return;
            }
            connection.setStatusHTTPFromApp(new HttpInfo(HttpPartType.Header, 0));
            mixMessage = buffer.removeBytes(bodySize);
            // mixMessage = improvement.improveBody(mixMessage, connection);
            bodyRelayWrite(mixMessage, connection);
            return;
        }
        if (improveable) {

            if (bodySize > buffer.getByteSize()) {
                // mixMessage =buffer.removeBytes(buffer.getByteSize());
                // readableConnections.add(connection);
                // return;
              //  mixMessage = buffer.removeBytes(buffer.getByteSize());
                    connection.setConnectionMessageIncomplete(true);
                return;
            } else {
                mixMessage = buffer.removeBytes(bodySize);

            }
            mixMessage = improvement.improveBody(mixMessage, connection);
            if (mixMessage != null) {
                bodyRelayWrite(mixMessage, connection);

            }
        } else {

            if (buffer.getByteSize() < bodySize) {
                mixMessage = buffer.removeBytes(buffer.getByteSize());
            } else {
                mixMessage = buffer.removeBytes(bodySize);
            }
            bodyRelayWrite(mixMessage, connection);
        }

    }

}
