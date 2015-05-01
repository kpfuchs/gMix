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
import java.util.Hashtable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.utils.IOUtils;

import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.Connection;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.improvement.EntryImprovementInterface;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.improvement.ExitImprovementInterface;

/**
 * @author bash
 * 
 * This class implements the header compression for the blackbox framework.
 * It implements the method to compress an header with gzip. 
 * 
 * Only the methods for header are implemented. 
 * They await a header as byte[] an returns gzip-compressed header as byte[].
 * 
 *  
 * The methods for bodys only returns the unmodificated body.
 * 
 * The methods if a body is improvable returns always false.
 * The writeOut variables are not modificated.
 * 
 * There are two private methods to compress and decompress a header with GZIP .
 *  The Apache commons libary provides the compression algorithm
 */
public class headerCompressionImprovement implements EntryImprovementInterface, ExitImprovementInterface {

	
    private boolean entryWriteOut;
    private boolean exitWriteOut;

    /**
	 * Constructor 
	 * Sets the write out variables to true. 
	 */
    public headerCompressionImprovement() {
        entryWriteOut = true;
        exitWriteOut = true;
    }

   
    @Override
    public boolean isWebBodyToMixImproveable(Hashtable<String, String> headerTable, Connection connection) {

        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * improvement.ExitImprovementInterface#isWebBodyImproveable(java.util.Hashtable
     * )
     */
    @Override
    public boolean isWebBodyFromMixImproveable(Hashtable<String, String> headerTable, Connection connection) {

        return false;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see improvement.ExitImprovementInterface#toWebHeaderImprovement(byte[])
     */
    @Override
    public byte[] toWebHeaderImprovement(byte[] message, Connection connection) {
        byte[] returnMessage = decompress(message);
        return returnMessage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * improvement.ExitImprovementInterface#fromWebHeaderImprovement(byte[])
     */
    @Override
    public byte[] fromWebHeaderImprovement(byte[] message, Connection connection) {
        byte[] returnMessage = compress(message);
        return returnMessage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see improvement.ExitImprovementInterface#toWebBodyImprovement(byte[])
     */
    @Override
    public byte[] toWebBodyImprovement(byte[] message, Connection connection) {
        // TODO Auto-generated method stub
        return message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see improvement.ExitImprovementInterface#fromWebBodyImprovement(byte[])
     */
    @Override
    public byte[] fromWebBodyImprovement(byte[] message, Connection connection) {
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
    public boolean isAppBodyToMixImproveable(Hashtable<String, String> headerTable, Connection connection) {

        return false;
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
    
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * improvement.EntryImprovementInterface#fromMixHeaderImprovement(byte[])
     */
    @Override
    public byte[] fromMixHeaderImprovement(byte[] message, Connection connection) {
        byte[] returnMessage = decompress(message);
        return returnMessage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see improvement.EntryImprovementInterface#toMixHeaderImprovement(byte[])
     */
    @Override
    public byte[] toMixHeaderImprovement(byte[] message, Connection connection) {
        byte[] returnMessage = compress(message);
        return returnMessage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see improvement.EntryImprovementInterface#fromMixBodyImprovement(byte[])
     */
    @Override
    public byte[] fromMixBodyImprovement(byte[] message, Connection connection) {
        return message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see improvement.EntryImprovementInterface#toMixBodyImprovement(byte[])
     */
    @Override
    public byte[] toMixBodyImprovement(byte[] message, Connection connection) {
        return message;
    }

    @Override
    public boolean isExitWriteOut() {

        return entryWriteOut;
    }

    @Override
    public boolean isEntryWriteOut() {
        return entryWriteOut;
    }

    private static byte[] compress(byte[] content) {
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

    private static byte[] decompress(byte[] contentBytes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            IOUtils.copy(new GZIPInputStream(new ByteArrayInputStream(contentBytes)), out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }

}
