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
package com.github.encdns;

import java.util.Arrays;

/**
 * Wrapper class for libSodium (http://github.com/jedisct1/libsodium).
 * Needs the C part of the wrapper.
 * libSodium >=0.4.1 must be installed on the system.
 * Implementation optimised for and tested with libSodium 0.4.1.
 * 
 * This class provides methods for encryption and decryption of cryptoboxes
 * with or without using a precomputed intermediate shared secret. If the
 * exchange of multiple messages with the same remote party using the same
 * keypair is expected, precomputed intermediate shared secrets should be cached
 * to increase performance.
 * 
 * @author Jens Lindemann
 */
public class LibSodiumWrapper {
    /** length of public keys in bytes */
    public final int PKBYTES;
    /** length of secret keys in bytes */
    public final int SKBYTES;
    /** length of nonces in bytes */
    public final int NONCEBYTES;
    /** number of 0-bytes at the beginning of a message */
    public final int ZEROBYTES;
    /** number of 0-bytes at the beginning of a cryptobox */
    public final int BOXZEROBYTES;

    public LibSodiumWrapper() {
    	// Run libSodiums init function. This will test the performance of all
        // available crypto implementations and choose the fastest one.
    	this.jni_init();
        
        PKBYTES = this.jni_getPKBytes();
        SKBYTES = this.jni_getSKBytes();
        NONCEBYTES = this.jni_getNonceBytes();
        ZEROBYTES = this.jni_getZeroBytes();
        BOXZEROBYTES = this.jni_getBoxZeroBytes();
    }
    
    /**
     * Open a cryptobox, i.e. decrypt its contents
     * @param cbox cryptobox to decrypt
     * @param rpk public key of remote party
     * @param sk own secret key
     * @param nonce nonce used to encrypt the cryptobox
     * @return decrypted unpadded message contained in the cryptobox
     */
    public byte[] openCryptoBox(byte[] cbox, byte[] rpk, byte[] sk, byte[] nonce) {
        // add 0-padding to the beginning of cryptobox, so libSodium can process it
        byte[] cboxpadded = new byte[cbox.length + BOXZEROBYTES];
        System.arraycopy(cbox, 0, cboxpadded, BOXZEROBYTES, cbox.length);

        // libSodium will return a 0-padded message. The padding is removed
        // before returning the message.
        byte[] mpadded = jni_crypto_box_open(cboxpadded, cboxpadded.length, nonce, rpk, sk);
        return Arrays.copyOfRange(mpadded, ZEROBYTES, mpadded.length);
    }
    
    /**
     * Encrypts a message and returns the corresponding cryptobox.
     * @param m message to be encrypted
     * @param rpk public key of remote party
     * @param sk own secret key
     * @param nonce nonce to use for encryption
     * @return unpadded cryptobox containing the encrypted message
     */
    public byte[] makeCryptoBox(byte[] m, byte[] rpk, byte[] sk, byte[] nonce) {
        // add 0-padding to message to allow libSodium to process it
        byte[] mpadded = new byte[m.length + ZEROBYTES];
        System.arraycopy(m, 0, mpadded, ZEROBYTES, m.length);
        
        // libSodium will return a 0-padded cryptobox. The padding is removed
        // before the cryptobox is returned.
        byte[] cboxpadded = jni_crypto_box(mpadded, mpadded.length, nonce, rpk, sk);
        return Arrays.copyOfRange(cboxpadded, BOXZEROBYTES, cboxpadded.length);
    }
    
    /**
     * Precomputes the intermediate shared secret.
     * @param pk remote party's public key
     * @param sk own secret key
     * @return intermediate shared secret for the specified keys
     */
    public byte[] cryptoBoxBeforenm(byte[] pk, byte[] sk) {
        return jni_crypto_box_beforenm(pk, sk);
    }
    
    /**
     * Opens a cryptobox (i.e. decrypts the message inside it) using a
     * precomputed intermediate shared secret.
     * @param cbox cryptobox containing the encrypted message
     * @param k precomputed intermediate shared secret
     * @param nonce nonce used for encryption
     * @return decrypted unpadded message
     */
    public byte[] openCryptoBoxAfternm(byte[] cbox, byte[] k, byte[] nonce) {
        // add 0-padding to the beginning of cryptobox, so libSodium can process it
        byte[] cboxpadded = new byte[cbox.length + BOXZEROBYTES];
        System.arraycopy(cbox, 0, cboxpadded, BOXZEROBYTES, cbox.length);
        
        // libSodium will return a 0-padded message. The padding is removed
        // before returning the message.
        byte[] mpadded = jni_crypto_box_open_afternm(cboxpadded, cboxpadded.length, nonce, k);
        return Arrays.copyOfRange(mpadded, ZEROBYTES, mpadded.length);
    }
    
    /**
     * Encrypts a message using a precomputed intermediate shared secret and 
     * returns the corresponding cryptobox.
     * @param m message to be encrypted
     * @param k precomputed intermediate shared secret
     * @param nonce nonce to use for encryption
     * @return unpadded cryptobox
     */
    public byte[] makeCryptoBoxAfternm(byte[] m, byte[] k, byte[] nonce) {
        // add 0-padding to message to allow libSodium to process it
        byte[] mpadded = new byte[m.length + ZEROBYTES];
        System.arraycopy(m, 0, mpadded, ZEROBYTES, m.length);

        // libSodium will return a 0-padded cryptobox. The padding is removed
        // before the cryptobox is returned.
        byte[] cboxpadded = jni_crypto_box_afternm(mpadded, mpadded.length, nonce, k);
        return Arrays.copyOfRange(cboxpadded, BOXZEROBYTES, cboxpadded.length);
    }
    
    private native void jni_init();

    private native int jni_getPKBytes();
    private native int jni_getSKBytes();
    private native int jni_getNonceBytes();
    private native int jni_getZeroBytes();
    private native int jni_getBoxZeroBytes();

    /**
     * Provides direct access to libSodium's crypto_box_keypair function for
     * generating a keypair.
     * @param pk empty byte[PKBYTES] which will be used to store the public key
     * @param sk empty byte[SKBYTES] which will be used to store the secret key
     */
    public native void jni_generateKeys(byte[] pk, byte[] sk);
    
    /**
     * Provides direct access to libSodium's crypto_box function for creating
     * a cryptobox. Other than makeCryptoBox, this method will return a 0-padded
     * cryptobox and take a 0-padded message as a parameter
     * @param m 0-padded message to encrypt
     * @param mlen length of the message in bytes
     * @param n nonce to use for encryption
     * @param pk public key of remote party
     * @param sk own secret key
     * @return 0-padded cryptobox containing the encrypted message
     */
    public native byte[] jni_crypto_box(byte[] m, int mlen, byte[] n, byte[] pk, byte[] sk);
    
    /**
     * Provides direct access to libSodium's crypto_box_open function for opening
     * a cryptobox. Other than openCryptoBox, this method must be called using
     * a 0-padded cryptobox and will return a 0-padded message.
     * @param c 0-padded cryptobox to decrypt
     * @param clen length of the cryptobox in bytes
     * @param n nonce used for encryption
     * @param pk public key of remote party
     * @param sk own secret key
     * @return 0-padded decrypted message
     */
    public native byte[] jni_crypto_box_open(byte[] c, int clen, byte[] n, byte[] pk, byte[] sk);
    
    /**
     * Provides direct access to libSodium's crypto_box_beforenm function for
     * precomputing the intermediate shared secret.
     * @param pk remote party's public key
     * @param sk own secret key
     * @return intermediate shared secret corresponding to given keys
     */
    public native byte[] jni_crypto_box_beforenm(byte[] pk, byte[] sk);
    
    /**
     * Provides direct access to libSodium's crypto_box_afternm for creating
     * a cryptobox using a precomputed intermediate shared secret.
     * Other than makeCryptoBoxAfternm this method will return a 0-padded
     * cryptobox and must be called using a 0-padded message.
     * @param m 0-padded message
     * @param mlen length of the message in bytes
     * @param n nonce to use for encryption
     * @param k intermediate shared secret
     * @return 0-padded cryptobox containing the message
     */
    public native byte[] jni_crypto_box_afternm(byte[] m, long mlen, byte[] n, byte[] k);
    
    /**
     * Provides direct access to libSodium's crypto_box_open_afternm function
     * for opening a cryptobox using a precomputed intermediate shared secret.
     * Other than openCryptoBoxAfternm this method will return a 0-padded
     * message and must be called using a 0-padded cryptobox.
     * @param c 0-padded cryptobox to be decrypted
     * @param clen length of cryptobox in bytes
     * @param n nonce used for encryption
     * @param k intermediate shared secret
     * @return 0-padded decrypted message
     */
    public native byte[] jni_crypto_box_open_afternm(byte[] c, long clen, byte[] n, byte[] k);
}
