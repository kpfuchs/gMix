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

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocketFactory;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import staticContent.evaluation.testbed.deploy.utility.ConfigManager;
import staticContent.evaluation.testbed.deploy.utility.ConfigManager.Type;

public class EventNotifier {
	protected Logger logger = Logger.getLogger(this.getClass());
	private ConfigManager config = ConfigManager.getInstance(Type.TESTNODE);
	
	/**
	 * Sends an event notification to the configured coordinator instance.
	 * 
	 * @param event
	 */
	public void notifyEvent(String event) {
		(new EventNotifierThread(event)).start();
	}
	
	private class EventNotifierThread extends Thread {
		String event;

		public EventNotifierThread(String event) {
			this.event = event;
		}
		
		protected boolean sendEvent() throws UnknownHostException, IOException {
			String coordinatorAddress = System.getProperty("gMixTest.coordinatorAddress");
			int coordinatorPort = config.getInt("eventListenerPort");
			
			SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
			Socket s = ssf.createSocket(coordinatorAddress, coordinatorPort);
			
			s.setSoTimeout(60000);
			
			String eventContent = "event-notification="+event+"\n";
			
			s.getOutputStream().write(eventContent.getBytes(), 0, eventContent.getBytes().length);
			
			logger.debug("Sent event '"+event+"' to coordinator.");
			
			s.close();
			
			return true;			
		}

		@Override
		public void run() {
			while(true) {
				try {
					if (sendEvent()) break;
				} catch (IOException e) {
					// TODO add sleep
					e.printStackTrace();
					logger.error("Sending an event notification failed. Try again ...", e);
				}			
			}
		}		
	}

	public static void main(String[] args) {		
		// Set log4j configuration file path
		PropertyConfigurator.configure(System.getProperty("user.dir") +"/config/log4j.properties");
		
		ConfigManager config = ConfigManager.getInstance(Type.COORDINATOR);		
		
		System.setProperty("javax.net.ssl.keyStore", config.getAbsoluteFilePath(System.getProperty("user.dir") +config.getString("testnodeKeystorePath")));
        System.setProperty("javax.net.ssl.keyStorePassword", config.getString("testnodeKeystorePassword"));
        System.setProperty("javax.net.ssl.trustStore", config.getAbsoluteFilePath(System.getProperty("user.dir") +config.getString("testnodeTruststorePath")));
        System.setProperty("javax.net.ssl.trustStorePassword", config.getString("testnodeTruststorePassword"));
        
        System.setProperty("gMixTest.coordinatorAddress", "127.0.0.1");
        EventNotifier en = new EventNotifier();
		
		en.notifyEvent("END_OF_TRACEFILE_REACHED");
	}

}
