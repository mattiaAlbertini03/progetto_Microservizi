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
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TokenService {

    @ConfigProperty(name = "jwt.secret")
    String secret;

    @ConfigProperty(name = "jwt.issuer", defaultValue = "auth-service")
    String issuer;

    public String generateToken(User user) {
        try {
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
}