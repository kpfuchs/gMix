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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.test;

import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.helper.FileReadHandler;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.helper.FileWriterHandler;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used to do automatic test via the testwebrowser
 * 
 * This class read the pages from a file an requested them via the
 * testwebbrowser.
 * 
 * the amount of time an requested pages are written to a file.
 * 
 * @author bash
 * 
 */
public class GreatSiteRequest {

    private FileReadHandler reader;
    private FileWriterHandler writer;
    private int counter;
    private int connections;
    // public Webbrowser webbrowser;
    private Socket entrySocket;
    private Socket exitSocket;

    /**
     * Constructor
     * 
     * @param inputFile
     * @param outputFile
     * @param connections
     * @throws InterruptedException
     */
    public GreatSiteRequest(String inputFile, String outputFile, int connections) throws InterruptedException {
        counter = 0;
        reader = new FileReadHandler(inputFile);
        writer = new FileWriterHandler(outputFile);
        this.connections = connections;
        establishControl();
        nextLine();
        try {
            writer.createFile("exitLog.txt");
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Method to start a request from a line from the testfile
     * 
     * @throws InterruptedException
     */
    public void nextLine() throws InterruptedException {
        String line = null;
        // while((line = reader.readIn()) != null) {
        if ((line = reader.readIn()) != null) {
            System.out.println(line);
            Pattern pattern = Pattern.compile("(\\\".+?\\\"): ([A-Za-z/0-9,\\.:-]+);(\\d+)");
            Matcher matcher = pattern.matcher(line);
            matcher.find();
            String key = matcher.group(1);
            String param = matcher.group(2);
            String amount = matcher.group(3);
            counter++;
            System.out.println("Eintrag: " + counter);
            doLine(key, param, amount);
        } else {
            try {
                sendCommand(2);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            new GreatSiteRequest(args[0], args[1], 5);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * execute the request
     * 
     * @param key
     * @param site
     * @param amount
     * @throws InterruptedException
     */
    public void doLine(String key, String site, String amount) throws InterruptedException {
        // try {
        // sendCommand(1);
        // } catch (IOException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        // Thread.sleep(3000);
        Webbrowser2 webbrowser = new Webbrowser2(connections, this);
        System.out.println(site);
        // webbrowser.startRequest("http://" +site);
        webbrowser.startRequest("http://10.1.1.61/site.pl?" + site);
        // writer.writeLine(key + " " + + " "+ amount);

    }

    /**
     * Write the result to the file
     * 
     * @param message
     * @throws InterruptedException
     */
    public void summarize(String message) throws InterruptedException {
        try {
            writer.writeLine(message);
            sendCommand(2);
            Thread.sleep(6000);
            entrySocket.close();
            exitSocket.close();
            establishControl();
            nextLine();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void establishControl() {

        try {
            InetAddress entryAddress = InetAddress.getByName("10.1.1.31");
            InetAddress exitAddress = InetAddress.getByName("10.1.1.41");

            // while (true) {

            entrySocket = new Socket();
            exitSocket = new Socket();

            entrySocket.setKeepAlive(true);
            exitSocket.setKeepAlive(true);

            SocketAddress entryAdd = new InetSocketAddress(entryAddress, 4060);

            SocketAddress exitAdd = new InetSocketAddress(exitAddress, 4060);
            entrySocket.connect(entryAdd);
            exitSocket.connect(exitAdd);
            System.out.println("Connection established!");

            // break;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            System.out.println("Etwas geht nicht!");
            e.printStackTrace();
            // break;
            // try {
            // Thread.sleep(2000);
            // } catch (InterruptedException e1) {
            // continue;
            // }
            // }

        }

    }

    public void sendCommand(int command) throws IOException {
        entrySocket.getOutputStream().write(command);
        entrySocket.getOutputStream().flush();
        exitSocket.getOutputStream().write(command);
        exitSocket.getOutputStream().flush();
    }

}
