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
package staticContent.evaluation.encDnsPingTool;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Random;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;


/**
 * Quartz Job for sending queries (part of the DNSPing tool).
 * 
 * @author Jens Lindemann
 */
public class SendJob implements Job {
        private long _tdiff;
        private DNSPing _main;
        private Random _rnd;
        private boolean _ignore; // Ignore this query for measurement purposes?
        
        /**
         * Constructor for creating a new SendJob
         */
        public SendJob() {
            _rnd = new Random();
        }
        
        public void execute(JobExecutionContext jec) throws JobExecutionException {
            JobDataMap jdm = jec.getJobDetail().getJobDataMap();
            _main = (DNSPing)(jdm.get("mainclassobj"));
            
            // Report that a new SendJob was started and determine whether the
            // RTT should be ignored for measurement purposes.
            _ignore = _main.reportSendJobStart();
            
            //System.out.println("SendJob started");
            
            // Create a new query message
            byte[] query = _main.getQueryBytes();

            // Start the query handling method
            byte[] reply = sendQueryAndListenForReply(query);

            // If the RTT should be ignored, do not report it to the DNSPing class
            if(_ignore) return;
            
            // Check whether response is valid (matching IDs and not null)
            boolean validReply = false;
            if (reply != null) {
                if((reply[3]&0x0f) == 0) { // check whether reply code is 0 (no error)
                    byte[] qid = Arrays.copyOfRange(query, 0, 2);
                    byte[] rid = Arrays.copyOfRange(reply, 0, 2);
                    if (Arrays.equals(qid, rid)) {
                        validReply = true;
                    }
                }
            }

            if(validReply) {
                // If reponse is valid, report success and RTT to DNSPing
                _main.responseReceived(_tdiff);
            } else {
                // else report an error.
                _main.queryError();
            }
        }

        /**
         * Sends a query to the nameserver and waits for a response
         * @param query query to send to nameserver
         * @return response received from nameserver
         */
        private byte[] sendQueryAndListenForReply(byte[] query) {
            try {
                // Open UDP socket on new port
                DatagramSocket udpSock = null;
                while (udpSock == null) {
                    try {
                        int port = 1024 + _rnd.nextInt(64512);
                        udpSock = new DatagramSocket(port);
                    } catch (SocketException e) {
                        // do nothing as this is being caught by the while loop
                    }
                }

                // Send query to nameserver
                DatagramPacket sendPacket = new DatagramPacket(query,
                        query.length, _main._targetNSAddress, _main._targetNSPort);
                long tbefore = System.nanoTime(); // record send time
                
                // Report that a new query is being sent
                _main.querySent(tbefore);
                
                udpSock.send(sendPacket);
                
                // Wait for a response
                byte[] rcvbytes = new byte[DNSPing.MAX_MSG_SIZE];
                DatagramPacket rcvPkt = new DatagramPacket(rcvbytes, rcvbytes.length);
                udpSock.setSoTimeout(_main.TIMEOUT);
                udpSock.receive(rcvPkt);
                
                // Record receive time of response and calculate RTT
                long tafter = System.nanoTime();
                _tdiff = tafter-tbefore;
                
                udpSock.close();
                byte[] rcvDNS = new byte[rcvPkt.getLength()];
                System.arraycopy(rcvPkt.getData(), rcvPkt.getOffset(), rcvDNS, 0, rcvPkt.getLength());
                //System.out.println("Received response from aNS");
                return rcvDNS;
            } catch (SocketTimeoutException e) {
                System.err.println("Query timed out.");
                if(!_ignore) {
                    // If this is not a warmup query, report timeout to DNSPing
                    _main.queryTimedOut();
                }
                return null;
            } catch (IOException e) {
                System.err.println("IO error:");
                System.err.println(e);
                return null;
            }
        }
}
