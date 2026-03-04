package dev.kakrizky.lightwind.export;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Exports data to CSV format.
 *
 * <p>Produces RFC 4180 compliant CSV output with UTF-8 encoding
 * and a BOM (Byte Order Mark) for Excel compatibility. Fields
 * containing commas, double quotes, or newlines are properly
 * quoted and escaped.</p>
 */
@ApplicationScoped
public class CsvExporter {

    private static final Logger LOG = Logger.getLogger(CsvExporter.class);
    private static final char SEPARATOR = ',';
    private static final char QUOTE = '"';
    private static final String LINE_END = "\r\n";
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    @Inject
    ExportConfig exportConfig;

    /**
     * Exports the given data to a CSV byte array.
     *
     * @param data    the list of objects to export
     * @param request the export configuration
     * @return the CSV file content as bytes (UTF-8 with BOM)
     */
    public byte[] export(List<?> data, ExportRequest request) {
        List<ExportRequest.ColumnDef> columns = resolveColumns(data, request);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Write UTF-8 BOM for Excel compatibility
            baos.write(UTF8_BOM);

            OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);

            // Header row
            if (request.isIncludeHeader()) {
                for (int i = 0; i < columns.size(); i++) {
                    if (i > 0) {
                        writer.write(SEPARATOR);
                    }
                    writer.write(escapeField(columns.get(i).getHeader()));
                }
                writer.write(LINE_END);
            }

            // Data rows
            int maxRows = Math.min(data.size(), exportConfig.maxRows());
            for (int i = 0; i < maxRows; i++) {
                Object item = data.get(i);

                for (int j = 0; j < columns.size(); j++) {
                    if (j > 0) {
                        writer.write(SEPARATOR);
                    }
                    ExportRequest.ColumnDef colDef = columns.get(j);
                    Object value = ExportUtil.getFieldValue(item, colDef.getField());
                    String formatted = ExportUtil.formatValue(value, colDef.getFormat(), exportConfig.dateFormat());
                    writer.write(escapeField(formatted));
                }
                writer.write(LINE_END);
            }

            writer.flush();
            LOG.debugf("CSV export completed: %d rows, %d columns", maxRows, columns.size());
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV export", e);
        }
    }

    /**
     * Escapes a field value according to RFC 4180.
     * Fields containing commas, double quotes, or newlines are enclosed in double quotes.
     * Double quotes within the field are escaped by doubling them.
     */
    private String escapeField(String value) {
        if (value == null) {
            return "";
        }

        boolean needsQuoting = value.indexOf(SEPARATOR) >= 0
                || value.indexOf(QUOTE) >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;

        if (needsQuoting) {
            return QUOTE + value.replace(String.valueOf(QUOTE), String.valueOf(QUOTE) + QUOTE) + QUOTE;
        }

        return value;
    }

    private List<ExportRequest.ColumnDef> resolveColumns(List<?> data, ExportRequest request) {
        if (request.getColumns() != null && !request.getColumns().isEmpty()) {
            return request.getColumns();
        }
        if (!data.isEmpty()) {
            return ExportUtil.extractColumns(data.get(0).getClass());
        }
        return List.of();
    }
}
