package plugIns.layer2recodingScheme.noDelay_v0_001;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer1NetworkClient;
import framework.core.interfaces.Layer2RecodingSchemeClient;
import framework.core.interfaces.Layer3OutputStrategyClient;
import framework.core.message.Reply;
import framework.core.message.Request;


public class ClientPlugIn extends Implementation implements Layer2RecodingSchemeClient {

	private int maxPayload;
	
	
	@Override
	public void constructor() {
		this.maxPayload = anonNode.MAX_PAYLOAD;
	}
	

	@Override
	public void initialize() {

	}

	@Override
	public void begin() {
		
	}
	
	
	@Override
	public void setReferences(
			Layer1NetworkClient layer1,
			Layer2RecodingSchemeClient layer2, 
			Layer3OutputStrategyClient layer3) {
		assert layer2 == this;
	}
	

	@Override
	public Request applyLayeredEncryption(Request request) {
		return request;
	}

	
	@Override
	public int getMaxPayloadForNextMessage() {
		return this.maxPayload;
	}

	
	@Override
	public int getMaxPayloadForNextReply() {
		return this.maxPayload;
	}

	
	@Override
	public Reply extractPayload(Reply reply) {
		return reply;
	}

}