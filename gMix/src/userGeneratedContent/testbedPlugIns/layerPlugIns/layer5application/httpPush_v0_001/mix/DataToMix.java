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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.mix;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

import staticContent.framework.config.Settings;
import staticContent.framework.util.Util;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.Connection;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.HttpInfo;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.HttpPartType;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.SynchronizedBuffer;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.helper.SocksHandler;


/**
 * @author bash
 * This class represents the superclass for all communication to mix from the webbrowser or webserver.
 * It contains methods which are used by the subclasses EntryDataToMix and ExitDataToMix
 * 
 */
public abstract class DataToMix extends Thread {
    private LinkedBlockingQueue<Connection> readableConnections;
    private MixWriteInterface mixTunnel;
    private int bufferSize;
    private Settings settings;

    // public TrafficLog trafficlog;

    /**
     * Constructor
     * 
     * @param readableConnections
     * @param mixTunnel
     */
    public DataToMix(LinkedBlockingQueue<Connection> readableConnections, MixWriteInterface mixTunnel, Settings settings) {;
        this.settings = settings;
        this.readableConnections = readableConnections;
        this.mixTunnel = mixTunnel;
        this.bufferSize = settings.getPropertyAsInt("HP_BUFFER_SIZE");
    }

    /**
     * This class checks if a connection contains readable data either from Webbrowser or webserver.
     * The data handling depends on the state of the connection
     */
    @Override
    public void run() {
        while (true) {
            Connection readableConnection = null;
            try {
            	// Takes connection from queue. This connection contains data ready to send to mix
                readableConnection = readableConnections.take();
                readableConnection.setInProgressSend(true);
            } catch (InterruptedException e1) {
                break;
            }
            int id = readableConnection.getId();
            SocketChannel socket = readableConnection.getServerSocket();
            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            buffer.clear();
            int readBytes;
            try {

                while ((readBytes = socket.read(buffer)) > 0)
                    ;
                //If true the connection is closed by the Host
                if (readBytes == -1) {
                    System.out.println("Connection " + readableConnection.getId() + " STOP!");
                    socket.close();


                } else {

                    readableConnection.selector.registerSocket(socket, readableConnection, SelectionKey.OP_READ);
                }
                byte[] message = Arrays.copyOfRange(buffer.array(), 0, buffer.position());
                readableConnection.setConnectionMessageIncomplete(false);
                readableConnection.getConnectionBuffer().addArrayToBuffer(message);
                improveMessage(id, readableConnection);

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Method to exercise the neccessary steps to improvement
     * @param id
     * @param readableConnection
     */
    private void improveMessage(int id, Connection readableConnection) {
        while (readableConnection.isConnectionBuffer()) {

            HttpPartType messageType = readableConnection.getStatusHTTPFromApp().getType();

            switch (messageType) {
            case Header:
                headerHandling(readableConnection);
                break;
            case Body:
                bodyHandling(readableConnection);
                break;
            case BodyChunk:
                bodyChunkHandling(readableConnection);
                break;
            case SocksRequest:

                sendSocksRequest(readableConnection);
                break;
            case SocksAuth:

                sendSocksAuth(readableConnection);
                break;
            case SocksReply:

                System.err
                        .println("Methode wird doch aufgerufen SocksReply!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            default:
                System.out.println("Unknown Type");
                break;
            }
            if(readableConnection.isConnectionMessageIncomplete()){
                break;
            }
        }
        readableConnection.setInProgressSend(false);
    }

    /**
     * Method to handle a http header
     * 
     * @param readableConnection
     */
    public abstract void headerHandling(Connection readableConnection);

    /**
     * Method to handle a http body which is not chunked type
     * 
     * @param readableConnection
     */
    public abstract void bodyHandling(Connection readableConnection);

    /**
     * Method to handle a http body chunked type
     * 
     * @param readableConnection
     */
    public abstract void bodyChunkHandling(Connection readableConnection);

    /** 
     * Method to write out a body after handling.
     * Same for both encodings
     * @param message
     * @param readableConnection
     * @return message as byte[]
     */
    public byte[] bodyRelayWrite(byte[] message, Connection readableConnection) {

        int id = readableConnection.getId();
        int bodyLength = readableConnection.getStatusHTTPFromApp().getLength();
        int newLength = bodyLength - message.length;
        readableConnection.getStatusHTTPFromApp().setLength((newLength));
        if (readableConnection.getHeaderBuffer() != null) {
            writeChunkToMix(id, Util.concatArrays(readableConnection.getHeaderBuffer(), message));
            readableConnection.setHeaderBuffer(null);
        } else {
            writeChunkToMix(id, message);
        }

        if (newLength <= 0 && readableConnection.getStatusHTTPFromApp().isBody()) {
            readableConnection.setStatusHTTPFromApp(new HttpInfo(HttpPartType.Header, 0));

        }
        return message;

    }

    /**
     * Method to handle SocksAuth
     * 
     * @param message
     * @param readableConnection
     */
    public void sendSocksAuth(Connection readableConnection) {
        SynchronizedBuffer buffer = readableConnection.getConnectionBuffer();
        byte[] message = buffer.removeBytes(2);
        int len = message[1];
        byte[] mixMessage = buffer.removeBytes(len);
        System.out.println("SocksAuth");
        boolean containsMethod = false;
        for (int i = 0; i < mixMessage.length; i++) {
            if (mixMessage[i] == (byte) 0) {
                containsMethod = true;
                break;
            }
        }
        System.out.println("SocksAuth Method" + containsMethod);
        byte[] reply;
        if (containsMethod) {
            reply = SocksHandler.sendSocks5MethodReply((byte) 0);

        } else {
            reply = SocksHandler.sendSocks5MethodReply((byte) 0xff);
        }
        System.out.println("SocksAuth Reply " + reply);
        readableConnection.writeChunk(ByteBuffer.wrap(reply));
        System.out.println("SocksAuth Reply written");
        readableConnection.setStatusHTTPFromApp(new HttpInfo(HttpPartType.SocksRequest, 0));
        // return message;
    }

    /**
     * Method to send a socks request to generate a Connection
     * @param readableConnection
     */
    public void sendSocksRequest(Connection readableConnection) {
        SynchronizedBuffer buffer = readableConnection.getConnectionBuffer();
        byte[] message = buffer.removeBytes(4);
        if (settings.getPropertyAsBoolean("HP_SKIP_ROUNDTRIP")) {
            int length = 0;
            if (message[3] == 0x01) {
                length = 10;
            } else if (message[3] == 0x02) {
                length = 6 + message[4];
            } else if (message[3] == 0x03) {
                length = 22;
            } else {
                System.err.println("ERROR in Socks parsing!");

            }
            byte[] mixMessage = new byte[length];
            System.arraycopy(message, 0, mixMessage, 0, 4);
            byte[] socksMessage = buffer.removeBytes(length - 4);
            System.arraycopy(socksMessage, 0, mixMessage, 4, length - 4);
            readableConnection.setStatusHTTPFromApp(new HttpInfo(HttpPartType.Header, 0));
            writeChunkToMix(readableConnection.getId(), mixMessage);
            byte code = 0x00;
            InetSocketAddress address = SocksHandler.getInetAddress(mixMessage);
            byte aTyp = message[3];
            byte[] socksReply = SocksHandler.sendSocks5ConnectionReply(code, aTyp, address);

            readableConnection.writeChunk(ByteBuffer.wrap(socksReply));

        }

    }


    /**
     * Write Message to Mix
     * 
     * @param id
     * @param message
     */
    protected void writeChunkToMix(int id, byte[] message) {
        int len = message.length;
        mixTunnel.writeChunk(Util.concatArrays(Util.intToByteArray(id),
                Util.concatArrays(Util.intToByteArray(len), message)));

    }
}
