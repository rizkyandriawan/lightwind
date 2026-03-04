package dev.kakrizky.lightwind.storage;

import java.io.InputStream;

/**
 * Interface for file storage providers.
 */
public interface StorageProvider {

    /**
     * Upload a file to the storage backend.
     *
     * @param path        the destination path/key
     * @param data        the file data
     * @param size        the file size in bytes
     * @param contentType the MIME content type
     * @return the stored file path/key
     */
    String upload(String path, InputStream data, long size, String contentType);

    /**
     * Download a file from the storage backend.
     *
     * @param path the file path/key
     * @return an input stream of the file contents
     */
    InputStream download(String path);

    /**
     * Delete a file from the storage backend.
     *
     * @param path the file path/key
     */
    void delete(String path);

    /**
     * Generate a presigned URL for accessing the file.
     *
     * @param path              the file path/key
     * @param expirationSeconds how long the URL should be valid
     * @return a URL string
     */
    String generatePresignedUrl(String path, long expirationSeconds);

    /**
     * Check whether a file exists in the storage backend.
     *
     * @param path the file path/key
     * @return true if the file exists
     */
    boolean exists(String path);
}
