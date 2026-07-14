package me.msaproject.authservice.dto;

public record UserResponse(
        String userId,
        String username,
        String email
) {}