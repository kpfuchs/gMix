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
package staticContent.evaluation.testbed.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.SSLServerSocketFactory;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import staticContent.evaluation.testbed.deploy.utility.ConfigManager;
import staticContent.evaluation.testbed.deploy.utility.ConfigManager.Type;

public class EventListener {
	protected Logger logger = Logger.getLogger(this.getClass());
	protected SingleExperiment experiment;
	protected ConfigManager config = ConfigManager.getInstance(Type.COORDINATOR);	
	protected boolean isStarted = false;	
	private static EventListener instance = null;
	
	/**
	 * Returns a singleton EventListener instance.
	 * 
	 * @param experiment
	 * @return EventListener
	 */
	public static EventListener getInstance(SingleExperiment experiment) {
		if (instance == null) {
			instance = new EventListener(experiment);
		}
		
		return instance;
	}
	
	private EventListener(SingleExperiment experiment) {
		this.experiment = experiment;
	}
	
	/**
	 * Registers a given Set of events.
	 * If one of the given events happens, the stop() method of the experiment is called.
	 * 
	 * @param events
	 */
	public void listenTo(Set<String> events) {
		if (!isStarted) {
			(new EventListenerServerThread(events)).start();
			isStarted = true;
			logger.debug("Event listener started.");
		}		
	}
	
	private class EventListenerServerThread extends Thread {
		protected Set<String> events;
		protected ServerSocket ss;
		
		public EventListenerServerThread (Set<String> events) {
			this.events = events;
		}
		
		protected void startServer() throws IOException {
			int receivePort = config.getInt("eventListenerPort");		
		    
		    if (ss == null) {
		    	SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			    ss = ssf.createServerSocket(receivePort);
		    }

		    while (true) {
		      Socket s = ss.accept();

		      (new EventListenerConnectionThread(events, s)).start();
		    }
		}

		@Override
		public void run() {
			while(true) {
				try {
					startServer();					
				} catch (IOException e) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
					}
					logger.error("EventListenerServerThread unexpectedly terminated. Start again ...", e);
				}
			}			
		}		
	}
	
	private class EventListenerConnectionThread extends Thread {
		protected Set<String> events;
		private Socket s;
		
		public EventListenerConnectionThread(Set<String> events, Socket s) {
			this.events = events;
			this.s = s;
		}

		@Override
		public void run() {
			try {
				s.setSoTimeout(1000);
				
				BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));				
				String line = in.readLine();

				in.close();
				
				if (!line.startsWith("event-notification=")) {
					return;
				}
				
				line = line.substring(19);
				
				logger.debug("Received event '"+line+"' from "+s.getInetAddress().getHostAddress()+".");
				
				if (events.contains(line)) {
					experiment.stop();
				}
			} 
			catch (SocketTimeoutException e) {} 
			catch (IOException e) {
				logger.error("Error during reading an event notification.", e);
			}
		}
		
	}
	
	public static void main(String[] args) {
		// Set log4j configuration file path
		PropertyConfigurator.configure(System.getProperty("user.dir") +"/config/log4j.properties");
		
		ConfigManager config = ConfigManager.getInstance(Type.COORDINATOR);

		System.setProperty("javax.net.ssl.keyStore", config.getAbsoluteFilePath(System.getProperty("user.dir") +config.getString("coordinatorKeystorePath")));
        System.setProperty("javax.net.ssl.keyStorePassword", config.getString("coordinatorKeystorePassword"));
        System.setProperty("javax.net.ssl.trustStore", config.getAbsoluteFilePath(System.getProperty("user.dir") + config.getString("coordinatorTruststorePath")));
        System.setProperty("javax.net.ssl.trustStorePassword", config.getString("coordinatorTruststorePassword"));	        
		
        SingleExperiment exp = new RealNetworkExperiment();
		
		EventListener el = new EventListener(exp);
		
		Set<String> events = new HashSet<String>();
		
		events.add("END_OF_TRACEFILE_REACHED");
		
		el.listenTo(events);
		
		while(true) {
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
