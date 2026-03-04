package dev.kakrizky.lightwind.export;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder-pattern class for configuring an export operation.
 *
 * <p>Use {@link ExportRequest#builder()} to create a new instance,
 * then chain configuration methods before passing it to
 * {@link LightExportService}.</p>
 */
public class ExportRequest {

    private String fileName;
    private ExportFormat format;
    private String title;
    private List<ColumnDef> columns;
    private boolean includeHeader;

    private ExportRequest() {
        this.columns = new ArrayList<>();
        this.includeHeader = true;
    }

    /**
     * Creates a new ExportRequest builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    // --- Getters ---

    public String getFileName() {
        return fileName;
    }

    public ExportFormat getFormat() {
        return format;
    }

    public String getTitle() {
        return title;
    }

    public List<ColumnDef> getColumns() {
        return columns;
    }

    public boolean isIncludeHeader() {
        return includeHeader;
    }

    /**
     * Column definition specifying header, field name, width, and format.
     */
    public static class ColumnDef {

        private String header;
        private String field;
        private int width;
        private String format;

        public ColumnDef() {
        }

        public ColumnDef(String header, String field, int width, String format) {
            this.header = header;
            this.field = field;
            this.width = width;
            this.format = format;
        }

        public String getHeader() {
            return header;
        }

        public void setHeader(String header) {
            this.header = header;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }
    }

    /**
     * Fluent builder for constructing an {@link ExportRequest}.
     */
    public static class Builder {

        private final ExportRequest request;

        private Builder() {
            this.request = new ExportRequest();
        }

        public Builder fileName(String fileName) {
            this.request.fileName = fileName;
            return this;
        }

        public Builder format(ExportFormat format) {
            this.request.format = format;
            return this;
        }

        public Builder title(String title) {
            this.request.title = title;
            return this;
        }

        public Builder columns(List<ColumnDef> columns) {
            this.request.columns = new ArrayList<>(columns);
            return this;
        }

        public Builder addColumn(String header, String field) {
            this.request.columns.add(new ColumnDef(header, field, -1, ""));
            return this;
        }

        public Builder addColumn(String header, String field, int width) {
            this.request.columns.add(new ColumnDef(header, field, width, ""));
            return this;
        }

        public Builder addColumn(String header, String field, int width, String format) {
            this.request.columns.add(new ColumnDef(header, field, width, format));
            return this;
        }

        public Builder includeHeader(boolean includeHeader) {
            this.request.includeHeader = includeHeader;
            return this;
        }

        public ExportRequest build() {
            if (this.request.format == null) {
                throw new IllegalStateException("Export format is required");
            }
            if (this.request.fileName == null || this.request.fileName.isBlank()) {
                this.request.fileName = "export";
            }
            return this.request;
        }
    }
}
