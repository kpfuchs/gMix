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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.test;

import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.helper.HtmlParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;

/**
 * @author bash
 * 
 */
public class Webbrowser2 {
    int counter;
    LinkedBlockingQueue<BrowserConnection> connectionQueue;
    LinkedBlockingQueue<String> requestQueue;
    LinkedBlockingQueue<String> returnValue;
    List<BrowserConnection> connectionList;
    int connections;
    long startTime;
    URL pathURL;
    GreatSiteRequest requester;

    /**
     * 
     */
    public Webbrowser2(int connections) {
        counter = 0;
        this.connections = connections;
        connectionList = new LinkedList<BrowserConnection>();
        connectionQueue = new LinkedBlockingQueue<BrowserConnection>();
        requestQueue = new LinkedBlockingQueue<String>();
        returnValue = new LinkedBlockingQueue<String>();
        for (int i = 0; i < connections; i++) {
            BrowserConnection browserConnection = new BrowserConnection();
            browserConnection.start();
            connectionList.add(browserConnection);
            connectionQueue.add(browserConnection);
        }
    }

    public Webbrowser2(int connections, GreatSiteRequest requester) {
        counter = 0;
        this.connections = connections;
        connectionList = new LinkedList<BrowserConnection>();
        connectionQueue = new LinkedBlockingQueue<BrowserConnection>();
        requestQueue = new LinkedBlockingQueue<String>();
        returnValue = new LinkedBlockingQueue<String>();
        this.requester = requester;
        System.out.print("Try to Connect");
        for (int i = 0; i < connections; i++) {
            BrowserConnection browserConnection = new BrowserConnection();
            browserConnection.start();
            connectionList.add(browserConnection);
            connectionQueue.add(browserConnection);
        }
        System.out.println("Connections are open");
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        System.out.println("Konsolentest");
        Webbrowser2 webbrowser = new Webbrowser2(Integer.parseInt(args[1]));
        System.out.println("start");
        try {
            webbrowser.startRequest(args[0]);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Method to start an request, also provides the result
     * 
     * @param website
     * @throws InterruptedException
     */
    public void startRequest(String website) throws InterruptedException {
        startTime = System.nanoTime();
        try {
            pathURL = new URL(website);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        connectionQueue.take().addRequest(website);
        // return returnValue.take();
    }

    /**
     * Callback for finished connections
     * 
     * @throws InterruptedException
     */
    public void afterRequest(BrowserConnection connection) throws InterruptedException {
        synchronized (requestQueue) {

            if (!requestQueue.isEmpty()) {
                connection.addRequest("http://" + pathURL.getHost() + requestQueue.take());
                while (!connectionQueue.isEmpty() && !requestQueue.isEmpty()) {
                    connectionQueue.take().addRequest("http://" + pathURL.getHost() + requestQueue.take());
                }
            } else {
                connectionQueue.put(connection);
                if (connectionQueue.size() == connections) {
                    endRequest();
                }
                // connectionQueue.put(connection);
            }
        }

    }

    /**
     * Callback if the complet site was loaded
     * 
     * @throws InterruptedException
     */
    public void endRequest() throws InterruptedException {
        String result;
        long endTime = System.nanoTime();
        long runningTimeMs = (endTime - startTime) / 1000000;
        System.out.println("Anzahl angeforderter Seite: " + counter);
        System.out.println("Zeit der Anforderung: " + runningTimeMs);
        result = counter + " " + runningTimeMs + "\r\n";
        for (BrowserConnection con : connectionList) {
            // connectionQueue.take();
            con.addRequest("STOP_THREAD");
        }
        // returnValue.put(result);
        requester.summarize(result);
    }

    /**
     * Private class Models connection
     * 
     * @author bash
     * 
     */
    private class BrowserConnection extends
            Thread {
        Socket connection;
        LinkedBlockingQueue<String> request;

        /**
         * Constructor
         * 
         * @param url
         */
        public BrowserConnection() {
            try {
                connection = generateConnection();
                request = new LinkedBlockingQueue<String>();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        /**
         * Generate Connection to socks proxy
         * 
         * @param url
         * @return connectionsocket
         * @throws IOException
         */
        public Socket generateConnection() throws IOException {
          //   URL url = new
            // URL("http://www.informatik.uni-hamburg.de/svs/team/fuchs.php");
            // Socket socket = new Socket(url.getHost(), 80);
        //    Socket socket = new Socket("localhost", 4007);
           Socket socket = new Socket("10.1.1.31", 4007);
            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();
            System.out.println("Socks");
            socksHandshake(input, output);
            return socket;
        }

        /**
         * Do socks handshake
         * 
         * @param input
         * @param output
         * @throws IOException
         */
        public void socksHandshake(InputStream input, OutputStream output) throws IOException {
            byte[] b = { 5, 1, 0 };

            output.write(b);
            output.flush();
            byte[] response = new byte[2];

            input.read(response);
            byte[] c = { 5, 1, 0, 1, 10, 1, 1, 61, 0, 80 };
      //      byte[] c = { 5, 1, 0, 1, -122 ,100,9,77, 0, 80 };

            output.write(c);
            output.flush();
            byte[] response2 = new byte[11];
            System.out.println("Wait for respons");
            int messageLength = input.read(response2);
System.out.println("Response received");
        }

        public void addRequest(String newRequest) {
            request.add(newRequest);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    String req = request.take();
                    if (req.equals("STOP_THREAD")) {
                        break;
                    }
                    try {
                        requestPage(req);

                    } catch (HttpException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } catch (InterruptedException e) {
                    try {
                        connection.close();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    break;
                }

            }

        }

        public void requestPage(String website) throws HttpException, IOException, InterruptedException {
           //  String pathNative =
           //  "http://www.informatik.uni-hamburg.de/svs/team";
        
                URL url = new URL(website);
            String host = url.getHost();
            String uri = url.getFile();

            HttpProcessor httpproc = HttpProcessorBuilder.create().add(new RequestContent())
                    .add(new RequestTargetHost()).add(new RequestConnControl()).add(new RequestUserAgent("Test/1.1"))
                    .add(new RequestExpectContinue(true)).build();
            HttpRequestExecutor httpexecutor = new HttpRequestExecutor();
            HttpCoreContext coreContext = HttpCoreContext.create();
            coreContext.setTargetHost(new HttpHost(host, 80));

            DefaultBHttpClientConnection conn = new DefaultBHttpClientConnection(8 * 1024);
            if (!conn.isOpen()) {
                conn.bind(connection);
            }
            BasicHttpRequest request = new BasicHttpRequest("GET", uri);
            System.out.println(">> Request URI: " + request.getRequestLine().getUri());

            httpexecutor.preProcess(request, httpproc, coreContext);

            HttpResponse response = httpexecutor.execute(request, conn, coreContext);
            httpexecutor.postProcess(response, httpproc, coreContext);

           System.out.println("<< Response: " + request.getRequestLine().getUri() + " "+ response.getStatusLine());
            

            BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            
            String inputLine;
            StringBuilder stringBuilder = new StringBuilder();
            while ((inputLine = in.readLine()) != null){
                stringBuilder.append(inputLine + "\r\n");
              
            }
            in.close();
       //     System.out.println(stringBuilder.toString());
            String page = stringBuilder.toString();
            if (response.getEntity().getContentType().getValue().contains("text/html")) {
                requestQueue.addAll(HtmlParser.getAllRessourcesHtml(website, page));
            } else if (response.getEntity().getContentType().getValue().contains("text/css")) {
                requestQueue.addAll(HtmlParser.getAllRessourcesCss(page));
            }
            counter++;
    //        System.out.println("<< Response: " + request.getRequestLine().getUri() + " complete " + counter);
            System.out.println("==============");
            EntityUtils.consume(response.getEntity());

            afterRequest(this);

        }
    }

}
