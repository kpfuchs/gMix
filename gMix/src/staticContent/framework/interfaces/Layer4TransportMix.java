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
package staticContent.framework.interfaces;

import staticContent.framework.message.Reply;
import staticContent.framework.message.Request;
import staticContent.framework.userDatabase.User;


public interface Layer4TransportMix extends ArchitectureInterface {

	/**
	 * must call anonNode.write(User user, byte[] data) which will forward the 
	 * byte[] to layer 3 and create a mix message (Reply instance) for 
	 * the byte[] (or combine several byte[]s to a single mix message)
	 * note: (unlike on other layers) NO header my be added here. the 
	 * (optional) layer 4 header must be added in addLayer4Header() (see 
	 * below). the reason for this is that layer 4 may only define one header 
	 * per mix message (i.e. one per layer3-message, not one per layer-4 
	 * message (note: a layer 4-message is the bypassed data-array)). however, 
	 * layer 3 may combine several layer-4 messages (i.e. several data-arrays 
	 * that were bypassed with anonNode.write()) to a single mix message, so 
	 * the layer 4 header must be added AFTER layer 3 has created a mix message 
	 * and thus the addLayer4Header() is required on this layer.
	 * 
	 * note: a typical layer4 header is for example a sequence number for order 
	 * preserving end-to-end transfer.
	 * 
	 * @param user
	 * @param data
	 */
	public void write(User user, byte[] data); 
	
	
	/**
	 * will be called by layer 3, after it has decided to send an (end-to-end) 
	 * mix message to the client, i.e. after layer 4 performed a write-operation 
	 * (or several write-operations) with the anonNode.write()-method mentioned 
	 * above (Note: Layer 3 decides when exactly a mix message shall be 
	 * sent and may combine individual write operations of this layer to a 
	 * single mix message). 
	 * make sure that getSizeOfLayer4Header() returns the correct length of the 
	 * header that is added here (or 0, if no header is added at all)
	 * 
	 * note: a typical layer4 header is for example a sequence number for order 
	 * preserving end-to-end transfer.
	 * 
	 * @return
	 */
	public Reply addLayer4Header(Reply reply);
	
	
	/**
	 * must return the size of the layer4 header (see comments above) or 0, if 
	 * no layer4 header is defined
	 * (note: layer 3 requires this information to decide about the maximum 
	 * payload size for layer3 messages (mix messages))
	 * @return
	 */
	public int getSizeOfLayer4Header();
	
	
	/**
	 * must return Layer3OutputStrategyMixController.getMaxSizeOfNextReply() -
	 * getSizeOfLayer4Header()
	 * Note: used by layer 5 (or other parts of layer 4) to determine the max
	 * number of bytes that can be transmitted in a single mix message on the 
	 * application layer
	 * @return
	 */
	public int getMaxSizeOfNextWrite();
	
	
	/**
	 * will be called by layer 3, when new data (i.e. a new request mix 
	 * message) has arrived.
	 * must call anonNode.forwardToLayer5(Request) to forward the request to 
	 * layer 5.
	 * must remove all layer4 headers.
	 * may delay requests, e.g. to restore the order of messages, as they were 
	 * sent by the client (if the order was changed by, e.g., the output 
	 * strategy and a header with seqence numbers is present (that was added by 
	 * the layer 4-instance of the client-side))
	 * @param request
	 */
	public void forwardRequest(Request request);
	
	
	/**
	 * must return Layer3OutputStrategyMixController.getMaxSizeOfNextRequest() -
	 * getSizeOfLayer4Header()
	 * Note: used by layer 5 (or other parts of layer 4) to determine the max
	 * number of bytes that can be received in a single mix message on the 
	 * application layer
	 * @return
	 */
	public int getMaxSizeOfNextRead();
	
}
