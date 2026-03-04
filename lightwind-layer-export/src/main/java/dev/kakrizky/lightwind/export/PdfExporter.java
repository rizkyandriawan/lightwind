package dev.kakrizky.lightwind.export;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Exports data to PDF format using OpenPDF.
 *
 * <p>Produces a PDF document with a title header on the first page,
 * a data table with alternating row colors, and page numbers in the
 * footer. Automatically switches to landscape orientation when the
 * number of columns exceeds a threshold.</p>
 */
@ApplicationScoped
public class PdfExporter {

    private static final Logger LOG = Logger.getLogger(PdfExporter.class);
    private static final int LANDSCAPE_COLUMN_THRESHOLD = 6;
    private static final Color HEADER_BG = new Color(66, 66, 66);
    private static final Color HEADER_FG = Color.WHITE;
    private static final Color ALT_ROW_BG = new Color(245, 245, 245);

    @Inject
    ExportConfig exportConfig;

    /**
     * Exports the given data to a PDF byte array.
     *
     * @param data    the list of objects to export
     * @param request the export configuration
     * @return the PDF file content as bytes
     */
    public byte[] export(List<?> data, ExportRequest request) {
        List<ExportRequest.ColumnDef> columns = resolveColumns(data, request);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Use landscape for many columns
        Rectangle pageSize = columns.size() > LANDSCAPE_COLUMN_THRESHOLD
                ? PageSize.A4.rotate() : PageSize.A4;

        Document document = new Document(pageSize, 36, 36, 54, 54);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new FooterPageEvent(exportConfig.defaultFontName()));
            document.open();

            // Title
            if (request.getTitle() != null && !request.getTitle().isBlank()) {
                Font titleFont = FontFactory.getFont(
                        exportConfig.defaultFontName(), 18, Font.BOLD, new Color(33, 33, 33));
                Paragraph titlePara = new Paragraph(request.getTitle(), titleFont);
                titlePara.setAlignment(Element.ALIGN_CENTER);
                titlePara.setSpacingAfter(20);
                document.add(titlePara);
            }

            // Table
            if (!columns.isEmpty()) {
                PdfPTable table = new PdfPTable(columns.size());
                table.setWidthPercentage(100);
                table.setSpacingBefore(10);

                // Calculate column widths
                float[] widths = new float[columns.size()];
                for (int i = 0; i < columns.size(); i++) {
                    ExportRequest.ColumnDef colDef = columns.get(i);
                    widths[i] = colDef.getWidth() > 0 ? colDef.getWidth() : 1;
                }
                table.setWidths(widths);

                // Header row
                if (request.isIncludeHeader()) {
                    Font headerFont = FontFactory.getFont(
                            exportConfig.defaultFontName(), 10, Font.BOLD, HEADER_FG);

                    for (ExportRequest.ColumnDef colDef : columns) {
                        PdfPCell cell = new PdfPCell(new Phrase(colDef.getHeader(), headerFont));
                        cell.setBackgroundColor(HEADER_BG);
                        cell.setPadding(6);
                        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        table.addCell(cell);
                    }
                }

                // Data rows
                Font dataFont = FontFactory.getFont(
                        exportConfig.defaultFontName(), 9, Font.NORMAL, new Color(33, 33, 33));

                int maxRows = Math.min(data.size(), exportConfig.maxRows());
                for (int i = 0; i < maxRows; i++) {
                    Object item = data.get(i);
                    boolean altRow = (i % 2 == 1);

                    for (ExportRequest.ColumnDef colDef : columns) {
                        Object value = ExportUtil.getFieldValue(item, colDef.getField());
                        String formatted = ExportUtil.formatValue(value, colDef.getFormat(), exportConfig.dateFormat());

                        PdfPCell cell = new PdfPCell(new Phrase(formatted, dataFont));
                        cell.setPadding(5);
                        if (altRow) {
                            cell.setBackgroundColor(ALT_ROW_BG);
                        }
                        table.addCell(cell);
                    }
                }

                document.add(table);
            }

            document.close();
            LOG.debugf("PDF export completed: %d rows, %d columns", data.size(), columns.size());
            return baos.toByteArray();

        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF export", e);
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

    /**
     * Page event handler that adds page numbers to the footer of each page.
     */
    private static class FooterPageEvent extends PdfPageEventHelper {

        private final String fontName;

        FooterPageEvent(String fontName) {
            this.fontName = fontName;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Font footerFont = FontFactory.getFont(fontName, 8, Font.NORMAL, Color.GRAY);
            Phrase footer = new Phrase("Page " + writer.getPageNumber(), footerFont);

            PdfPTable footerTable = new PdfPTable(1);
            footerTable.setTotalWidth(document.getPageSize().getWidth() - 72);
            PdfPCell cell = new PdfPCell(footer);
            cell.setBorder(Rectangle.TOP);
            cell.setBorderColor(Color.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPaddingTop(4);
            footerTable.addCell(cell);

            footerTable.writeSelectedRows(0, -1,
                    36, 42,
                    writer.getDirectContent());
        }
    }
}
