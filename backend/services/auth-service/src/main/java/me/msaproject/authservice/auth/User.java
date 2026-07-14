package me.msaproject.authservice.auth;

public record User(
        String id,
        String username,
        String password,
        String email
) {}
