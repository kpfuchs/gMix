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

import exception.InformationRetrieveException;
import framework.Controller;
import framework.LocalClassLoader;
import framework.Mix;
import architectureInterface.ExternalInformationPortInterface;


/**
 * Controller class of component <code>ExternalInformationPort</code>. 
 * Implements the architecture interface 
 * <code>ExternalInformationPortInterface</code>. 
 * <p>
 * Used for <code>Information</code> (for example a public key) exchange with 
 * "external" communication partners (for example other mixes or clients). This 
 * component doesn't affect the sending of mix messages directly (That's done 
 * by the <code>InputOutputHandler</code>: see 
 * <code>architectureInterface.InputOutputHandlerInterface</code>).
 * <p>
 * Each exchangeable type of information is specified in the enumeration 
 * <code>Information</code>.
 * 
 * @author Karl-Peter Fuchs
 */
public class ExternalInformationPortController extends Controller implements ExternalInformationPortInterface {

	private ExternalInformationPortInterface externalInformationPortImplementation;
	
	
	public ExternalInformationPortController(Mix mix) {
		super(mix);
	}

	
	@Override
	public Object getInformation(InetAddress informationProviderAddress,
			int informationProviderPort, Information informationOfInterest)
			throws InformationRetrieveException {
		return externalInformationPortImplementation.getInformation(informationProviderAddress, informationProviderPort, informationOfInterest);
	}

	
	@Override
	public Object getInformation(InetAddress informationProviderAddress,
			int informationProviderPort, Information informationOfInterest,
			byte[] data) throws InformationRetrieveException {
		return externalInformationPortImplementation.getInformation(informationProviderAddress, informationProviderPort, informationOfInterest, data);
	}

	
	@Override
	public Object[] getInformationFromAll(
			InetAddress informationProviderAddress,
			int informationProviderPort, Information informationOfInterest)
			throws InformationRetrieveException {
		return externalInformationPortImplementation.getInformationFromAll(informationProviderAddress, informationProviderPort, informationOfInterest);
	}

	
	@Override
	public Object[] getInformationFromAll(
			InetAddress informationProviderAddress,
			int informationProviderPort, Information informationOfInterest,
			byte[] data) throws InformationRetrieveException {
		return externalInformationPortImplementation.getInformationFromAll(informationProviderAddress, informationProviderPort, informationOfInterest, data);
	}

	@Override
	public void instantiateSubclass() {
		this.externalInformationPortImplementation = LocalClassLoader.instantiateExternalInformationPortImplementation(this);
	}

}