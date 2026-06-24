package com.afet.monitoring.domain.service.report;

import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.model.GeoLocation;
import com.afet.monitoring.domain.model.Magnitude;
import com.afet.monitoring.domain.model.RiskLevel;
import com.afet.monitoring.domain.model.RiskScore;
import com.afet.monitoring.domain.model.SeismicReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Template Method Pattern — proves the skeleton, not the subclass, owns the algorithm:
 * a probe renderer records which steps run and in what order, and the two real renderers
 * are checked for correct, locale-stable output (including the empty-report branch).
 */
@DisplayName("ReportRenderer locks the step skeleton; subclasses fill the steps")
class ReportRendererTest {

    private static Earthquake quake(Long id, double magnitude, double depthKm,
                                    Instant occurredAt, RiskLevel level) {
        Earthquake e = Earthquake.reconstitute(
                id, new Magnitude(magnitude), depthKm,
                new GeoLocation(40.0, 29.0), "TEST", occurredAt, null);
        return level == null ? e : e.assessedWith(new RiskScore(50.0, level));
    }

    private static SeismicReport populatedReport() {
        return SeismicReport.builder()
                .title("Probe report")
                .generatedAt(Instant.parse("2026-06-21T00:00:00Z"))
                .addEarthquake(quake(1L, 3.0, 10, Instant.parse("2026-06-20T00:00:00Z"), RiskLevel.LOW))
                .addEarthquake(quake(2L, 6.4, 12, Instant.parse("2026-06-20T08:00:00Z"), RiskLevel.HIGH))
                .addEarthquake(quake(3L, 5.0, 20, Instant.parse("2026-06-19T00:00:00Z"), RiskLevel.MEDIUM))
                .build();
    }

    private static SeismicReport emptyReport() {
        return SeismicReport.builder()
                .title("Empty")
                .generatedAt(Instant.parse("2026-06-21T00:00:00Z"))
                .build();
    }

    /** Records each step it is asked to perform, so the test can assert the order. */
    private static final class ProbeRenderer extends ReportRenderer {
        final List<String> calls = new ArrayList<>();
        @Override public boolean supports(ReportFormat format) { return false; }
        @Override public String contentType() { return "text/plain"; }
        @Override protected void renderHeader(StringBuilder o, SeismicReport r)  { calls.add("header"); }
        @Override protected void renderSummary(StringBuilder o, SeismicReport r) { calls.add("summary"); }
        @Override protected void renderBody(StringBuilder o, SeismicReport r)    { calls.add("body"); }
        @Override protected void renderFooter(StringBuilder o, SeismicReport r)  { calls.add("footer"); }
        @Override protected void renderEmpty(StringBuilder o, SeismicReport r)   { calls.add("empty"); }
    }

    @Test
    @DisplayName("a populated report runs header → summary → body → footer, in that order")
    void skeleton_order_for_populated_report() {
        ProbeRenderer probe = new ProbeRenderer();

        probe.render(populatedReport());

        assertThat(probe.calls).containsExactly("header", "summary", "body", "footer");
    }

    @Test
    @DisplayName("an empty report takes the different branch — only renderEmpty runs")
    void skeleton_takes_empty_branch() {
        ProbeRenderer probe = new ProbeRenderer();

        probe.render(emptyReport());

        assertThat(probe.calls).containsExactly("empty");
    }

    @Test
    @DisplayName("plain-text renderer formats numbers with a dot even on a comma locale")
    void plain_text_uses_dot_decimal_under_turkish_locale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR")); // the trap: "%.2f" would give "4,80"
            String out = new PlainTextReportRenderer().render(populatedReport());

            assertThat(out)
                    .contains("=== Probe report ===")
                    .contains("Earthquakes   : 3")
                    .contains("Max magnitude : 6.40")
                    .contains("Avg magnitude : 4.80")   // dot, not comma
                    .contains("HIGH    : 1")
                    .contains("Strongest event id: 2")
                    .doesNotContain("4,80");
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    @DisplayName("markdown renderer emits headings and a GitHub table")
    void markdown_structure() {
        String out = new MarkdownReportRenderer().render(populatedReport());

        assertThat(out)
                .contains("# Probe report")
                .contains("## Summary")
                .contains("## Risk breakdown")
                .contains("| Level | Count |")
                .contains("| HIGH | 1 |")
                .contains("- Average magnitude: 4.80")
                .contains("**Strongest event:** #2");
    }

    @Test
    @DisplayName("empty report renders a clear placeholder in each format")
    void empty_report_placeholder() {
        assertThat(new PlainTextReportRenderer().render(emptyReport()))
                .contains("No earthquakes on record.");
        assertThat(new MarkdownReportRenderer().render(emptyReport()))
                .contains("_No earthquakes on record._");
    }

    @Test
    @DisplayName("factory self-selects the renderer for each format")
    void factory_selects_by_format() {
        ReportRendererFactory factory = new ReportRendererFactory(
                List.of(new PlainTextReportRenderer(), new MarkdownReportRenderer()));

        assertThat(factory.forFormat(ReportFormat.TEXT))
                .isInstanceOf(PlainTextReportRenderer.class);
        assertThat(factory.forFormat(ReportFormat.MARKDOWN))
                .isInstanceOf(MarkdownReportRenderer.class);
    }

    @Test
    @DisplayName("unknown format strings fall back to TEXT")
    void format_parsing_falls_back() {
        assertThat(ReportFormat.fromString(null)).isEqualTo(ReportFormat.TEXT);
        assertThat(ReportFormat.fromString("")).isEqualTo(ReportFormat.TEXT);
        assertThat(ReportFormat.fromString("nonsense")).isEqualTo(ReportFormat.TEXT);
        assertThat(ReportFormat.fromString("MarkDown")).isEqualTo(ReportFormat.MARKDOWN);
    }
}
