package dev.kakrizky.lightwind.storage;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "lightwind.storage")
public interface StorageConfig {

    /**
     * Storage provider: "local" or "s3".
     */
    @WithDefault("local")
    String provider();

    /**
     * Base path for local file storage.
     */
    @WithDefault("./uploads")
    String localBasePath();

    /**
     * S3 bucket name.
     */
    Optional<String> s3Bucket();

    /**
     * S3 region (e.g. "us-east-1").
     */
    Optional<String> s3Region();

    /**
     * Custom S3 endpoint URL (for MinIO or other S3-compatible services).
     */
    Optional<String> s3Endpoint();

    /**
     * S3 access key (static credentials).
     */
    Optional<String> s3AccessKey();

    /**
     * S3 secret key (static credentials).
     */
    Optional<String> s3SecretKey();

    /**
     * Maximum file size in bytes (default: 10MB).
     */
    @WithDefault("10485760")
    long maxFileSize();

    /**
     * Allowed MIME types. Empty list means all types are allowed.
     */
    @WithDefault("")
    List<String> allowedTypes();
}
