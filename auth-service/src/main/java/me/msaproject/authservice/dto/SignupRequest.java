package me.msaproject.authservice.dto;

public record SignupRequest(
        String username,
        String password,
        String email
) {}