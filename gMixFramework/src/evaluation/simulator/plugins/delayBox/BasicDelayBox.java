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
package evaluation.simulator.plugins.delayBox;

import evaluation.simulator.Simulator;
import evaluation.simulator.pluginRegistry.DelayBox;


public class BasicDelayBox extends DelayBoxImpl {
	
	
	private SimplexDelayBox sendDelayBox;
	private SimplexDelayBox receiveDelayBox;
	private boolean hasUnlimitedBandwidthSend;
	private boolean hasUnlimitedBandwidthReceive;
	
	
	// bandwidth in MBit/sec
	// latency in ms
	public BasicDelayBox(int bandwidthSend, int bandwidthReceive, int latency) {
		super();
		this.sendDelayBox = new SimplexDelayBox(bandwidthSend, latency);
		this.receiveDelayBox = new SimplexDelayBox(bandwidthReceive, latency);
		this.hasUnlimitedBandwidthSend = bandwidthSend == DelayBox.UNLIMITD_BANDWIDTH;
		this.hasUnlimitedBandwidthReceive = bandwidthReceive == DelayBox.UNLIMITD_BANDWIDTH;
	}
	

	@Override
	public int getReceiveDelay(int numberOfBytesToReceive) {
		if (hasUnlimitedBandwidthReceive) {
			return receiveDelayBox.latency;
		} else {
			int delay = (int)this.receiveDelayBox.getDelay(numberOfBytesToReceive);	
			//System.out.println("reveiceDelay: " +delay);
			return delay;
		}
	}


	@Override
	public int getSendDelay(int numberOfBytesToSend) {
		if (hasUnlimitedBandwidthSend) {
			return sendDelayBox.latency;
		} else {
			int delay = (int)this.sendDelayBox.getDelay(numberOfBytesToSend);	
			//System.out.println("reveiceDelay: " +delay);
			return delay;
		}
	}
	
	
	private class SimplexDelayBox {
		
		private final static int NOT_SCHEDULED = -100;
		private final static int NOT_SET = -101;
		
		private int latency; // in ms
		private int packetSize = new Integer(Simulator.settings.getProperty("NETWORK_PACKET_PAYLOAD_SIZE"));
		private int freeSpaceInPacket = packetSize;
		private double sendInterval; // abstand in dem pakete geschickt werden können
		private long timeOfOutputForNextNotFullPacket;
		
		private long lastPulse = NOT_SET;
		private boolean isBusy;
		
		
		public SimplexDelayBox(int bandwidth, int latency) {
			this.latency = latency;
			if (bandwidth != DelayBox.UNLIMITD_BANDWIDTH) {
				double packetsPerSecond = Math.round((double)bandwidth/(double)packetSize);
				this.sendInterval = (double)1000 / packetsPerSecond;
				if (sendInterval < 1) {
					sendInterval = 1;
					packetSize = (int) Math.round((double)bandwidth/1000d);
				} 
			}
		}

		
		public long getDelay(int numberOfBytesToSend) {
			
			int packetsTransmittedSinceLastCall;
			long transmitDuration; // when sending begins; see "delayTillTransmitBegins"
			long delayTillTransmitBegins;
			
			if (numberOfBytesToSend == 0)
				return 0;
			
			// Vergangene Zeit seit letztem Aufruf berücksichtigen
			packetsTransmittedSinceLastCall = getNumberOfPacketsTransmittedSinceLastCall();

			if (timeOfOutputForNextNotFullPacket < Simulator.getNow()) { // Leitung ist wieder frei
				timeOfOutputForNextNotFullPacket = NOT_SCHEDULED;
				freeSpaceInPacket = packetSize;
				lastPulse = NOT_SET;
				isBusy = false;
			} else if (packetsTransmittedSinceLastCall > 0) {
				lastPulse += Math.round((double)packetsTransmittedSinceLastCall * sendInterval);
			}

			if (!isBusy) { // Leitung frei -> neues Paket aufmachen
				
				if (numberOfBytesToSend < freeSpaceInPacket) { // kein neues Paket nötig (Daten passen in ein Paket und fällen dieses nicht komplett)
					
					transmitDuration = 0;
					delayTillTransmitBegins = Math.round(sendInterval);
					freeSpaceInPacket -= numberOfBytesToSend;
					timeOfOutputForNextNotFullPacket = Simulator.getNow() + delayTillTransmitBegins;
					lastPulse = Simulator.getNow();
					isBusy = true;
					
				} else { // neues Paket nötig					
					
					int packetsNeeded = (int)Math.floor((double)numberOfBytesToSend / (double)packetSize); // counts only full packets
					freeSpaceInPacket = packetSize - (numberOfBytesToSend % packetSize);
					delayTillTransmitBegins = Math.round(sendInterval);
					timeOfOutputForNextNotFullPacket = Simulator.getNow() + Math.round((double)packetsNeeded * sendInterval);
					if (freeSpaceInPacket == packetSize) // auf letztes Paket muss NICHT gewartet werden
						transmitDuration = Math.round((double)packetsNeeded * sendInterval);
					else // auf letztes Paket muss gewartet werden
						transmitDuration = Math.round((double)((double)packetsNeeded+(double)1) * sendInterval);
					
					lastPulse = Simulator.getNow();
					isBusy = true;
					
				}
				
			} else { // Leitung nicht frei

				delayTillTransmitBegins = timeOfOutputForNextNotFullPacket - Simulator.getNow();
				if (delayTillTransmitBegins < 0)
					throw new RuntimeException("ERROR: delayTillTransmitBegins < 0!");
				
				if (numberOfBytesToSend < freeSpaceInPacket) { // kein neues Paket nötig (Daten passen in ein Paket und fällen dieses nicht komplett)
					
					transmitDuration = 0;
					freeSpaceInPacket -= numberOfBytesToSend;
					
				} else { // neues Paket nötig	
					
					numberOfBytesToSend -= freeSpaceInPacket;
					int packetsNeeded = 1 + (int)Math.floor((double)numberOfBytesToSend / (double)packetSize); // counts only full packets ("1 +" weil aktuelles nicht volles Paket auch gez�hlt werden muss)
					freeSpaceInPacket = packetSize - (numberOfBytesToSend % packetSize);
					long timeTillNextPulse = Math.round((double)lastPulse + sendInterval) - Simulator.getNow();
					timeOfOutputForNextNotFullPacket += timeTillNextPulse + Math.round((double)packetsNeeded * sendInterval);
					
					if (freeSpaceInPacket == packetSize) // auf letztes Paket muss NICHT gewartet werden
						transmitDuration = timeTillNextPulse + Math.round((double)packetsNeeded * sendInterval);
					else // auf letztes Paket muss gewartet werden
						transmitDuration = Math.round((double)((double)packetsNeeded+(double)1) * sendInterval);

				}

			}
			
			return delayTillTransmitBegins + transmitDuration + latency;
			
		}
		
		
		private int getNumberOfPacketsTransmittedSinceLastCall() {
			
			int result;
			
			if (lastPulse == NOT_SET)
				result = 0;
			else
				result = (int) Math.floor((double)(Simulator.getNow() - lastPulse) / (double)sendInterval);

			if (result < 0) 
				throw new RuntimeException("ERROR: numberOfPacketsTransmittedSinceLastCall < 0!");
			
			return result;
			
		}
		
	}
	
	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	/*public static void main(String[] args) {
		
		Settings.initialize("properties.txt");
		Simulator s = new Simulator(null);
		BasicDelayBox sdb = new BasicDelayBox(s, 100, 100, 10);
		sdb.getSendDelay(100000000);
		s.setNow(10000);
		sdb.getSendDelay(100000000); 
		s.setNow(14000);
		sdb.getSendDelay(100000000); 
		s.setNow(100000);
		sdb.getSendDelay(100000000);
		sdb.getSendDelay(100000000);
		sdb.getSendDelay(100000000);
		sdb.getSendDelay(100000000);
		s.setNow(1000000);
		sdb.getSendDelay(100000000+100000000+100000000+100000000);
		s.setNow(1020000);
		sdb.getSendDelay(100000000+100000000+100000000+100000000);
		s.setNow(2000000);
		sdb.getSendDelay(1);
		sdb.getSendDelay(1);
		sdb.getSendDelay(1);
		sdb.getSendDelay(1);
		sdb.getSendDelay(1);
		sdb.getSendDelay(1);
		sdb.getSendDelay(1);
		sdb.getSendDelay(1);
		sdb.getSendDelay(1);
		sdb.getSendDelay(1);
		s.setNow(2000001);
		sdb.getSendDelay(1);
		sdb.getSendDelay(1);
		sdb.getSendDelay(1);
		sdb.getSendDelay(1);
		sdb.getSendDelay(1);
		s.setNow(2000002);
		sdb.getSendDelay(1);
		sdb.getSendDelay(1);
		sdb.getSendDelay(1);
		sdb.getSendDelay(1);
		sdb.getSendDelay(1);
	}*/

}
