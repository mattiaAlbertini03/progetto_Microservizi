package me.msaproject.acmemobilityservice.dto.auth;

public record AuthUserResponse(
        String userId,
        String username,
        String email
) {}