package com.afet.monitoring.domain.service.report;

import com.afet.monitoring.domain.model.RiskLevel;
import com.afet.monitoring.domain.model.SeismicReport;

/**
 * Template Method — concrete renderer for {@link ReportFormat#MARKDOWN}. Same step order
 * as {@link PlainTextReportRenderer}, completely different output: Markdown headings and
 * a GitHub-style table. The contrast is the proof that the skeleton, not the subclass,
 * owns the algorithm.
 */
public class MarkdownReportRenderer extends ReportRenderer {

    @Override
    public boolean supports(ReportFormat format) {
        return format == ReportFormat.MARKDOWN;
    }

    @Override
    public String contentType() {
        return "text/markdown";
    }

    @Override
    protected void renderHeader(StringBuilder out, SeismicReport r) {
        out.append("# ").append(r.title()).append("\n\n");
        out.append("_Generated ").append(ts(r.generatedAt())).append("_\n\n");
    }

    @Override
    protected void renderSummary(StringBuilder out, SeismicReport r) {
        out.append("## Summary\n\n");
        out.append("- Earthquakes: ").append(r.totalEarthquakes()).append("\n");
        out.append("- Max magnitude: ").append(num(r.maxMagnitude())).append("\n");
        out.append("- Average magnitude: ").append(num(r.averageMagnitude())).append("\n");
        out.append("- Average depth: ").append(num(r.averageDepthKm())).append(" km\n");
        out.append("- Window: ").append(ts(r.earliest()))
                .append(" → ").append(ts(r.latest())).append("\n\n");
    }

    @Override
    protected void renderBody(StringBuilder out, SeismicReport r) {
        out.append("## Risk breakdown\n\n");
        out.append("| Level | Count |\n");
        out.append("| --- | --- |\n");
        for (RiskLevel level : RiskLevel.values()) {
            long count = r.riskBreakdown().getOrDefault(level, 0L);
            out.append("| ").append(level.name()).append(" | ").append(count).append(" |\n");
        }
        out.append("\n");
    }

    @Override
    protected void renderFooter(StringBuilder out, SeismicReport r) {
        Long id = r.strongestEarthquakeId();
        out.append("**Strongest event:** ")
                .append(id == null ? "_none_" : "#" + id)
                .append("\n");
    }

    @Override
    protected void renderEmpty(StringBuilder out, SeismicReport r) {
        out.append("# ").append(r.title()).append("\n\n");
        out.append("_Generated ").append(ts(r.generatedAt())).append("_\n\n");
        out.append("_No earthquakes on record._\n");
    }
}
