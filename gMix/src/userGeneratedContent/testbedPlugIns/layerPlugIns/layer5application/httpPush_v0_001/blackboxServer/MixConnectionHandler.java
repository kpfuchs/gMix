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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.blackboxServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import staticContent.framework.util.Util;

/**
 * @author bash
 * 
 */
public class MixConnectionHandler implements Runnable {

    // private ServerSocket exitConnectionSocket;
    private Socket entryConnection;
    private Socket exitConnection;
    private long delayValue;

    private DelayQueue<BufferStructure> entryToExitBuffer;
    private DelayQueue<BufferStructure> exitToEntryBuffer;
    
    private LinkedBlockingQueue<byte[]> en2Ex;
    private LinkedBlockingQueue<byte[]> ex2En;

    
    /**
     * Constructor
     * @param entryConnection
     * @param delayValue
     */
    public MixConnectionHandler(Socket entryConnection, long delayValue) {
        this.entryConnection = entryConnection;
        this.delayValue = delayValue;

        openConnectioToMix();

        // Buffer initialisieren
        this.entryToExitBuffer = new DelayQueue<BufferStructure>();
        this.exitToEntryBuffer = new DelayQueue<BufferStructure>();
        
        en2Ex = new LinkedBlockingQueue<byte[]>();
        ex2En = new LinkedBlockingQueue<byte[]>();

    }

    /**
     * Connection to Exitclient
     */
    private void openConnectioToMix() {
        InetAddress address = null;
       // System.out.println("mix: Try to connect to mixnetwork via localhost!");

        try {
            address = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        exitConnection = new Socket();
        try {
            exitConnection.setKeepAlive(true);

            SocketAddress receiverAddress = new InetSocketAddress(address, 4003);
            exitConnection.connect(receiverAddress);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            // try {
            // Thread.sleep(2000);
            // } catch (InterruptedException e1) {
            // continue;
            // }
        }

     //   System.out.println("client: multiplexed tunnel to mix (" + address + ":" + 4051 + ") established!");
    }

    @Override
    public void run() {

        try {
            InputStreamService entryToExitInputStream = new InputStreamService(entryConnection.getInputStream(),
                    entryToExitBuffer, "entryToExit",en2Ex);
            InputStreamService exitToEntryInputStream = new InputStreamService(exitConnection.getInputStream(),
                    exitToEntryBuffer, "exitToEntry", ex2En);

            Thread entryToExitIn = new Thread(entryToExitInputStream);
            Thread exitToEntryIn = new Thread(exitToEntryInputStream);

            entryToExitIn.start();
            exitToEntryIn.start();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            OutputStreamService entryToExitOutputStream = new OutputStreamService(exitConnection.getOutputStream(),
                    entryToExitBuffer, "entryToExit", en2Ex);
            OutputStreamService exitToEntryOutputStream = new OutputStreamService(entryConnection.getOutputStream(),
                    exitToEntryBuffer, "exitToEntry", ex2En);

            Thread entryToExitOut = new Thread(entryToExitOutputStream);
            Thread exitToEntryOut = new Thread(exitToEntryOutputStream);
            entryToExitOut.start();
            exitToEntryOut.start();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Structure for Buffer
     * 
     * @author bash
     */
    private class BufferStructure implements Delayed {
        public long timestamp;
        public long expireTime;
        private byte[] data;

        public BufferStructure(byte[] data) {
            this.timestamp = new java.util.Date().getTime();
            this.data = data;
            this.expireTime = timestamp +delayValue;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        @Override
        public long getDelay(TimeUnit timeUnit) {
    //        long remainingDelay = timestamp + delayValue - new java.util.Date().getTime();
            long remainingDelay = expireTime - new java.util.Date().getTime();
           // System.out.println(new String(data) + " "+ remainingDelay);
            return timeUnit.convert(remainingDelay, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed arg0) {
            return this.timestamp > ((BufferStructure) arg0).timestamp ? 1
                    : this.timestamp < ((BufferStructure) arg0).timestamp ? -1 : 0;
        }

    }

    /**
     * Subclass for inputStream
     * 
     * @author bash
     */
    private class InputStreamService implements Runnable {
      
        private InputStream inputStream;
        private DelayQueue<BufferStructure> buffer;
        private LinkedBlockingQueue<byte[]> testQ;

        public InputStreamService(InputStream inputStream, DelayQueue<BufferStructure> buffer, String side, LinkedBlockingQueue<byte[]> testQ) {
            this.inputStream = inputStream;
            this.buffer = buffer;
           
            this.testQ = testQ;

        }

        @Override
        public void run() {
            byte[] incomingData;

            while (true) {

                int id;
                int len;
                byte[] message;

                try {
                    id = Util.forceReadInt(inputStream);
                    len = Util.forceReadInt(inputStream);
                    message = Util.forceRead(inputStream, len);
                    incomingData = Util.concatArrays(Util.intToByteArray(id),
                            Util.concatArrays(Util.intToByteArray(len), message));
                   
                    buffer.add(new BufferStructure(incomingData));
                    testQ.add(incomingData);
                    

                } catch (IOException e) {

                    System.out.println("mix: entry closed connection!");
                    break;

                }

            }

        }

    }

    /**
     * Subclass for outputstream
     * 
     * @author bash
     * 
     */
    private class OutputStreamService implements Runnable {
        
        private OutputStream outputStream;
        private DelayQueue<BufferStructure> buffer;
        private LinkedBlockingQueue<byte[]> testQ;

        public OutputStreamService(OutputStream outputStream, DelayQueue<BufferStructure> buffer, String side,LinkedBlockingQueue<byte[]> testQ) {
            this.outputStream = outputStream;
            this.buffer = buffer;
         
            this.testQ = testQ;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    BufferStructure bufferElement = buffer.take();
                    byte[] data = bufferElement.getData();
                    byte[] data2 = testQ.take();
                    if(!Arrays.equals(data, data2)) {
                   //    trafficlog.writeLine(new String(data)); 
                    //   trafficlog.writeLine(new String(data2));
                     //  trafficlog.writeLine(buffer.size() + " " + testQ.size());
                     //  long timeNext = buffer.take().getTimestamp();
                      // trafficlog.writeLine(Long.toString(timeNext - oldStamp));
                        outputStream.write(data2);
                       
                    } else {
                  //  trafficlog.writeLine(new String(data));
                    outputStream.write(data);
                    }
                    outputStream.flush();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }

    }

}
