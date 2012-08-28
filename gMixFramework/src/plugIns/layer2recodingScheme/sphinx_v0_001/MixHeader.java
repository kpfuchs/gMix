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
package plugIns.layer2recodingScheme.sphinx_v0_001;

import java.security.SecureRandom;
import java.util.Arrays;

import framework.core.util.Util;


public class MixHeader {

	public static final byte MIX_PREFIX = (byte)0;
	public static final byte SPECIAL_DEST_PREFIX = (byte)1;
	public static final byte CLIENT_PREFIX = (byte)2;
	
	
	private byte[] _x = new byte[32]; // the random x (private key)
	private byte[] _gX = new byte[32]; // g^x (public key)
	private byte[][] _betas;
	private byte[][] _gammas;
	private byte[][] _fillerStrings;
	private byte[][] _secrets;
	private byte[][][] _asbTuples;
	private Sphinx_Config config;
	private SecureRandom secureRandom;
	private Route route;

	
	public MixHeader(Sphinx_Config config, SecureRandom secureRandom, Route route) {
		this.config = config;
		this.secureRandom = secureRandom;
		this.route = route;
		byte[][] keyPair = Sphinx.generateKeyPair(config);
		_gX = keyPair[0]; // public
		_x = keyPair[1]; // private
	}


	// byte[] id == address the last mix will receive (first byte must be MIXPREFIX, SPECIAL_DEST_REQUEST or SPECIAL_DEST_REPLY)
	/**
	 * Returns the Mixheader M_0: (Alpha_0, Beta_0, Gamma_0)
	 */
	public byte[][] createHeader(byte[] id) throws Exception {
		assert id.length == config.SECURITY_PARAMETER_SIZE;
		assert id[0] == MIX_PREFIX || id[0] == SPECIAL_DEST_PREFIX || id[0] == CLIENT_PREFIX;
		this._betas = new byte[config.ROUTE_LENGTH][];
		this._gammas = new byte[config.ROUTE_LENGTH][];
		this._fillerStrings = new byte[config.ROUTE_LENGTH][];
		this._secrets = new byte[config.ROUTE_LENGTH][];
		this._asbTuples = new byte[config.ROUTE_LENGTH][][];
		computeTuples();
		computeFillerStrings();
		computeBetasGammas(id);

		return new byte[][] {_asbTuples[0][0], _betas[0], _gammas[0]};
	}

	
	public byte[] getSecret(int id) {
		return _secrets[id];
	}
	
	
	// generate a sequence of _r tuples: (group element, shared secret, blinding
	// factor)
	private void computeTuples() throws Exception {

		byte[][] blinds = new byte[config.ROUTE_LENGTH+1][];
		byte[][] alphas = new byte[config.ROUTE_LENGTH][];

		byte[] alpha = new byte[32];
		byte[] sharedSecret = new byte[32];
		byte[] blindingFactor = new byte[32];

		blinds[0] = _x;

		// round 0
		alpha = _gX;

		assert route.publicKeysOfMixes[0] != null;
		assert blinds != null;
		assert blinds[0] != null;
		
		sharedSecret = Sphinx.genSharedSecret(route.publicKeysOfMixes[0], blinds);

		blindingFactor = Sphinx.hashB(alpha, sharedSecret);
		blinds[1] = blindingFactor;
		alphas[0] = alpha;
		_secrets[0] = sharedSecret;
		_asbTuples[0] = new byte[][] {alpha, sharedSecret, blindingFactor};

		// round 1...(v-1)
		for (int i = 1; i < config.ROUTE_LENGTH; i++) {
			// base=previous alpha, exp=previous blind
			assert alphas[i-1] != null;
			assert blinds[i] != null;
			alpha = Sphinx.genSharedSecret(alphas[i-1], blinds[i]);

			// base=public key of mix, exp= all blinds (including x)
			sharedSecret = Sphinx.genSharedSecret(route.publicKeysOfMixes[i], blinds);

			blindingFactor = Sphinx.hashB(alpha, sharedSecret);

			blinds[i+1] =  blindingFactor;
			alphas[i] =  alpha;
			_secrets[i] =  sharedSecret;
			_asbTuples[i] = new byte[][] { alpha, sharedSecret, blindingFactor };
		}
	}

	private void computeFillerStrings() throws Exception {
		_fillerStrings[0] = new byte[0]; // phi_0
		// phi_1 ... phi_(v-1)
		for (int i = 1; i < config.ROUTE_LENGTH; i++) {
			// previous phi padded with 2*_k zero bytes
			byte[] paddedPhi = Util.concatArrays(_fillerStrings[i-1], Sphinx.ZERO32);

			// The PRG generated with the shared secred s_(i-1)
			byte[] prg = Sphinx.rho(Sphinx.hashRho(_secrets[i-1], config.SECURITY_PARAMETER_SIZE), config.ROUTE_LENGTH, config.SECURITY_PARAMETER_SIZE);

			int min = (2 * (config.ROUTE_LENGTH - i) + 3) * config.SECURITY_PARAMETER_SIZE;
			int max = (2 * config.ROUTE_LENGTH + 3) * config.SECURITY_PARAMETER_SIZE; // exclusiv
			byte[] prgTruncated = Arrays.copyOfRange(prg, min, max);
			assert prgTruncated.length == paddedPhi.length;
			_fillerStrings[i] = Sphinx.xor(paddedPhi, prgTruncated);
		}
	}

	
	private void computeBetasGammas(byte[] id) throws Exception {
		byte[] random = new byte[(2 * (config.ROUTE_LENGTH - config.ROUTE_LENGTH) + 2) * config.SECURITY_PARAMETER_SIZE];
		secureRandom.nextBytes(random);	// TODO: wieso wird hier gepadded? hat header 32 byte platz für dest-id?
		
		// Destination + ID padded with random bytes
		byte[] paddedDestId = Util.concatArrays(id, random); // Destination + ID padded with random bytes 
		
		byte[] prg = Sphinx.rho(Sphinx.hashRho(_secrets[config.ROUTE_LENGTH-1], config.SECURITY_PARAMETER_SIZE), config.ROUTE_LENGTH, config.SECURITY_PARAMETER_SIZE); // The PRG generated with the shared secred s_(v-1)
		byte[] prgTruncated = Arrays.copyOf(prg, (2 * (config.ROUTE_LENGTH - config.ROUTE_LENGTH) + 3) * config.SECURITY_PARAMETER_SIZE);

		assert paddedDestId.length == prgTruncated.length;

		// beta_v-1
		byte[] beta = Util.concatArrays(Sphinx.xor(paddedDestId, prgTruncated), _fillerStrings[config.ROUTE_LENGTH-1]);
		_betas[config.ROUTE_LENGTH-1] = beta;
		
		// gamma_v-1
		byte[] gamma = Sphinx.mu(Sphinx.hashMu(_secrets[config.ROUTE_LENGTH-1], config.SECURITY_PARAMETER_SIZE), beta, config.SECURITY_PARAMETER_SIZE);
		_gammas[config.ROUTE_LENGTH-1] = gamma;

		// betas and gammas for 0<=i<v-1
		for (int i=config.ROUTE_LENGTH-2; i>=0; i--) {
			// the first (2*r-1)_k bytes of the previous beta
			byte[] betaTruncated = Arrays.copyOf(_betas[i+1], (2 * config.ROUTE_LENGTH - 1) * config.SECURITY_PARAMETER_SIZE);

			prg = Sphinx.rho(Sphinx.hashRho(_secrets[i], config.SECURITY_PARAMETER_SIZE), config.ROUTE_LENGTH, config.SECURITY_PARAMETER_SIZE);
			prgTruncated = Arrays.copyOf(prg, (2 * config.ROUTE_LENGTH + 1) * config.SECURITY_PARAMETER_SIZE); // the first (2*r+1)*_k bytes of the PRG
			beta = Sphinx.xor(Util.concatArrays(new byte[][] {route.mixIdsSphinx[i+1], gamma, betaTruncated}), prgTruncated);
			gamma = Sphinx.mu(Sphinx.hashMu(_secrets[i], config.SECURITY_PARAMETER_SIZE), beta, config.SECURITY_PARAMETER_SIZE);

			_betas[i] = beta;
			_gammas[i] = gamma;
		}

	}

}
