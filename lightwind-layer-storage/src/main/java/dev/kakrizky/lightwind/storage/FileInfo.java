package dev.kakrizky.lightwind.storage;

/**
 * DTO representing metadata about an uploaded file.
 */
public record FileInfo(
        String path,
        String fileName,
        String contentType,
        long size,
        String url
) {
}
