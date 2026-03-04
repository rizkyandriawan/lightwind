package dev.kakrizky.lightwind.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

@ConfigMapping(prefix = "lightwind")
public interface LightwindConfig {

    @WithDefault("false")
    boolean devMode();

    AuthConfig auth();

    interface AuthConfig {

        @WithDefault("false")
        boolean bypassEnabled();

        /**
         * Access token expiration in seconds (default: 15 minutes).
         */
        @WithDefault("900")
        long tokenExpiration();

        /**
         * Refresh token expiration in seconds (default: 30 days).
         */
        @WithDefault("2592000")
        long refreshTokenExpiration();

        @WithDefault("lightwind")
        String issuer();
    }
}
