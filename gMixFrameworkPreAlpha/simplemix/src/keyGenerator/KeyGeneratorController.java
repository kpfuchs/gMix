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

package keyGenerator;


import framework.Controller;
import framework.LocalClassLoader;
import framework.Mix;
import architectureInterface.KeyGeneratorInterface;


/**
 * Controller class of component <code>KeyGenerator</code>. Implements the 
 * architecture interface <code>KeyGeneratorInterface</code>.
 * <p>
 * Used to generate cryptographic keys. A class constant with 
 * <code>public</code> accessibility is available for each supported type of 
 * key.
 * <p>
 * Example of usage: 
 * <p>
 * <code>
 * KeyGeneratorController kgc = new KeyGeneratorController();
 * KeyPair kp = (KeyPair) kgc.generateKey(KeyGeneratorController.KEY_PAIR);
 * </code>
 * 
 * @author Karl-Peter Fuchs
 */
public class KeyGeneratorController extends Controller implements KeyGeneratorInterface {

	
	private KeyGeneratorInterface keyGeneratorImplementation;
	
	/**
	 * Generates a new <code>KeyGenerator</code> component, used to generate 
	 * cryptographic keys. A class constant with <code>public</code> 
	 * accessibility is available for each supported type of key.
	 */
	public KeyGeneratorController(Mix mix) {
		super(mix);
	}
	

	@Override
	public void instantiateSubclass() {
		this.keyGeneratorImplementation = LocalClassLoader.instantiateKeyGeneratorImplementation(this);
	}


	@Override
	public Object generateKey(int identifier) {
		return keyGeneratorImplementation.generateKey(identifier);
	}
}
