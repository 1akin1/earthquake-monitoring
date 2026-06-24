package com.afet.monitoring.domain.service.report;

/**
 * The output formats a {@link SeismicReport} can be rendered into. Kept in the domain
 * (not the web layer) because "which formats exist" is a domain decision; the controller
 * merely maps a query string onto one of these.
 */
public enum ReportFormat {
    TEXT,
    MARKDOWN;

    /**
     * Lenient parse for an HTTP query value. Unknown or blank input falls back to
     * {@link #TEXT} so the endpoint never 500s on a typo; case-insensitive.
     */
    public static ReportFormat fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return TEXT;
        }
        try {
            return ReportFormat.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return TEXT;
        }
    }
}
