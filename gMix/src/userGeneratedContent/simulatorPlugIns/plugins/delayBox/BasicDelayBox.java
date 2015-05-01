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
package userGeneratedContent.simulatorPlugIns.plugins.delayBox;

import staticContent.evaluation.simulator.Simulator;
import staticContent.evaluation.simulator.annotations.plugin.Plugin;
import staticContent.evaluation.simulator.annotations.property.IntSimulationProperty;
import userGeneratedContent.simulatorPlugIns.pluginRegistry.DelayBox;

@Plugin(pluginKey = "BASIC_DELAY_BOX", pluginName = "Basic delay" )
public class BasicDelayBox extends DelayBoxImpl {

	@IntSimulationProperty(	name = "Packet Size (byte)", 
			key = "NETWORK_PACKET_PAYLOAD_SIZE", 
			min = 0,
			max = 9000) // Jumboframe Ethernet frame size
	private int packetSize = new Integer(Simulator.settings.getProperty("NETWORK_PACKET_PAYLOAD_SIZE"));
	
	private class SimplexDelayBox {

		private final static int NOT_SCHEDULED = -100;
		private final static int NOT_SET = -101;

		private int freeSpaceInPacket = this.packetSize;
		private boolean isBusy;
		private long lastPulse = NOT_SET;
		private final int latency; // in ms
		
		private int packetSize;

		private double sendInterval; // abstand in dem pakete geschickt werden
										// können
		private long timeOfOutputForNextNotFullPacket;

		public SimplexDelayBox(int bandwidth, int latency, int packetSize) {
			this.packetSize = packetSize;
			this.latency = latency;
			if (bandwidth != DelayBox.UNLIMITD_BANDWIDTH) {
				double packetsPerSecond = Math.round((double) bandwidth
						/ (double) this.packetSize);
				this.sendInterval = 1000 / packetsPerSecond;
				if (this.sendInterval < 1) {
					this.sendInterval = 1;
					this.packetSize = (int) Math.round(bandwidth / 1000d);
				}
			}
		}

		public long getDelay(int numberOfBytesToSend) {

			int packetsTransmittedSinceLastCall;
			long transmitDuration; // when sending begins; see
									// "delayTillTransmitBegins"
			long delayTillTransmitBegins;

			if (numberOfBytesToSend == 0) {
				return 0;
			}

			// Vergangene Zeit seit letztem Aufruf berücksichtigen
			packetsTransmittedSinceLastCall = this
					.getNumberOfPacketsTransmittedSinceLastCall();

			if (this.timeOfOutputForNextNotFullPacket < Simulator.getNow()) { // Leitung
																				// ist
																				// wieder
																				// frei
				this.timeOfOutputForNextNotFullPacket = NOT_SCHEDULED;
				this.freeSpaceInPacket = this.packetSize;
				this.lastPulse = NOT_SET;
				this.isBusy = false;
			} else if (packetsTransmittedSinceLastCall > 0) {
				this.lastPulse += Math.round(packetsTransmittedSinceLastCall
						* this.sendInterval);
			}

			if (!this.isBusy) { // Leitung frei -> neues Paket aufmachen

				if (numberOfBytesToSend < this.freeSpaceInPacket) { // kein
																	// neues
																	// Paket
																	// nötig
																	// (Daten
																	// passen in
																	// ein Paket
																	// und
																	// fällen
																	// dieses
																	// nicht
																	// komplett)

					transmitDuration = 0;
					delayTillTransmitBegins = Math.round(this.sendInterval);
					this.freeSpaceInPacket -= numberOfBytesToSend;
					this.timeOfOutputForNextNotFullPacket = Simulator.getNow()
							+ delayTillTransmitBegins;
					this.lastPulse = Simulator.getNow();
					this.isBusy = true;

				} else { // neues Paket nötig

					int packetsNeeded = (int) Math
							.floor((double) numberOfBytesToSend
									/ (double) this.packetSize); // counts only
																	// full
																	// packets
					this.freeSpaceInPacket = this.packetSize
							- (numberOfBytesToSend % this.packetSize);
					delayTillTransmitBegins = Math.round(this.sendInterval);
					this.timeOfOutputForNextNotFullPacket = Simulator.getNow()
							+ Math.round(packetsNeeded * this.sendInterval);
					if (this.freeSpaceInPacket == this.packetSize) {
						transmitDuration = Math.round(packetsNeeded
								* this.sendInterval);
					} else {
						transmitDuration = Math
								.round(((double) packetsNeeded + (double) 1)
										* this.sendInterval);
					}

					this.lastPulse = Simulator.getNow();
					this.isBusy = true;

				}

			} else { // Leitung nicht frei

				delayTillTransmitBegins = this.timeOfOutputForNextNotFullPacket
						- Simulator.getNow();
				if (delayTillTransmitBegins < 0) {
					throw new RuntimeException(
							"ERROR: delayTillTransmitBegins < 0!");
				}

				if (numberOfBytesToSend < this.freeSpaceInPacket) { // kein
																	// neues
																	// Paket
																	// nötig
																	// (Daten
																	// passen in
																	// ein Paket
																	// und
																	// fällen
																	// dieses
																	// nicht
																	// komplett)

					transmitDuration = 0;
					this.freeSpaceInPacket -= numberOfBytesToSend;

				} else { // neues Paket nötig

					numberOfBytesToSend -= this.freeSpaceInPacket;
					int packetsNeeded = 1 + (int) Math
							.floor((double) numberOfBytesToSend
									/ (double) this.packetSize); // counts only
																	// full
																	// packets
																	// ("1 +"
																	// weil
																	// aktuelles
																	// nicht
																	// volles
																	// Paket
																	// auch
																	// gez�hlt
																	// werden
																	// muss)
					this.freeSpaceInPacket = this.packetSize
							- (numberOfBytesToSend % this.packetSize);
					long timeTillNextPulse = Math.round(this.lastPulse
							+ this.sendInterval)
							- Simulator.getNow();
					this.timeOfOutputForNextNotFullPacket += timeTillNextPulse
							+ Math.round(packetsNeeded * this.sendInterval);

					if (this.freeSpaceInPacket == this.packetSize) {
						transmitDuration = timeTillNextPulse
								+ Math.round(packetsNeeded * this.sendInterval);
					} else {
						transmitDuration = Math
								.round(((double) packetsNeeded + (double) 1)
										* this.sendInterval);
					}

				}

			}

			return delayTillTransmitBegins + transmitDuration + this.latency;

		}

		private int getNumberOfPacketsTransmittedSinceLastCall() {

			int result;

			if (this.lastPulse == NOT_SET) {
				result = 0;
			} else {
				result = (int) Math.floor((Simulator.getNow() - this.lastPulse)
						/ this.sendInterval);
			}

			if (result < 0) {
				throw new RuntimeException(
						"ERROR: numberOfPacketsTransmittedSinceLastCall < 0!");
			}

			return result;

		}

	}

	private final boolean hasUnlimitedBandwidthReceive;
	private final boolean hasUnlimitedBandwidthSend;
	private final SimplexDelayBox receiveDelayBox;

	private final SimplexDelayBox sendDelayBox;

	// bandwidth in MBit/sec
	// latency in ms
	public BasicDelayBox(int bandwidthSend, int bandwidthReceive, int latency) {
		super();
		this.sendDelayBox = new SimplexDelayBox(bandwidthSend, latency, packetSize);
		this.receiveDelayBox = new SimplexDelayBox(bandwidthReceive, latency, packetSize);
		this.hasUnlimitedBandwidthSend = bandwidthSend == DelayBox.UNLIMITD_BANDWIDTH;
		this.hasUnlimitedBandwidthReceive = bandwidthReceive == DelayBox.UNLIMITD_BANDWIDTH;
	}

	@Override
	public int getReceiveDelay(int numberOfBytesToReceive) {
		if (this.hasUnlimitedBandwidthReceive) {
			return this.receiveDelayBox.latency;
		} else {
			int delay = (int) this.receiveDelayBox
					.getDelay(numberOfBytesToReceive);
			// System.out.println("reveiceDelay: " +delay);
			return delay;
		}
	}

	@Override
	public int getSendDelay(int numberOfBytesToSend) {
		if (this.hasUnlimitedBandwidthSend) {
			return this.sendDelayBox.latency;
		} else {
			int delay = (int) this.sendDelayBox.getDelay(numberOfBytesToSend);
			// System.out.println("reveiceDelay: " +delay);
			return delay;
		}
	}

	/**
	 * Comment
	 * 
	 * @param args
	 *            Not used.
	 */
	/*
	 * public static void main(String[] args) {
	 * 
	 * Settings.initialize("properties.txt"); Simulator s = new Simulator(null);
	 * BasicDelayBox sdb = new BasicDelayBox(s, 100, 100, 10);
	 * sdb.getSendDelay(100000000); s.setNow(10000);
	 * sdb.getSendDelay(100000000); s.setNow(14000);
	 * sdb.getSendDelay(100000000); s.setNow(100000);
	 * sdb.getSendDelay(100000000); sdb.getSendDelay(100000000);
	 * sdb.getSendDelay(100000000); sdb.getSendDelay(100000000);
	 * s.setNow(1000000);
	 * sdb.getSendDelay(100000000+100000000+100000000+100000000);
	 * s.setNow(1020000);
	 * sdb.getSendDelay(100000000+100000000+100000000+100000000);
	 * s.setNow(2000000); sdb.getSendDelay(1); sdb.getSendDelay(1);
	 * sdb.getSendDelay(1); sdb.getSendDelay(1); sdb.getSendDelay(1);
	 * sdb.getSendDelay(1); sdb.getSendDelay(1); sdb.getSendDelay(1);
	 * sdb.getSendDelay(1); sdb.getSendDelay(1); s.setNow(2000001);
	 * sdb.getSendDelay(1); sdb.getSendDelay(1); sdb.getSendDelay(1);
	 * sdb.getSendDelay(1); sdb.getSendDelay(1); s.setNow(2000002);
	 * sdb.getSendDelay(1); sdb.getSendDelay(1); sdb.getSendDelay(1);
	 * sdb.getSendDelay(1); sdb.getSendDelay(1); }
	 */

}
