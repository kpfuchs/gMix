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
package staticContent.evaluation.testbed.plan;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import staticContent.evaluation.testbed.plan.global.GlobalExecutionPlan;
import staticContent.evaluation.testbed.plan.node.NodeExecutionPlanException;

public class XMLConfigReader {	
	protected static Logger logger = Logger.getLogger(XMLConfigReader.class);
	
	public static GlobalExecutionPlan createPlanV1(String configFilePath) {
		GlobalExecutionPlan globalPlan = null;
		
		try {
			
			// eine neue factory erstellen
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        // Leerzeichen werden entfernt
	        factory.setIgnoringElementContentWhitespace(true);
	
	        // bevor ein 'Document' erstellt werden kann wird ein 'DocumentBuilder' benötigt
	        DocumentBuilder builder = factory.newDocumentBuilder();
	
	        // Speicherort der XML-Datei
	        File fileExperiment = new File(configFilePath);
	        
	        if (!fileExperiment.exists()) {
				logger.error("Experiment config file: "+fileExperiment.getAbsolutePath()+" does not exist.");
				System.exit(1);
			}
	        
	        Document documentExperiment = builder.parse(fileExperiment);
	
	        // Erstellen eines Literatur-Objektes
	        globalPlan = XMLtoObject.createGlobalExecutionPlan(documentExperiment.getDocumentElement());
        
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return globalPlan;
	}
	
	public static Map<String, Set<String>> createNodeVirtualIpAssignment(File virtualModelFile) {
		Map<String, Set<String>> result = new HashMap<String, Set<String>>();
		
		try {
			
			// eine neue factory erstellen
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        // Leerzeichen werden entfernt
	        factory.setIgnoringElementContentWhitespace(true);
	
	        // bevor ein 'Document' erstellt werden kann wird ein 'DocumentBuilder' benötigt
	        DocumentBuilder builder = factory.newDocumentBuilder();
	
	        Document documentExperiment = builder.parse(virtualModelFile);
	
	        // Erstellen eines Literatur-Objektes
	        result = XMLtoObject.createNodeVirtualIpAssignment(documentExperiment.getDocumentElement());
        
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}

	public static void main(String[] args) {
		GlobalExecutionPlan globalPlan = XMLConfigReader.createPlanV1(System.getProperty("user.dir") +"/inputOutput/testbed/experimentDefinitions/test4experiment.xml");
		
		Map<String, Set<String>> map = XMLConfigReader.createNodeVirtualIpAssignment(new File(System.getProperty("user.dir") +"/inputOutput/testbed/tmp/virtual_model.xml"));
		
//		System.out.println(globalPlan.getTerminationMode());
//		System.out.println(globalPlan.getTerminationEvents());		
//		System.out.println(globalPlan.getExecutionTime());
		
		System.out.println(globalPlan.getTestnodeNames());
		System.out.println(globalPlan.getModelnetEmulatorNames());
		
		try {
			globalPlan.configureVirtualIps(map);
		} catch (NodeExecutionPlanException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
