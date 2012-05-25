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
package framework.core.config;

import framework.core.util.Util;


public class MatchingMechanism {

	
	/**
	 * Comment
	 *
	 * @param args Not used.
	 */
	public static void main(String[] args) {
		Settings settings = new Settings(Paths.PATH_TO_PATH_CONFIG_FILE);
		settings.addProperties(Paths.GENERAL_CONFIG_PROPERTY_FILE_PATH);
		isConfigValid(settings);
	} 
	
	

	
	// validates if the configuration for this mix/client is valid (are the plug-ins compatible?)
	public static boolean isConfigValid(Settings settings) {
		
		Settings selectedPlugIns = new Settings("./inputOutput/anonNode/pluginComposition/" + settings.getProperty("PLUG_IN_COMPOSITION"));
		
		// part I: validate if plug-ins match the user design choices
		System.out.println("validating if plug-ins match the user design choices"); 
		Settings userDesignChoices = new Settings("./inputOutput/anonNode/matchingMechanism/userDesignChoices.txt");
		Settings ruleDefinitions = new Settings("./inputOutput/anonNode/matchingMechanism/globalRules/ruleDefinitions.txt");
		System.out.println("user choices: \n" +userDesignChoices.getConfig()); 
		System.out.println("\nrule definitions: \n" +ruleDefinitions.getConfig()); 
		
		for (String ruleName: userDesignChoices.getPropertiesObject().stringPropertyNames()) {
			System.out.println("\n\n ruleName: " +ruleName); 
			
			Settings ruleSet = getRuleSet(ruleName, userDesignChoices.getProperty(ruleName), ruleDefinitions);
			String[] clientRequirementsAllLayers = ruleSet.getProperty("CLIENT_REQUIREMENTS_FOR_ALL_LAYERS").split(",");
			String[] mixRequirementsAllLayers = ruleSet.getProperty("MIX_REQUIREMENTS_FOR_ALL_LAYERS").split(",");
			System.out.println("   clientRequirementsAllLayers: " +java.util.Arrays.asList(clientRequirementsAllLayers)); 
			System.out.println("   mixRequirementsAllLayers: " +java.util.Arrays.asList(mixRequirementsAllLayers));
			
			for (int layer=1; layer<=5; layer++) { // for each layer
				System.out.println("  layer " +layer +":"); 
				
				// load plug-in requirements:
				String[] clientRequirements = getClientPlugInRequirements(layer, ruleSet);
				String[] mixRequirements = getMixPlugInRequirements(layer, ruleSet);
				System.out.println("   clientRequirements: " +java.util.Arrays.asList(clientRequirements)); 
				System.out.println("   mixRequirements: " +java.util.Arrays.asList(mixRequirements));
				
				// load plug-in capabilities:
				String[] clientCapabilities = getGlobalClientPlugInCapabilities(layer, selectedPlugIns, ruleName);
				String[] mixCapabilities = getGlobalMixPlugInCapabilities(layer, selectedPlugIns, ruleName);
				System.out.println("   clientCapabilities: " +java.util.Arrays.asList(clientCapabilities));
				System.out.println("   mixCapabilities: " +java.util.Arrays.asList(mixCapabilities));
				
				// match requirements and capabilities:
				if (	!isGlobalCompatible(layer, clientRequirementsAllLayers, clientCapabilities) ||
						!isGlobalCompatible(layer, mixRequirementsAllLayers, mixCapabilities) ||
						!isGlobalCompatible(layer, clientRequirements, clientCapabilities) ||
						!isGlobalCompatible(layer, mixRequirements, mixCapabilities)
						)
					return false;
				
			}
			
		}
		
		
		// part II: validate if the chosen plug-ins are compatible 
		System.out.println("\n\nvalidating if the chosen plug-ins are compatible "); 
		
		for (int layer=1; layer<=5; layer++) {
			System.out.println(" layer " +layer +":"); 
			
			Settings mixPlugInSettings = getMixPlugInSettings(layer, selectedPlugIns);
			Settings clientPlugInSettings = getClientPlugInSettings(layer, selectedPlugIns);
			
			System.out.println("  bothUseSamePlugIn(): " +bothUseSamePlugIn(selectedPlugIns, layer)); 
			// validate if client and mix plug-ins are compatible
			if (!bothUseSamePlugIn(selectedPlugIns, layer)) {
				
				System.out.println("\n  validate if client and mix plug-ins are compatible"); 
				String[] compatibleClientPlugIns = mixPlugInSettings.getProperty("COMPATIBLE_CLIENT_PLUG_INS").split(",");
				String[] compatibleMixPlugIns = clientPlugInSettings.getProperty("COMPATIBLE_MIX_PLUG_INS").split(",");
				System.out.println("   compatibleClientPlugIns: " +java.util.Arrays.asList(compatibleClientPlugIns));
				System.out.println("   requirement: " +selectedPlugIns.getProperty("LAYER_" +layer +"_PLUG-IN_CLIENT")); 
				System.out.println("   compatibleMixPlugIns: " +java.util.Arrays.asList(compatibleMixPlugIns));
				System.out.println("   requirement: " +selectedPlugIns.getProperty("LAYER_" +layer +"_PLUG-IN_MIX")); 
				
				if (	!contains(selectedPlugIns.getProperty("LAYER_" +layer +"_PLUG-IN_CLIENT"), compatibleClientPlugIns) ||
						!contains(selectedPlugIns.getProperty("LAYER_" +layer +"_PLUG-IN_MIX"), compatibleMixPlugIns)
					) {
					System.err.println("the selected client and mix plug-ins on layer " +layer +" are not compatible");
					return false;
				}
				
			}
			
			System.out.println("\n  validate if static function requirements are met"); 
			// validate if static function requirements are met
			String[] sameLayerRequirementsClient = mixPlugInSettings.getProperty("SAME_LAYER_REQUIREMENTS").split(",");
			String[] sameLayerRequirementsMix = mixPlugInSettings.getProperty("SAME_LAYER_REQUIREMENTS").split(",");
			System.out.println("   sameLayerRequirementsClient: " +java.util.Arrays.asList(sameLayerRequirementsClient));
			System.out.println("   sameLayerRequirementsMix: " +java.util.Arrays.asList(sameLayerRequirementsMix));
			
			//for (String requirement:sameLayerRequirementsClient) {
				// TODO: check if file is available (e.g. new File("pathToFile").isFile());
				// return false;
			//}
			//for (String requirement:sameLayerRequirementsMix) {
				// TODO: check if file is available (e.g. new File("pathToFile").isFile())
				// return false;
			//}

			System.out.println("\n  validate if static function requirements between layers are met"); 
			for (int lay=1; lay<=5; lay++) {
				if (lay != layer) {
					String[] requirementsClient = clientPlugInSettings.getProperty("LAYER_" +lay +"_CLIENT_REQUIREMENTS").split(",");
					String[] requirementsMix = mixPlugInSettings.getProperty("LAYER_" +lay +"_MIX_REQUIREMENTS").split(",");
					String[] capabilitiesClient = getClientPlugInSettings(lay, selectedPlugIns).getProperty("CAPABILITIES_CLIENT").split(",");
					String[] capabilitiesMix = getMixPlugInSettings(lay, selectedPlugIns).getProperty("CAPABILITIES_MIX").split(",");
					System.out.println("    lay " +lay); 
					System.out.println("    requirementsClient: " +java.util.Arrays.asList(requirementsClient));
					System.out.println("    capabilitiesClient: " +java.util.Arrays.asList(capabilitiesClient));
					System.out.println("    requirementsMix: " +java.util.Arrays.asList(requirementsMix));
					System.out.println("    capabilitiesMix: " +java.util.Arrays.asList(capabilitiesMix));
					
					if (	!isStaticFunctionCompatible(layer, lay, requirementsClient, capabilitiesClient) ||
							!isStaticFunctionCompatible(layer, lay, requirementsMix, capabilitiesMix)
						) {
						System.err.println("the selected plug-ins on layer " +layer +" and " +lay +" are not compatible");
						return false;
					}
					
				}
			}

		}
		
		System.out.println("\n\nconfig seems valid"); 
		return true;
	}
	

	private static boolean bothUseSamePlugIn(Settings selectedPlugIns, int layer) {
		String clientPlugIn = selectedPlugIns.getProperty("LAYER_" +layer +"_PLUG-IN_CLIENT");
		String mixPlugIn = selectedPlugIns.getProperty("LAYER_" +layer +"_PLUG-IN_MIX");
		return clientPlugIn.equals(mixPlugIn);
	}
	
	
	private static boolean contains(String searchFor, String[] in) {
		for (String s:in)
			if (s.equals(searchFor))
				return true;
		return false;
	}
	
	
	private static boolean isGlobalCompatible(int layer, String[] requirements, String[] capabilitis) {
		requirementLoop:
		for (String requirement:requirements) {
			
			if (requirement.equals(""))
				continue;
			
			for (String capability:capabilitis)
				if (capability.equals(requirement))
					continue requirementLoop;
			
			System.err.println(
				"inadquate plug-in selected for layer " +layer 
				+"\nthe layer " +layer +" plugin must support " 
				+requirement
			); 
			
			return false;
		}
		return true;
	}
	
	
	private static boolean isStaticFunctionCompatible(int srcLayer, int dstLayer, String[] requirements, String[] capabilitis) {
		requirementLoop:
		for (String requirement:requirements) {
			
			if (requirement.equals(""))
				continue;
			
			for (String capability:capabilitis)
				if (capability.equals(requirement))
					continue requirementLoop;
			
			System.err.println(
				"inadquate plug-in selected for layer " +dstLayer 
				+"\nthe layer " +srcLayer +" plugin requires a  " +dstLayer
				+"plug-in that supports " +requirement
				);
			
			return false;
		}
		return true;
	}
	
	
	private static Settings getRuleSet(String ruleName, String userChoice, Settings ruleDefinitions) {
		String ruleDefinition = ruleDefinitions.getProperty(ruleName); // form: RULE_NAME=[CHOICE_1],[CHOICE_2],...[CHOICE_n], with [CHOICE_x] = CHOICE_NAME:REQUIREMENTS_FILE; example: TOPOLOGY=FIXED_ROUTE:fixedRoute.txt,FREE_ROUTE:freeRoute.txt
		String ruleContentFileName = Util.getTextBetweenAAndB(ruleDefinition, userChoice +":", ".txt");
		System.out.println(" loading ruleset " +"./inputOutput/anonNode/matchingMechanism/globalRules/" +ruleContentFileName +".txt"); 
		return new Settings("./inputOutput/anonNode/matchingMechanism/globalRules/" +ruleContentFileName +".txt");
		
	}

	
	private static String[] getClientPlugInRequirements(int layer, Settings ruleSet) {
		return ruleSet.getProperty("CLIENT_REQUIREMENTS_FOR_LAYER_" +layer).split(",");
	}
	
	
	private static String[] getMixPlugInRequirements(int layer, Settings ruleSet) {
		return ruleSet.getProperty("MIX_REQUIREMENTS_FOR_LAYER_" +layer).split(",");
	}
	
	
	private static String[] getGlobalClientPlugInCapabilities(int layer, Settings selectedPlugIns, String ruleName) {
		Settings plugInSettings = getClientPlugInSettings(layer, selectedPlugIns);
		return plugInSettings.getProperty(ruleName + "_CLIENT").split(",");
	}
	
	
	private static String[] getGlobalMixPlugInCapabilities(int layer, Settings selectedPlugIns, String ruleName) {
		Settings plugInSettings = getMixPlugInSettings(layer, selectedPlugIns);
		return plugInSettings.getProperty(ruleName + "_MIX").split(",");
	}
	
	
	private static Settings getClientPlugInSettings(int layer, Settings selectedPlugIns) {
		return getPlugInSettings(layer, selectedPlugIns, "CLIENT");
	}
	
	
	private static Settings getMixPlugInSettings(int layer, Settings selectedPlugIns) {
		return getPlugInSettings(layer, selectedPlugIns, "MIX");
	}
	
	
	private static Settings getPlugInSettings(int layer, Settings selectedPlugIns, String mixOrClient) {
		switch (layer) {
			case 1:
				return new Settings("./src/plugIns/layer1network/" +selectedPlugIns.getProperty("LAYER_1_PLUG-IN_" +mixOrClient) +"/PlugInSettings.txt");
			case 2:
				return new Settings("./src/plugIns/layer2recodingScheme/" +selectedPlugIns.getProperty("LAYER_2_PLUG-IN_" +mixOrClient) +"/PlugInSettings.txt");
			case 3:
				return new Settings("./src/plugIns/layer3outputStrategy/" +selectedPlugIns.getProperty("LAYER_3_PLUG-IN_" +mixOrClient) +"/PlugInSettings.txt");
			case 4:
				return new Settings("./src/plugIns/layer4transport/" +selectedPlugIns.getProperty("LAYER_4_PLUG-IN_" +mixOrClient) +"/PlugInSettings.txt");
			case 5:
				return new Settings("./src/plugIns/layer5application/" +selectedPlugIns.getProperty("LAYER_5_PLUG-IN_" +mixOrClient) +"/PlugInSettings.txt");
			default:
				throw new RuntimeException("not found " + layer); 
		} 
	}
	
}
