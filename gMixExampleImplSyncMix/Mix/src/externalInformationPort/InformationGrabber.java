/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2011  Karl-Peter Fuchs
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
 */

package externalInformationPort;


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import java.util.Arrays;
import java.util.LinkedList;

import client.Client;

import exception.InformationRetrieveException;

import externalInformationPort.Information;

import util.Util;


/**
 * Can be used to retrieve <code>Information</code> from "external" 
 * communication partners (e. g. other mixes or clients) vie network (UDP).
 * 
 * @author Karl-Peter Fuchs
 * 
 * @see Client
 * @see Information
 * 
 */
final class InformationGrabber {

	/** 
	 * Maximum size a packet received by <code>getInformation()</code> may 
	 * have.
	 * 
	 * @see #getInformation(InetAddress, int, Information, byte[])
	 */
	protected final static int MAX_PACKET_SIZE = 1024;
	
	/** Timeout for reply on a request. */
	private final static int TIMEOUT = 5000;
	
	
	/**
	 * Creates a new <code>InformationGrabber</code> that can be used to 
	 * retrieve <code>Information</code> from "external" communication partners 
	 * (e. g. other mixes or clients) vie network (UDP). Empty Constructor, 
	 * since all methods are static.
	 */
	protected InformationGrabber() {
		
	}
	
	
	/**
	 * Generic method to receive some <code>Information</code> from a 
	 * specified <code>InformationProvider</code>-component.
	 * <p>
	 * If communication channel isn't safe, but transmitted data is sensitive, 
	 * cryptographic measures (like authentication and encryption) should be 
	 * applied.
	 * 
	 * @param informationProviderAddress	Address of the <code>
	 * 										InformationProvider</code>
	 * 										-component to receive data from.
	 * @param informationProviderPort		Port the <code>InformationProvider
	 * 										</code>-component to receive data 
	 * 										from runs on.
	 * @param informationOfInterest			Type of <code>Information</code>,
	 * 										that shall be received.
	 * 
	 * @return								The requested <code>Information
	 * 										</code>.
	 * 
	 * @throws	InformationRetrieveException 	Thrown, when requested <code>
	 * 											Information</code> not 
	 * 											available.
	 */
	protected static byte[] getInformation(	
			InetAddress informationProviderAddress,
			int informationProviderPort, 
			Information informationOfInterest,
			byte[] data
			) throws InformationRetrieveException {

		try {
			
			// generate request
			byte[] request = 
				Util.intToByteArray(informationOfInterest.getIdentifier());
	
			if (data != null) {
				
				byte[] lengthHeader = Util.intToByteArray(data.length);
				request = Util.mergeArrays(request, lengthHeader);
				request = Util.mergeArrays(request, data);
				
			} else {
				
				byte[] lengthHeader = Util.intToByteArray(0);
				request = Util.mergeArrays(request, lengthHeader);
				
			}
			
			// send request
			DatagramSocket socket = new DatagramSocket();
			socket.setSoTimeout(TIMEOUT);
	
			DatagramPacket packet = 
				new DatagramPacket(	request, 
									request.length,
									informationProviderAddress,
									informationProviderPort
									);
	
			socket.send(packet);
	
			// wait for response
			byte[] response = new byte[MAX_PACKET_SIZE];
			packet = new DatagramPacket(response, response.length);
			
			try {
				
				socket.receive(packet);
				
			} catch (SocketTimeoutException ste) {
				// send new request and wait again for reply
				
				return getInformation(	informationProviderAddress, 
										informationProviderPort, 
										informationOfInterest,
										data
										);
				
			}
			
			socket.close();
			
			// read fist header (= identifier):
			int identifier = 
				Util.byteArrayToInt(Arrays.copyOfRange(response, 0, 4));
	
			if (identifier == Information.NOT_AVAILABLE.getIdentifier()) {
	
				// InformationProvider couldn't answer the request
				return null;
	
			} else {
	
				// read second header (= length):
				int length = 
					Util.byteArrayToInt(Arrays.copyOfRange(response, 4, 8));
	
				if (length > (MAX_PACKET_SIZE - 8)) {
	
					// invalid packet
					throw new InformationRetrieveException();
	
				} else {
	
					// retrieve and return payload
					return Arrays.copyOfRange(response, 8, (8 + length));
	
				}
				
			}
			
		} catch (Exception e) {

			throw new InformationRetrieveException();
			
		}

	}
	
	
	/**
	 * Generic method to receive some <code>Information</code> from several 
	 * <code>ExternalInformationPort</code> components. The first component's 
	 * address and port number must be specified. The other components' 
	 * addresses are received by their predecessor  (= telescope 
	 * implementation).
	 * <p>
	 * If communication channel isn't safe, but transmitted data is sensitive, 
	 * cryptographic measures (like authentication and encryption) should be 
	 * applied.
	 * 
	 * @param informationProviderAddress	Address of the first <code>
	 * 										InformationProvider</code> 
	 * 										component to receive data from.
	 * @param informationProviderPort		Port the first 
	 * 										<code>InformationProvider</code> 
	 * 										component to receive data from runs 
	 * 										on.
	 * @param informationOfInterest			Type of <code>Information</code>,
	 * 										that shall be received.
	 * @param data							Data to be transmitted.
	 * 
	 * @return								The requested <code>Information
	 * 										</code>s.
	 * 
	 * @throws	InformationRetrieveException 	Thrown, when requested <code>
	 * 											Information</code> not 
	 * 											available.
	 */
	protected static byte[][] getInformationFromAll(
			InetAddress informationProviderAddress,
			int informationProviderPort, 
			Information informationOfInterest,
			byte[] data
			) throws InformationRetrieveException {
		
		LinkedList<byte[]> retreivedInformation = new LinkedList<byte[]>();
		
		try {
			
			// receive "Information" from communication partners
			do {
				
				// ask for information
				byte[] info = getInformation(	informationProviderAddress,
												informationProviderPort,
												informationOfInterest,
												data
												);
				
				if (info == null) { // mix didn't send the requested information
					
					throw new InformationRetrieveException();
					
				} else { // mix has sent the requested information
					
					retreivedInformation.add(info);
					
					// ask for the next mix' address
					byte[] nextAddress = 
						getInformation(	informationProviderAddress,
										informationProviderPort,
										Information.NEXT_MIX_ADDRESS,
										null
										);
					
					if (nextAddress == null) { // no further mixes in cascade
						
						break;
						
					} 
					
					// ask for the next mix' port
					byte[] nextMixPort = 
						getInformation(	informationProviderAddress,
										informationProviderPort,
										Information.NEXT_MIX_INFO_PORT,
										null
										);
					
					// save information for next loop cycle
					informationProviderAddress = 
						InetAddress.getByAddress(nextAddress);
					
					informationProviderPort = Util.byteArrayToInt(nextMixPort);
					
				}
	
			} while (true);
			
		} catch (Exception e) {
			
			throw new InformationRetrieveException();
			
		}

		// convert LinkedList to array and return it
		return retreivedInformation.toArray(new byte[0][0]);
		
	}
	
}
