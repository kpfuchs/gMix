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
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.mix.DataFromMix;


/**
 * @author bash
 * 
 * This class is a subclass from DataFromMix
 * It handles specific message transfer on the entryexit
 * 
 *  It implements the abstract methods from the superclass
 * 
 */
public class ExitDataFromMix extends DataFromMix {

	private ExitImprovement improvement;

	/**
	 * @param writeableChunks
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 */
	public ExitDataFromMix(LinkedBlockingQueue<Connection> writeableChunks, Settings settings) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		super(writeableChunks);
		improvement = new ExitImprovement(settings.getProperty("HP_PLUGIN"));

		// TODO Auto-generated constructor stub
	}

	/**
	 * Method for Headerhandling
	 * 
	 * @param message
	 * @param connection
	 */
	public SynchronizedBuffer headerHandling(
			Connection connection) {
		SynchronizedBuffer buffer = connection.getMixBuffer();
		short result;
		byte[] hlength = buffer.removeBytes(2);
		result = Util.byteArrayToShort(hlength);
		byte[] message = buffer.removeBytes(result);
	//	connection.logger.logEntryReqHead(connection.getId(),
	//			connection.incrementPackageCounter(), message.length, null);

		byte[] readableMessage = improvement.unImproveHeader(message, connection);

		String headerString = new String(readableMessage);
		Hashtable<String, String> headerTable = HttpParser
				.parseHeader(headerString);
		String method;
	//	String ressource= headerTable.get("uri");
		if(headerTable.containsKey("method")){
			method = headerTable.get("method");
			connection.addMethodToQueue(method);
		} else {
			method = connection.peekMethodFromQueue();
		}
	//	connection.getIdentifierQueue().add(ressource);
		int bodyType = HttpParser.determineBodyType(headerTable, method);
		if (bodyType != 0) {
		connection.getStatusHTTPFromMix().setBodyImprovable(
				improvement.isBodyFromMixImproveable(headerTable,connection));
		
		}
		
		readableMessage = postHeaderHandling(readableMessage, connection, improvement.writeOut());
		
		return buffer;

	}

	/**
	 * Method for bodyhandling
	 * 
	 * @param message
	 * @param connection
	 */
	public SynchronizedBuffer bodyHandling(Connection connection) {
		byte[] mixMessage;
		SynchronizedBuffer buffer = connection.getMixBuffer();
		if (connection.getStatusHTTPFromMix().isBodyImprovable()) {
			int bodyLength = connection.getStatusHTTPFromMix().getLength();
			if (bodyLength > buffer.getByteSize()) {
				writeableChunks.add(connection);
				return null;
			} else {
//				connection.logger.logEntryReqBody(connection.getId(),
//						connection.inspectPackageCounterTail(), bodyLength, null);
				mixMessage = buffer.removeBytes(bodyLength);
				mixMessage = improvement.unImproveBody(mixMessage, connection);
			
				mixMessage = bodyRelayWrite(mixMessage, connection, improvement.writeOut());
			
				// TODO Chache
				return buffer;
			}
		} else {
			// Relaywrite
			byte[] message = buffer.removeTopOfBuffer();
			mixMessage = bodyRelayWrite(message, connection, improvement.writeOut());
//			connection.logger.logEntryResBody(connection.getId(),
//					connection.inspectPackageCounterTail(), mixMessage.length,
//					null);
			return buffer;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see mix.DataFromMix#bodyChunkHandling(dataObjects.Connection)
	 */
	public SynchronizedBuffer bodyChunkHandling(Connection connection) {
		byte[] mixMessage;
		SynchronizedBuffer buffer = connection.getMixBuffer();
		if (connection.getStatusHTTPFromMix().getLength() <= 0) {
			String headline = HttpParser.getFirstStringFromChunk(buffer.peekBuffer()); 
			int bodySize = HttpParser.getBodyLengthFromHeadLine(headline);
			connection.getStatusHTTPFromMix().setLength(
					(short) (bodySize +headline.getBytes().length));
			if(bodySize == 0 ){
				mixMessage = buffer.removeBytes(headline.getBytes().length);
				connection.setStatusHTTPFromMix(new HttpInfo(
							HttpPartType.Header, 0));
				
				mixMessage = bodyRelayWrite(mixMessage, connection, improvement.writeOut());
//				connection.logger.logEntryReqBodyChunk(connection.getId(),
//						connection.removeCounterFromList(), bodySize,
//							"Terminating Chunk");
				return buffer;
			}
		}
		int bodyLength = connection.getStatusHTTPFromMix().getLength();
//		connection.logger.logEntryReqBodyChunk(connection.getId(),
//				connection.peekPackageCounter(), bodyLength,
//					null);
		if (connection.getStatusHTTPFromMix().isBodyImprovable()) {
			
			if ( bodyLength > buffer.getByteSize()) {
				writeableChunks.add(connection);
				return null;
			} else {
				
				mixMessage = buffer.removeBytes(bodyLength);
				mixMessage = improvement.unImproveBody(mixMessage, connection);
				if(mixMessage != null){
				mixMessage = bodyRelayWrite(mixMessage, connection, improvement.writeOut());
				}
			}
		} else {
			// Relaywrite
			
			if(buffer.getByteSize() < bodyLength){
				mixMessage = buffer.removeBytes(buffer.getByteSize());
			} else {	
				mixMessage = buffer.removeBytes(bodyLength);
			}
			mixMessage = bodyRelayWrite(mixMessage, connection, improvement.writeOut());
		}
		return buffer;
	}

}
