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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.plugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.zip.DeflaterInputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.utils.IOUtils;

import staticContent.framework.util.Util;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.Cache;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.Connection;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.HttpInfo;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.HttpPartType;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.helper.HtmlParser;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.helper.HttpParser;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.improvement.EntryImprovementInterface;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.improvement.ExitImprovementInterface;


/**
 * @author bash
 * 
 * This class implements the activeCache Improvement.
 *  
 * The methods isAppBodyToMixImproveable() and isAppBodyFromMixImproveable() always returns false. There is no case where a HTTP Message 
 * from a Webbrowser has a improveable Body.
 *
 * The method isWebBodyToMixImproveable() and isWebBodyFromMixImproveable() checks a header if a Body exists.
 * Returns true if they exists and false otherwise.
 * 
 * The methods toMixBodyImprovement() and toWebBodyImprovement() should not called. It would return an unmodificated Body. The reason
 * is the same as above.
 * 
 * The method toMixHeaderImprovement() checks if a body is listed in the cache by the identifier (URI). 
 * If true the entryWriteOut variable is set to false. Then the method checks if the repsonse is completly stored in the cache. 
 * If it is completely stored in the cache the method returns the response to the webbrowser. Otherwise it added the request 
 * requestlist and set the entry status to requested. 
 * If the request is not listed in the chache the request is forwarded to the mix and adds the request to the responselist.
 * 
 * The method toWebHeaderImprovement() only adds the Identifier (URI) the to the responselist.
 * 
 *  The method fromWebHeaderImprovement() polls the identifier and adds it to the message.
 *  
 * The method fromWebBodyImprovement() search a body via RegEx for suprequests and send them to the web.
 * It also stores the Identifier to the responselist
 * 
 * The method fromMixHeaderImprovement() adds an entry to the cache an set the the status of complete to false.
 * The identifier for the entry is polled from the responselist. The the header is added to the cache
 * 
 * The method fromMixBodyImprovement() search a body via RegEx for suprequests and adds the identifier to the responselist.
 * Then the methods adds the body part to the cache. If it ist the complete body or the last chunk by chunked encoding it set the status of the entry to complete
 * If the entry is complete it checks if the entry is already requested. If true the method returns the complete response.
 * 
 * There are four private methods to compress and decompress a body compressed with GZIP or Deflate.
 * They are similar to the methods in the headerCompression-Plugin.
 * 
 * 
 */
public class ActiveCacheImprovement2 implements EntryImprovementInterface, ExitImprovementInterface {
    private boolean entryWriteOut;
    private boolean exitWriteOut;
   

    private String actualIdentifier;

    /**
	 * 
	 */
    public ActiveCacheImprovement2() {
        entryWriteOut = true;
        exitWriteOut = true;
       

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * improvement.ExitImprovementInterface#isWebBodyImproveable(java.util.Hashtable
     * )
     */
    @Override
    public boolean isWebBodyToMixImproveable(Hashtable<String, String> headerTable, Connection connection) {
        String contentType = headerTable.get("content-type");
        if (contentType.contains("text/html") || contentType.contains("text/css")) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see improvement.ExitImprovementInterface#toWebHeaderImprovement(byte[],
     * dataObjects.Connection)
     */
    @Override
    public byte[] toWebHeaderImprovement(byte[] message, Connection connection) {
        String identifier = connection.getStatusHTTPFromMix().getHeader().get("uri");
        connection.requestQueue.add(connection.requestId);
        connection.requestId++;
        connection.getIdentifierQueue().add(identifier);
        return message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * improvement.ExitImprovementInterface#fromWebHeaderImprovement(byte[],
     * dataObjects.Connection)
     */
    @Override
    public byte[] fromWebHeaderImprovement(byte[] message, Connection connection) {
        int requestId = connection.requestQueue.poll();
        connection.actualRequestId = requestId;
        message = Util.concatArrays(Util.intToByteArray(requestId), message);
        return message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see improvement.ExitImprovementInterface#toWebBodyImprovement(byte[],
     * dataObjects.Connection)
     */
    @Override
    public byte[] toWebBodyImprovement(byte[] message, Connection connection) {
        return message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see improvement.ExitImprovementInterface#fromWebBodyImprovement(byte[],
     * dataObjects.Connection)
     */
    @Override
    public byte[] fromWebBodyImprovement(byte[] message, Connection connection) {
        int id = connection.actualRequestId;
        byte[] improvedMessage = message;
        Hashtable<String, String> header = connection.getStatusHTTPFromApp().getHeader();

        // DEcompress message
        if (header.containsKey("content-encoding")) {
            String compressionType = header.get("content-encoding");
            if (compressionType.equals("gzip")) {
                improvedMessage = decompressGzip(improvedMessage);
            } else if (compressionType.equals("deflate")) {
                improvedMessage = decompressDeflate(improvedMessage);
            } else {
                System.out.println("Unknown Compression");
                return message;
            }
        }
        Hashtable<String, String> headerReq = connection.getStatusHTTPFromMix().getHeader();

        // Determine contenttype
        String contentType = header.get("content-type");
        List<String> subpages;
        if (contentType.contains("html")) {
            subpages = HtmlParser.getAllRessourcesHtml(headerReq.get("uri"), new String(improvedMessage));
        } else if (contentType.contains("css")) {
            subpages = HtmlParser.getAllRessourcesCss(new String(improvedMessage));
        } else {
            return message;
        }

        // Generate subrequests

        for (String ressource : subpages) {
            String saveUri = headerReq.get("uri");
            headerReq.put("uri", ressource);
         //   System.out.println(ressource);
            // TODO: Referer setzen
            String request = HttpParser.composeGetRequest(headerReq);
            connection.getIdentifierQueue().add(ressource);
            connection.addMethodToQueue("GET");
            connection.requestQueue.add(id);
            connection.writeChunk(ByteBuffer.wrap(request.getBytes())); // TODO:
                                                                        // neue
                                                                        // connections
                                                                        // aufmachen?
            headerReq.put("uri", saveUri);
        }

        return message;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * improvement.EntryImprovementInterface#isAppBodyImproveable(java.util.
     * Hashtable)
     */
    @Override
    public boolean isAppBodyFromMixImproveable(Hashtable<String, String> headerTable, Connection connection) {
        if (headerTable.contains("content-type")) {
            String contentType = headerTable.get("content-type");
            if (contentType.contains("text/html") || contentType.contains("text/css")) {
                return true;
            } else {
                return true;
            }
        }
        return true;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * improvement.EntryImprovementInterface#fromMixHeaderImprovement(byte[],
     * dataObjects.Connection)
     */
    @Override
    public byte[] fromMixHeaderImprovement(byte[] message, Connection connection) {
        int id = Util.byteArrayToInt(Arrays.copyOfRange(message, 0, 4));
        message = Arrays.copyOfRange(message, 4, message.length);

        connection.actualRequestId = id;
        String identifier = connection.requestTable.get(id).poll();
      //  System.out.println("Request-Uri " + identifier);

        Cache cache = connection.getCache();
        actualIdentifier = identifier;
        cache.addNewCacheEntry(identifier, message);
        entryWriteOut = false;
        return message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see improvement.EntryImprovementInterface#toMixHeaderImprovement(byte[],
     * dataObjects.Connection)
     */
    @Override
    public byte[] toMixHeaderImprovement(byte[] message, Connection connection) {
        Hashtable<String, String> header = connection.getStatusHTTPFromApp().getHeader();
        String identifier = header.get("uri");
        Cache cache = connection.getCache();
      

        if (cache.containsEntry(identifier)) {

            message = null;
        } else {
            int id = connection.requestId;
            connection.requestQueue.add(id);
            connection.requestId++;
            connection.addIdToTable(id, identifier);
             
        }
        
        cache.invokeEventsOfConnection(connection, identifier);

        return message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see improvement.EntryImprovementInterface#fromMixBodyImprovement(byte[],
     * dataObjects.Connection)
     */
    @Override
    public byte[] fromMixBodyImprovement(byte[] message, Connection connection) {
        int id = connection.actualRequestId;
        byte[] improvedMessage = message;
        HttpInfo status = connection.getStatusHTTPFromMix();
        Hashtable<String, String> header = status.getHeader();
        String identifier = actualIdentifier;

        Cache cache = connection.getCache();

        cache.appendMessageToEntry(identifier, message);
       
        // Check if entry complete
        HttpPartType type = status.getType();
       // System.out.println("fromMixBodyImprovement "+new String(message));
        if (new String(message).equals("0\r\n\r\n") || type == HttpPartType.Body) {
            cache.setMessageComplete(identifier, true);
            cache.invokeEvents(identifier);
        }

        if (header.containsKey("content-encoding")) {
            String compressionType = header.get("content-encoding");
            if (compressionType.equals("gzip")) {
                improvedMessage = decompressGzip(improvedMessage);
            } else if (compressionType.equals("deflate")) {
                improvedMessage = decompressDeflate(improvedMessage);
            } else {
                System.out.println("Unknown Compression");
                return message;
            }
        }
        if (header != null) {
            String contentType = header.get("content-type");
            List<String> subpages;
            if (contentType == null) {

                System.out.println("Debug");
            }
            if (contentType.contains("html")) {
                subpages = HtmlParser.getAllRessourcesHtml(connection.getStatusHTTPFromApp().getHeader().get("uri"),
                        new String(improvedMessage));

            } else if (contentType.contains("css")) {
                subpages = HtmlParser.getAllRessourcesCss(new String(improvedMessage));

            } else {
                return message;
            }
            for (String ressource : subpages) {
              //  System.out.println(ressource);
                // connection.getIdentifierQueue().add(ressource);
                connection.requestTable.get(id).add(ressource);
                connection.requestQueue.add(id);
                connection.addMethodToQueue("GET");
                cache.addNewCacheEntry(ressource, null);
                cache.setWebRequest(ressource);

            }
        }
        entryWriteOut = false;
        return message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see improvement.EntryImprovementInterface#toMixBodyImprovement(byte[],
     * dataObjects.Connection)
     */
    @Override
    public byte[] toMixBodyImprovement(byte[] message, Connection connection) {

        return null;
    }

    @Override
    public boolean isExitWriteOut() {

        return exitWriteOut;
    }

    @Override
    public boolean isEntryWriteOut() {
        return entryWriteOut;
    }

    // Helpermethods

    /**
     * Methos to compress a byte[] with Gzip
     * 
     * @param content
     * @return gzipcompressed bytey[]
     */

    private static byte[] compressGzip(byte[] content) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
            gzipOutputStream.write(content);
            gzipOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.printf("Compression ratio %f\n", (1.0f * content.length / byteArrayOutputStream.size()));
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Method to decompress a byte[]
     * 
     * @param contentBytes
     *            compresed with Gzip
     * @return an uncompressed byte[]
     */
    private static byte[] decompressGzip(byte[] contentBytes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            IOUtils.copy(new GZIPInputStream(new ByteArrayInputStream(contentBytes)), out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }

    /**
     * Methos to compress a byte[] with Deflate
     * 
     * @param content
     * @return deflated byte[]
     */
    private static byte[] compressDeflate(byte[] content) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream);
            deflaterOutputStream.write(content);
            deflaterOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.printf("Compression ratio %f\n", (1.0f * content.length / byteArrayOutputStream.size()));
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Method to decompress a byte[]
     * 
     * @param contentBytes
     *            compresed with Deflate
     * @return an uncompressed byte[]
     */
    private static byte[] decompressDeflate(byte[] contentBytes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            IOUtils.copy(new DeflaterInputStream(new ByteArrayInputStream(contentBytes)), out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }

    @Override
    public boolean isWebBodyFromMixImproveable(Hashtable<String, String> headerTable, Connection connection) {

        return false;
    }

    @Override
    public boolean isAppBodyToMixImproveable(Hashtable<String, String> headerTable, Connection connection) {

        return false;
    }

//    private byte[] requestSubpage(byte[] request) {
//        byte[] returnValue;
//        Socket socket = null;
//        try {
//            socket = new Socket("localhost", 4007);
//        } catch (UnknownHostException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//        try {
//            InputStream input = socket.getInputStream();
//
//            OutputStream output = socket.getOutputStream();
//            
//            output.write(request);
//            output.flush();
//            String answer = new String();
//            returnValue.
//            String sign = input.read();
//            while (sign != -1) {
//                answer += sign;
//                sign = (char) input.read();
//
//            }
//            System.out.println(answer);
//            socket.close();
//            
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        
//      
//
//        return returnValue;
//    }

}
