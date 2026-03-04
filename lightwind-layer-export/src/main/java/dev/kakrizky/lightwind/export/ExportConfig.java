package dev.kakrizky.lightwind.export;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration for the Lightwind export layer.
 *
 * <p>All properties live under the {@code lightwind.export} prefix.</p>
 */
@ConfigMapping(prefix = "lightwind.export")
public interface ExportConfig {

    /**
     * Maximum number of rows allowed in a single export.
     */
    @WithDefault("10000")
    int maxRows();

    /**
     * Temporary directory for export file generation.
     */
    @WithDefault("/tmp/lightwind-export")
    String tempDir();

    /**
     * Default date format used when formatting date values.
     */
    @WithDefault("yyyy-MM-dd HH:mm:ss")
    String dateFormat();

    /**
     * Default font name used in PDF and Excel exports.
     */
    @WithDefault("Helvetica")
    String defaultFontName();
}
