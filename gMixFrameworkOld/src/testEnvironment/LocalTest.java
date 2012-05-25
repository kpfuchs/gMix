/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2011  Karl-Peter Fuchs
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

package testEnvironment;

import java.net.InetAddress;
import java.net.UnknownHostException;

import infoService.EipEventListener;
import infoService.InfoServiceClient_v1;
import infoService.InfoServiceServer_v1;
import framework.Mix;
import framework.Paths;
import framework.Settings;


public class LocalTest implements EipEventListener {

	public InfoServiceClient_v1 infoService;
	
	public LocalTest() {
		 new InfoServiceServer_v1();
		//InfoServiceClient infoService;// = new InfoServiceServer_v2(); // TODO: implementationspecific!
		
		try {infoService = new InfoServiceClient_v1(InetAddress.getByName("localhost"), 22002);} catch (UnknownHostException e) {e.printStackTrace();}
		System.out.println("local test - setting up mixes"); 
		startMixes();
		System.out.println("cascade is up"); 
		startClients();
		//BasicInfoServiceClient_v1 infoServiceClient = new BasicInfoServiceClient_v1(); // TODO: implementationspecific!
		//infoServiceClient.
	}
	
	// TODO: getFromAll wiedereinführen...



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
			Settings settings = new Settings(Paths.PATH_TO_GLOBAL_CONFIG);

			mixStarters[i] = new MixStarter(settings);
			mixStarters[i].start();
			try {
				Thread.sleep(3000);			/*settings.setProperty("BIND_ADDRESS", "localhost");
			settings.setProperty("PORT", ""+(port++));
			settings.setProperty("INFO_PORT", ""+(infoPort++));
			settings.setProperty("NEXT_MIX_ADDRESS", "localhost"); // TODO: in next und prev-mix con-handler integrieren.... einfach in initialize() von info-service abfragen
			settings.setProperty("NEXT_MIX_PORT", ""+(port));
			settings.setProperty("PREVIOUS_MIX_ADDRESS", "localhost");
			settings.setProperty("PREVIOUS_MIX_PORT", ""+(port-2));*/
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	public void startClients() {
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("all mixes up");
		System.out.println("starting clients"); 
		UserSimulator us = new UserSimulator();
		us.start();
		UserSimulator us2 = new UserSimulator();
		us2.start();
		
		/*
		// set up ClientSimulator(s)
		int targetNumberOfClients = ClientSimulator.getVariable("Z");
		int numberOfStartedClients = 0;
		
		while (numberOfStartedClients < targetNumberOfClients) {
			// "Start X clients every Y ms until Z clients are started!"	
			
			int x = ClientSimulator.getVariable("X");
			
			for (int i=0; i<x; i++) {
				
				new ClientSimulator();
				
			}
			
			numberOfStartedClients += x;
			
			try {
				
				Thread.sleep((long)ClientSimulator.getVariable("Y"));
				
			} catch (InterruptedException e) {
				
				LOGGER.severe(e.getMessage());
				continue;
				
			}
			
		}*/
	}

	
	class MixStarter extends Thread {
		
		private Settings settings;
		
		
		public MixStarter(Settings settings) {
			this.settings = settings;
		}
		
		
		public void run() {
			new Mix(settings);
		}
		
	}
	
	
	public static void main(String[] args) {
		new LocalTest(); 
	}

}
