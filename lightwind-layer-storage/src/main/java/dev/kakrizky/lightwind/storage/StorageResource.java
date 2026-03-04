package dev.kakrizky.lightwind.storage;

import dev.kakrizky.lightwind.response.LightResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;

/**
 * Abstract REST resource for file storage operations.
 * Extend this class and annotate with @Path to expose storage endpoints.
 *
 * <pre>
 * &#64;Path("/files")
 * public class FileResource extends StorageResource {
 *     // inherits POST /, GET /{path}, DELETE /{path}
 * }
 * </pre>
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public abstract class StorageResource {

    @Inject
    LightStorageService storageService;

    /**
     * Override to specify the storage directory for uploads.
     */
    protected String getUploadDirectory() {
        return "uploads";
    }

    /**
     * Upload a file via multipart form.
     * Subclasses should override and annotate with @Consumes(MediaType.MULTIPART_FORM_DATA)
     * to handle the multipart parsing specific to their framework version.
     *
     * @param fileName    the original file name
     * @param data        the file input stream
     * @param size        the file size in bytes
     * @param contentType the MIME content type
     * @return file metadata
     */
    protected LightResponse<FileInfo> doUpload(String fileName, InputStream data,
                                                long size, String contentType) {
        FileInfo info = storageService.upload(getUploadDirectory(), fileName, data, size, contentType);
        return LightResponse.ok(info);
    }

    /**
     * Download a file by path.
     */
    @GET
    @Path("{path: .+}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response download(@PathParam("path") String path) {
        InputStream stream = storageService.download(path);
        return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + extractFileName(path) + "\"")
                .build();
    }

    /**
     * Delete a file by path.
     */
    @DELETE
    @Path("{path: .+}")
    public LightResponse<Void> delete(@PathParam("path") String path) {
        storageService.delete(path);
        return LightResponse.ok(null);
    }

    private String extractFileName(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
}
