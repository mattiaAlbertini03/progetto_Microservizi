package me.msaproject.acmemobilityservice.auth;

public record AuthenticatedUser(
        String userId,
        String username
) {
}