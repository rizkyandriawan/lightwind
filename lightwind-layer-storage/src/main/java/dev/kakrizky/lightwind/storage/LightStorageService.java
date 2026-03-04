package dev.kakrizky.lightwind.storage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Facade service for file storage operations.
 * Validates file constraints and delegates to the configured storage provider.
 */
@ApplicationScoped
public class LightStorageService {

    @Inject
    StorageConfig config;

    @Inject
    LocalStorageProvider localProvider;

    @Inject
    S3StorageProvider s3Provider;

    /**
     * Upload a file to the configured storage provider.
     *
     * @param directory   the target directory/prefix
     * @param fileName    the original file name
     * @param data        the file data stream
     * @param size        the file size in bytes
     * @param contentType the MIME content type
     * @return file metadata
     */
    public FileInfo upload(String directory, String fileName, InputStream data,
                           long size, String contentType) {
        validateFileSize(size);
        validateContentType(contentType);

        String uniqueName = generateUniqueName(fileName);
        String path = directory.endsWith("/")
                ? directory + uniqueName
                : directory + "/" + uniqueName;

        StorageProvider provider = getProvider();
        String storedPath = provider.upload(path, data, size, contentType);
        String url = provider.generatePresignedUrl(storedPath, 3600);

        return new FileInfo(storedPath, uniqueName, contentType, size, url);
    }

    /**
     * Download a file from the configured storage provider.
     *
     * @param path the file path/key
     * @return an input stream of the file contents
     */
    public InputStream download(String path) {
        return getProvider().download(path);
    }

    /**
     * Delete a file from the configured storage provider.
     *
     * @param path the file path/key
     */
    public void delete(String path) {
        getProvider().delete(path);
    }

    /**
     * Generate a presigned/direct URL for accessing a file.
     *
     * @param path              the file path/key
     * @param expirationSeconds how long the URL should be valid
     * @return a URL string
     */
    public String getPresignedUrl(String path, long expirationSeconds) {
        return getProvider().generatePresignedUrl(path, expirationSeconds);
    }

    private StorageProvider getProvider() {
        String provider = config.provider();
        if ("s3".equalsIgnoreCase(provider)) {
            return s3Provider;
        }
        return localProvider;
    }

    private void validateFileSize(long size) {
        long maxSize = config.maxFileSize();
        if (size > maxSize) {
            throw new RuntimeException(
                    "File size " + size + " bytes exceeds maximum allowed size of " + maxSize + " bytes");
        }
    }

    private void validateContentType(String contentType) {
        List<String> allowedTypes = config.allowedTypes();
        if (allowedTypes == null || allowedTypes.isEmpty()) {
            return;
        }
        // Filter out empty strings from the default empty list
        List<String> filtered = allowedTypes.stream()
                .filter(t -> t != null && !t.isBlank())
                .toList();
        if (filtered.isEmpty()) {
            return;
        }
        if (!filtered.contains(contentType)) {
            throw new RuntimeException(
                    "Content type '" + contentType + "' is not allowed. Allowed types: " + filtered);
        }
    }

    private String generateUniqueName(String originalName) {
        String uuid = UUID.randomUUID().toString();
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            String extension = originalName.substring(dotIndex);
            String baseName = originalName.substring(0, dotIndex);
            return uuid + "_" + baseName + extension;
        }
        return uuid + "_" + originalName;
    }
}
