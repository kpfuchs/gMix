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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.encDNS_v0_001;

import java.net.*;

import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;


public class TestCl {
	
	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) throws Exception {
		//BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress IPAddress = InetAddress.getByName("localhost");
		byte[] receiveData = new byte[32000];
		//String sentence = inFromUser.readLine();
		//sendData = sentence.getBytes();
		//Message.newQuery(Record.newRecord(Name.fromString("wpad.uni-regensburg.de."), Type.A, DClass.IN))
		Message dnsQuery = Message.newQuery(Record.newRecord(Name.fromString("wpad.uni-regensburg.de."), Type.A, DClass.IN));
		byte[] raw = dnsQuery.toWire();
		DatagramPacket sendPacket = new DatagramPacket(raw, raw.length, IPAddress, 53);
		clientSocket.send(sendPacket);
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		clientSocket.receive(receivePacket);
		String modifiedSentence = new String(receivePacket.getData());
		System.out.println("FROM SERVER:" + modifiedSentence);
		clientSocket.close(); 
	} 
	
}
