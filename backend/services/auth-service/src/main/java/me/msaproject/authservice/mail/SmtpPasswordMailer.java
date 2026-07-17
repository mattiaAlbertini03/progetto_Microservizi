package me.msaproject.authservice.mail;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Properties;

@ApplicationScoped
public class SmtpPasswordMailer implements PasswordMailer {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String from;
    private final boolean startTls;

    @Inject
    public SmtpPasswordMailer(
            @ConfigProperty(name = "auth-mail.smtp.host", defaultValue = "disabled") String host,
            @ConfigProperty(name = "auth-mail.smtp.port", defaultValue = "587") int port,
            @ConfigProperty(name = "auth-mail.smtp.username", defaultValue = "disabled") String username,
            @ConfigProperty(name = "auth-mail.smtp.password", defaultValue = "disabled") String password,
            @ConfigProperty(name = "auth-mail.from", defaultValue = "noreply@acme.local") String from,
            @ConfigProperty(name = "auth-mail.smtp.starttls", defaultValue = "true") boolean startTls
    ) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.from = from;
        this.startTls = startTls;
    }

    @Override
    public void sendTemporaryPassword(String recipient, String temporaryPassword) {
        if ("disabled".equals(host)) {
            throw new ServiceUnavailableException("Password email delivery is not configured");
        }

        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", Integer.toString(port));
        properties.put("mail.smtp.starttls.enable", Boolean.toString(startTls));
        properties.put("mail.smtp.auth", Boolean.toString(!"disabled".equals(username)));

        Authenticator authenticator = "disabled".equals(username) ? null : new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        };

        try {
            MimeMessage message = new MimeMessage(Session.getInstance(properties, authenticator));
            message.setFrom(new InternetAddress(from));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            message.setSubject("ACME Mobility - temporary password");
            message.setText("Your temporary ACME Mobility password is: " + temporaryPassword
                    + "\n\nLog in and change it from your profile.");
            Transport.send(message);
        } catch (MessagingException exception) {
            throw new ServiceUnavailableException(
                    Response.status(Response.Status.SERVICE_UNAVAILABLE)
                            .entity("Could not deliver the password email")
                            .build(),
                    exception);
        }
    }
}
