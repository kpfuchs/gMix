package framework.core.launcher;

import evaluation.loadGenerator.LoadGenerator;
import evaluation.localTest.LocalTest;
import evaluation.simulator.core.Simulator;
import framework.core.AnonNode;
import framework.infoService.InfoServiceServer;

public enum ToolName {
	
	NOT_SET				(new String[] {}),
	LOCAL_TEST			(new String[] {"localTest", "1"}), 
	INFO_SERVICE		(new String[] {"infoService", "2"}),
	MIX					(new String[] {"mix", "3"}), 
	CLIENT				(new String[] {"client", "4"}), 
	P2P					(new String[] {"p2p", "mixAndClient", "5"}), 
	LOAD_GENERATOR		(new String[] {"loadGenerator", "6"}), 
	SIMULATOR			(new String[] {"simulator", "7" });
	
	public String[] identifiers;
	
	
	// TODO: auch beschreibung des tools etc übergeben lassen?
	private ToolName(String[] identifiers) {
		this.identifiers = identifiers;
	}
	
	
	public static void execute(CommandLineParameters parameters) {
		if (parameters.gMixTool == null)
			parameters.gMixTool = ToolName.NOT_SET;
		
		switch (parameters.gMixTool) {
			case NOT_SET:
				throw new RuntimeException("cannot execute the tool, as \"ToolName\" is not set!"); 
			case MIX:
				new AnonNode(parameters);
				break;
			case CLIENT:
				new AnonNode(parameters);
				break;
			case P2P:
				new AnonNode(parameters);
				break;
			case SIMULATOR:
				new Simulator(parameters);
				break;
			case LOAD_GENERATOR:
				new LoadGenerator(parameters);
				break;
			case LOCAL_TEST:
				new LocalTest(parameters);
				break;
			case INFO_SERVICE:
				new InfoServiceServer(parameters);
				break;
			default:
				throw new RuntimeException("unknown tool; add an entry in framework.core.launcher.ToolName.java"); 
		} 
	}
	
	
	public static ToolName getToolByIdentifier(String identifier) {
		for (ToolName tool: ToolName.values())
			for (String ident:tool.identifiers)
				if (ident.equalsIgnoreCase(identifier) || ident.equalsIgnoreCase("-"+identifier))
					return tool;
		return ToolName.NOT_SET;
	}
	
}
