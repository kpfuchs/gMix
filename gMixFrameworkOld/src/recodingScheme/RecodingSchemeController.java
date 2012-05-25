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

package recodingScheme;

import message.Reply;
import message.Request;
import userDatabase.User;
import framework.Controller;
import framework.LocalClassLoader;
import framework.Mix;


public class RecodingSchemeController extends Controller implements RecodingScheme, DummyGenerator {

	private RecodingScheme recodingSchemeImplementation;
	private DummyGenerator dummyGenerator;
	private boolean supportsDummyTraffic;
	
	
	public RecodingSchemeController(Mix mix) {
		super(mix);
	}

	
	@Override
	public void instantiateSubclass() {
		
		this.recodingSchemeImplementation = 
			LocalClassLoader.instantiateImplementation(this, RecodingScheme.class, settings);

		this.supportsDummyTraffic = recodingSchemeImplementation.supportsDummyTraffic();
		if (supportsDummyTraffic)
			this.dummyGenerator = (DummyGenerator)recodingSchemeImplementation;
		
	}


	@Override
	public Request generateDummy(User user) {
		assert supportsDummyTraffic;
		return this.dummyGenerator.generateDummy(user);
	}


	@Override
	public Request generateDummy() {
		assert supportsDummyTraffic;
		return this.dummyGenerator.generateDummy();
	}


	@Override
	public Reply generateDummyReply(User user) {
		assert supportsDummyTraffic;
		return this.dummyGenerator.generateDummyReply(user);
	}
	
	
	@Override
	public Reply generateDummyReply() {
		assert supportsDummyTraffic;
		return this.dummyGenerator.generateDummyReply();
	}


	@Override
	public boolean supportsDummyTraffic() {
		return this.recodingSchemeImplementation.supportsDummyTraffic();
	}


	@Override
	public String getPropertyKey() {
		return "RECODING_SCHEME";
	}


	@Override
	public int getMaxPayloadForNextReply(User user) {
		return recodingSchemeImplementation.getMaxPayloadForNextReply(user);
	}


	@Override
	public void constructor() {
		// TODO
	}
	

	/*public static RecodingSchemeClient getInstance(Client callingInstance) {
	
	if (Settings.getProperty("RECODING_SCHEME").equals("RSA_OAEP_AES_OFB_v1"))
		return new RSA_OAEP_AES_OFB_v1_Client(callingInstance);
	else if (Settings.getProperty("RECODING_SCHEME").equals("Sphinx_v1"))
		return new Sphinx_v1_Client(callingInstance);
	else
		throw new RuntimeException("unknown recoding scheme specified: " +Settings.getProperty("RECODING_SCHEME")); 
}


public static RecodingSchemeMix getInstance(Mix callingInstance) {
	if (Settings.getProperty("RECODING_SCHEME").equals("RSA_OAEP_AES_OFB_v1"))
		return new RSA_OAEP_AES_OFB_v1_Mix(callingInstance);
	else if (Settings.getProperty("RECODING_SCHEME").equals("Sphinx_v1"))
		return new Sphinx_v1_Mix(callingInstance);
	else
		throw new RuntimeException("unknown recoding scheme specified: " +Settings.getProperty("RECODING_SCHEME")); 	
}
*/
	
	
}
