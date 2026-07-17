package me.msaproject.authservice.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class TokenService {

        private final String secret;
        private final String issuer;

        @Inject
        public TokenService(
                        @ConfigProperty(name = "jwt.secret") String secret,
                        @ConfigProperty(name = "jwt.issuer", defaultValue = "auth-service") String issuer) {

                this.secret = secret;
                this.issuer = issuer;
        }

        public String generateToken(User user) {
                try {
                        if (secret == null || secret.isBlank()) {
                                throw new IllegalStateException(
                                                "JWT_SECRET is not configured");
                        }
                        Instant now = Instant.now();

                        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                                        .issuer(issuer)
                                        .subject(user.id())
                                        .claim("username", user.username())
                                        .claim("email", user.email())
                                        .claim("groups", List.of("USER"))
                                        .issueTime(Date.from(now))
                                        .expirationTime(Date.from(
                                                        now.plus(1, ChronoUnit.HOURS)))
                                        .build();

                        SignedJWT jwt = new SignedJWT(
                                        new JWSHeader.Builder(JWSAlgorithm.HS256)
                                                        .type(JOSEObjectType.JWT)
                                                        .build(),
                                        claims);

                        jwt.sign(new MACSigner(
                                        secret.getBytes(StandardCharsets.UTF_8)));

                        return jwt.serialize();

                } catch (Exception exception) {
                        throw new IllegalStateException(
                                        "Could not generate authentication token",
                                        exception);
                }
        }

        public String authenticateUserId(String authorizationHeader) {
                if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                        throw unauthorized("Missing authorization token");
                }

                try {
                        SignedJWT jwt = SignedJWT.parse(authorizationHeader.substring("Bearer ".length()).trim());
                        boolean valid = JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())
                                        && jwt.verify(new MACVerifier(secret.getBytes(StandardCharsets.UTF_8)))
                                        && issuer.equals(jwt.getJWTClaimsSet().getIssuer())
                                        && jwt.getJWTClaimsSet().getExpirationTime() != null
                                        && jwt.getJWTClaimsSet().getExpirationTime().toInstant().isAfter(Instant.now());
                        String userId = jwt.getJWTClaimsSet().getSubject();

                        if (!valid || userId == null || userId.isBlank()) {
                                throw unauthorized("Invalid authorization token");
                        }
                        return userId;
                } catch (WebApplicationException exception) {
                        throw exception;
                } catch (Exception exception) {
                        throw unauthorized("Invalid authorization token");
                }
        }

        private WebApplicationException unauthorized(String message) {
                return new WebApplicationException(message, Response.Status.UNAUTHORIZED);
        }
}
