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
package staticContent.evaluation.testbed.deploy.process;

import java.rmi.RemoteException;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import staticContent.evaluation.testbed.deploy.testnode.ITestNode;

public class ProcessInfo {
	protected Logger logger = Logger.getLogger(getClass());
	protected ITestNode testnode;
	protected String testnodeName;
	protected String vpid;
	protected String command;
	protected String hostName;

	public ProcessInfo(String vpid, String command)
	{
		this.vpid	 = vpid;
		this.command = command;
	}

	public ProcessInfo(String testnodeName, ITestNode testnode, String vpid, String command, String hostName) {
		this.testnode     = testnode;
		this.testnodeName = testnodeName;
		this.vpid          = vpid;
		this.command      = command;
		this.hostName     = hostName;
	}

	/**
	 * @return the command
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * @return the hostName
	 */
	public String getHostName() {
		return hostName;
	}

	/**
	 * @return the virtual process id of the process.
	 */
	public String getVpid() {
		return vpid;
	}

	/**
	 * @return the name of the testnode the process is running on.
	 */
	public String getTestnodeName() {
		return testnodeName;
	}

	/**
	 * Kills the process on the testnode it is running on.
	 *
	 * @return true if the process was killed successfully, otherwise false.
	 */
	public boolean kill() {
		try {
			testnode.killProcess(vpid);
		} catch (RemoteException e) {
			logger.error(e.getMessage(), e);
			return false;
		}

		return true;
	}

	/**
	 * Creates a JSON representation of this object.
	 *
	 * @return JSON string
	 *
	 * @throws JSONException
	 */
	public JSONObject toJson() throws JSONException {
		JSONObject jObj = new JSONObject();

	    jObj.put("vpid", getVpid());
	    jObj.put("command", getCommand());

	    return jObj;
	}
}
