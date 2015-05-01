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


public interface Layer3OutputStrategyMix extends ArchitectureInterface {
	

	/**
	 * will be called by layer 2 whenever it has encrypted a request. 
	 * 
	 * note: use anonNode.putOutRequest() to forward messages to the next mix 
	 * (or layer 4, if this mix is "the last" mix). 
	 * 
	 * @param reply
	 */
	public void addRequest(Request request);
	
	
	/**
	 * will be called by layer 2 whenever it has encrypted a reply. the reply 
	 * may have arrived from another mix (on behalf of a client) or may have 
	 * been created by the implementation of this interface earlier (in the 
	 * write()-method below) for layer 4. (this allows the implementation of 
	 * this interface, layer 3, to mix its own mix messages with those received 
	 * from other mixes (i.e. put them in the same message pool, so that they 
	 * share the same anonymity set). however, if the messages of this mix 
	 * shall be forwarded independently from those received from other mixes, 
	 * the method reply.wasCreatedOnThisMix() can be used to distinguish 
	 * between the two types.
	 * 
	 * note: use anonNode.putOutReply() to forward messages to the next mix (or 
	 * final receiver/client) via layer 1.
	 * 
	 * @param reply
	 */
	public void addReply(Reply reply);
	
	
	/**
	 * called by layer 4 instances to determine the preferred message size of 
	 * this layer (and implicitly all layers below)
	 * 
	 * must return Layer2RecodingSchemeMixController.getMaxSizeOfNextReply() -
	 * layer3HeaderSize, where layer3HeaderSize is the size of the end-to-end-
	 * header of this layer 3 implementation, that is (optionally) added to 
	 * each mix message in addReply() (note: for the vast majority of output 
	 * strategies, there is no layer 3 header. in this case, simply return 
	 * Layer2RecodingSchemeMixController.getMaxSizeOfNextReply()).
	 * 
	 * @return
	 */
	public int getMaxSizeOfNextWrite();
	
	
	/**
	 * called by layer 4 instances to determine the maximum message size of a 
	 * request
	 * 
	 * must return Layer2RecodingSchemeMixController.getMaxSizeOfNextRequest() -
	 * layer3HeaderSize, where layer3HeaderSize is the size of the end-to-end-
	 * header of this layer 3 implementation, that is (optionally) added to 
	 * each mix message in the client-side plug-in of this layer (note: for the 
	 * vast majority of output strategies, no layer 3 header is required. in 
	 * that case, simply return Layer2RecodingSchemeMixController.getMaxSizeOfNe
	 * xtRequest()).
	 * 
	 * @return
	 */
	public int getMaxSizeOfNextRead();
	
	
	/**
	 * Will be called by layer 4, when data shall be sent to a client, i.e. 
	 * will only be called for end-to-end data transfer between this mix and 
	 * the client (not for messages received from other mixes on behalf of 
	 * other clients).
	 * 
	 * Note: the implementation of this interface may combine the  
	 * data-arrays of several calls (for the same user/client) of this method 
	 * into a single mix message (i.e. concat and/or split the arrays). 
	 * however, it is required that no more than Layer3OutputStrategyMixControll
	 * er.getMaxSizeOfNextReply() - Layer4TransportController.getSizeOfLayer4Hea
	 * der() bytes are combined in a single mix message in total.
	 * 
	 * Note: layer 4 is not allowed to bypass data-arrays larger than 
	 * getMaxSizeOfNextReply() bytes.
	 * 
	 * Note: the implementation must create a reply-instance (i.e. a mix 
	 * message, with Reply reply = MixMessage.getInstanceReply(payload, user); 
	 * and reply.isFirstReplyHop = true;), add the payload, add the layer4 
	 * header (by calling Layer4TransportController.addLayer4Header(reply);), 
	 * (optionally) add a layer 3 header and forward the resulting reply to 
	 * layer 2 (which will apply (layered) encryption) by calling anonNode.forwa
	 * rdToLayer2(reply).
	 * 
	 * Note: messages that are forwarded to layer 2 (with anonNode.forwardToLaye
	 * r2(Reply)) will arrive later at this layer again (together with mix 
	 * messages from other mixes, that were not created on this mix but 
	 * received from other mixes on behalf of a client) via a call of the 
	 * method addReply(Reply). I.e. the addReply-method will be called for both 
	 * types of messages. the reasoning behind this is that layer 3 may decide 
	 * to mix its own mix messages with those received from other mixes (i.e. 
	 * put them in the same message pool, so that they share the same anonymity 
	 * set). if the messages of this mix shall be forwarded independently from 
	 * those received from other mixes, use reply.wasCreatedOnThisMix() in the 
	 * addReply()-method to identify messages that were created on this 
	 * mix and forward them (immediately) to layer 1 by calling anonNode.putOutR
	 * eply()
	 * 
	 * @param user
	 * @param data
	 */
	public void write(User user, byte[] data);
	
}
