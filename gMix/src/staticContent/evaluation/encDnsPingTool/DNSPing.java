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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.DirectSchedulerFactory;

import userGeneratedContent.testbedPlugIns.layerPlugIns.layer2recodingScheme.encDNS_v0_001.EncDNSHelper;


/**
 * The DNSPing utility will send queries to a specified nameserver and measure
 * the time it takes for a corresponding answer to arrive. It will then output
 * minimum, maximum, average, 25th, 50th (median) and 75th percentile of the
 * round trip times.
 * 
 * @author Jens Lindemann
 */
public class DNSPing {
    /** maximum DNS message size */
    public static final int MAX_MSG_SIZE = 65535;
    private final int NUM_QUERIES;
    /** interval between queries in msecs */
    private final int INTERVAL;
    /** timeout for queries */
    protected final int TIMEOUT;
    /** number of queries to be ignored at start of measurement */
    private final int IGNORE;
    
    private int _qSent;
    private int _qErr;
    private int _qTimeout;
    private int _repliesRcv;
    private ArrayList<Long> _rtts;
    //private ArrayList<Long> _sendTimes;
    private int _ignore;
    
    private byte[] _qname;
    private short _qtype;
    private short _qclass;
    private int _qNum;
    
    /** target nameserver's address/IP */
    protected final InetAddress _targetNSAddress;
    /** target nameserver's UDP port */
    protected final short _targetNSPort;
    
    private Random _rnd;
    
    private static Options _opt;
    
    /**
     * Constructor for DNSPing class
     * @param numQueries number of queries to send
     * @param interval interval between individual queries (in ms)
     * @param ignore number of queries to ignore at start of measurement
     * @param ns nameserver's address / IP
     * @param port nameserver's UDP port
     * @param timeout timeout for queries
     * @param qname query name (will be prefixed by a counter to avoid caching by recursive nameservers)
     * @param qtype query type
     * @param qclass query class
     * @throws UnknownHostException @see UnknownHostException will be thrown if nameserver's address / IP is unknown
     */
    public DNSPing(int numQueries, int interval, int ignore, String ns, int port, int timeout, byte[] qname, short qtype, short qclass) throws UnknownHostException {
        NUM_QUERIES = numQueries;
        INTERVAL = interval;
        IGNORE = ignore;
        _ignore = IGNORE;
        
        // initialize counters
        _qSent = 0;
        _qErr = 0;
        _qTimeout = 0;
        _repliesRcv = 0;
        
        TIMEOUT = timeout;
        _rtts = new ArrayList<Long>(NUM_QUERIES);
        //_sendTimes = new ArrayList<Long>(NUM_QUERIES);
        _rnd = new Random();
        _targetNSAddress = InetAddress.getByName(ns);
        _targetNSPort = (short)port;
        _qname = qname;
        _qtype = qtype;
        _qclass = qclass;
        _qNum = 0;
        
        sendQueryQuartz();
    }
    
    /**
     * Increments the counter for number of queries sent.
     */
    protected synchronized void querySent(long time) {
        _qSent++;
        //_sendTimes.add(time);
    }
    
    /**
     * Increments the counter for number of responses received and records round
     * trip time
     * @param tdiff round trip time (nanoseconds)
     */
    protected synchronized void responseReceived(long tdiff) {
        _repliesRcv++;
        _rtts.add(tdiff);
    }
    
    /**
     * Increments the counter for number of query errros.
     */
    protected synchronized void queryError() {
        _qErr++;
    }
    
    /**
     * Increments the counter for number of query timeouts.
     */
    protected synchronized void queryTimedOut() {
        _qTimeout++;
    }
    
    /**
     * This method will be called when a new SendJob is being started. It will
     * return to the caller whether the query is to be ignored for measurement
     * purposes and will decrement the ignore counter.
     * 
     * @return true if query is to be ignored, false otherwise
     */
    protected synchronized boolean reportSendJobStart() {
    	if(_ignore == 0) {
    		return false;
    	} else {
    		_ignore--;
    		return true;
    	}
    }
    
    /**
     * Sends the queries using the Quartz scheduling framework.
     */
    private void sendQueryQuartz() {
        try {
            DirectSchedulerFactory df = DirectSchedulerFactory.getInstance();
            df.createVolatileScheduler((TIMEOUT/INTERVAL)+2); // create sufficient number of threads to be able to handle all queries, even if all of them time out
            Scheduler sched = df.getScheduler();
            
            sched.start();
            
            // Create a JobDataMap and put a reference to this class into it, 
            // so the jobs can access it.
            JobDataMap jdm = new JobDataMap();
            jdm.put("mainclassobj", this);
            
            JobDetail job = JobBuilder.newJob(SendJob.class)
                    .withIdentity("sendJob")
                    .usingJobData(jdm)
                    .build();
            
            // Create a trigger which will automatically start the SendJobs
            // NUM_QUERIES times with INTERVAL ms between calls.
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("sendTrigger")
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMilliseconds(INTERVAL)
                        .withRepeatCount(NUM_QUERIES-1))
                    .build();
            sched.scheduleJob(job, trigger);
            
            // Wait until all queries have finished or timed out.
            Thread.sleep((NUM_QUERIES*INTERVAL)+1000);
            while((_qSent < NUM_QUERIES) || ((_repliesRcv + _qErr + IGNORE) < NUM_QUERIES)) {
                Thread.sleep(1000);
            }

            sched.shutdown();
            
            // Calculate and output statistics.
            System.out.println("Queries sent: " + _qSent);
            System.out.println("Queries ignored: " + IGNORE);
            System.out.println("Replies received: " + _repliesRcv);
            System.out.println("Queries timed out: " + _qTimeout);
            System.out.println("Query errors: " + _qErr);
            
            Collections.sort(_rtts);
            double min = (double)_rtts.get(0)/1000000;
            double max = (double)_rtts.get(_rtts.size()-1)/1000000;
            double median = (double)_rtts.get((int)Math.round(0.5*(_rtts.size()-1)))/1000000;
            double perc25 = (double)_rtts.get((int)Math.round(0.25*(_rtts.size()-1)))/1000000;
            double perc75 = (double)_rtts.get((int)Math.round(0.75*(_rtts.size()-1)))/1000000;
            
            double[] dblRTTs = new double[_rtts.size()];
            for(int i = 0; i < _rtts.size(); i++) {
                dblRTTs[i] = _rtts.get(i);
            }
            
            Mean meanCalc = new Mean();
            double mean = meanCalc.evaluate(dblRTTs)/1000000;
            StandardDeviation sdCalc = new StandardDeviation();
            double sd = sdCalc.evaluate(dblRTTs)/1000000;
            
            System.out.println("Min RTT: " + min + " ms");
            System.out.println("Max RTT: " + max + " ms");
            System.out.println("RTT Median: " + median + " ms");
            System.out.println("RTT 25-percentile: " + perc25 + " ms");
            System.out.println("RTT 75-percentile: " + perc75 + " ms");
            System.out.println("Mean RTT: " + mean + " ms");
            System.out.println("RTT SD: " + sd + " ms");
            /*try {
                // TODO Write to file
                PrintWriter out = new PrintWriter("dnsping.stats");
                out.println("Min RTT: " + min + " ms");
                out.println("Max RTT: " + max + " ms");
                out.println("RTT Median: " + median + " ms");
                out.println("RTT 25-percentile: " + perc25 + " ms");
                out.println("RTT 75-percentile: " + perc75 + " ms");
                out.println("Mean RTT: " + mean + " ms");
                out.println("RTT SD: " + sd + " ms");
                out.close();
                
                out = new PrintWriter("dnsping.sendtimes");
                for(int i = 0; i < _sendTimes.size(); i++) {
                    out.println(_sendTimes.get(i));
                }
                out.close();
                
                out = new PrintWriter("dnsping.rtt");
                for(int i = 0; i < _rtts.size(); i++) {
                    out.println(_rtts.get(i));
                }
                out.close();
            } catch (FileNotFoundException ex) {
                // TODO error handling
                System.err.println(ex);
            }*/
            
        } catch(SchedulerException e) {
            System.err.println(e);
        } catch(InterruptedException e) {
            System.err.println(e);
        }
    }

    /**
     * Builds the query message
     * @param qName query name
     * @param type query type
     * @param qClass query class
     * @return query message
     */
    private byte[] buildQuery(byte[] qName, short type, short qClass) {
        ByteBuffer buf = ByteBuffer.allocate(MAX_MSG_SIZE);

        // --- DNS HEADER ---
        // Choose random ID
        byte[] id = new byte[2];
        _rnd.nextBytes(id);
        buf.put(id);
        buf.put((byte) 1); // QR=0 (Query), OpCode=0000 (Query), AA=0, TC=0, RD=1
        buf.put((byte) 0); // RA,Z,AD,CD,RCode=0
        buf.putShort((short) 1); // question count = 1
        buf.putShort((short) 0); // answer count = 0
        buf.putShort((short) 0); // authority count = 0
        buf.putShort((short) 0); // additional count = 0

        // --- DNS QUESTION SECTION ---
        buf.put(qName); // NAME
        buf.putShort(type); // TYPE
        buf.putShort(qClass); // CLASS

        // convert to byte[]
        byte[] out = new byte[buf.position()];
        buf.position(0);
        buf.get(out);

        return out;
    }
    
    /**
     * Creates a new query message with a unique prefix.
     * @return query message
     */
    public byte[] getQueryBytes() {
    	byte[] label1length = {36};
    	byte[] label1 = ("a123456789a123456789a123456789a12345".getBytes());
    	
    	byte[] prefix = new byte[19];
    	prefix[0] = 18;
    	prefix[1] = 'a';
    	prefix[2] = '1';
    	prefix[3] = '2';
    	prefix[4] = '3';
    	prefix[5] = '4';
    	prefix[6] = '5';
    	prefix[7] = '6';
    	prefix[8] = '7';
    	prefix[9] = '8';
    	prefix[10] = 'w';
    	String numstr = String.format("%08d", _qNum);
    	if(_qNum>=99999999) {
    		_qNum = 0;
    	} else {
    		_qNum++;
    	}
    	for(int i = 11; i < prefix.length; i++) {
    		prefix[i] = (byte)numstr.charAt(i-11);
    	}
    	byte[] qname = ArrayUtils.addAll(prefix, label1length);
    	qname = ArrayUtils.addAll(qname, label1);
    	//qname = ArrayUtils.addAll(qname, label1length);
    	//qname = ArrayUtils.addAll(qname, label1);
    	qname = ArrayUtils.addAll(qname, _qname);
    	//byte[] qname = ArrayUtils.addAll(prefix, _qname);
    	byte[] query = buildQuery(qname, _qtype, _qclass);
        return query;
    }

    public static void main(String[] args) {
        try {
            _opt = new Options();
            _opt.addOption("h", "help", false, "prints this message");
            _opt.addOption("n", "queries", true, "number of queries (default: 100)");
            _opt.addOption("i", "interval", true, "interval between queries (msecs) (default: 100)");
            _opt.addOption("w", "ignore", true, "number of queries to ignore at start of measurement (default: 0)");
            _opt.addOption("a", "nameserver", true, "nameserver's IP address or hostname (default: 127.0.0.1)");
            _opt.addOption("p", "port", true, "nameserver's port (default: 53)");
            _opt.addOption("t", "timeout", true, "query timeout (msecs) (default: 5000)");
            
            Option qnameOpt = new Option("q", "qname", true, "query name suffix (will be prefixed with wnnnnnnnn.) (REQUIRED)");
            qnameOpt.setRequired(true);
            _opt.addOption(qnameOpt);
            
            _opt.addOption("T", "qtype", true, "query type (QTYPE) (default: 1 (A))");
            _opt.addOption("c", "qclass", true, "query class (default: 1 (IN))");
            
            Options helpOpts = new Options();
            helpOpts.addOption("h", "help", false, "prints this message");
            
            CommandLineParser parser = new BasicParser();
            
            // We need to check for the help parameter first, as parsing all
            // simultaneously will throw an exception if a required option
            // is missing.
            CommandLine cmd = parser.parse(helpOpts, args, true);
            if (cmd.hasOption("help")) {
                HelpFormatter hf = new HelpFormatter();
                hf.printHelp("DNSPing", _opt);
                return;
            }
            
            // Now we can parse all options
            cmd = parser.parse(_opt, args);
            
            // Get parameters from CLI
            String numQueriesStr = cmd.getOptionValue("n");
            String intervalStr = cmd.getOptionValue("i");
            String ignoreStr = cmd.getOptionValue("w");
            String nsStr = cmd.getOptionValue("a");
            String portStr = cmd.getOptionValue("port");
            String timeoutStr = cmd.getOptionValue("timeout");
            String qnameStr = cmd.getOptionValue("q");
            String qtypeStr = cmd.getOptionValue("qtype");
            String qclassStr = cmd.getOptionValue("c");
            
            // Set values from CLI or use defaults, if not present
            int numQueries = 100;
            if(numQueriesStr != null) numQueries = Integer.parseInt(numQueriesStr);
            
            int interval = 100;
            if(intervalStr != null) interval = Integer.parseInt(intervalStr);
            
            int ignore = 0;
            if(ignoreStr != null) ignore = Integer.parseInt(ignoreStr);
            
            if(nsStr == null) nsStr = "127.0.0.1";
            
            int port = 53;
            if(portStr != null) port = Integer.parseInt(portStr);
            
            int timeout = 5000;
            if(timeoutStr != null) timeout = Integer.parseInt(timeoutStr);
            
            short qtype = 1;
            // This must be parsed as an Integer as otherwise values having a
            // highest-value bit of 1 would be considered out of range
            if(qtypeStr != null) {
                int qtypeInt = Integer.parseInt(qtypeStr);

                if(qtypeInt>65535) {
                    System.err.println("Query type must be <= 65535!");
                    return;
                }
                qtype = (short)qtypeInt;
            }
            
            short qclass = 1;
            if (qclassStr != null) qclass = Short.parseShort(qclassStr);
            
            byte[] qname = EncDNSHelper.parseZoneNameString(qnameStr);
                    
            new DNSPing(numQueries, interval, ignore, nsStr, port, timeout, qname, qtype, qclass);
        } catch (MissingOptionException e) {
            // Print error message and help if required option is missing
             System.out.println("Missing required option: " + e.getMessage());
             HelpFormatter hf = new HelpFormatter();
             hf.printHelp("DNSPing", _opt);
        } catch (UnknownHostException ex) {
            // Print error message if nameserver address/IP is invalid
            System.err.println("Invalid destination address!");
        } catch (ParseException e) {
            System.err.println(e);
        }
    }
}
