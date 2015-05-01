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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer2recodingScheme.noDelay_v0_001;

import java.util.Arrays;

import staticContent.framework.util.Util;


public class HeaderBlock {

	// [totalBlockLen] [header_1] ... [header_n]
	// with: [headern_n] = [headerLen] [headerData]
	private byte[] headerForClient;
	
	private byte[] headerForThisMix;
	private byte[] headerForNextMix;
	
	
	/**
	 * use on client side
	 */
	public HeaderBlock(byte[][] headers) {
		this.headerForClient = new byte[0];
		for (int i=0; i<headers.length; i++) { // add [header_1] ... [header_n]
			this.headerForClient = Util.concatArrays(
					this.headerForClient, 
					Util.concatArrays(Util.shortToByteArray(headers[i].length), headers[i])
					);
		} 
		this.headerForClient = Util.concatArrays(Util.shortToByteArray(this.headerForClient.length), this.headerForClient); // add [totalBlockLen]
	}
	
	
	/**
	 * use on mix side
	 */
	public HeaderBlock(byte[] headers) {
		int length = Util.byteArrayToShort(Arrays.copyOf(headers, 2));
		if (length > 0) {
			this.headerForThisMix = Arrays.copyOfRange(headers, 2, length + 2);
			if (this.headerForThisMix.length == (headers.length-2))  // nothing left
				return;
			byte[] remaining = Arrays.copyOfRange(headers, length + 2, headers.length);
			this.headerForNextMix = Util.concatArrays(
					Util.shortToByteArray(remaining.length),
					remaining
				);
		}
	}
	
	
	/**
	 * use on mix side
	 */
	public byte[] getHeaderForThisMix() {
		return this.headerForThisMix;
	}
	
	
	/**
	 * use on mix side
	 */
	public byte[] getHeaderForNextMix() {
		return this.headerForNextMix;
	}
	
	
	/**
	 * use on client side
	 */
	public byte[] getHeaderForClient() {
		if (this.headerForClient == null)
			throw new RuntimeException("use on client side only; use constructor HeaderBlock(byte[][] headers)"); 
		return headerForClient;
	}

}
