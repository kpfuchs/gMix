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
package staticContent.framework;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.Vector;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import staticContent.framework.config.Settings;
import staticContent.framework.controller.Controller;
import staticContent.framework.controller.Layer1NetworkMixController;
import staticContent.framework.controller.Layer2RecodingSchemeMixController;
import staticContent.framework.controller.Layer3OutputStrategyMixController;
import staticContent.framework.controller.Layer4TransportMixController;
import staticContent.framework.controller.Layer5ApplicationMixController;
import staticContent.framework.launcher.CommandLineParameters;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer2recodingScheme.encDNS_v0_001.EncDNSHelper;


/**
 * This is the EncDNS server proxy (also known as remote proxy). It should be
 * executed on the same computer as the remote recursive nameserver or at least 
 * be connected to it via a trustworthy connection. The server proxy will 
 * decrypt encrypted EncDNS queries it receives and pass the decrypted
 * standard DNS query on to the remote recursive nameserver. It will also
 * encrypt the standard DNS responses received from the remote recursive
 * nameserver and pass the EncDNS response on to the local recursive nameserver.
 * 
 * Note: don't use anonnode.java as we dont need the info service etc (encDns 
 * is a single node system)
 */
public class EncDnsServer {

	private Settings settings;
	public static CommandLineParameters commandLineParameters;
	
	// references to controllers
	protected Layer1NetworkMixController networkLayerMix;
	protected Layer2RecodingSchemeMixController recodingLayerMix;
	protected Layer3OutputStrategyMixController outputStrategyLayerMix;
	protected Layer4TransportMixController transportLayerMix;
	protected Layer5ApplicationMixController applicationLayerMix;
		
	private Vector<Controller> components = new Vector<Controller>(); // instantiated controllers
	
    private static Options _opt;
    
    public static InetAddress bindAddress;
	public static int bindPort;
	
	public static String zoneurl;
	public static InetAddress nsaddr;
	public static int port;
	public static int cachesize;
	public static int timeout;
	public static int maxThreads;
	public static String pkPath;
	public static String skPath;
	public static boolean encryption;
	public static int verbosity;
	public static boolean displayThreadStatusBool;
	public static int displayThreadStatusInt;
	public static boolean displayThroughputBool;
	public static boolean useInternalResolver;
	public static boolean resolveToLocalhost;
	/** maximum size of an EncDNS message */
    public static final int MAX_MSG_SIZE = 65535; // = maximum DNS msg length 
    public static Random rnd = new SecureRandom();
    
    
    /**
     * Creates a new instance of EncDNSServerProxy.new file
     * @param zoneurl EncDNS zone name (The IP this proxy is listening on must be listed as an authoritative nameserver IP for this zone in the parent zone)
     * @param nsaddr remote recursive nameserver's address
     * @param port remote recursive nameserver's port
     * @param cachesize size of cache for intermediate shared secrets
     * @param pkPath path to public key file
     * @param skPath path to secret key file
     * @param noEncryption If true, disables encryption. If false, encryption is enabled.
     * @param verbosity verbosity level
     */
    public EncDnsServer() {
    	if (commandLineParameters == null) {
        	settings = CommandLineParameters.loadPluginSettings("./inputOutput/anonNode/encDns.txt");
    	} else {
        	settings = CommandLineParameters.loadPluginSettings("./inputOutput/anonNode/encDns.txt", commandLineParameters.overwriteParameters);
        	// settigns in Paths.PATH_TO_PATH_CONFIG_FILE have higher priority than normal plug-in settings -> overwrite (again)
            settings.addProperties("./inputOutput/anonNode/encDns.txt");
            if (commandLineParameters.overwriteParameters != null) // overwriteParameters have higher priority than all other settings -> overwrite (again)
     			Settings.overwriteSettings(settings.getPropertiesObject(), commandLineParameters.overwriteParameters, true);
        }
    	generateComponents();
		setReferencesBetweenComponents();
		loadImplementations(); // loads the plug-ins
		callConstructors();
		initializeComponents();
		beginMixing();
    }
    
    
	private void generateComponents() {
		// NOTE: the order of the items is important (do not change it)
		//this.outputStrategyLayerMix = new Layer3OutputStrategyMixController(null, settings, null, null, null);
		//this.outputStrategyLayerMix.instantiateSubclass();
		//this.components.add(outputStrategyLayerMix);
		this.recodingLayerMix = new Layer2RecodingSchemeMixController(null, settings, null, null, null);
		this.recodingLayerMix.instantiateSubclass();
		this.components.add(recodingLayerMix);
		this.networkLayerMix = new Layer1NetworkMixController(null, settings, null, null, null);
		this.networkLayerMix.instantiateSubclass();
		this.components.add(networkLayerMix);
		//this.transportLayerMix = new Layer4TransportMixController(null, settings, null, null, null);
		//this.transportLayerMix.instantiateSubclass();
		//this.components.add(transportLayerMix);
		this.applicationLayerMix = new Layer5ApplicationMixController(null, settings, null, null, null);
		this.applicationLayerMix.instantiateSubclass();
		this.components.add(applicationLayerMix);
	}
	
	
	private void setReferencesBetweenComponents() {
		for (Controller c:components)
			c.setComponentReferences(networkLayerMix, null, recodingLayerMix, null, outputStrategyLayerMix, null, transportLayerMix, null, applicationLayerMix, null, null, null, null, null);
	}
	
	
	public void registerComponent(Controller controllerToRegister) {
		components.add(controllerToRegister);
	}
	
	
	private void callConstructors() {
		for (Controller c:components)
			if (c.implementation != null)
				c.implementation.constructor();
	}
	
	
	private void loadImplementations() {
		for (Controller c:components)
			c.instantiateSubclass();
	}
	
	
	private void initializeComponents() {
		for (Controller c:components)
			c.initialize();
	}
	
	
	private void beginMixing() {
		for (Controller c:components)
			c.begin(); 
	}
	
	
    /**
     * Main method
     * @param args the command line arguments (see -h for documentation)
     */
    public static void main(String[] args) {
        try {
            _opt = new Options();
            _opt.addOption("h", "help", false, "prints this message");
            _opt.addOption("s", "libsodiumwrap", true, "path to libsodiumWrap library (default: in ./lib/)");
            _opt.addOption("a", "nameserver", true, "nameserver's IP address or hostname (default: 127.0.0.1)");
            _opt.addOption("p", "port", true, "nameserver's port (default: 53)");
            _opt.addOption("ba", "bindAddress", true, "IP address the EncDNS server will listen for EncDns requests (default: 127.0.0.1)");
            _opt.addOption("bp", "bindPort", true, "Port the EncDNS server will listen for EncDns requests (default: 53)");
            _opt.addOption("pk", "public-key", true, "path to public key (default: ./lib/encdns.pk)");
            _opt.addOption("sk", "secret-key", true, "path to secret key (default: ./lib/encdns.sk)");
            _opt.addOption("c", "cache", true, "shared secret cache size (default: 100)");
            _opt.addOption("t", "timeout", true, "timeout for queries to authoritative nameservers in ms (default: 5000)");
            _opt.addOption("mt", "max-threads", true, "maximum number of threads (default: 100)");
            _opt.addOption("d", "disable-encryption", false, "disable encryption");
           _opt.addOption("v", "verbose", false, "enable verbose output");
           _opt.addOption("dts", "displayThreadStatus", false, "display info about threads (idle, encrypting...)");
           _opt.addOption("dtsi", "displayThreadStatusInterval", true, "interval to display info about threads in ms (default: 1000)");
           _opt.addOption("dt", "displayThroughputStatistics", false, "display info about current throughput");
           _opt.addOption("ir", "useInternalResolver", false, "use a simple java resolver integrated with encDns instead of the local name server");
           _opt.addOption("rl", "resolveToLocalhost", false, "display info about current throughput");
           
           Option zOpt = new Option("z", "zonename", true, "zone name (REQUIRED)");
            zOpt.setRequired(true);
            _opt.addOption(zOpt);

            Options helpOpts = new Options();
            helpOpts.addOption("h", "help", false, "prints this message");
            
            CommandLineParser parser = new BasicParser();
            
            // We need to check for the help parameter first, as parsing all
            // simultaneously will throw an exception if a required option
            // is missing.
            CommandLine cmd = parser.parse(helpOpts, args, true);
            if (cmd.hasOption("help")) {
                HelpFormatter hf = new HelpFormatter();
                hf.printHelp("EncDNSServerProxy", _opt);
                return;
            }
            
            // Now we can parse all options
            cmd = parser.parse(_opt, args);

            // get parameter values passed by CLI
            String libStr = cmd.getOptionValue("libsodiumwrap");
            String nsStr = cmd.getOptionValue("nameserver");
            String portStr = cmd.getOptionValue("port");
            String baStr = cmd.getOptionValue("bindAddress");
            String bpStr = cmd.getOptionValue("bindPort");
            String zoneStr = cmd.getOptionValue("zonename");
            String cacheStr = cmd.getOptionValue("cache");
            String timeoutStr = cmd.getOptionValue("timeout");
            String threadsStr = cmd.getOptionValue("max-threads");
            String pkStr = cmd.getOptionValue("public-key");
            String skStr = cmd.getOptionValue("secret-key");
            String displayThreadStatusIntStr = cmd.getOptionValue("displayThreadStatusInterval");
            
            // set standard values if corresponding CLI parameters are not set
            if(libStr == null) {
                if(EncDNSHelper.osIsWindows()) {
                	libStr = System.getProperty("user.dir") + "\\lib\\libsodiumWrap.dll";
                } else { // assume a Unix-like OS
                    libStr = System.getProperty("user.dir") + "/lib/libsodiumWrap.so";
                }
            }
            
            if(nsStr == null) nsStr = "127.0.0.1";
            int port = 53;
            if (portStr != null) {
                port = Integer.parseInt(portStr);
            }
            if(baStr == null) baStr = "127.0.0.1";
            int baPort = 53;
            if (bpStr != null) {
            	baPort = Integer.parseInt(bpStr);
            }
            int cache = 100;
            if(cacheStr != null) {
                cache = Integer.parseInt(cacheStr);
            }
            int timeout = 5000;
            if(timeoutStr != null) {
                timeout = Integer.parseInt(timeoutStr);
            }
            int maxThreads = 100;
            if(threadsStr != null) {
                maxThreads = Integer.parseInt(threadsStr);
            }
            boolean encryption = true;
            if(cmd.hasOption("disable-encryption")) {
            	encryption = false;
            }
            
            if(pkStr == null) {
                if(EncDNSHelper.osIsWindows()) {
                    pkStr = System.getProperty("user.dir")  + "\\lib\\encdns.pk";
                } else { // assume a Unix-like OS
                	pkStr = System.getProperty("user.dir") + "/lib/encdns.pk";
                }
            }
            
            if(skStr == null) {
                if(EncDNSHelper.osIsWindows()) {
                    skStr = System.getProperty("user.dir") + "\\lib\\encdns.sk";
                } else { // assume a Unix-like OS
                	skStr = System.getProperty("user.dir") + "/lib/encdns.sk";
                }
            }
            
            int verbosity;
            if(cmd.hasOption("verbose")) {
                verbosity = 1;
            } else {
                verbosity = 0;
            }
            
            boolean displayThreadStatusBool;
            if(cmd.hasOption("displayThreadStatus")) {
            	displayThreadStatusBool = true;
            } else {
            	displayThreadStatusBool = false;
            }
            
            int displayThreadStatusInt = 1000;
            if(displayThreadStatusIntStr != null) {
            	displayThreadStatusInt = Integer.parseInt(displayThreadStatusIntStr);
            }
            
            boolean displayThroughputStatisticsBool;
            if(cmd.hasOption("displayThroughputStatistics")) {
            	displayThroughputStatisticsBool = true;
            } else {
            	displayThroughputStatisticsBool = false;
            }

            boolean resolveToLocalhostBool;
            if(cmd.hasOption("resolveToLocalhost")) {
            	resolveToLocalhostBool = true;
            } else {
            	resolveToLocalhostBool = false;
            }
            
            boolean useInternalResolverBool;
            if(cmd.hasOption("useInternalResolver")) {
            	useInternalResolverBool = true;
            } else {
            	useInternalResolverBool = false;
            }
            
            // load the required C wrapper library
            if (encryption) {
            	System.load(libStr);
            }
            // resolve the remote recursive nameserver's address or IP
            InetAddress nsaddr = InetAddress.getByName(nsStr);
            
            InetAddress baaddr = InetAddress.getByName(baStr);
            
            EncDnsServer.bindAddress = baaddr;
            EncDnsServer.bindPort = baPort;
            EncDnsServer.zoneurl = zoneStr;
            EncDnsServer.nsaddr = nsaddr;
            EncDnsServer.port = port;
            EncDnsServer.cachesize = cache;
            EncDnsServer.timeout = timeout;
            EncDnsServer.maxThreads = maxThreads;
            EncDnsServer.pkPath = pkStr;
            EncDnsServer.skPath = skStr;
            EncDnsServer.encryption = encryption;
            EncDnsServer.verbosity = verbosity;
            EncDnsServer.displayThreadStatusBool = displayThreadStatusBool;
            EncDnsServer.displayThreadStatusInt = displayThreadStatusInt;
            EncDnsServer.displayThroughputBool = displayThroughputStatisticsBool;
            EncDnsServer.useInternalResolver = useInternalResolverBool;
            EncDnsServer.resolveToLocalhost = resolveToLocalhostBool;

            new EncDnsServer();
            
        } catch (MissingOptionException e) {
            // print an error and the help if a required option is missing
            System.out.println(e.getMessage());
            HelpFormatter hf = new HelpFormatter();
            hf.printHelp("EncDNSServerProxy", _opt);
        } catch (ParseException e) {
            System.err.println("Error when parsing command line options:");
            System.err.println(e);
        } catch (UnknownHostException e) {
            // print an error if the remote recursive nameserver's address 
            // cannot be resolved
            System.err.println("Could not resolve remote recursive nameserver's address");
        } catch (UnsatisfiedLinkError e) {
        	System.err.println("Could not access libsodium; was \"export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/home/user/gmix/lib/\" executed? is libsodiumWrap.so compatible to your system? error message: " +e.getMessage()); 
        }
    }
}
