package dev.kakrizky.lightwind.email;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

/**
 * Configuration for the Lightwind email layer.
 *
 * <p>All properties live under the {@code lightwind.email} prefix.</p>
 */
@ConfigMapping(prefix = "lightwind.email")
public interface EmailConfig {

    @WithDefault("noreply@localhost")
    String fromAddress();

    @WithDefault("Lightwind App")
    String fromName();

    @WithDefault("true")
    boolean asyncEnabled();

    @WithDefault("emails/")
    Optional<String> templatePath();
}
