package com.afet.monitoring.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Builder Pattern — verifies the report builder accumulates earthquakes correctly and
 * produces an immutable, fully-populated summary, including the empty-report edge case.
 */
@DisplayName("SeismicReport.Builder aggregates earthquakes into an immutable summary")
class SeismicReportBuilderTest {

    private static Earthquake quake(Long id, double magnitude, double depthKm,
                                    Instant occurredAt, RiskLevel level) {
        Earthquake e = Earthquake.reconstitute(
                id, new Magnitude(magnitude), depthKm,
                new GeoLocation(40.0, 29.0), "TEST", occurredAt, null);
        return level == null ? e : e.assessedWith(new RiskScore(50.0, level));
    }

    @Test
    @DisplayName("an empty report has zeroed stats and null time window")
    void empty_report() {
        SeismicReport report = SeismicReport.builder()
                .title("Empty")
                .generatedAt(Instant.parse("2026-06-21T00:00:00Z"))
                .build();

        assertThat(report.totalEarthquakes()).isZero();
        assertThat(report.maxMagnitude()).isZero();
        assertThat(report.averageMagnitude()).isZero();
        assertThat(report.averageDepthKm()).isZero();
        assertThat(report.earliest()).isNull();
        assertThat(report.latest()).isNull();
        assertThat(report.strongestEarthquakeId()).isNull();
        assertThat(report.riskBreakdown())
                .containsEntry(RiskLevel.LOW, 0L)
                .containsEntry(RiskLevel.CRITICAL, 0L);
    }

    @Test
    @DisplayName("multiple earthquakes are aggregated into correct stats")
    void aggregates_many() {
        List<Earthquake> quakes = List.of(
                quake(1L, 3.0, 10, Instant.parse("2026-06-20T00:00:00Z"), RiskLevel.LOW),
                quake(2L, 6.4, 12, Instant.parse("2026-06-20T08:00:00Z"), RiskLevel.HIGH),
                quake(3L, 5.0, 20, Instant.parse("2026-06-19T00:00:00Z"), RiskLevel.MEDIUM));

        SeismicReport report = SeismicReport.builder()
                .generatedAt(Instant.parse("2026-06-21T00:00:00Z"))
                .addAll(quakes)
                .build();

        assertThat(report.totalEarthquakes()).isEqualTo(3);
        assertThat(report.maxMagnitude()).isCloseTo(6.4, within(1e-9));
        assertThat(report.averageMagnitude()).isCloseTo(4.8, within(1e-9));   // (3.0+6.4+5.0)/3
        assertThat(report.averageDepthKm()).isCloseTo(14.0, within(1e-9));    // (10+12+20)/3
        assertThat(report.riskBreakdown())
                .containsEntry(RiskLevel.LOW, 1L)
                .containsEntry(RiskLevel.MEDIUM, 1L)
                .containsEntry(RiskLevel.HIGH, 1L)
                .containsEntry(RiskLevel.CRITICAL, 0L);
        assertThat(report.earliest()).isEqualTo(Instant.parse("2026-06-19T00:00:00Z"));
        assertThat(report.latest()).isEqualTo(Instant.parse("2026-06-20T08:00:00Z"));
        assertThat(report.strongestEarthquakeId()).isEqualTo(2L); // the M6.4 event
    }

    @Test
    @DisplayName("unscored earthquakes count in totals but not in the risk breakdown")
    void unscored_excluded_from_breakdown() {
        SeismicReport report = SeismicReport.builder()
                .generatedAt(Instant.parse("2026-06-21T00:00:00Z"))
                .addEarthquake(quake(1L, 6.0, 10, Instant.parse("2026-06-20T00:00:00Z"), RiskLevel.HIGH))
                .addEarthquake(quake(2L, 4.0, 10, Instant.parse("2026-06-20T01:00:00Z"), null)) // unscored
                .build();

        assertThat(report.totalEarthquakes()).isEqualTo(2);
        assertThat(report.riskBreakdown())
                .containsEntry(RiskLevel.HIGH, 1L)
                .containsEntry(RiskLevel.MEDIUM, 0L);
    }

    @Test
    @DisplayName("title and generatedAt fall back to defaults when not set")
    void defaults_applied() {
        SeismicReport report = SeismicReport.builder().build();

        assertThat(report.title()).isEqualTo("Seismic Report");
        assertThat(report.generatedAt()).isNotNull();
    }

    @Test
    @DisplayName("the risk breakdown map is immutable")
    void breakdown_is_immutable() {
        SeismicReport report = SeismicReport.builder().build();

        assertThatThrownBy(() -> report.riskBreakdown().put(RiskLevel.LOW, 99L))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
