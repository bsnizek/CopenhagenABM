package copenhagenabm.tools;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailTool2 {

	private String to;
	String host = "smtp.gmail.com";
	Properties props = System.getProperties();
	Session session;
	private String from;
	private String password;

	public EmailTool2(String to) {
		this.to = to;

		this.from = "bs@metascapes.org";
		this.password = "Goo2012gle!";

		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		session = Session.getDefaultInstance(props,
		
		new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(from, password);
			}
		  });

	}

	public void sendMail(String text) {
		try{
			// Create a default MimeMessage object.
			MimeMessage message = new MimeMessage(session);

			// Set From: header field of the header.
			message.setFrom(new InternetAddress(from));

			// Set To: header field of the header.
			message.addRecipient(Message.RecipientType.TO,
					new InternetAddress(to));

			// Set Subject: header field
			message.setSubject("copenhagenABM");

			// Now set the actual message
			message.setText(text);

			// Send message
			Transport.send(message);
			System.out.println("Sent message successfully....");
		}catch (MessagingException mex) {
			mex.printStackTrace();
		}
	}


	public static void main(String [] args)
	{
		EmailTool2 emt = new EmailTool2("besn@life.ku.dk");
		emt.sendMail("yeah");
	}
}
