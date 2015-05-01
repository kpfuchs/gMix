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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class represents a cache for the intermediate shared secrets
 * used by a remote proxy to communicate with local proxies. If a local proxy
 * queries a remote proxy more than once using the same keypair, some of the 
 * calculations to generate the shared secret can be avoided by storing an
 * intermediate value.
 * 
 * The implementation is based on @see LinkedHashMap and will automatically
 * remove the eldest entry if the maximum size is exceeded.
 * 
 * @author Jens Lindemann
 */
@SuppressWarnings("serial")
public class SharedSecretCache extends LinkedHashMap<EncDNSKey, byte[]> {
    /** maximum size */ 
    private final int SIZE;
    
    /**
     * Constructor for the SharedSecretCache.
     * @param size maximum cache size
     */
    public SharedSecretCache(int size) {
        // The super call will create a LinkedHashMap which can store exactly
        // size+1 elements to avoid costly resizing operations. As the eldest
        // entry is only removed after adding a new element, we need to be able
        // to store one more element than we want to cache.
        super(size+1, 1, true);
        SIZE = size;
    }
    
    @SuppressWarnings("rawtypes")
	@Override
    protected boolean removeEldestEntry(Map.Entry eldest) {
        // If this method returns true, the eldest entry in the list will be
        // removed. Thus, it should return true if the current size exceeds
        // the maximum size.
        return this.size() > SIZE;
    }
    
    /**
     * This is an alternative implementation of the put method for convenience.
     * It will take a byte[] as the key instead of an EncDNSKey object.
     * @param key public key of remote party
     * @param value intermediate shared secret
     */
    public byte[] put(byte[] key, byte[] value) {
        return this.put(new EncDNSKey(key), value);
    }
    
    /**
     * This is an alternative implementation of the get method for convenience.
     * It will take a byte[] as the key instead of an EncDNSKey object.
     * @param key
     */
    public byte[] get(byte[] key) {
        return this.get(new EncDNSKey(key));
    }
}