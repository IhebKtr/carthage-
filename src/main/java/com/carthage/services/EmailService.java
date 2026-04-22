package com.carthage.services;

import com.carthage.utils.EnvConfig;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class EmailService {

    private final Session session;
    private final String fromAddress;
    private final String fromName;

    public EmailService() {
        EnvConfig config = EnvConfig.getInstance();

        String host = config.getRequired("MAIL_HOST");
        int port = config.getIntRequired("MAIL_PORT");
        String username = config.getRequired("MAIL_USERNAME");
        String password = config.getRequired("MAIL_PASSWORD");

        this.fromAddress = username;
        this.fromName = config.getRequired("MAIL_FROM_NAME");

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", "true");

        this.session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    public void send(String to, String subject, String body) throws EmailException {
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress, fromName));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject, "UTF-8");
            message.setText(body, "UTF-8");

            Transport.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new EmailException(
                    "Échec de l'envoi de l'email à " + to + " : " + e.getMessage(), e);
        }
    }

    public static class EmailException extends Exception {
        public EmailException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}