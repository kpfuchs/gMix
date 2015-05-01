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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class SimpleSftpClient {
	private Session session;
	
	public SimpleSftpClient(String username, String password, String host, int port) throws JSchException {
		JSch jsch = new JSch();
		session = jsch.getSession(username,host,22);
		session.setPassword(password);
		session.setConfig("StrictHostKeyChecking", "no");
	}
	
	public boolean putFile(String filename, String remoteDirectoryPath) throws JSchException, FileNotFoundException, SftpException {
		if (!session.isConnected()) session.connect();
		
		ChannelSftp channel = null;
		channel = (ChannelSftp)session.openChannel("sftp");
		channel.connect();
		
	    File localFile = new File(filename);

	    channel.cd(remoteDirectoryPath);
		channel.put(new FileInputStream(localFile),localFile.getName(), ChannelSftp.OVERWRITE);
		
		channel.disconnect();
		
		return true;
	}
	
	public boolean removeFile(String remoteDirectoryPath, String filename) throws JSchException, SftpException {
		if (!session.isConnected()) session.connect();
		
		ChannelSftp channel = null;
		channel = (ChannelSftp)session.openChannel("sftp");
		channel.connect();

	    channel.cd(remoteDirectoryPath);
	    channel.rm(filename);
		
		channel.disconnect();
		
		return true;		
	}
	
	public void disconnect() {
		session.disconnect();
	}
	

	public static void main(String[] args) {		
		try {
			SimpleSftpClient client = new SimpleSftpClient("root", "123456", "mnemu", 22);
		
			System.err.println("warning: /root/modelnetTest2 is still hardcoded"); // TODO
			client.putFile(System.getProperty("user.dir") +"/inputOutput/testbed/tmp/virtual_model.xml", "/root/modelnetTest2"); //static
			client.putFile(System.getProperty("user.dir") +"/inputOutput/testbed/tmp/virtual_route.xml", "/root/modelnetTest2"); //static
			
			client.disconnect();
			
System.out.println("put");
			
			client = new SimpleSftpClient("root", "123456", "mnemu", 22);
			
			System.err.println("warning: /root/modelnetTest2 is still hardcoded"); // TODO
			client.removeFile("/root/modelnetTest2", "virtual_model.xml"); //static
			client.removeFile("/root/modelnetTest2", "virtual_route.xml"); //static
			
			client.disconnect();
		}
		catch(Exception e) {
			e.printStackTrace();
		}

	}
}
