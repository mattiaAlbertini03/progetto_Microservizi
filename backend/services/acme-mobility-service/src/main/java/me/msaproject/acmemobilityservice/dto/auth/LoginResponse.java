package me.msaproject.acmemobilityservice.dto.auth;

public record LoginResponse(
        String userId,
        String username,
        String email,
        String token
) {
}