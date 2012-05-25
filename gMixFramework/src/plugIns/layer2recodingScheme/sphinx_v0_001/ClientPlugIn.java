package plugIns.layer2recodingScheme.sphinx_v0_001;

import framework.core.controller.Implementation;
import framework.core.interfaces.Layer1NetworkClient;
import framework.core.interfaces.Layer2RecodingSchemeClient;
import framework.core.interfaces.Layer3OutputStrategyClient;
import framework.core.message.Reply;
import framework.core.message.Request;


public class ClientPlugIn extends Implementation implements Layer2RecodingSchemeClient {

	private Sphinx_Config config;
	private Sphinx messageCreator;
	
	
	@Override
	public void constructor() {
		this.config = new Sphinx_Config(anonNode, true);
		this.messageCreator = new Sphinx(anonNode, config);
		this.messageCreator.initAsClient();
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
		return messageCreator.applyLayeredEncryption(request);
	}

	
	@Override
	public int getMaxPayloadForNextMessage() {
		return messageCreator.getMaxPayloadForNextMessage();
	}

	
	@Override
	public int getMaxPayloadForNextReply() {
		return messageCreator.getMaxPayloadForNextReply();
	}

	
	@Override
	public Reply extractPayload(Reply reply) {
		return messageCreator.extractPayload(reply);
	}

}