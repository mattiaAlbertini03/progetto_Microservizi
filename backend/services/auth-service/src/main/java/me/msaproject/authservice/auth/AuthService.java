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

import java.util.UUID;

@ApplicationScoped
public class AuthService {

    @Inject
    UsersRepository usersRepository;

    @Inject
    TokenService tokenService;

    public LoginResponse login(LoginRequest request) {
        User user = usersRepository.findByUsername(request.username())
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

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.id(),
                user.username(),
                user.email());
    }
}