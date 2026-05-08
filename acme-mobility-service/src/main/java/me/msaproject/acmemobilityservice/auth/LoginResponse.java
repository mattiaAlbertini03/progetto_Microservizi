package me.msaproject.acmemobilityservice.auth;

public class LoginResponse {
    public boolean success;
    public String userId;
    public String username;

    public LoginResponse() {}

    public LoginResponse(boolean success, String userId, String username) {
        this.success = success;
        this.userId = userId;
        this.username = username;
    }
}