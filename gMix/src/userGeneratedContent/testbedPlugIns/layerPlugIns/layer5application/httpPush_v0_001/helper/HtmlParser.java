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
/**
 * 
 */
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.helper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author bash
 *
 * This class contains all neccesary methods to parse a html file or a css file
 *
 */
public class HtmlParser {

	
	
	/**
	 * Parses all subressiurces from a html file
	 * @param startRequest
	 * @param message
	 * @return List of sub resources
	 */
	public static List<String> getAllRessourcesHtml(String startRequest, String message){
		LinkedList<String> returnvalue = new LinkedList<String>();
		Pattern pattern = Pattern.compile("<(?:(?:img|script)\\s[^>]*\\bsrc\\s*=\\s*[\"']([^\"']*)[\"']|link\\s[^>]*\\bhref\\s*=\\s*[\"']([^\"']*)[\"'])[^>]*>");
		Matcher matcher = pattern.matcher(message);
		while (matcher.find()){
			String tester;
			tester = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
			if(!tester.contains("mailto")){
				try {
                   returnvalue.add((new URI(startRequest).resolve(tester).getPath())+"?"+(new URI(startRequest).resolve(tester).getQuery()) );
				//    returnvalue.add((new URI(startRequest).resolve(tester).getPath()) );
                    //returnvalue.add((new URI(startRequest).resolve(tester).getPath())//+"?"+(new URI(startRequest).resolve(tester).getQuery()) 
                      //      );

				} catch (URISyntaxException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
			}
		}
		return returnvalue;
	}
	
	   /**
     * Parses all sub resources from a css file
     * @param startRequest
     * @param message
     * @return List of sub resources
     */
	public static List<String> getAllRessourcesCss(String message){
		LinkedList<String> returnvalue = new LinkedList<String>();
		Pattern pattern = Pattern.compile("background: [^;]*?url\\([\"']([^\"']*?)[\"']\\)[^;]*?;");
		Matcher matcher = pattern.matcher(message);
		while (matcher.find()){
			returnvalue.add(matcher.group(1));
		}
		return returnvalue;
	}

}
