package dev.kakrizky.lightwind.storage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

/**
 * AWS S3 (and S3-compatible, e.g. MinIO) storage provider.
 * Lazily initializes the S3 client from configuration.
 */
@ApplicationScoped
public class S3StorageProvider implements StorageProvider {

    @Inject
    StorageConfig config;

    private volatile S3Client s3Client;
    private volatile S3Presigner s3Presigner;

    private S3Client getClient() {
        if (s3Client == null) {
            synchronized (this) {
                if (s3Client == null) {
                    s3Client = buildClient();
                }
            }
        }
        return s3Client;
    }

    private S3Presigner getPresigner() {
        if (s3Presigner == null) {
            synchronized (this) {
                if (s3Presigner == null) {
                    s3Presigner = buildPresigner();
                }
            }
        }
        return s3Presigner;
    }

    private S3Client buildClient() {
        S3ClientBuilder builder = S3Client.builder();

        config.s3Region().ifPresent(r -> builder.region(Region.of(r)));

        config.s3Endpoint().ifPresent(endpoint ->
                builder.endpointOverride(URI.create(endpoint))
                        .forcePathStyle(true));

        if (config.s3AccessKey().isPresent() && config.s3SecretKey().isPresent()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                            config.s3AccessKey().get(),
                            config.s3SecretKey().get()
                    )
            ));
        }

        return builder.build();
    }

    private S3Presigner buildPresigner() {
        S3Presigner.Builder builder = S3Presigner.builder();

        config.s3Region().ifPresent(r -> builder.region(Region.of(r)));

        config.s3Endpoint().ifPresent(endpoint ->
                builder.endpointOverride(URI.create(endpoint)));

        if (config.s3AccessKey().isPresent() && config.s3SecretKey().isPresent()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                            config.s3AccessKey().get(),
                            config.s3SecretKey().get()
                    )
            ));
        }

        return builder.build();
    }

    private String getBucket() {
        return config.s3Bucket()
                .orElseThrow(() -> new IllegalStateException("S3 bucket not configured (lightwind.storage.s3-bucket)"));
    }

    @Override
    public String upload(String path, InputStream data, long size, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(getBucket())
                .key(path)
                .contentType(contentType)
                .contentLength(size)
                .build();

        getClient().putObject(request, RequestBody.fromInputStream(data, size));
        return path;
    }

    @Override
    public InputStream download(String path) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(getBucket())
                .key(path)
                .build();

        return getClient().getObject(request);
    }

    @Override
    public void delete(String path) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(getBucket())
                .key(path)
                .build();

        getClient().deleteObject(request);
    }

    @Override
    public String generatePresignedUrl(String path, long expirationSeconds) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expirationSeconds))
                .getObjectRequest(req -> req
                        .bucket(getBucket())
                        .key(path))
                .build();

        return getPresigner().presignGetObject(presignRequest).url().toString();
    }

    @Override
    public boolean exists(String path) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(getBucket())
                    .key(path)
                    .build();

            getClient().headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
