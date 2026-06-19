package me.msaproject.acmemobilityservice.dto.auth;

public record SignupRequest(
        String username,
        String password,
        String email
) {}