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
package evaluation.traceParser.statistics;

import java.util.Calendar;

import evaluation.traceParser.engine.Protocol;
import evaluation.traceParser.engine.dataStructure.Packet;
import evaluation.traceParser.engine.filter.PacketFilter;
import evaluation.traceParser.engine.filter.PacketFilterTester;
import framework.core.util.Util;


public class GeneralPacketStatistics implements PacketFilter {
		
	private Packet lastPacket;
	private long packetCounter = 0;
	private long[] protocolDistribution = new long[Protocol.values().length];
	private double bytesTransferredOnLayerOne = 0.0d;
	private double bytesTransferredOnLayerTwo = 0.0d;
	private double bytesTransferredOnLayerThree = 0.0d;
	private double bytesTransferredOnLayerFour = 0.0d;
	private Calendar timestampFirstPacket;
	private long start;
	
	//int synToWan = 0;
	//int syn = 0;
	@Override
	public Packet newRecord(Packet packet) {
		//if (start == 0)
		//	start = System.currentTimeMillis();
		packetCounter++;
		
		/*if (packetCounter < 10000000) { // TODO: remove
			if (packet.getTCPflags() == TCPflags.SYN && packet.getLayer3dstAddress().equalsIgnoreCase("80")) {
				syn++;
				if (packet.getLayer1dstAddress().equalsIgnoreCase("0015C7568000"))
					synToWan++;
			}
			//System.out.println(packet.getLayer4protocol() +packet.+", src: " +packet.getLayer1srcAddress() +", dst: " +packet.getLayer1dstAddress()); 
			
			
			//System.out.println(packet.getLayer1dstAddress()); 
			//System.out.println(packet);
			//System.out.println(); 
			//0015C7568000
			//0015C7568000
			//000A4240A800
		}
		if  (packetCounter == (10000000 -1)) {
			double percentage = ((double)synToWan / (double)syn) * 100d;
			System.out.println("percentage: " +percentage +"%"); 
		}*/
		if (packetCounter % 1000000 == 0)
			System.out.println("read " +packetCounter +" packets so far"); 
		protocolDistribution[packet.getHighestLevelProtocol().ordinal()]++;
		double layer1size = packet.getLayer1length();
		if (layer1size > 0.0d )
			bytesTransferredOnLayerOne += layer1size;
		double layer2size = packet.getLayer2length();
		if (layer2size > 0.0d )
			bytesTransferredOnLayerTwo += layer2size;
		double layer3size = packet.getLayer3length();
		if (layer3size > 0.0d )
			bytesTransferredOnLayerThree += layer3size;
		double layer4size = packet.getLayer4length();
		if (layer4size > 0.0d )
			bytesTransferredOnLayerFour += layer4size;
		if (timestampFirstPacket == null)
			timestampFirstPacket = packet.getTimestamp();
		lastPacket = packet;
		return packet;
	}

	
	@Override
	public void finished() {
		double totalSeconds = (double)(lastPacket.getTimestamp().getTime().getTime() - timestampFirstPacket.getTime().getTime()) / 1000d;
		System.out.println("finished reading trace file\n"); 
		System.out.println("\nSTATISTICS: "); 
		System.out.println("file was recorded between " +timestampFirstPacket.getTime().toString() +" and " +lastPacket.getTimestamp().getTime().toString()); 
		System.out.println("total packets: " +packetCounter);
		System.out.println("packets/sec: " +((double)packetCounter)/totalSeconds);
		System.out.println("avg packet size (layer 1): " +Util.humanReadableByteCount((long)((double)bytesTransferredOnLayerOne/(double)packetCounter), false)); 
		System.out.println("\nbytes transferred on layer 1: " +Util.humanReadableByteCount((long)bytesTransferredOnLayerOne, false) +" (= " +Util.humanReadableByteCount((long)(((double)bytesTransferredOnLayerOne)/totalSeconds), false) +"/sec)");
		System.out.println("bytes transferred on layer 2: " +Util.humanReadableByteCount((long)bytesTransferredOnLayerTwo, false) +" (= " +Util.humanReadableByteCount((long)(((double)bytesTransferredOnLayerTwo)/totalSeconds), false) +"/sec)");
		System.out.println("bytes transferred on layer 3: " +Util.humanReadableByteCount((long)bytesTransferredOnLayerThree, false) +" (= " +Util.humanReadableByteCount((long)(((double)bytesTransferredOnLayerThree)/totalSeconds), false) +"/sec)");
		System.out.println("bytes transferred on layer 4: " +Util.humanReadableByteCount((long)bytesTransferredOnLayerFour, false) +" (= " +Util.humanReadableByteCount((long)(((double)bytesTransferredOnLayerFour)/totalSeconds), false) +"/sec)");
		System.out.println("\nprotocol distribution:");
		for (int i=0; i<protocolDistribution.length; i++)
			if (protocolDistribution[i] != 0)
				System.out.println(protocolDistribution[i] +"x\t" +Protocol.getProtocol(i)); 
		System.out.println("it took " +((float)(System.currentTimeMillis() - start)/1000f) + " seconds to create these statistics"); 
	}


	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		//new PacketFilterTester(new GeneralPacketStatistics(), PacketFilterTester.ERF_TEST_FILE_LONG);
		///inputOutput/global/traces/erfTests/auckland10sample
		new PacketFilterTester(new GeneralPacketStatistics(), "./inputOutput/global/traces/erfTests/auckland10sample/");
		//new PacketFilterTester(new GeneralPacketStatistics(), "/Volumes/Traces/auck10/");
		// "/Volumes/Traces/auck10/", null);
	}
	
}
