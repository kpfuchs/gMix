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
package framework.core.routing;

import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.Arrays;

import framework.core.util.Util;


public class MixList {
	
	public int numberOfMixes;
	public int[] mixIDs;
	public InetAddress[] addresses;
	public int[] ports;
	
	private static SecureRandom random = new SecureRandom();
	
	
	public MixList() {
		
	}
	
	
	public InetAddress getAddress(int mixID) {
		for (int i=0; i<mixIDs.length; i++)
			if (mixIDs[i] == mixID)
				return addresses[i];
		return null;
	}
	
	
	public int getPort(int mixID) {
		for (int i=0; i<mixIDs.length; i++)
			if (mixIDs[i] == mixID)
				return ports[i];
		return -1;
	}
	
	
	public int getMixID(InetAddress address) {
		for (int i=0; i<addresses.length; i++)
			if (addresses[i].equals(address))
				return mixIDs[i];
		return -1;
	}
	
	
	public int getMixID(InetAddress address, int port) {
		for (int i=0; i<addresses.length; i++)
			if (addresses[i].equals(address) && ports[i] == port)
				return mixIDs[i];
		return -1;
	}
	
	
	
	public MixList getRandomRoute() {
		int hops = random.nextInt(mixIDs.length-2) + 1; 
		return getRandomRoute(hops);
	}
	
	
	public MixList getRandomRoute(int numberOfMixes) {
		if (numberOfMixes > this.numberOfMixes)
			throw new RuntimeException("not enough mixes available for the route length specified (" +numberOfMixes +">" + this.numberOfMixes +")"); 
		MixList randomRoute = new MixList();
		randomRoute.numberOfMixes = numberOfMixes;
		randomRoute.mixIDs = new int[numberOfMixes];
		Arrays.fill(randomRoute.mixIDs, Util.NOT_SET);
		randomRoute.addresses = new InetAddress[numberOfMixes];
		randomRoute.ports = new int[numberOfMixes];
		for (int i=0; i<randomRoute.numberOfMixes; i++) {
			int pos;
			do { // get (unused) mix id at random
				pos = random.nextInt(this.numberOfMixes);
			} while (Util.contains(pos, randomRoute.mixIDs));
			randomRoute.mixIDs[i] = mixIDs[pos];
			randomRoute.addresses[i] = addresses[pos];
			randomRoute.ports[i] = ports[pos];
		}
		return randomRoute;
	}
	
	
	public MixList getRandomRoute(int numberOfMixes, int finalHopId) {
		if (numberOfMixes > this.numberOfMixes)
			throw new RuntimeException("not enough mixes available for the route length specified (" +numberOfMixes +">" + this.numberOfMixes +")"); 
		MixList randomRoute = new MixList();
		randomRoute.numberOfMixes = numberOfMixes;
		randomRoute.mixIDs = new int[numberOfMixes];
		Arrays.fill(randomRoute.mixIDs, Util.NOT_SET);
		randomRoute.addresses = new InetAddress[numberOfMixes];
		randomRoute.ports = new int[numberOfMixes];
		for (int i=0; i<randomRoute.numberOfMixes-1; i++) {
			int pos;
			do { // get (unused) mix id at random
				pos = random.nextInt(this.numberOfMixes);
			} while (Util.contains(pos, mixIDs));
			randomRoute.mixIDs[i] = randomRoute.mixIDs[pos];
			randomRoute.addresses[i] = addresses[pos];
			randomRoute.ports[i] = ports[pos];
		}
		int finalHopPos = Util.NOT_SET;
		for (int i=0; i<mixIDs.length; i++) {
			if (mixIDs[i] == finalHopId) {
				finalHopPos = i;
				break;
			}
		}
		assert finalHopPos != Util.NOT_SET;
		randomRoute.mixIDs[randomRoute.mixIDs.length-1] = mixIDs[finalHopPos];
		randomRoute.addresses[randomRoute.addresses.length-1] = addresses[finalHopPos];
		randomRoute.ports[randomRoute.ports.length-1] = ports[finalHopPos];
		return randomRoute;
	}
	
	
	public int getRandomMixId() {
		int pos = Util.getRandomInt(0, mixIDs.length-1, random);
		return mixIDs[pos];
	}
	
	
	public static byte[] packIdArray(int[] ids) {
		byte[][] chunks = new byte[ids.length][];
		for (int i=0; i<ids.length; i++)
			chunks[i] = Util.intToByteArray(ids[i]);
		return Util.concatArrays(chunks);
	}
	
	
	public static int[] unpackIdArray(byte[] ids) {
		byte[][] chunks = Util.split(4, ids);
		int[] result = new int[chunks.length];
		for (int i=0; i<chunks.length; i++)
			result[i] = Util.byteArrayToInt(chunks[i]);
		return result;
	}
	
	
	public static byte[] packIdArray(int[] route, short pos) {
		UnpackedIdArray idObj = new UnpackedIdArray();
		idObj.route = Arrays.copyOfRange(route, 1, route.length);
		idObj.pos = pos;
		return packIdArray(idObj);
	}
	
	
	public static byte[] packIdArray(UnpackedIdArray idObj) {
		byte[][] chunks = new byte[idObj.route.length][];
		for (int i=0; i<idObj.route.length; i++) {
			assert idObj.route[i] != Util.NOT_SET;
			chunks[i] = Util.intToByteArray(idObj.route[i]);
		}
		byte[] result = Util.concatArrays(
				Util.shortToByteArray(idObj.pos), 
				Util.concatArrays(chunks)
				);
		return result;
	}
	
	
	public static UnpackedIdArray unpackIdArrayWithPos(byte[] ids) {
		UnpackedIdArray result = new UnpackedIdArray();
		byte[][] posAndRest = Util.split(2, ids);
		result.pos = Util.byteArrayToShort(posAndRest[0]);
		byte[][] chunks = Util.splitInChunks(4, posAndRest[1]);
		result.route = new int[chunks.length];
		for (int i=0; i<chunks.length; i++)
			result.route[i] = Util.byteArrayToInt(chunks[i]);
		return result;
	}
	
	
	@Override
	public String toString() {
		return "MixList (" +this.hashCode() +"): " 
				+numberOfMixes +" mixes; ids: " 
				+Arrays.toString(mixIDs) +"; addresses: " 
				+Arrays.toString(addresses) +"; ports: " 
				+Arrays.toString(ports); 
	}
	
}
