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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.exitClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import staticContent.framework.clock.Clock;
import staticContent.framework.config.Settings;
import staticContent.framework.util.Util;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.Cache;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.Connection;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.HttpInfo;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.HttpPartType;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.helper.SocksHandler;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.mix.ConnectionPoolInterface;


/**
 * This class manages all connections to a webserver. 
 * 
 * It generates a new connection if a socks requests arrives and stores it in the connection map.
 * 
 * If data arrives on a connection socket, it adds the connection to the readable connections queue.
 *
 * @author bash
 *
 */
public class ExitConnectionPool extends Thread implements ConnectionPoolInterface {

    private Cache cache;
    private LinkedBlockingQueue<Connection> readableConnections;
    private ConcurrentHashMap<Integer, Connection> connectionMap;
    private MixExitConnection mixConnection;
    private Selector selector = null;
    private Clock clock;
    private Settings settings;

    /**
     * @param bufferSize
     * @param readableConnections
     * @param connectionMap
     */
    public ExitConnectionPool(int bufferSize, LinkedBlockingQueue<Connection> readableConnections,
            ConcurrentHashMap<Integer, Connection> connectionMap, MixExitConnection mixConnection, Settings settings) {
        this.mixConnection = mixConnection;

        this.readableConnections = readableConnections;
        this.connectionMap = connectionMap;
        clock = new Clock(settings);
        this.settings = settings;
        cache = new Cache(settings.getPropertyAsInt("HP_CACHE_SIZE"), connectionMap);
        try {
            selector = Selector.open();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * @return the mixConnection
     */
    public MixExitConnection getMixConnection() {
        return mixConnection;
    }

    /**
     * @param mixConnection
     *            the mixConnection to set
     */
    public void setMixConnection(MixExitConnection mixConnection) {
        this.mixConnection = mixConnection;
    }

    /**
     * Generate new Connection from Socks Request
     * 
     * @param id
     * @param socksRequest
     */
    public void generateConnectionFromSocks(int id, byte[] socksRequest) {

        byte aTyp = socksRequest[3];
        InetSocketAddress address = SocksHandler.getInetAddress(socksRequest);

    //    System.out.println("mix: connection to " + address.toString() + ":" + address.getPort());

        SocketChannel socketChannel = null;
        try {
            socketChannel = SocketChannel.open();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        byte[] socksReply;
        try {

            if (socketChannel.connect(address)) {
                byte code = 0x00;
      //          System.out.println("mix: connection to " + address.toString() + ":" + address.getPort()
        //                + " established");

                socksReply = SocksHandler.sendSocks5ConnectionReply(code, aTyp, address);

                socketChannel.configureBlocking(false);
                Connection connection = new Connection(socketChannel, id, clock, settings, this, cache);
                connection.setStatusHTTPFromMix(new HttpInfo(HttpPartType.Header,  0));
                connection.setStatusHTTPFromApp(new HttpInfo(HttpPartType.Header,  0));

                connectionMap.put(id, connection);
          //      System.out.println("mix: send socksrequestresponse");
                registerSocket(socketChannel, connection, SelectionKey.OP_READ);
                int len =  socksReply.length;
                byte[] sendMessage = Util.concatArrays(
                        Util.concatArrays(Util.intToByteArray(id), Util.intToByteArray(len)), socksReply);
                // mixConnection.writeChunk(sendMessage);
            } else {
                byte code = 0x04;
                socksReply = SocksHandler.sendSocks5ConnectionReply(code, aTyp, address);

            }
            //System.out.println("mix: return socksreply");
        } catch (IOException e) {
            byte code = 0x04;
            socksReply = SocksHandler.sendSocks5ConnectionReply(code, aTyp, address);
        }

    }

    @Override
    public void run() {
        while (true) {

            try {
                selector.select();

                // Synchronized is necessary to prevent a race condition
                // with the registerConnection Method

            } catch (IOException e) {
                System.err.println("Critical Error! No selector registered! System halt!");
                e.printStackTrace();
                break;
            }
            synchronized (this) {
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectedKeys.iterator();

                while (it.hasNext()) {
                    SelectionKey key = (SelectionKey) it.next();
                    if (key.isReadable()) {
                        try {
           //                 System.out.println("Antwort erhalten");
                            this.readableConnections.add((Connection) key.attachment());
                            key.cancel();
                        } catch (Exception e) {
                            e.printStackTrace();

                        }
                        key.cancel();
                        it.remove();
                    }

                }

            }
        }

    }

    /**
     * Reads a couple of bytes depending on the address type and returns this
     * address as an InetAddress-Object.
     * 
     * @param aTyp
     *            valid values: 0x01, 0x03, 0x04
     * @return InetAddress
     */
    @SuppressWarnings("unused")
    private InetSocketAddress getInetAddress(byte aTyp, byte[] message) {
        ByteBuffer wrapped;
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

                wrapped = ByteBuffer.wrap(Arrays.copyOfRange(message, 9, 10));
                port = wrapped.getInt();
                break;
            case 0x03: // Domain, first byte defines the length of it
                length = Util.unsignedByteToShort(message[4]);
                byteAddress = Arrays.copyOfRange(message, 5, 5 + length);
                String stringAddress = Util.getStringWithoutNewLines(byteAddress);
                address = InetAddress.getByName(stringAddress);
                wrapped = ByteBuffer.wrap(Arrays.copyOfRange(message, 5 + length, 6 + length));
                port = wrapped.getInt();
                break;
            case 0x04: // IP v6
                length = 16;
                byteAddress = Arrays.copyOfRange(message, 4, 20);
                address = InetAddress.getByAddress(byteAddress);
                wrapped = ByteBuffer.wrap(Arrays.copyOfRange(message, 21, 22));
                port = wrapped.getInt();
                break;
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        InetSocketAddress socketAdress = new InetSocketAddress(address, port);

        return socketAdress;
    }

    @Override
    public Selector getSelector() {
        return selector;
    }

    /**
     * Method to register a read event for the underlying connection
     * 
     * @return SelectionKey (null if fails)
     * @throws IOException
     */
    public SelectionKey registerSocket(SocketChannel serverSocket, Connection connection, int event) throws IOException {
        SelectionKey newKey = null;
        synchronized (this) {
            selector.wakeup();
            selector.selectNow();
            newKey = serverSocket.register(selector, event, connection);
            return newKey;

        }

    }

    /**
     * @return the connectionMap
     */
    public ConcurrentHashMap<Integer, Connection> getConnectionMap() {
        return connectionMap;
    }

    /**
     * @param connectionMap the connectionMap to set
     */
    public void setConnectionMap(ConcurrentHashMap<Integer, Connection> connectionMap) {
        this.connectionMap = connectionMap;
    }

}
