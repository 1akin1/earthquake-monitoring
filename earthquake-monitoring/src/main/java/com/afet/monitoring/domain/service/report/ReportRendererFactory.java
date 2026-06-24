package com.afet.monitoring.domain.service.report;

import java.util.List;

/**
 * Picks the renderer for a requested {@link ReportFormat}. Same self-selecting design as
 * {@code DisasterHandlerFactory} and {@code RiskScoringService}: the factory holds every
 * renderer and asks each whether it {@code supports} the format, so adding a new format
 * (PDF, HTML, …) is a new {@link ReportRenderer} subclass plus one bean line — this class
 * never changes (Open/Closed).
 */
public class ReportRendererFactory {

    private final List<ReportRenderer> renderers;

    public ReportRendererFactory(List<ReportRenderer> renderers) {
        this.renderers = List.copyOf(renderers);
    }

    public ReportRenderer forFormat(ReportFormat format) {
        return renderers.stream()
                .filter(r -> r.supports(format))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No renderer registered for format: " + format));
    }
}
