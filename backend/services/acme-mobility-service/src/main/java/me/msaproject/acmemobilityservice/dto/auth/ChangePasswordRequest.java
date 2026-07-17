package me.msaproject.acmemobilityservice.dto.auth;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {
}
