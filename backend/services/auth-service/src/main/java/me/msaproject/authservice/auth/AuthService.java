package me.msaproject.authservice.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import me.msaproject.authservice.db.UsersRepository;
import me.msaproject.authservice.dto.LoginRequest;
import me.msaproject.authservice.dto.LoginResponse;
import me.msaproject.authservice.dto.SignupRequest;
import me.msaproject.authservice.dto.UserResponse;
import me.msaproject.authservice.dto.ChangePasswordRequest;
import me.msaproject.authservice.dto.ForgotPasswordRequest;
import me.msaproject.authservice.mail.PasswordMailer;

import java.security.SecureRandom;
import java.util.UUID;

@ApplicationScoped
public class AuthService {

    private static final String PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Inject
    UsersRepository usersRepository;

    @Inject
    TokenService tokenService;

    @Inject
    PasswordMailer passwordMailer;

    public LoginResponse login(LoginRequest request) {
        requireText(request.username(), "Username or email is required");
        requireText(request.password(), "Password is required");

        User user = usersRepository.findByUsername(request.username())
                .or(() -> usersRepository.findByEmail(request.username()))
                .orElseThrow(() -> new WebApplicationException(
                        "Invalid credentials",
                        Response.Status.UNAUTHORIZED));

        if (!user.password().equals(request.password())) {
            throw new WebApplicationException(
                    "Invalid credentials",
                    Response.Status.UNAUTHORIZED);
        }

        String token = tokenService.generateToken(user);

        return new LoginResponse(
                user.id(),
                user.username(),
                user.email(),
                token);
    }

    public UserResponse signup(SignupRequest request) {
        requireText(request.username(), "Username is required");
        requireText(request.password(), "Password is required");
        requireText(request.email(), "Email is required");

        if (usersRepository.existsByUsername(request.username())) {
            throw new WebApplicationException(
                    "Username already exists",
                    Response.Status.CONFLICT);
        }

        if (usersRepository.existsByEmail(request.email())) {
            throw new WebApplicationException(
                    "Email already exists",
                    Response.Status.CONFLICT);
        }

        User user = new User(
                UUID.randomUUID().toString(),
                request.username(),
                request.password(),
                request.email());

        usersRepository.save(user);

        return toResponse(user);
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        requireText(request.email(), "Email is required");
        usersRepository.findByEmail(request.email()).ifPresent(user -> {
            String temporaryPassword = generateTemporaryPassword();
            passwordMailer.sendTemporaryPassword(user.email(), temporaryPassword);
            usersRepository.save(new User(user.id(), user.username(), temporaryPassword, user.email()));
        });
    }

    public void changePassword(String authorizationHeader, ChangePasswordRequest request) {
        requireText(request.currentPassword(), "Current password is required");
        requireText(request.newPassword(), "New password is required");
        if (request.newPassword().length() < 8) {
            throw new WebApplicationException("New password must contain at least 8 characters",
                    Response.Status.BAD_REQUEST);
        }

        String userId = tokenService.authenticateUserId(authorizationHeader);
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new WebApplicationException("User not found", Response.Status.NOT_FOUND));

        if (!user.password().equals(request.currentPassword())) {
            throw new WebApplicationException("Current password is incorrect", Response.Status.UNAUTHORIZED);
        }

        usersRepository.save(new User(user.id(), user.username(), request.newPassword(), user.email()));
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(14);
        for (int index = 0; index < 14; index++) {
            password.append(PASSWORD_ALPHABET.charAt(SECURE_RANDOM.nextInt(PASSWORD_ALPHABET.length())));
        }
        return password.toString();
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new WebApplicationException(message, Response.Status.BAD_REQUEST);
        }
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.id(),
                user.username(),
                user.email());
    }
}
