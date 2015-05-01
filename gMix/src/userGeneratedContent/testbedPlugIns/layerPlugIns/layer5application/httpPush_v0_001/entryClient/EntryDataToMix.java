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
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.mix.DataToMix;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.mix.MixWriteInterface;

/**
 * @author bash
 * 
 *         This class is a subclass from DataToMix
 *         It handles specific message transfer on the entryside from the
 *         mix to the webbrowser. It holds
 *         methods adjusted for this purpose.
 * 
 *         It implements the abstract classes from the superclass
 * 
 * 
 */
public class EntryDataToMix extends DataToMix {
	// public TrafficLog trafficlog;
	private EntryImprovement improvement;

	/**
	 * @param readableConnections
	 * @param mixTunnel
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 */
	public EntryDataToMix(LinkedBlockingQueue<Connection> readableConnections,
			MixWriteInterface mixTunnel, Settings settings)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException {

		super(readableConnections, mixTunnel, settings);
		improvement = new EntryImprovement(settings.getProperty("HP_PLUGIN"));
		// trafficlog = new TrafficLog("entrynodesender.txt");
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see mix.DataToMix#headerHandling(dataObjects.Connection)
	 */
	public void headerHandling(Connection readableConnection) {
		SynchronizedBuffer buffer = readableConnection.getConnectionBuffer();

		// trafficlog.writeLine("Header from " + readableConnection.getId());
		// trafficlog.writeLine(buffer.getAllDataAsString());
		// trafficlog.writeLine(readableConnection.getCache().getCompleteCache());
		// trafficlog.writeLine(readableConnection.getCache().getCompleteEventList());

		String headerLine = buffer.getHeaderString();
		String headerString = readableConnection.getSendBuffer();
		while (headerLine != null) {
			headerString += headerLine;
			if (headerLine.equals("\r\n")) {
				readableConnection.getStatusHTTPFromApp().setHeaderComplete(
						true);
				break;
			}
			headerLine = buffer.getHeaderString();
		}

		if (readableConnection.getStatusHTTPFromApp().isHeaderComplete()) {
			Hashtable<String, String> header = HttpParser
					.parseHeader(headerString);
			readableConnection.setSendBuffer("");
			readableConnection.getStatusHTTPFromApp().setHeader(header);
			byte[] mixMessage = headerString.getBytes();
			mixMessage = improvement.improveHeader(mixMessage,
					readableConnection);

			readableConnection.getStatusHTTPFromApp().setBodyImprovable(
					improvement.isBodyToMixImproveable(header,
							readableConnection));

			// Add method to method queue
			String method = header.get("method");
			readableConnection.addMethodToQueue(method);

			int bodyExistens = HttpParser.determineBodyType(header, method);

			switch (bodyExistens) {
			case 0:
				readableConnection.getStatusHTTPFromApp().setType(
						HttpPartType.Header);
				if (mixMessage != null) {
					// trafficlog.writeLine(new String(mixMessage));
					short hlength = (short) mixMessage.length;
					mixMessage = Util.concatArrays(
							Util.shortToByteArray(hlength), mixMessage);
					writeChunkToMix(readableConnection.getId(), mixMessage);
				}
				break;
			case 1:
				readableConnection.getStatusHTTPFromApp().setType(
						HttpPartType.Body);
				int bodyLength = HttpParser.getBodyLengthFromHeader(header);
				readableConnection.getStatusHTTPFromApp().setLength(bodyLength);
				if (mixMessage != null) {
					readableConnection.setHeaderBuffer(mixMessage);
				}
				break;
			case 2:
				readableConnection.getStatusHTTPFromApp().setType(
						HttpPartType.BodyChunk);
				readableConnection.getStatusHTTPFromApp().setLength(-1);
				if (mixMessage != null) {
					readableConnection.setHeaderBuffer(mixMessage);
				}
				break;
			default:
				break;
			}
			// readableConnection.logger.logEntryReqHead(
			// readableConnection.getId(),
			// readableConnection.incrementPackageCounter(),
			// mixMessage.length, readableConnection
			// .getStatusHTTPFromApp().getType().toString());
			// trafficlog.writeLine("MIXMESSAGE:");
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
		boolean improveable = readableConnection.getStatusHTTPFromApp()
				.isBodyImprovable();
		// readableConnection.logger.logEntryReqBody(readableConnection.getId(),
		// readableConnection.inspectPackageCounterTail(),
		// mixMessage.length, null);

		if (improveable) {
			mixMessage = improvement
					.improveBody(mixMessage, readableConnection);
			if (mixMessage != null) {
				bodyRelayWrite(mixMessage, readableConnection);
			}

		} else {
			bodyRelayWrite(mixMessage, readableConnection);

		}

	}

	/*
	 * (non-Javadoc)
	 * @see mix.DataToMix#bodyChunkHandling(dataObjects.Connection)
	 */
	public void bodyChunkHandling(Connection connection) {
		byte[] mixMessage;
		SynchronizedBuffer buffer = connection.getConnectionBuffer();
		connection.getId();
		// TODO Chache
		mixMessage = buffer.getHeaderString().getBytes();
		boolean improveable = connection.getStatusHTTPFromApp()
				.isBodyImprovable();
		if (connection.getStatusHTTPFromApp().getLength() <= 0) {

			String headline = HttpParser.getFirstStringFromChunk(mixMessage);
			int bodySize = HttpParser.getBodyLengthFromHeadLine(headline);
			mixMessage = buffer.removeBytes(bodySize);
			connection.getStatusHTTPFromApp().setLength(
					(bodySize + headline.getBytes().length));
			if (bodySize == 0) {
				connection.setStatusHTTPFromApp(new HttpInfo(
						HttpPartType.Header, 0));
				// connection.logger.logEntryReqBodyChunk(connection.getId(),
				// connection.inspectPackageCounterTail(), bodySize,
				// "Terminating Chunk");
			} else {
				// connection.logger.logEntryReqBodyChunk(connection.getId(),
				// connection.inspectPackageCounterTail(), bodySize, null);
			}
		}

		if (improveable) {
			mixMessage = improvement.improveBody(mixMessage, connection);
			if (mixMessage != null) {
				bodyRelayWrite(mixMessage, connection);
			}
		} else {
			bodyRelayWrite(mixMessage, connection);
		}

	}

}
