package dev.kakrizky.lightwind.export;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jboss.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * Exports data to Excel XLSX format using Apache POI.
 *
 * <p>Produces a workbook with a single sheet containing an optional
 * header row (bold, with auto-filter) followed by data rows. Dates
 * and numbers are formatted according to column definitions.</p>
 */
@ApplicationScoped
public class ExcelExporter {

    private static final Logger LOG = Logger.getLogger(ExcelExporter.class);

    @Inject
    ExportConfig exportConfig;

    /**
     * Exports the given data to an XLSX byte array.
     *
     * @param data    the list of objects to export
     * @param request the export configuration
     * @return the XLSX file content as bytes
     */
    public byte[] export(List<?> data, ExportRequest request) {
        List<ExportRequest.ColumnDef> columns = resolveColumns(data, request);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            String sheetName = request.getTitle() != null ? request.getTitle() : "Export";
            // Sheet name max 31 chars, no special characters
            if (sheetName.length() > 31) {
                sheetName = sheetName.substring(0, 31);
            }
            Sheet sheet = workbook.createSheet(sheetName);

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontName(exportConfig.defaultFontName());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // Date style
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper creationHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(creationHelper.createDataFormat().getFormat(exportConfig.dateFormat()));

            int rowIndex = 0;

            // Header row
            if (request.isIncludeHeader()) {
                Row headerRow = sheet.createRow(rowIndex++);
                for (int i = 0; i < columns.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns.get(i).getHeader());
                    cell.setCellStyle(headerStyle);
                }

                // Auto-filter
                if (!columns.isEmpty()) {
                    sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                            0, 0, 0, columns.size() - 1));
                }
            }

            // Data rows
            int maxRows = Math.min(data.size(), exportConfig.maxRows());
            for (int i = 0; i < maxRows; i++) {
                Object item = data.get(i);
                Row row = sheet.createRow(rowIndex++);

                for (int j = 0; j < columns.size(); j++) {
                    ExportRequest.ColumnDef colDef = columns.get(j);
                    Cell cell = row.createCell(j);
                    Object value = ExportUtil.getFieldValue(item, colDef.getField());
                    setCellValue(cell, value, colDef, dateStyle, workbook);
                }
            }

            // Auto-size columns
            for (int i = 0; i < columns.size(); i++) {
                ExportRequest.ColumnDef colDef = columns.get(i);
                if (colDef.getWidth() > 0) {
                    sheet.setColumnWidth(i, colDef.getWidth() * 256);
                } else {
                    sheet.autoSizeColumn(i);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            LOG.debugf("Excel export completed: %d rows, %d columns", maxRows, columns.size());
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel export", e);
        }
    }

    private void setCellValue(Cell cell, Object value, ExportRequest.ColumnDef colDef,
                              CellStyle dateStyle, Workbook workbook) {
        if (value == null) {
            cell.setBlank();
            return;
        }

        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
            if (colDef.getFormat() != null && !colDef.getFormat().isBlank()) {
                CellStyle numStyle = workbook.createCellStyle();
                CreationHelper helper = workbook.getCreationHelper();
                numStyle.setDataFormat(helper.createDataFormat().getFormat(colDef.getFormat()));
                cell.setCellStyle(numStyle);
            }
        } else if (value instanceof LocalDateTime) {
            cell.setCellValue(java.sql.Timestamp.valueOf((LocalDateTime) value));
            cell.setCellStyle(dateStyle);
        } else if (value instanceof LocalDate) {
            cell.setCellValue(java.sql.Date.valueOf((LocalDate) value));
            CellStyle localDateStyle = workbook.createCellStyle();
            CreationHelper helper = workbook.getCreationHelper();
            String pattern = (colDef.getFormat() != null && !colDef.getFormat().isBlank())
                    ? colDef.getFormat() : "yyyy-MM-dd";
            localDateStyle.setDataFormat(helper.createDataFormat().getFormat(pattern));
            cell.setCellStyle(localDateStyle);
        } else if (value instanceof Date) {
            cell.setCellValue((Date) value);
            cell.setCellStyle(dateStyle);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else {
            cell.setCellValue(String.valueOf(value));
        }
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
