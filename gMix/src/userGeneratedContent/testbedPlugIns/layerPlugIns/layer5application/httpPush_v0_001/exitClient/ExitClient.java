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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import staticContent.framework.config.Settings;
import staticContent.framework.socket.socketInterfaces.StreamAnonSocketMix;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.Connection;

/**
 * This class represents the exit client. It handles all components.
 * 
 * It starts the exitconnectionpool to communicate with the webserver.
 * 
 * It builds up a connection with the mix.
 * 
 * It also handles the improvement thread to improve a message to the mix and
 * unimprove a message from the mix.
 * 
 * 
 * @author bash
 * 
 */
public class ExitClient extends Thread {

    private ConcurrentHashMap<Integer, Connection> connectionMap;

    private MixExitConnection mixConnection;
    private int improvmentThreadCounter;

    private ExitConnectionPool connectionPool;
    
    private InputStream fromControlStream;
    private OutputStream toControlStream;

    /**
     * This queue contains the connection which contains data from the outside
     */
    public LinkedBlockingQueue<Connection> readableConnections;

    /**
     * This queue contains the connection which contains data from the mix
     */
    public LinkedBlockingQueue<Connection> writeableConnections;

    private Settings settings;
    
    private ExitDataToMix[] improvementThreads;
   private  ExitDataFromMix[] unImprovementThreads;

    /**
     * Constructor
     * 
     * @param mixConnectionSocket
     * @param settings
     */
    public ExitClient(StreamAnonSocketMix mixConnectionSocket, Settings settings) {
        this.readableConnections = new LinkedBlockingQueue<Connection>();
        this.writeableConnections = new LinkedBlockingQueue<Connection>();
        this.connectionMap = new ConcurrentHashMap<Integer, Connection>();
        this.connectionPool = new ExitConnectionPool(settings.getPropertyAsInt("HP_BUFFER_SIZE"), readableConnections,
                connectionMap, mixConnection, settings);
        this.mixConnection = new MixExitConnection(mixConnectionSocket, connectionMap, connectionPool,
                writeableConnections);
        improvmentThreadCounter = settings.getPropertyAsInt("HP_THREAD_COUNTER");
        this.connectionPool.start();
        this.mixConnection.start();
        this.settings = settings;
        this.connectionPool.setMixConnection(mixConnection);
        startImprovement();
        /*establishControlChannel();
        try {
            checkControlStream();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("Client Stopped!");*/
    }

    /**
     * Method to start the thread to improve the messages
     */
    public void startImprovement() {
       improvementThreads = new ExitDataToMix[improvmentThreadCounter];
        unImprovementThreads = new ExitDataFromMix[improvmentThreadCounter];

        for (int i = 0; i < improvmentThreadCounter; i++) {
            try {
                improvementThreads[i] = new ExitDataToMix(readableConnections, mixConnection, settings);
                improvementThreads[i].start();
                unImprovementThreads[i] = new ExitDataFromMix(writeableConnections, settings);
                unImprovementThreads[i].start();
            } catch (ClassNotFoundException e) {
                System.err.println("Plugin not found, please check path!");
                e.printStackTrace();
            } catch (InstantiationException e) {
                System.err.println("Could not instatiate the plugin!");
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                System.err.println("Plugin not accessable, please check rights!");
                e.printStackTrace();
            }

        }

    }
    
    public void resetConnectionPool() {

        ConcurrentHashMap<Integer, Connection> pool = connectionPool.getConnectionMap();
        for(Thread t: improvementThreads) {
            t.interrupt()
            ;
        }
        
        for(Thread t: unImprovementThreads) {
            t.interrupt();
        }
        startImprovement();
        for (Connection con : pool.values()) {
            try {
                con.getServerSocket().close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        connectionPool.setConnectionMap(new ConcurrentHashMap<Integer, Connection>());
        System.out.println("Control: Client reseted");
    }

    /*public void establishControlChannel() {
        InetAddress address = null;
        System.out.println("control: Awaits controlconnection via 4060");

        try {
            address = InetAddress.getByName("10.1.1.41");

            ServerSocket anonServerSocket = new ServerSocket(4060, 30, address);
            System.out.println("control: waiting for controlconnections on " + address + ": 4060"
                   );

            Socket client = anonServerSocket.accept();
            System.out.println("control: controlconnection accepted (from " + client.getInetAddress() + ":"
                    + client.getPort() + ")");
            fromControlStream = client.getInputStream();
            toControlStream = client.getOutputStream();
            
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e1) {

            e1.printStackTrace();
            throw new RuntimeException("controlchannel could not bind" + address + ": 4060");
        }

    }
    
    public void checkControlStream() throws IOException {
        int readBytes;
        boolean flow = true;
        while(flow) {
            int command = fromControlStream.read();
         
            switch (command) {
            case -1:
                flow = false;
                break;
            case 1:
                resetConnectionPool();
                break;
            case 2: 
                stopClient();
                break;
            default:
                break;
            }
        }
    }*/
    
    public void stopClient() {
        for(Thread t: improvementThreads) {
            t.interrupt()
            ;
        }
        
        for(Thread t: unImprovementThreads) {
            t.interrupt();
        }
        mixConnection.stopConnection();
        connectionPool.stop();
        System.exit(0);
    }
    


}
