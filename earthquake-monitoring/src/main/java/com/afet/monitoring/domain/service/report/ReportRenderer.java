package com.afet.monitoring.domain.service.report;

import com.afet.monitoring.domain.model.SeismicReport;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Template Method Pattern. {@link #render(SeismicReport)} is the fixed production
 * skeleton — it locks the <b>order</b> of the steps (header → summary → body → footer)
 * and the one structural decision (an empty report takes a different branch). Subclasses
 * never see that algorithm; they only fill in <i>how</i> each step is written.
 *
 * <p>{@code render} is {@code final} on purpose: a subclass cannot reorder the steps,
 * skip the empty-report branch, or otherwise rewrite the algorithm. That invariant is
 * the whole point of the pattern — the skeleton is closed, the steps are open.
 *
 * <p>Pure domain — no Spring, no JPA. The {@code Locale.US} on every number and the
 * UTC timestamp formatter are deliberate: on a Turkish-locale JVM
 * {@code String.format("%.2f", 4.8)} yields {@code "4,80"} (comma), which would silently
 * change the rendered output and break format-sensitive callers and tests. Pinning the
 * locale keeps the output stable on any machine.
 */
public abstract class ReportRenderer {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US)
                    .withZone(ZoneOffset.UTC);

    /**
     * The template method. Fixed skeleton; do not override (it is {@code final}).
     */
    public final String render(SeismicReport report) {
        Objects.requireNonNull(report, "report");
        StringBuilder out = new StringBuilder();
        if (report.totalEarthquakes() == 0) {
            renderEmpty(out, report);
        } else {
            renderHeader(out, report);
            renderSummary(out, report);
            renderBody(out, report);
            renderFooter(out, report);
        }
        return out.toString();
    }

    /** Does this renderer produce {@code format}? Self-selecting, like the other factories. */
    public abstract boolean supports(ReportFormat format);

    /** MIME type of the produced body, e.g. {@code "text/plain"}. A plain String keeps the domain framework-free. */
    public abstract String contentType();

    // --- primitive steps: subclasses decide HOW, the skeleton decides WHEN ---

    protected abstract void renderHeader(StringBuilder out, SeismicReport report);

    protected abstract void renderSummary(StringBuilder out, SeismicReport report);

    protected abstract void renderBody(StringBuilder out, SeismicReport report);

    protected abstract void renderFooter(StringBuilder out, SeismicReport report);

    /** The different branch: how an empty report is presented. */
    protected abstract void renderEmpty(StringBuilder out, SeismicReport report);

    // --- shared helpers, locale-pinned so output is identical on every JVM ---

    /** Two-decimal number with a dot separator, regardless of the JVM's default locale. */
    protected final String num(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    /** UTC timestamp, or an em dash for a missing instant. */
    protected final String ts(Instant when) {
        return when == null ? "—" : TIMESTAMP.format(when);
    }
}
