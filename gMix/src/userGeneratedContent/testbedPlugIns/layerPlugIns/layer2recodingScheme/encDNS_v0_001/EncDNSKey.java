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
package userGeneratedContent.testbedPlugIns.layerPlugIns.layer2recodingScheme.encDNS_v0_001;

import java.util.Arrays;

/**
 * This class encapsulates an EncDNS key. It is used as a key for 
 * the @see LinkedHashMap used in @see SharedSecretCache .
 * 
 * @author Jens Lindemann
 */
public class EncDNSKey {
    private final byte[] _key;
    
    public EncDNSKey(byte[] key) {
        _key = key;
    }
    
    /**
     * Two instances of this class are equal if the key material is equal.
     * @param o Object to compare with
     * @return true if o is equal to this instance, false if not
     */
    public boolean equals(Object o) {
        if(o instanceof EncDNSKey) {
            return Arrays.equals(((EncDNSKey)o)._key, _key);
        } else {
            return false;
        }
    }
    
    public int hashCode() {
        return Arrays.hashCode(_key);
    }
    
    public byte[] getKey() {
        return _key;
    }
}