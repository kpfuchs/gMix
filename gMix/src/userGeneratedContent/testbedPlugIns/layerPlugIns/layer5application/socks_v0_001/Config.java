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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.socks_v0_001;

import staticContent.framework.config.Settings;

public class Config {
	
    /** IP address of the ServerSocket on the client that will accept SOCKS connections from user applications (e.g. web browsers) */
    public final String CLIENT_SOCKS_IP_ADDRESS;
    
    /** Port of the ServerSocket on the client that will accept SOCKS connections from user applications (e.g. web browsers) */
    public final int CLIENT_SOCKS_PORT;

    /** Port of the mix. The mix will wait for BIND Connections on this port (from Webserver). */
    public final int MIX_BIND_PORT; // SocksHandler will add a random int between 10 and 10000. Necessary for more than one Client.
   
    /** Port of the mix.The mix will wait for datagrams on this port. */
    public final int MIX_DATAGRAM_PORT; // SocksHandler will add a random int between 10 and 10000. Necessary for more than one Client.
    
    /** Defines how much bytes the Multiplexer can read from a user application. */
    public final int BUFFER_SIZE;
    
    /** If true, skips the roundtrip between client and proxy for Socks5 version identifier/method selection message */
    public final boolean SKIP_ROUNDTRIP;


    /** UDP: read until N zero bytes */
    public final static int N_ZERO_BYTES = 2;

    /*****************************
     * DEBUG
     *****************************/
    
    /** If true, prints some stackTraces of exceptions */
    public final boolean DEBUG;

    /** If true, the programm will "talk a lot" on the console. */
    public final boolean TALK_A_LOT;

    /*****************************
     * TESTs
     *****************************/
    
    /** Port of the TestServer */
    public final static int TEST_SERVER_PORT = 22330;
    
    /** Bytes to be concatenated in Socks5UDPTest to the end of the payload. The Socks5 UDP Request Header starts with two zero bytes. */
    public final static byte[] CONCAT_BYTES = { 0, 0 };
    
    
    public Config(Settings settings) {
        this.CLIENT_SOCKS_IP_ADDRESS = settings.getProperty("SOCKS_LISTENING_IP_ADDRESS");
        this.CLIENT_SOCKS_PORT = settings.getPropertyAsInt("SOCKS_LISTENING_PORT");
        this.MIX_BIND_PORT = settings.getPropertyAsInt("SOCKS_LISTENING_PORT_MIX");
        this.MIX_DATAGRAM_PORT = settings.getPropertyAsInt("SOCKS_LISTENING_PORT_MIX_UDP");
        this.BUFFER_SIZE = settings.getPropertyAsInt("SOCKS_BUFFER_SIZE");
        this.SKIP_ROUNDTRIP = settings.getPropertyAsBoolean("SOCKS_SKIP_ROUNDTRIP");
        this.DEBUG = settings.getPropertyAsBoolean("SOCKS_DEBUG");
        this.TALK_A_LOT = settings.getPropertyAsBoolean("SOCKS_TALK_A_LOT");
    }
    
}
