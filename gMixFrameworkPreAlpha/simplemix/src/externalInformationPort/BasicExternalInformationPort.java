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

package externalInformationPort;

import java.net.InetAddress;

import architectureInterface.ExternalInformationPortInterface;
import exception.InformationRetrieveException;
import framework.Implementation;


public class BasicExternalInformationPort extends Implementation implements ExternalInformationPortInterface {

	@Override
	public Object getInformation(InetAddress informationProviderAddress,
			int informationProviderPort, Information informationOfInterest)
			throws InformationRetrieveException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getInformation(InetAddress informationProviderAddress,
			int informationProviderPort, Information informationOfInterest,
			byte[] data) throws InformationRetrieveException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object[] getInformationFromAll(
			InetAddress informationProviderAddress,
			int informationProviderPort, Information informationOfInterest)
			throws InformationRetrieveException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object[] getInformationFromAll(
			InetAddress informationProviderAddress,
			int informationProviderPort, Information informationOfInterest,
			byte[] data) throws InformationRetrieveException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void constructor() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void begin() {
		// TODO Auto-generated method stub
		
	}

	
	@Override
	public String[] getCompatibleImplementations() {
		return (new String[] {	"outputStrategy.BasicBatch",
								"outputStrategy.BasicPool",
								"inputOutputHandler.BasicInputOutputHandler",
								"keyGenerator.BasicKeyGenerator",
								"messageProcessor.BasicMessageProcessor",
								"externalInformationPort.BasicExternalInformationPort",
								"networkClock.BasicSystemTimeClock",
								"userDatabase.BasicUserDatabase",
								"message.BasicMessage"	
			});
	}
	

	@Override
	public boolean usesPropertyFile() {
		// TODO Auto-generated method stub
		return false;
	}

}
