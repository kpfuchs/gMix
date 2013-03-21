/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2012  Karl-Peter Fuchs
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
 */
package evaluation.localTest;

import evaluation.loadGenerator.LoadGenerator;
import framework.core.AnonNode;
import framework.core.config.Settings;
import framework.core.launcher.CommandLineParameters;
import framework.core.launcher.GMixTool;
import framework.core.launcher.ToolName;
import framework.infoService.EipEventListener;
import framework.infoService.InfoServiceClient;
import framework.infoService.InfoServiceServer;

import java.net.InetAddress;
import java.net.UnknownHostException;


public class LocalTest extends GMixTool implements EipEventListener {

	private InfoServiceClient infoService;
	private Settings settings;
	
	
	public LocalTest(CommandLineParameters params) {
		new InfoServiceServer(params);
		this.settings = params.generateSettingsObject();
		try {
			infoService = new InfoServiceClient(
					InetAddress.getByName("localhost"), 
					settings.getPropertyAsInt("GLOBAL_INFO_SERVICE_PORT"));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		System.out.println("local test - setting up mixes"); 
		startMixes();
		System.out.println("cascade is up"); 
		startClients();
	}

	
	@Override
	public void registrationPhaseFinished() {
		System.out.println("registration phase finished"); 
	}
	
	
	@Override
	public void initializationPhaseFinished() {
		System.out.println("initialization phase finished"); 
	}
	
	
	@Override
	public void beginPhaseFinished() {
		System.out.println("begin phase finished"); 
		startClients();
	}
	
	
	public void startMixes() {
		
		int numberOfMixes = infoService.getNumberOfMixes();
		MixStarter[] mixStarters = new MixStarter[numberOfMixes];
		// set up mix cascade
		for (int i=0; i<numberOfMixes; i++) {
			mixStarters[i] = new MixStarter();
			mixStarters[i].start();
		}
		for (int i=0; i<numberOfMixes; i++) {
			try {
				mixStarters[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				continue;
			}
		}
	}
	
	
	public void startClients() {
		System.out.println("all mixes up");
		System.out.println("starting clients");
		CommandLineParameters params = new CommandLineParameters(new String[0]);
		params.overwriteParameters = "LAYER_5_PLUG-IN_CLIENT=loadGeneratorPlugIn_v0_001";
		params.gMixTool = ToolName.LOCAL_TEST;
		new LoadGenerator(params);
	}

	
	class MixStarter extends Thread {
		
		public void run() {
			CommandLineParameters params = new CommandLineParameters(new String[0]);
			params.gMixTool = ToolName.MIX;
			params.overwriteParameters = "GLOBAL_INFO_SERVICE_ADDRESS=localhost,GLOBAL_LOCAL_MODE_ON=TRUE,LAYER_5_PLUG-IN_MIX=loadGeneratorPlugIn_v0_001";
			new AnonNode(params);
		}
		
	}
	
	
	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		CommandLineParameters params = new CommandLineParameters(args);
		params.gMixTool = ToolName.LOCAL_TEST;
		new LocalTest(params);
	}
	

}

