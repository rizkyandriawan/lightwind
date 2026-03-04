package dev.kakrizky.lightwind.export;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Facade service for data export operations.
 *
 * <p>Delegates to the appropriate format-specific exporter based on the
 * {@link ExportRequest#getFormat()} and provides convenience methods
 * for returning JAX-RS {@link Response} objects with correct headers.</p>
 */
@ApplicationScoped
public class LightExportService {

    private static final Logger LOG = Logger.getLogger(LightExportService.class);

    private static final String CONTENT_TYPE_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String CONTENT_TYPE_CSV = "text/csv";
    private static final String CONTENT_TYPE_PDF = "application/pdf";

    @Inject
    ExcelExporter excelExporter;

    @Inject
    CsvExporter csvExporter;

    @Inject
    PdfExporter pdfExporter;

    /**
     * Exports data to the specified format and returns the raw bytes.
     *
     * @param data    the list of objects to export
     * @param request the export configuration
     * @return the exported file content as bytes
     */
    public byte[] export(List<?> data, ExportRequest request) {
        LOG.debugf("Starting export: format=%s, rows=%d, fileName=%s",
                request.getFormat(), data.size(), request.getFileName());

        return switch (request.getFormat()) {
            case XLSX -> excelExporter.export(data, request);
            case CSV -> csvExporter.export(data, request);
            case PDF -> pdfExporter.export(data, request);
        };
    }

    /**
     * Exports data and wraps the result in a JAX-RS {@link Response} with
     * the correct Content-Type and Content-Disposition headers.
     *
     * @param data    the list of objects to export
     * @param request the export configuration
     * @return a JAX-RS Response ready to be returned from a REST endpoint
     */
    public Response exportAsResponse(List<?> data, ExportRequest request) {
        byte[] content = export(data, request);

        String contentType = getContentType(request.getFormat());
        String extension = getExtension(request.getFormat());
        String fileName = request.getFileName() + "." + extension;

        return Response.ok(content, contentType)
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .header("Content-Length", content.length)
                .build();
    }

    private String getContentType(ExportFormat format) {
        return switch (format) {
            case XLSX -> CONTENT_TYPE_XLSX;
            case CSV -> CONTENT_TYPE_CSV;
            case PDF -> CONTENT_TYPE_PDF;
        };
    }

    private String getExtension(ExportFormat format) {
        return switch (format) {
            case XLSX -> "xlsx";
            case CSV -> "csv";
            case PDF -> "pdf";
        };
    }
}
