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
package evaluation.simulator.gui.customElements.structure;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import evaluation.simulator.gui.customElements.PluginPanel;


public class HelpPropValues {
	
	private static Logger logger = Logger.getLogger(PluginPanel.class);
	
	private StringTokenizer tokenizer;
	
	private List<String> values;
	private Class type;
	private String typename;
	
	public HelpPropValues(String value, Class type){
		
		values = new LinkedList<String>();
		this.type = type;
		typename="UNKOWN";
		
		tokenizer = new StringTokenizer( value, "," );
		while (tokenizer.hasMoreTokens()){
			values.add((String) tokenizer.nextElement());
		}
		
	}
	
	public boolean isValid(){
		
		boolean[] testresults = new boolean[values.size()];
		int i = 0;
		
		for(String s : values){
			
			logger.log(Level.DEBUG, s);
//			logger.log(Level.DEBUG, type.toString());
//			logger.log(Level.DEBUG, Integer.class.toString());
			
			if (this.type == Integer.class){
				logger.log(Level.DEBUG, "checking for Integer");
				typename="Integer";
				testresults[i] = true;
				try{
					Integer.parseInt(s);
				}
				catch(Exception e){
					testresults[i] = false;
					logger.log(Level.DEBUG, s + " is not an Integer");
				}
			}
			
			if (this.type == Boolean.class){
				logger.log(Level.DEBUG, "checking for Boolean");
				typename="Boolean";
				testresults[i] = true;
				try{					
					Boolean.parseBoolean(s);
				}
				catch(Exception e){
					testresults[i] = false;
					logger.log(Level.DEBUG, s + " is not a Boolean");
				}
			}
			
			if (this.type == Float.class){
				logger.log(Level.DEBUG, "checking for Float");
				typename="Float";
				testresults[i] = true;
				try{
					Float.parseFloat(s);
				}
				catch(Exception e){
					testresults[i] = false;
					logger.log(Level.DEBUG, s + " is not a Float");
				}
			}
			
			if (this.type == Double.class){
				logger.log(Level.DEBUG, "checking for Double");
				typename="Double";
				testresults[i] = true;
				try{
					Double.parseDouble(s);
				}
				catch(Exception e){
					testresults[i] = false;
					logger.log(Level.DEBUG, s + " is not a Double");
				}
			}
			i++;
			
		}		
		
		for(boolean b : testresults){
			if (b != true){
				return false;
			}
		}		
		return true;
	}
	
	public Class getType(){
		return type;
	}
	
	public String getTypeName(){
		return typename;
	}
	
	public List<String> getValues(){
		return values;
	}

}
