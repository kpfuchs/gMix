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

package evaluation.simulator.delayBox;

import evaluation.simulator.core.Simulator;


public abstract class DelayBox {
	
	
	public static DelayBox getInstance(int bandwidthSend, int bandwidthReceive, int latency) {
		
		String type = Simulator.settings.getProperty("TYPE_OF_DELAY_BOX");
		
		if (type.equals("BASIC_DELAY_BOX"))
			return new BasicDelayBox(bandwidthSend, bandwidthReceive, latency);
		else if (type.equals("NONE"))
			return new NoDelayDelayBox();
		else
			throw new RuntimeException("ERROR: Unknown TYPE_OF_DELAY_BOX!");

	}
	
	
	public static DelayBox getInstance() {
		
		String type = Simulator.settings.getProperty("TYPE_OF_DELAY_BOX");
		
		if (type.equals("NONE"))
			return new NoDelayDelayBox();
		else
			throw new RuntimeException("ERROR: Unknown TYPE_OF_DELAY_BOX!");

	}
	
	
	protected DelayBox() {
		
	}
	
	
	public abstract int getSendDelay(int numberOfBytesToSend);
	
	
	public abstract int getReceiveDelay(int numberOfBytesToSend);
	
}
