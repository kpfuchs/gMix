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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.helper;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import staticContent.framework.util.Util;

/**
 * This class provides methodes for communication via SOCKS5-Protocol
 * @author bash
 *
 */
public class SocksHandler {
	
	
	
	/**
	 * Extract an Array of methods for identification
	 * +----+----------+---------+
     * |VER | NMETHODS | METHODS |
     * +----+----------+---------+
     * | 1  |   1      | 1 to 255|
     * +----+----------+---------+
	 * 
	 * @param message
	 * @return byte Array containing AuthMethodes
	 */
	public static byte[] readAuthMethodRequest(byte[] message){
		byte length = message[1];
		byte[] methodArray = Arrays.copyOfRange(message, 2, length+2);
			
		return methodArray; 
	}
	
	
	
	
	  /**
     * Sends a SOCKS5 Method Reply Message to user.
     *  
     * +----+--------+
     * |VER | METHOD |
     * +----+--------+
     * | 1  |   1    |
     * +----+--------+
     * 
     * @param method  method for authentification
     * @return socksReply as byte array
     */
    public static byte[] sendSocks5MethodReply(byte method)
    {
   
    	byte[] socksAuthReply = new byte[]{5,method};
    	return socksAuthReply;

    }
    
    
    /**
     * Generate SOCKS5 request.
     * The message looks like:
     * 
     * 		+----+-----+-------+------+----------+----------+
     * 		|VER | CMD | RSV   | ATYP | BND.ADDR | BND.PORT |
     * 		+----+-----+-------+------+----------+----------+ 
     * 		| 1  | 1   | X'00' | 1    | Variable | 2        |
     *		+----+-----+-------+------+----------+----------+
     * 
     *
     * @param message
     * @return type of socks request as byte
     */
    public static byte getSocksCommand(byte[] message){
    	return message[1];	
    }  
    
    /**
	 * Reads a couple of bytes depending on the address type and returns this
	 * address as an InetAddress-Object.
	 * 
	 * @param  message
	 * @return InetAddress
	 */
	public static InetSocketAddress getInetAddress(byte[] message) {
		byte aTyp = message[1];
		byte[] wrapped;
		byte[] byteAddress = null;
		int length;
		int port = 0;
		InetAddress address = null;
		try {
			switch (aTyp) {
			case 0x01: // IP v4
				length = 4;
				byteAddress = Arrays.copyOfRange(message, 4, 8);
				address = InetAddress.getByAddress(byteAddress);

				wrapped = Arrays.copyOfRange(message, 8, 10);
				port = Util.unsignedShortToInt(wrapped);
				break;
			case 0x03: // Domain, first byte defines the length of it
				length = Util.unsignedByteToShort(message[4]);
				byteAddress = Arrays.copyOfRange(message, 5, 5 + length);
				String stringAddress = Util
						.getStringWithoutNewLines(byteAddress);
				address = InetAddress.getByName(stringAddress);
				wrapped = Arrays.copyOfRange(message,
						5 + length, 6 + length);
				port = Util.unsignedShortToInt(wrapped);
				break;
			case 0x04: // IP v6
				length = 16;
				byteAddress = Arrays.copyOfRange(message, 4, 20);
				address = InetAddress.getByAddress(byteAddress);
				wrapped = Arrays.copyOfRange(message, 21, 22);
				port = Util.unsignedShortToInt(wrapped);
				break;
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		InetSocketAddress socketAdress = new InetSocketAddress(address, port);
		return socketAdress;
	}
    
    
    /**
     * 
     * Generate SOCKS5 reply for the connect (0x01) command.
     * The message looks like:
     * 
     * 		+----+-----+-------+------+----------+----------+
     * 		|VER | REP | RSV   | ATYP | BND.ADDR | BND.PORT |
     * 		+----+-----+-------+------+----------+----------+ 
     * 		| 1  | 1   | X'00' | 1    | Variable | 2        |
     *		+----+-----+-------+------+----------+----------+
     *
     *	 X'00' SUCCEEDED
     *   X'01' general SOCKS server failure
     *   X'02' connection not allowed by ruleset 
     *   X'03' Network unreachable
     *   X'04' Host unreachable
     *   X'05' Connection refused
     *   X'06' TTL expired
     * 
     * @param replyCode 	SOCKS5 reply field
     * @param aTyp			valid values: 0x01, 0x03, 0x04
     * @param address		BindAddress as InetSocketAddress
     * @return Replymessage as byte[]
     */
    public static byte[] sendSocks5ConnectionReply(byte replyCode, byte aTyp, InetSocketAddress address){
    	 
    	byte[] addressAsByte = address.getAddress().getAddress();
    	byte[] portAsByte = Util.shortToByteArray(address.getPort());
    	byte[] header = {0x05, replyCode, 0x00, aTyp};
    	byte[] connectReply= Util.concatArrays(header, addressAsByte);
    	connectReply = Util.concatArrays(connectReply, portAsByte);
    	
    	return connectReply;
    	
    }
    
	
	
	
	
	
	
	
	

}
