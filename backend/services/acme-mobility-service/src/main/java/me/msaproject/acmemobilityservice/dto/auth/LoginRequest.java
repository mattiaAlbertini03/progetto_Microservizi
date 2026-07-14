package me.msaproject.acmemobilityservice.dto.auth;

public record LoginRequest(
        String username,
        String password
) {}