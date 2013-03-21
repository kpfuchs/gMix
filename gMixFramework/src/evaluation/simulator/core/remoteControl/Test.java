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
package evaluation.simulator.core.remoteControl;

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;


public class Test {

	
	public static void postMail(	String recipient, 
									String subject,
									String message, 
									String from
									) throws MessagingException {
		
		Properties props = new Properties();
		props.put("mail.smtp.host", "mailhost.informatik.uni-hamburg.de");
		Session session = Session.getDefaultInstance(props);
		Message msg = new MimeMessage(session);
		InternetAddress addressFrom = new InternetAddress(from);
		msg.setFrom(addressFrom);
		InternetAddress addressTo = new InternetAddress(recipient);
		msg.setRecipient(Message.RecipientType.TO, addressTo);
		msg.setSubject(subject);
		msg.setContent(message, "text/plain");
		Transport.send(msg);
		
	}

	
	public static void main(String[] args) {
		try {
			postMail("fuchs@informatik.uni-hamburg.de", "Test", "simulation finished", "svs@informatik.uni-hamburg.de");
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}

}
