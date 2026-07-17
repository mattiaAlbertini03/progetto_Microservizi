package me.msaproject.authservice.dto;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {
}
