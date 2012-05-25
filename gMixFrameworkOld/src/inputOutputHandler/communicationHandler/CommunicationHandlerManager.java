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

package inputOutputHandler.communicationHandler;

import inputOutputHandler.InputOutputHandlerInternal;
import framework.Implementation;
import framework.LocalClassLoader;
import framework.SubImplementation;


public abstract class CommunicationHandlerManager extends SubImplementation implements CommunicationHandler  {

	
	protected <T extends CommunicationHandler> T instantiateCommunicationHandler(String className, Implementation owner, Class<T> desiredType) {
		CommunicationHandler ch = LocalClassLoader.instantiateSubImplementation(
				this.getClass().getPackage().getName(), 
				className, 
				owner, 
				CommunicationHandler.class
				);
		assert owner instanceof InputOutputHandlerInternal : "CommunicationHandler.java requires a InputOutputHandler-implementation imlpementing the interface InputOutputHandlerInternal.java";
		ch.setCommunicationHandlerLink((InputOutputHandlerInternal)owner);
		T subImpl = desiredType.cast(ch);
		return subImpl;
	}
	
	
}
