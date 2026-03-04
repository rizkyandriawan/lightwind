package dev.kakrizky.lightwind.auth;

import dev.kakrizky.lightwind.config.LightwindConfig;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class LightAuthService {

    @Inject
    LightwindConfig config;

    public String generateToken(UUID userId, String userName, Set<String> roles) {
        return Jwt.issuer(config.auth().issuer())
                .upn(userName)
                .claim("user_id", userId.toString())
                .claim("user_name", userName)
                .groups(roles)
                .expiresIn(Duration.ofSeconds(config.auth().tokenExpiration()))
                .sign();
    }
}
