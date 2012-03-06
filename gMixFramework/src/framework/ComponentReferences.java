/*
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2011  Karl-Peter Fuchs
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

package framework;

import networkClock.NetworkClockController;
import infoService.InfoServiceClient_v1;
import inputOutputHandler.InputOutputHandlerController;
import outputStrategy.OutputStrategyController;
import recodingScheme.RecodingSchemeController;
import userDatabase.UserDatabaseController;


public interface ComponentReferences {

	public InputOutputHandlerController getInputOutputHandler();

	public OutputStrategyController getOutputStrategy();

	//public MessageProcessorController getMessageProcessor();

	public RecodingSchemeController getRecodingScheme();
	
	public InfoServiceClient_v1 getInfoService();

	public NetworkClockController getNetworkClock();

	public UserDatabaseController getUserDatabase();
	
	public Settings getSettings();
	
	public Mix getMix(); // the instance of class "Mix", associated with these components
	
}