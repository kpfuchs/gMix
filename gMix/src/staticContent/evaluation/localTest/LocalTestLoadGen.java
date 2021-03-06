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
package staticContent.evaluation.localTest;

import java.net.InetAddress;
import java.net.UnknownHostException;

import staticContent.evaluation.loadGenerator.LoadGenerator;
import staticContent.framework.AnonNode;
import staticContent.framework.config.Settings;
import staticContent.framework.infoService.EipEventListener;
import staticContent.framework.infoService.InfoServiceClient;
import staticContent.framework.infoService.InfoServiceServer;
import staticContent.framework.launcher.CommandLineParameters;
import staticContent.framework.launcher.GMixTool;
import staticContent.framework.launcher.ToolName;


public class LocalTestLoadGen extends GMixTool implements EipEventListener {

	private InfoServiceClient infoService;
	private Settings settings;
	private CommandLineParameters params;
	
	
	public LocalTestLoadGen(CommandLineParameters params) {
		this.params = params;
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
		CommandLineParameters parameters = new CommandLineParameters(new String[0], ToolName.LOAD_GENERATOR, params.configFile);
		new LoadGenerator(parameters);
	}

	
	class MixStarter extends Thread {
		
		public void run() {
			CommandLineParameters parameters = new CommandLineParameters(new String[0], ToolName.MIX, params.configFile);
			new AnonNode(parameters);
		}
		
	}
	
	
	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		CommandLineParameters params = new CommandLineParameters(args, ToolName.LOADGEN_DEMO, "localLoadgenTest.txt");
		params.gMixTool = ToolName.LOADGEN_DEMO;
		new LocalTestLoadGen(params);
	}
	

}

