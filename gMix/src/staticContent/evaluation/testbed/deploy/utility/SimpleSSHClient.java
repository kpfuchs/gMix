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
package staticContent.evaluation.testbed.deploy.utility;

import java.io.IOException;
import java.io.InputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SimpleSSHClient {
	private Session session;
	
	public SimpleSSHClient(String username, String password, String host, int port) throws JSchException {
		JSch jsch = new JSch();
		session = jsch.getSession(username,host,22);
		session.setPassword(password);
		session.setConfig("StrictHostKeyChecking", "no");
	}

	public boolean executeCommand(String cmd) throws JSchException {
		if (!session.isConnected()) session.connect();
		
		Channel channel = session.openChannel("exec");
		((ChannelExec) channel).setCommand(cmd);

		// channel.setInputStream(System.in);
		// channel.setInputStream(null);

		// channel.setOutputStream(System.out);

		// ((ChannelExec)channel).setErrStream(System.err);

		try {
			InputStream in = channel.getInputStream();

			channel.connect();

			byte[] tmp = new byte[1024];
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
				}
				if (channel.isClosed()) {
					if (in.available() > 0)
						continue;
					break;
				}
				try {
					Thread.sleep(50);
				} catch (Exception ee) {
				}
			}

			channel.disconnect();
		} catch (IOException e) {
		}
		
		return channel.getExitStatus() == 0;
	}
	
	public void disconnect() {
		session.disconnect();
	}
	

	public static void main(String[] args) {		
		try {
			SimpleSSHClient client = new SimpleSSHClient("root", "123456", "mnemu", 22);
		
			System.out.println(client.executeCommand("hostname")); //static
//			client.executeCommand("/usr/local/bin/deployhost /root/gmixTest/install/virtual_model.xml /root/gmixTest/install/virtual_route.xml"); //static
			
			client.disconnect();
		}
		catch(Exception e) {
			e.printStackTrace();
		}

	}

}
