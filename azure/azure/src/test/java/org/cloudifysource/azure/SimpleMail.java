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
