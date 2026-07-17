package me.msaproject.authservice.mail;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

@Alternative
@Priority(1)
@ApplicationScoped
public class CapturingPasswordMailer implements PasswordMailer {

    private volatile String recipient;
    private volatile String temporaryPassword;

    @Override
    public void sendTemporaryPassword(String recipient, String temporaryPassword) {
        this.recipient = recipient;
        this.temporaryPassword = temporaryPassword;
    }

    public String recipient() {
        return recipient;
    }

    public String temporaryPassword() {
        return temporaryPassword;
    }
}
