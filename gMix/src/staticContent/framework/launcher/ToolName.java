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
package staticContent.framework.launcher;

import staticContent.evaluation.encDnsPingTool.DNSPing;
import staticContent.evaluation.loadGenerator.LoadGenerator;
import staticContent.evaluation.localTest.LocalTestLoadGen;
import staticContent.evaluation.localTest.LocalTestNormal;
import staticContent.evaluation.simulator.Simulator;
import staticContent.evaluation.testbed.core.ExperimentSeries;
import staticContent.evaluation.testbed.deploy.discovery.DiscoveryNode;
import staticContent.evaluation.testbed.deploy.registry.DiscoveryRegistry;
import staticContent.framework.AnonNode;
import staticContent.framework.EncDnsClient;
import staticContent.framework.EncDnsServer;
import staticContent.framework.infoService.InfoServiceServer;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer2recodingScheme.encDNS_v0_001.EncDNSKeyGenerator;


public enum ToolName {
	
	NOT_SET (
		new String[] {},
		null, // max length: 15 characters
		null, 
		null
		),                                                     
    
	LOCAL_TEST_SOCKS_NO_DELAY (     
    	new String[] {"socksDemo1"},
    	"starts a SOCKS anonymity service on localhost        (3 Mixes, output strategy: none)",
    	new OnExecute() {public void execute(CommandLineParameters parameters) {
    		new LocalTestNormal(new CommandLineParameters(parameters.origArgs, ToolName.LOCAL_TEST_SOCKS_NO_DELAY, "localSocksTest-NoDelay.txt"));
    	}}),
    	
    LOCAL_TEST_SOCKS_BATCHWTO (
    	new String[] {"socksDemo2"},
    	"starts a SOCKS anonymity service on localhost        (3 Mixes, output strategy: batch with timeout)",
    	new OnExecute() {public void execute(CommandLineParameters parameters) {
    		new LocalTestNormal(new CommandLineParameters(parameters.origArgs, ToolName.LOCAL_TEST_SOCKS_BATCHWTO, "localSocksTest-BatchWithTimeout.txt"));
    	}}),
    	
    LOCAL_TEST_SOCKS_COTTRELLRD (
    	new String[] {"socksDemo3"},
    	"starts a SOCKS anonymity service on localhost        (3 Mixes, output strategy: cottrell random delay)",
    	new OnExecute() {public void execute(CommandLineParameters parameters) {
    		new LocalTestNormal(new CommandLineParameters(parameters.origArgs, ToolName.LOCAL_TEST_SOCKS_COTTRELLRD, "localSocksTest-CottrellRandomDelay.txt"));
    	}}),
    	
    LOCAL_TEST_SOCKS_SGMIX (
    	new String[] {"socksDemo4"},
    	"starts a SOCKS anonymity service on localhost        (3 Mixes, output strategy: stop-and-go)", 
    	new OnExecute() {public void execute(CommandLineParameters parameters) {
    		new LocalTestNormal(new CommandLineParameters(parameters.origArgs, ToolName.LOCAL_TEST_SOCKS_SGMIX, "localSocksTest-StopAndGo.txt"));
    	}}),
    
    LOCAL_TEST_SOCKS_TOTBATCH (
    	new String[] {"socksDemo5"},
    	"starts a SOCKS anonymity service on localhost        (3 Mixes, output strategy: threshold or timed batch)", 
    	new OnExecute() {public void execute(CommandLineParameters parameters) {
    		new LocalTestNormal(new CommandLineParameters(parameters.origArgs, ToolName.LOCAL_TEST_SOCKS_TOTBATCH, "localSocksTest-ThresholdOrTimedBatch.txt"));
    	}}),
    
    LOCAL_TEST_SOCKS_TBATCH (
    	new String[] {"socksDemo6"},
    	"starts a SOCKS anonymity service on localhost        (3 Mixes, output strategy: timed batch)", 
    	new OnExecute() {public void execute(CommandLineParameters parameters) {
    		new LocalTestNormal(new CommandLineParameters(parameters.origArgs, ToolName.LOCAL_TEST_SOCKS_TBATCH, "localSocksTest-TimedBatch.txt"));
    	}}),
    
    LOCAL_TEST_DNS_NO_DELAY (
    	new String[] {"socksDemo7"},
    	"starts a DNS anonymity service on localhost          (3 Mixes, output strategy: none)", 
    	new OnExecute() {public void execute(CommandLineParameters parameters) {
    		new LocalTestNormal(new CommandLineParameters(parameters.origArgs, ToolName.LOCAL_TEST_DNS_NO_DELAY, "localDNSTest-NoDelay.txt"));
    	}}),
    
    LOCAL_TEST_ENC_DNS (
    	new String[] {"encDnsDemo"},
    	"starts a EncDNS anonymity service on localhost", 
    	new OnExecute() {public void execute(CommandLineParameters parameters) {
    		new Thread(
    				new Runnable() {
    					public void run() {
    						EncDnsServer.main(new String[] {"-ba", "127.0.0.1", "-bp", "22322", "-rl", "-v", "-d", "-z", "encdns.test"});
    					}
    				}
    			).start(); 
    		EncDnsClient.main(new String[] {"-la", "127.0.0.1", "-lp", "22322", "-ra", "encdns.test", "-d", /*"-bp", "22321"*/}); 
    	}}),
    
    SIMULATOR_DEMO (
    	new String[] {"simulatorDemo"},
    	"starts a demo experiment with the gMix discrete eventsimulator",
    	new OnExecute() {public void execute(CommandLineParameters parameters) {
    		parameters.passthroughParameters = new String[]{"example_plotType_lineChart.cfg"};
    		new Simulator(parameters);
    	}}),
    	
    LOADGEN_DEMO (
    	new String[] {"loadgenDemo"},
    	"starts a demo experiment with the gMix load generator on localhost", 
    	new OnExecute() {public void execute(CommandLineParameters parameters) {
    		new LocalTestLoadGen(new CommandLineParameters(parameters.origArgs, ToolName.LOADGEN_DEMO, "localLoadgenTest.txt"));
    	}}),
    	
    INFO_SERVICE (
    	new String[] {"infoService"},
    	"starts the information service", "starts the information service (used by clients and mixes for discovery and exchanging public data like ip addresses or public keys)",
    	new OnExecute() {public void execute(CommandLineParameters parameters) {
    		new InfoServiceServer(parameters);
    	}}),
    	
    MIX (
    	new String[] {"mix"},
    	"starts a gMix mix",
    	new OnExecute() {public void execute(CommandLineParameters parameters) {
    		new AnonNode(parameters);
    	}}),
    	
    CLIENT (
    	new String[] {"client"},
    	"starts a gMix client",
        new OnExecute() {public void execute(CommandLineParameters parameters) {
        	new AnonNode(parameters);
        }}),
        
    P2P (
    	new String[] {"p2p", "mixAndClient"},
    	"starts both a gMix mix and client",
        new OnExecute() {public void execute(CommandLineParameters parameters) {
        	new AnonNode(parameters);
        }}),
        
    LOAD_GENERATOR (
    	new String[] {"loadGenerator"},
    	"starts the gMix load generator for distributed experiments",
    	"starts the load generator (can be used to start and simulate the behaviour of several clients for testing or performance evaluation)",
        new OnExecute() {public void execute(CommandLineParameters parameters) {
        	new LoadGenerator(parameters);
        }}),
        
    SIMULATOR (
    	new String[] {"simulator"},
    	"starts the gMix discrete event simulator (batch-mode)",
        new OnExecute() {public void execute(CommandLineParameters parameters) {
        	new Simulator(parameters);
        }}),
       
    /*SIMULATOR_GUI (
    	new String[] {"simulatorGui"},
    	"starts the gMix discrete event simulator (GUI-mode)",
    	new OnExecute() {public void execute(CommandLineParameters parameters) {
    		GuiLauncher.main(parameters.passthroughParameters);
    	}}),*/
                
    ENC_DNS_ENC1_US_TO (
    	new String[] {"encDnsEnc1UsTo"},
    	"starts an EncDNS Client that will use the online testanonymity service \"enc1.us.to\"",
        new OnExecute() {public void execute(CommandLineParameters parameters) {
        	String[] pars = {"-la", "8.8.8.8"/*134.100.9.61"*/,  "-ra", "enc1.us.to", "-rk" , "./lib/enc1usto.pk"};
        	parameters = new CommandLineParameters(pars);
        	EncDnsClient.commandLineParameters = parameters;
			EncDnsClient.main(pars);
        }}), 
        
    ENC_DNS_CLIENT (
    	new String[] {"encDnsClient"},
    	"starts an EncDNS Client",
        new OnExecute() {public void execute(CommandLineParameters parameters) {
        	EncDnsClient.commandLineParameters = parameters;
			EncDnsClient.main(parameters.passthroughParameters);
        }}),
        
    ENC_DNS_SERVER (
    	new String[] {"encDnsServer"},
    	"starts an EncDNS Server (Mix)",
        new OnExecute() {public void execute(CommandLineParameters parameters) {
        	EncDnsServer.commandLineParameters = parameters;
			EncDnsServer.main(parameters.passthroughParameters);
        }}),
        
    ENC_DNS_KEYGEN (
    	new String[] {"encDnsKeyGen"},
    	"generates a keypair for an EncDNS Server (Mix)",
        new OnExecute() {public void execute(CommandLineParameters parameters) {
        	EncDNSKeyGenerator.main(parameters.passthroughParameters);
        }}),
        
    ENC_DNS_PING_TOOL (
    	new String[] {"encDnsPingTool"},
    	"used for measuring EncDNS RTTs",
        new OnExecute() {public void execute(CommandLineParameters parameters) {
        	DNSPing.main(parameters.passthroughParameters);
        }}),
        
    TESTBED_REGISTRY (
    	new String[] {"testbedRegistry"},
    	"used for discovery when carrying out automated distributed experiments",
        new OnExecute() {public void execute(CommandLineParameters parameters) {
        	DiscoveryRegistry.main(parameters.passthroughParameters);
        }}),
	
    TESTBED_NODE (
        new String[] {"testbedNode"},
        "remote controlled stub node used for automated distributed experiments; will act e.g. as mix/client etc.",
        new OnExecute() {public void execute(CommandLineParameters parameters) {
        	DiscoveryNode.main(parameters.passthroughParameters);
        }}),
	
	TESTBED_EXPERIMENT (
	    new String[] {"testbedExperim"},
	    "will execute a specified experiment in a testbed (please provide name of experimentScript as command line parameter)",
	    new OnExecute() {public void execute(CommandLineParameters parameters) {
	    	ExperimentSeries.main(parameters.passthroughParameters);
	    }});
	
	
	public String[] identifiers;
	public String descriptionShort;
	public String descriptionLong;
	public String numericIdentifier;
	public OnExecute onExecute;
	

	private ToolName(String[] identifiers, String descriptionShort, OnExecute onExecute) {
		init(identifiers, descriptionShort, descriptionShort, onExecute);
	}
	
	
	private ToolName(String[] identifiers, String descriptionShort, String descriptionLong, OnExecute onExecute) {
		init(identifiers, descriptionShort, descriptionLong, onExecute);
	}
	
	
	private void init(String[] identifiers, String descriptionShort, String descriptionLong, OnExecute onExecute) {
		this.identifiers = identifiers;
		this.descriptionShort = descriptionShort;
		this.descriptionLong = descriptionLong;
		this.numericIdentifier = ""+Ctr.getValue();
		this.onExecute = onExecute;
	}
	
	
	public static void execute(CommandLineParameters parameters) {
		if (parameters.gMixTool == null || parameters.gMixTool == NOT_SET)
			throw new RuntimeException("cannot execute the tool, as \"gMixTool\" is not set!"); 
		parameters.gMixTool.onExecute.execute(parameters);
	}
	
	
	public static ToolName[] validValues() {
		ToolName[] all = ToolName.values();
		ToolName[] result = new ToolName[all.length - 1];
		for (int i=1; i<all.length; i++)
			result[i-1] = all[i];
		return result;
	}
	
	
	public static ToolName getToolByIdentifier(String identifier) {
		for (ToolName tool: ToolName.values()) {
			for (String ident:tool.identifiers)
				if (ident.equalsIgnoreCase(identifier) || ident.equalsIgnoreCase("-"+identifier))
					return tool;
			if (tool.numericIdentifier.equals(identifier))
				return tool;
		}
		return ToolName.NOT_SET;
	}
}
