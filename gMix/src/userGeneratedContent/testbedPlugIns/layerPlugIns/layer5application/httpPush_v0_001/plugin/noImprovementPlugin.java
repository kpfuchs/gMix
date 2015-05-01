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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.plugin;

import java.util.Hashtable;

import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.dataObjects.Connection;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.improvement.EntryImprovementInterface;
import userGeneratedContent.testbedPlugIns.layerPlugIns.layer5application.httpPush_v0_001.improvement.ExitImprovementInterface;

/**
 * @author bash
 *
 */
public class noImprovementPlugin implements EntryImprovementInterface,
		ExitImprovementInterface {

    private boolean entryWriteOut;
    private boolean exitWriteOut;
    
	/**
	 * 
	 */
	public noImprovementPlugin() {
		entryWriteOut = true;
		exitWriteOut = true;
	}

	
	
	/* (non-Javadoc)
	 * @see improvement.ExitImprovementInterface#isWebBodyImproveable(java.util.Hashtable)
	 */
	@Override
	public boolean isWebBodyToMixImproveable(Hashtable<String, String> headerTable, Connection connection) {
		
		return false;
	}
	
	   /* (non-Javadoc)
     * @see improvement.ExitImprovementInterface#isWebBodyImproveable(java.util.Hashtable)
     */
    @Override
    public boolean isWebBodyFromMixImproveable(Hashtable<String, String> headerTable, Connection connection) {
        
        return false;
    }

	/* (non-Javadoc)
	 * @see improvement.ExitImprovementInterface#toWebHeaderImprovement(byte[])
	 */
	@Override
	public byte[] toWebHeaderImprovement(byte[] message, Connection connection) {
	
		return message;
	}

	/* (non-Javadoc)
	 * @see improvement.ExitImprovementInterface#fromWebHeaderImprovement(byte[])
	 */
	@Override
	public byte[] fromWebHeaderImprovement(byte[] message, Connection connection) {
		return message;
	}

	/* (non-Javadoc)
	 * @see improvement.ExitImprovementInterface#toWebBodyImprovement(byte[])
	 */
	@Override
	public byte[] toWebBodyImprovement(byte[] message, Connection connection) {
		// TODO Auto-generated method stub
		return message;
	}

	/* (non-Javadoc)
	 * @see improvement.ExitImprovementInterface#fromWebBodyImprovement(byte[])
	 */
	@Override
	public byte[] fromWebBodyImprovement(byte[] message, Connection connection) {
		return message;
	}

	/* (non-Javadoc)
	 * @see improvement.EntryImprovementInterface#isAppBodyImproveable(java.util.Hashtable)
	 */
	@Override
	public boolean isAppBodyToMixImproveable(Hashtable<String, String> headerTable, Connection connection) {
		
		return false;
	}
	
	   /* (non-Javadoc)
     * @see improvement.EntryImprovementInterface#isAppBodyImproveable(java.util.Hashtable)
     */
    @Override
    public boolean isAppBodyFromMixImproveable(Hashtable<String, String> headerTable, Connection connection) {
        
        return false;
    }

	/* (non-Javadoc)
	 * @see improvement.EntryImprovementInterface#fromMixHeaderImprovement(byte[])
	 */
	@Override
	public byte[] fromMixHeaderImprovement(byte[] message, Connection connection) {
		return message;
	}

	/* (non-Javadoc)
	 * @see improvement.EntryImprovementInterface#toMixHeaderImprovement(byte[])
	 */
	@Override
	public byte[] toMixHeaderImprovement(byte[] message, Connection connection) {
		return message;
	}

	/* (non-Javadoc)
	 * @see improvement.EntryImprovementInterface#fromMixBodyImprovement(byte[])
	 */
	@Override
	public byte[] fromMixBodyImprovement(byte[] message, Connection connection) {
		return message;
	}

	/* (non-Javadoc)
	 * @see improvement.EntryImprovementInterface#toMixBodyImprovement(byte[])
	 */
	@Override
	public byte[] toMixBodyImprovement(byte[] message, Connection connection) {
		return message;
	}



    @Override
    public boolean isExitWriteOut() {
  
        return entryWriteOut;
    }



    @Override
    public boolean isEntryWriteOut() {
        return entryWriteOut;
    }



}
