package dev.kakrizky.lightwind.export;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.util.List;

/**
 * Abstract REST resource for export endpoints.
 *
 * <p>Subclasses provide the data and column configuration; this base class
 * handles format negotiation, request building, and response generation.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * &#64;Path("/users")
 * public class UserExportResource extends ExportResource {
 *
 *     &#64;Inject
 *     UserService userService;
 *
 *     &#64;Override
 *     protected List&lt;?&gt; getData(UriInfo uriInfo) {
 *         return userService.findAll();
 *     }
 *
 *     &#64;Override
 *     protected ExportRequest.Builder configureExport(UriInfo uriInfo) {
 *         return ExportRequest.builder()
 *                 .addColumn("Name", "name")
 *                 .addColumn("Email", "email");
 *     }
 * }
 * </pre>
 */
public abstract class ExportResource {

    @Inject
    LightExportService exportService;

    /**
     * Provides the data to be exported. Subclasses may use query parameters
     * from the {@link UriInfo} to filter or sort the data.
     *
     * @param uriInfo the JAX-RS URI info containing query parameters
     * @return the list of objects to export
     */
    protected abstract List<?> getData(UriInfo uriInfo);

    /**
     * Configures the export columns and settings. Subclasses return a
     * pre-configured builder; the format, fileName, and title are
     * applied from query parameters automatically.
     *
     * @param uriInfo the JAX-RS URI info containing query parameters
     * @return a partially configured export request builder
     */
    protected abstract ExportRequest.Builder configureExport(UriInfo uriInfo);

    /**
     * Export endpoint. Accepts format, fileName, and title as query parameters.
     *
     * @param format   the export format (xlsx, csv, pdf)
     * @param fileName the output file name without extension
     * @param title    the document title
     * @param uriInfo  JAX-RS URI info
     * @return a binary response with the exported file
     */
    @GET
    @Path("export")
    public Response export(
            @QueryParam("format") @DefaultValue("xlsx") String format,
            @QueryParam("fileName") @DefaultValue("export") String fileName,
            @QueryParam("title") @DefaultValue("") String title,
            @Context UriInfo uriInfo
    ) {
        ExportFormat exportFormat = parseFormat(format);
        List<?> data = getData(uriInfo);

        ExportRequest.Builder builder = configureExport(uriInfo);
        builder.format(exportFormat)
                .fileName(fileName);

        if (title != null && !title.isBlank()) {
            builder.title(title);
        }

        ExportRequest request = builder.build();
        return exportService.exportAsResponse(data, request);
    }

    private ExportFormat parseFormat(String format) {
        if (format == null || format.isBlank()) {
            return ExportFormat.XLSX;
        }
        try {
            return ExportFormat.valueOf(format.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ExportFormat.XLSX;
        }
    }
}
