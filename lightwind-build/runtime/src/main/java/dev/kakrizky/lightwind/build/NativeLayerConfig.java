package dev.kakrizky.lightwind.build;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

/**
 * Configuration for Lightwind native image layered builds.
 *
 * <p>When enabled, the native build uses a pre-built base layer containing
 * the Lightwind framework and Quarkus runtime, reducing app build time
 * from 3-5 minutes to 15-30 seconds.</p>
 */
@ConfigMapping(prefix = "lightwind.native.layer")
public interface NativeLayerConfig {

    /**
     * Enable layered native image build.
     * When true, the build will look for a pre-built base layer
     * and compile only app-specific code on top.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Path to the pre-built base layer (.nil file).
     * If not set, the build will look in the cache directory.
     */
    Optional<String> baseLayerPath();

    /**
     * Directory for caching downloaded base layers.
     */
    @WithDefault("${user.home}/.lightwind/layers")
    String cacheDir();

    /**
     * Base layer version to use. Defaults to the current Lightwind version.
     */
    Optional<String> baseLayerVersion();

    /**
     * URL template for downloading base layers.
     * Placeholders: {version}, {os}, {arch}
     */
    @WithDefault("https://github.com/rizkyandriawan/lightwind/releases/download/v{version}/lightwind-base-{os}-{arch}.nil")
    String registryUrl();
}
