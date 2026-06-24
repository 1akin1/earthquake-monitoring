package com.afet.monitoring.domain.service.report;

import com.afet.monitoring.domain.model.RiskLevel;
import com.afet.monitoring.domain.model.SeismicReport;

/**
 * Template Method — concrete renderer for {@link ReportFormat#TEXT}. Fills in each step
 * as human-readable plain text. It owns no algorithm of its own; the step order is
 * dictated entirely by {@link ReportRenderer#render(SeismicReport)}.
 */
public class PlainTextReportRenderer extends ReportRenderer {

    @Override
    public boolean supports(ReportFormat format) {
        return format == ReportFormat.TEXT;
    }

    @Override
    public String contentType() {
        return "text/plain";
    }

    @Override
    protected void renderHeader(StringBuilder out, SeismicReport r) {
        out.append("=== ").append(r.title()).append(" ===\n");
        out.append("Generated: ").append(ts(r.generatedAt())).append("\n\n");
    }

    @Override
    protected void renderSummary(StringBuilder out, SeismicReport r) {
        out.append("Summary\n");
        out.append("  Earthquakes   : ").append(r.totalEarthquakes()).append("\n");
        out.append("  Max magnitude : ").append(num(r.maxMagnitude())).append("\n");
        out.append("  Avg magnitude : ").append(num(r.averageMagnitude())).append("\n");
        out.append("  Avg depth (km): ").append(num(r.averageDepthKm())).append("\n");
        out.append("  Window        : ").append(ts(r.earliest()))
                .append(" -> ").append(ts(r.latest())).append("\n\n");
    }

    @Override
    protected void renderBody(StringBuilder out, SeismicReport r) {
        out.append("Risk breakdown\n");
        for (RiskLevel level : RiskLevel.values()) {
            long count = r.riskBreakdown().getOrDefault(level, 0L);
            out.append("  ").append(pad(level.name())).append(": ").append(count).append("\n");
        }
        out.append("\n");
    }

    @Override
    protected void renderFooter(StringBuilder out, SeismicReport r) {
        Long id = r.strongestEarthquakeId();
        out.append("Strongest event id: ").append(id == null ? "n/a" : id).append("\n");
    }

    @Override
    protected void renderEmpty(StringBuilder out, SeismicReport r) {
        out.append("=== ").append(r.title()).append(" ===\n");
        out.append("Generated: ").append(ts(r.generatedAt())).append("\n\n");
        out.append("No earthquakes on record.\n");
    }

    /** Right-pad a level name to a fixed width so the column lines up. */
    private static String pad(String name) {
        return String.format("%-8s", name);
    }
}
