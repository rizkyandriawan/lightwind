package dev.kakrizky.lightwind.storage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Local filesystem storage provider.
 * Stores files under the configured base path.
 */
@ApplicationScoped
public class LocalStorageProvider implements StorageProvider {

    @Inject
    StorageConfig config;

    @Override
    public String upload(String path, InputStream data, long size, String contentType) {
        try {
            Path filePath = resolveFilePath(path);
            Files.createDirectories(filePath.getParent());
            Files.copy(data, filePath, StandardCopyOption.REPLACE_EXISTING);
            return path;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to local storage: " + path, e);
        }
    }

    @Override
    public InputStream download(String path) {
        try {
            Path filePath = resolveFilePath(path);
            if (!Files.exists(filePath)) {
                throw new RuntimeException("File not found: " + path);
            }
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to download file from local storage: " + path, e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            Path filePath = resolveFilePath(path);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file from local storage: " + path, e);
        }
    }

    @Override
    public String generatePresignedUrl(String path, long expirationSeconds) {
        // In local/dev mode, just return the file path directly
        return resolveFilePath(path).toString();
    }

    @Override
    public boolean exists(String path) {
        return Files.exists(resolveFilePath(path));
    }

    private Path resolveFilePath(String path) {
        return Path.of(config.localBasePath()).resolve(path);
    }
}
