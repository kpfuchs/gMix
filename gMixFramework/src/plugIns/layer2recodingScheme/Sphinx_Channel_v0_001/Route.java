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
package plugIns.layer2recodingScheme.Sphinx_Channel_v0_001;


public class Route {
	
	public byte[][] mixIdsSphinx;
	public byte[][] publicKeysOfMixes;
	
	
	// note that the mixList is already a random sublist (=route) of the global list of mixes (see layer 3 client plug-ins for details)
	public Route(int[] route, Sphinx_Config sphinxConfig) {
		this.mixIdsSphinx = new byte[route.length][];
		this.publicKeysOfMixes = new byte[route.length][];
		for (int i=0; i<route.length; i++) {
			mixIdsSphinx[i] = sphinxConfig.mixIdsSphinx[route[i]]; // TODO: possible side-effect -> won't work anymore if mix-ids are chosen differently
			publicKeysOfMixes[i] = sphinxConfig.publicKeysOfMixes[route[i]];
		} 
	}
	
}
