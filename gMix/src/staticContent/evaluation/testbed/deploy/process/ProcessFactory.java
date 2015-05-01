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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import staticContent.evaluation.testbed.deploy.testnode.ITestNode;

public class ProcessFactory {

	/**
	 * Creates a set of processes from a json string.
	 *
	 * @param json         json string which contains process informations
	 * @param testnodeName name of the testnode on which the processes are running
	 * @param testnode     reference to the testnode on which the processes are running
	 *
	 * @return set of processes
	 *
	 * @throws JSONException
	 */
	public static Set<ProcessInfo> getProcessesFromJson(String json, String testnodeName, ITestNode testnode) throws JSONException {
		Set<ProcessInfo> result = new HashSet<ProcessInfo>();

		if (json.equals("")) {
			return result;
		}

		JSONObject jObj      = new JSONObject(json);
		String hostName      = jObj.getString("hostName");
		JSONObject processes = jObj.getJSONObject("processes");
		
		Iterator<?> keys = processes.keys();

        while(keys.hasNext()){
            String key            = (String)keys.next();
            JSONObject processObj = processes.getJSONObject(key);

            result.add(new ProcessInfo(testnodeName, testnode, processObj.getString("vpid"), processObj.getString("command"), hostName));
        }

		return result;
	}
}
