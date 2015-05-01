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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.encDNS_v0_001;

import java.net.InetAddress;
import java.util.Arrays;

import staticContent.framework.message.Request;


public class EncDnsRequest extends Request {

	public InetAddress stubResolverAdr;
	public int stubResolverPort;
	
	
	public EncDnsRequest(InetAddress stubResolverAdr, int stubResolverPort, byte[] payload) {
		this.stubResolverAdr = stubResolverAdr;
		this.stubResolverPort = stubResolverPort;
		this.setByteMessage(payload);
	}
	
	
	public EncDnsReply createReplyDataStructure() {
		return new EncDnsReply(stubResolverAdr, stubResolverPort, Arrays.copyOfRange(super.getByteMessage(), 0, 2));
	}
	
}
