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
import java.util.Vector;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.github.encdns.LibSodiumWrapper;

import staticContent.framework.config.Settings;
import staticContent.framework.controller.Controller;
import staticContent.framework.controller.Layer1NetworkClientController;
import staticContent.framework.controller.Layer2RecodingSchemeClientController;
import staticContent.framework.controller.Layer3OutputStrategyClientController;
import staticContent.framework.controller.Layer4TransportClientController;
import staticContent.framework.controller.Layer5ApplicationClientController;
import staticContent.framework.launcher.CommandLineParameters;
import staticContent.framework.util.Util;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer2recodingScheme.encDNS_v0_001.EncDNSHelper;


/**
 * This is the EncDNS client proxy (also known as local proxy). It should be
 * executed on the client computer or at least be connected to it via a
 * trustworthy connection. The client proxy will encrypt standard DNS requests
 * sent to it by a stub resolver and pass the encrypted request on to the local
 * recursive nameserver. It will also decrypt the encrypted response received 
 * from the local recursive nameserver and pass the decrypted standard DNS 
 * response on to the stub resolver.
 * 
 * This implementation currently supports UDP only.
 * 
 * Note: don't use anonnode.java as we dont need the info service etc (encDns 
 * is a single node system)
 */
public class EncDnsClient {

	private Settings settings;
	public static CommandLineParameters commandLineParameters;
	
	// references to controllers
	public Layer1NetworkClientController networkLayerClient;
	public Layer2RecodingSchemeClientController recodingLayerClient;
	public Layer3OutputStrategyClientController outputStrategyLayerClient;
	public Layer4TransportClientController transportLayerClient;
	public Layer5ApplicationClientController applicationLayerClient;
		
	private Vector<Controller> components = new Vector<Controller>(); // instantiated controllers
	
    /** maximum message size */
    public static final int MAX_MSG_SIZE = 65535;
    /** query timeout */
    public static final int TIMEOUT = 5000;
    /** maximum number of threads */
    public static int threads = 100;
    
    public static int bindPort;
    public static InetAddress bindAddr;
    public static int verbosity;
    public static boolean encryption;
    public static boolean isLoadgen;
    public static byte[] remoteNS;
    public static InetAddress localNS;
    public static int localNSPort;
    public static String rrnskeyPath;
    
    private static EncDnsClient encDnsClient;
    
    public static LibSodiumWrapper libSodium;
   
    public static SecureRandom rand = new SecureRandom();
	
    public static byte[] pk;
    //private byte[] _sk;
    private byte[] _rPK;
    public static byte[] k;
    public static byte[] magicString; 
    
    private static Options _opt;

    
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
    public EncDnsClient() {
    	//System.err.println("warning: listening port for encDns client is not 53 (but 22322); debug..."); 
    	if (EncDnsClient.encDnsClient != null)
    		throw new RuntimeException("this is a sigletone");
    	EncDnsClient.encDnsClient = this;
        if (encryption) {
        	EncDnsClient.libSodium = new LibSodiumWrapper();
        	// Generate new local key pair. We do not need to save this persistently
        	// as the public key will always be sent along with a query and using
        	// the same key for multiple sessions would make these linkable.
        	EncDnsClient.pk = new byte[EncDnsClient.libSodium.PKBYTES];
        	byte[] sk = new byte[EncDnsClient.libSodium.SKBYTES];
        	EncDnsClient.libSodium.jni_generateKeys(EncDnsClient.pk, sk);
        
        	// Load remote recursive nameserver's PK from file
        	_rPK = EncDNSHelper.readByteArrayFromFile(rrnskeyPath);
        	if(_rPK == null) {
        		System.err.println("ERROR: Could not load remote recursive "
        				+ "nameserver's public key. Exiting.");
            	return;
        	} else if (Util.toHex(_rPK).equals("CF3CB86BEA08A9D7D7B09443418DD9CB6DB047395BB662241CA038FF997BC174")) {
        		System.err.println("WARNING: you are using a publicly known test key; never use this key to transmit sensitive data!"); 
        	}
        	
        	long ktime = System.nanoTime();
        	EncDnsClient.k = EncDnsClient.libSodium.cryptoBoxBeforenm(_rPK, sk);
        	if(EncDnsClient.verbosity >= 1)
        		System.out.println("key generation time (ns) = " + (System.nanoTime()-ktime));
        }
        EncDnsClient.magicString = new byte[]{0x20, 0x45, 0x5e};

        if (isLoadgen) {
        	if (commandLineParameters == null) {
        		settings = CommandLineParameters.loadPluginSettings("./inputOutput/anonNode/encDnsLoadGen.txt");
        	} else {
        		settings = CommandLineParameters.loadPluginSettings("./inputOutput/anonNode/encDnsLoadGen.txt", commandLineParameters.overwriteParameters);
        		// settigns in Paths.PATH_TO_PATH_CONFIG_FILE have higher priority than normal plug-in settings -> overwrite (again)
                settings.addProperties("./inputOutput/anonNode/encDnsLoadGen.txt");
                if (commandLineParameters.overwriteParameters != null) // overwriteParameters have higher priority than all other settings -> overwrite (again)
         			Settings.overwriteSettings(settings.getPropertiesObject(), commandLineParameters.overwriteParameters, true);
        	}
        } else {
        	if (commandLineParameters == null) {
        		settings = CommandLineParameters.loadPluginSettings("./inputOutput/anonNode/encDns.txt");
        	} else {
        		settings = CommandLineParameters.loadPluginSettings("./inputOutput/anonNode/encDns.txt", commandLineParameters.overwriteParameters);
        		// settigns in Paths.PATH_TO_PATH_CONFIG_FILE have higher priority than normal plug-in settings -> overwrite (again)
                settings.addProperties("./inputOutput/anonNode/encDns.txt");
                if (commandLineParameters.overwriteParameters != null) // overwriteParameters have higher priority than all other settings -> overwrite (again)
         			Settings.overwriteSettings(settings.getPropertiesObject(), commandLineParameters.overwriteParameters, true);
        	}
        }
        
        generateComponents();
		setReferencesBetweenComponents();
		loadImplementations(); // loads the plug-ins
		callConstructors();
		initializeComponents();
		beginMixing();
    }
    
    
    public static EncDnsClient getInstance() {
    	if (EncDnsClient.encDnsClient == null)
    		throw new RuntimeException("please use main method in class EncDnsClient.java to start an EncDNS-Client");
    	return encDnsClient;
    }
    
    
	private void generateComponents() {
		// NOTE: the order of the items is important (do not change it)
		//this.outputStrategyLayerClient = new Layer3OutputStrategyClientController(null, settings, null, null, null);
		//this.outputStrategyLayerClient.loadClientPluginInstance();
		//this.components.add(outputStrategyLayerClient);
		this.recodingLayerClient = new Layer2RecodingSchemeClientController(null, settings, null, null, null);
		this.recodingLayerClient.loadClientPluginInstance();
		this.components.add(recodingLayerClient);
		this.networkLayerClient = new Layer1NetworkClientController(null, settings, null, null, null);
		this.networkLayerClient.loadClientPluginInstance();
		this.components.add(networkLayerClient);
		//this.transportLayerClient = new Layer4TransportClientController(null, settings, null, null, null);
		//this.transportLayerClient.loadClientPluginInstance();
		//this.components.add(transportLayerClient);
		this.applicationLayerClient = new Layer5ApplicationClientController(null, settings, null, null, null);
		this.components.add(applicationLayerClient);
	}
	
	
	private void setReferencesBetweenComponents() {
		for (Controller c:components)
			c.setComponentReferences(null, networkLayerClient, null, recodingLayerClient, null, outputStrategyLayerClient, null, transportLayerClient, null, applicationLayerClient, null, null, null, null);
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
     * @param args the command line arguments (see -h for documentation)
     */
    public static void main(String[] args) {
        try {
            _opt = new Options();
            _opt.addOption("h", "help", false, "prints this message");
            _opt.addOption("d", "disable-encryption", false, "disable encryption");
            _opt.addOption("s", "libsodiumwrap", true, "path to libsodiumWrap library (default: in ./lib/)");
            _opt.addOption("rk", "rrns-key", true, "path to remote recursive nameserver's public key (default: ./lib/encdns.pk)");
            _opt.addOption("v", "verbose", false, "enable verbose output");
            _opt.addOption("l", "loadgen", false, "start this client as load generator");
            _opt.addOption("mt", "threads", true, "number of threads (default: 100)");
            _opt.addOption("ba", "bindAddress", true, "IP address the EncDNS clinet will listen for DNS requests (default: 127.0.0.1)");
            _opt.addOption("bp", "bindPort", true, "Port the EncDNS clinet will listen for DNS requests (default: 53)");
            
            Option laOpt = new Option("la", "localns", true, "local nameserver's IP address or hostname (REQUIRED)");
            laOpt.setRequired(true);
            _opt.addOption(laOpt);
            
            _opt.addOption("lp", "localnsport", true, "local nameserver's port (default: 53)");
            
            Option raOpt = new Option("ra", "remotens", true, "remote nameserver's zone name (REQUIRED)");
            raOpt.setRequired(true);
            _opt.addOption(raOpt);
            
            Options helpOpts = new Options();
            helpOpts.addOption("h", "help", false, "prints this message");
            
            CommandLineParser parser = new BasicParser();
            
            // We need to check for the help parameter first, as parsing all
            // simultaneously will throw an exception if a required option
            // is missing.
            CommandLine cmd = parser.parse(helpOpts, args, true);
            if (cmd.hasOption("help")) {
                HelpFormatter hf = new HelpFormatter();
                hf.printHelp("EncDNSClientProxy", _opt);
                return;
            }
            
            // Now we can parse all options
            cmd = parser.parse(_opt, args);

            // get parameter values passed by CLI
            String libStr = cmd.getOptionValue("libsodiumwrap");
            String localnsStr = cmd.getOptionValue("localns");
            String localnsportStr = cmd.getOptionValue("localnsport");
            String remotensStr = cmd.getOptionValue("remotens");
            String rrnskeyStr = cmd.getOptionValue("rrns-key");
            String threadsStr = cmd.getOptionValue("threads");
            String baStr = cmd.getOptionValue("bindAddress");
            String bpStr = cmd.getOptionValue("bindPort");
            
            // set standard values if corresponding CLI parameters are not set
            if(libStr == null) {
                if(EncDNSHelper.osIsWindows()) {
                    libStr = System.getProperty("user.dir") + "\\lib\\libsodiumWrap.dll";
                } else { // assume a Unix-like OS
                    libStr = System.getProperty("user.dir") + "/lib/libsodiumWrap.so";
                }
            }
            if(localnsStr == null) localnsStr = "127.0.0.1";
            int localnsport = 53;
            if(localnsportStr != null) {
                localnsport = Integer.parseInt(localnsportStr);
            }
            
            if(baStr == null) baStr = "127.0.0.1";
            int bPort = 53;
            if (bpStr != null) {
            	bPort = Integer.parseInt(bpStr);
            }
            
            boolean encryption = true;
            if(cmd.hasOption("disable-encryption")) {
            	encryption = false;
            }
            
            boolean isLoadGen = false;
            if(cmd.hasOption("loadgen")) {
            	isLoadGen = true;
            }
            
            int verbosity;
            if(cmd.hasOption("verbose")) {
                verbosity = 1;
            } else {
                verbosity = 0;
            }
            
            if(rrnskeyStr == null) {
                if(EncDNSHelper.osIsWindows()) {
                    rrnskeyStr = System.getProperty("user.dir") + "\\lib\\encdns.pk";
                } else { // assume Unix-like OS
                    rrnskeyStr = System.getProperty("user.dir") + "/lib/encdns.pk";
                }
            }
            

            int threads = 100;
            if(threadsStr != null) {
                threads = Integer.parseInt(threadsStr);
            }

            // load the required C wrapper library
            if (encryption)
            	System.load(libStr);
            
            // resolve the local recursive nameserver's address or IP
            InetAddress localns = InetAddress.getByName(localnsStr);
            
            InetAddress bpaddr = InetAddress.getByName(baStr);
            
            EncDnsClient.bindAddr = bpaddr;
            EncDnsClient.bindPort = bPort;
            EncDnsClient.remoteNS = EncDNSHelper.parseZoneNameString(remotensStr);
            EncDnsClient.localNS = localns;
            EncDnsClient.localNSPort = localnsport;
            EncDnsClient.rrnskeyPath = rrnskeyStr;
            EncDnsClient.encryption = encryption;
            EncDnsClient.isLoadgen = isLoadGen;
            EncDnsClient.verbosity = verbosity;
            EncDnsClient.threads = threads;
            
            new EncDnsClient();
        
        } catch (MissingOptionException e) {
            // print and error and the help if a required option is missing
             System.out.println(e.getMessage());
             HelpFormatter hf = new HelpFormatter();
             hf.printHelp("EncDNSClientProxy", _opt);
        } catch (ParseException e) {
            System.err.println(e);
        } catch (UnknownHostException e) {
            // print an error if the local recursive nameserver's address cannot
            // be resolved
            System.err.println("local recursive nameserver's address could not be resolved");
        } catch (UnsatisfiedLinkError e) {
        	System.err.println("Could not access libsodium; was \"export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/home/user/gmix/lib/\" executed? is libsodiumWrap.so compatible to your system? error message: " +e.getMessage()); 
        }
    }
    
    
}
