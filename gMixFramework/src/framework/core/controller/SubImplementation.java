package framework.core.controller;

import framework.core.AnonNode;
import framework.core.clock.Clock;
import framework.core.config.Settings;
import framework.core.interfaces.ThreePhaseStart;
import framework.core.userDatabase.UserDatabase;
import framework.infoService.InfoServiceClient;


public abstract class SubImplementation implements ThreePhaseStart {

	protected AnonNode anonNode;
	protected Settings settings;
	protected UserDatabase userDatabase;
	protected Clock clock;
	protected InfoServiceClient infoService;
	
	protected Layer1NetworkMixController networkLayerMix;
	protected Layer1NetworkClientController networkLayerClient;
	protected Layer2RecodingSchemeMixController recodingLayerMix;
	protected Layer2RecodingSchemeClientController recodingLayerClient;
	protected Layer3OutputStrategyMixController outputStrategyLayerMix;
	protected Layer3OutputStrategyClientController outputStrategyLayerClient;
	protected Layer4TransportMixController transportLayerMix;
	protected Layer4TransportClientController transportLayerClient;
	protected Layer5ApplicationMixController applicationLayerMix;
	protected Layer5ApplicationClientController applicationLayerClient;
	
	
	protected void setAnonNode(AnonNode anonNode) {
		this.anonNode = anonNode;
		this.settings = anonNode.getSettings();
		this.userDatabase = anonNode.getUserDatabase();
		this.clock = anonNode.getClock();
		this.infoService = anonNode.getInfoService();
		
		this.networkLayerMix = anonNode.getNetworkLayerControllerMix();
		this.networkLayerClient = anonNode.getNetworkLayerControllerClient();
		this.recodingLayerMix = anonNode.getRecodingLayerControllerMix();
		this.recodingLayerClient = anonNode.getRecodingLayerControllerClient();
		this.outputStrategyLayerMix = anonNode.getOutputStrategyLayerControllerMix();
		this.outputStrategyLayerClient = anonNode.getOutputStrategyLayerControllerClient();
		this.transportLayerMix = anonNode.getTransportLayerControllerMix();
		this.transportLayerClient = anonNode.getTransportLayerControllerClient();
		this.applicationLayerMix = anonNode.getApplicationLayerControllerMix();
		this.applicationLayerClient = anonNode.getApplicationLayerControllerClient();
	}
	
}
