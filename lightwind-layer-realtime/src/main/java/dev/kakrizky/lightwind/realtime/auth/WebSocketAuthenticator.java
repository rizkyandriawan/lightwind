package dev.kakrizky.lightwind.realtime.auth;

import dev.kakrizky.lightwind.exception.UnauthorizedException;
import io.smallrye.jwt.auth.principal.JWTParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
public class WebSocketAuthenticator {

    private static final Logger LOG = Logger.getLogger(WebSocketAuthenticator.class);

    @Inject
    JWTParser jwtParser;

    public UUID authenticate(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Authentication required for WebSocket connection");
        }

        String rawToken = token.startsWith("Bearer ") ? token.substring(7) : token;

        try {
            JsonWebToken jwt = jwtParser.parse(rawToken);
            String userId = jwt.getClaim("user_id");
            if (userId == null) {
                // Fall back to "sub" claim
                userId = jwt.getSubject();
            }
            if (userId == null) {
                throw new UnauthorizedException("Token missing user_id claim");
            }
            return UUID.fromString(userId);
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            LOG.debugf("WebSocket authentication failed: %s", e.getMessage());
            throw new UnauthorizedException("Invalid or expired token");
        }
    }
}
