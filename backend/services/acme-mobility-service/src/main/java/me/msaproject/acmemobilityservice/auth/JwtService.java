package me.msaproject.acmemobilityservice.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class JwtService {

    private final String secret;
    private final String expectedIssuer;

    @Inject
    public JwtService(
            @ConfigProperty(name = "jwt.secret") String secret,
            @ConfigProperty(
                    name = "jwt.issuer",
                    defaultValue = "auth-service") String expectedIssuer) {

        this.secret = secret;
        this.expectedIssuer = expectedIssuer;
    }

    public AuthenticatedUser authenticate(String authorizationHeader) {
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            throw unauthorized("Missing authorization token");
        }

        String token = authorizationHeader
                .substring("Bearer ".length())
                .trim();

        if (token.isBlank()) {
            throw unauthorized("Missing authorization token");
        }

        try {
            SignedJWT signedJwt = SignedJWT.parse(token);

            if (!JWSAlgorithm.HS256.equals(
                    signedJwt.getHeader().getAlgorithm())) {
                throw unauthorized("Unsupported token algorithm");
            }

            boolean validSignature = signedJwt.verify(
                    new MACVerifier(
                            secret.getBytes(StandardCharsets.UTF_8)));

            if (!validSignature) {
                throw unauthorized("Invalid token signature");
            }

            JWTClaimsSet claims = signedJwt.getJWTClaimsSet();

            if (!expectedIssuer.equals(claims.getIssuer())) {
                throw unauthorized("Invalid token issuer");
            }

            Date expirationTime = claims.getExpirationTime();

            if (expirationTime == null
                    || expirationTime.toInstant().isBefore(Instant.now())) {
                throw unauthorized("Token expired");
            }

            String userId = claims.getSubject();
            String username = claims.getStringClaim("username");

            if (userId == null || userId.isBlank()
                    || username == null || username.isBlank()) {
                throw unauthorized("Invalid token claims");
            }

            return new AuthenticatedUser(userId, username);

        } catch (WebApplicationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unauthorized("Invalid authorization token");
        }
    }

    private WebApplicationException unauthorized(String message) {
        return new WebApplicationException(
                message,
                Response.Status.UNAUTHORIZED);
    }
}
