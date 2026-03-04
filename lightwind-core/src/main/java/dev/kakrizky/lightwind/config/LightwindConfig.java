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

        @WithDefault("86400")
        long tokenExpiration();

        @WithDefault("lightwind")
        String issuer();
    }
}
