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

package testEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import client.ClientController;
import client.MixSocket;
import framework.Logger;
import framework.Paths;
import framework.Settings;
import framework.Util;


@SuppressWarnings("unused")
public class UserSimulator extends Thread {

	private Random random = new Random();
	
	
	public UserSimulator() {
		
	}
	
	
	@Override
	public void run() {
		Logger.init();
		Settings s = new Settings(Paths.PATH_TO_GLOBAL_CONFIG);
		ClientController client = new ClientController(s);
		MixSocket socket = client.getMixSocket();
		
		try {
			socket.connect();
			OutputStream outputStream = socket.getOutputStream();
			byte[] data = new byte[512];
			random.nextBytes(data);
			int ctr = 0;
			while(true) { 
				outputStream.write(data);
				/*if (ctr++ == 150) {
					ctr=0;
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}*/
			}
			
			/*
			InputStream inputStream = socket.getInputStream();
			String toSend = "1234567890";
			for (int i=0; i<1000; i++)
				toSend +="1234567890";
			System.out.println(client +" sending (pt1) " +toSend.length() +" bytes");
			outputStream.write(toSend.getBytes());
			outputStream.flush();
			
			if (InfoServiceServer.DUPLEX_MIX) {
				byte[] reply = new byte[toSend.length()];
				Util.forceRead(inputStream, reply);
				System.out.println(client +" all data read (pt1)");
			}
			
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(client +" sending (pt2) " +toSend.length() +" bytes");
			outputStream.write(toSend.getBytes());
			outputStream.flush();
			
			if (InfoServiceServer.DUPLEX_MIX) {
				byte[] reply = new byte[toSend.length()];
				Util.forceRead(inputStream, reply);
				System.out.println(client +" all data read (pt2)");
			}
			*/
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
