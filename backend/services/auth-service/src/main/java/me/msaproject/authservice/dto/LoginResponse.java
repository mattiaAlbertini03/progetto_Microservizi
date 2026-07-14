package me.msaproject.authservice.dto;

public record LoginResponse(
        String userId,
        String username,
        String email,
        String token
) {
}