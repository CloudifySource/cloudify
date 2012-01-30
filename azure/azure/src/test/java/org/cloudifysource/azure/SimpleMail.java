/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.azure;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import java.util.*;
import java.io.*;

public class SimpleMail {
	public static void send(String content, File fileAttachment) throws Exception {
		String title="SGTest Suite Azure results";
		String username="tgrid";
		String password="tgrid";
		String mailHost="192.168.10.6";
		List<String> recipients = Arrays.asList(new String[] {
			"itaif@gigaspaces.com",
			"kobi@gigaspaces.com",
			"alexb@gigaspaces.com",
			"dank@gigaspaces.com",
			"barakme@gigaspaces.com",
			"adaml@gigaspaces.com",
			"noak@gigaspaces.com",
			"guy@gigaspaces.com"
	    });
		send(mailHost,username,password,title,content,recipients,fileAttachment);

	}
	
     private static void send(String mailHost, final String user, final String password, String title, String content, List<String> recipients, File fileAttachment) throws Exception{
        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.put("mail.smtp.port", "25");
        props.setProperty("mail.host", mailHost);
        props.setProperty("mail.user", user);
        props.setProperty("mail.password", password);

        Session mailSession = Session.getDefaultInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });
        Transport transport = mailSession.getTransport();

        InternetAddress[] address = new InternetAddress[1];
        address[0] = new InternetAddress("tgrid@gigaspaces.com");

        MimeMessage message = new MimeMessage(mailSession);
        message.addFrom(address);
        message.setSubject(title);
        //message.setContent(content, "text/html; charset=ISO-8859-1");

        InternetAddress[] recipientAddresses = new InternetAddress[recipients.size()];
        for(int i = 0; i < recipients.size(); i++){
            recipientAddresses[i] = new InternetAddress(recipients.get(i));
        }
        message.addRecipients(Message.RecipientType.TO, recipientAddresses);

		// Part one is the email body
		Multipart multipart = new MimeMultipart();
		MimeBodyPart contentPart = new MimeBodyPart();
		contentPart.setText(content);
		multipart.addBodyPart(contentPart);
		// Part two is the attachment
		MimeBodyPart filePart = new MimeBodyPart();
		DataSource source = new FileDataSource(fileAttachment);
		filePart.setDataHandler(new DataHandler(source));
		filePart.setFileName(source.getName());		
		
		multipart.addBodyPart(filePart);

		// Put parts in message
		message.setContent(multipart);
		
        transport.connect();
        transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
        transport.close();
    }
}
