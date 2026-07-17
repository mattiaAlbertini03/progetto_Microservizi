package me.msaproject.authservice.mail;

public interface PasswordMailer {

    void sendTemporaryPassword(String recipient, String temporaryPassword);
}
