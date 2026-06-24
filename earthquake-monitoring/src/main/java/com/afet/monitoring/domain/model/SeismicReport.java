package com.afet.monitoring.domain.model;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builder Pattern. An immutable summary of a set of earthquakes — many fields, several
 * of them derived (averages, time window, strongest event), which is exactly when a
 * telescoping constructor becomes unreadable and a Builder pays off (Effective Java,
 * Item 2).
 *
 * <p>The {@link Builder} here does real work: callers feed it earthquakes one by one
 * (or in bulk) and it accumulates the running totals; {@link Builder#build()} then
 * finalises the averages and produces this immutable, validated object. So the mutable
 * assembly state stays in the Builder and never leaks into the finished report.
 *
 * <p>Pure domain — no Spring, no JPA.
 */
public final class SeismicReport {

    private final String title;
    private final Instant generatedAt;
    private final int totalEarthquakes;
    private final double maxMagnitude;
    private final double averageMagnitude;
    private final double averageDepthKm;
    private final Map<RiskLevel, Long> riskBreakdown; // immutable, every level present
    private final Instant earliest;                   // null when the report is empty
    private final Instant latest;                     // null when the report is empty
    private final Long strongestEarthquakeId;         // null when empty or unsaved

    private SeismicReport(Builder b) {
        if (b.count < 0) {
            throw new IllegalStateException("count cannot be negative");
        }
        this.title = (b.title == null || b.title.isBlank()) ? "Seismic Report" : b.title;
        this.generatedAt = b.generatedAt == null ? Instant.now() : b.generatedAt;
        this.totalEarthquakes = b.count;
        this.maxMagnitude = b.count == 0 ? 0.0 : b.maxMagnitude;
        this.averageMagnitude = b.count == 0 ? 0.0 : b.sumMagnitude / b.count;
        this.averageDepthKm = b.count == 0 ? 0.0 : b.sumDepthKm / b.count;
        this.riskBreakdown = Map.copyOf(b.breakdown);
        this.earliest = b.earliest;
        this.latest = b.latest;
        this.strongestEarthquakeId = b.strongestId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String title() { return title; }
    public Instant generatedAt() { return generatedAt; }
    public int totalEarthquakes() { return totalEarthquakes; }
    public double maxMagnitude() { return maxMagnitude; }
    public double averageMagnitude() { return averageMagnitude; }
    public double averageDepthKm() { return averageDepthKm; }
    public Map<RiskLevel, Long> riskBreakdown() { return riskBreakdown; }
    public Instant earliest() { return earliest; }
    public Instant latest() { return latest; }
    public Long strongestEarthquakeId() { return strongestEarthquakeId; }

    /** Fluent, accumulating builder. Not thread-safe (build one report per builder). */
    public static final class Builder {

        private String title;
        private Instant generatedAt;

        private int count;
        private double maxMagnitude;
        private double sumMagnitude;
        private double sumDepthKm;
        private final Map<RiskLevel, Long> breakdown = new EnumMap<>(RiskLevel.class);
        private Instant earliest;
        private Instant latest;
        private double strongestMagnitude = -1.0;
        private Long strongestId;

        private Builder() {
            for (RiskLevel level : RiskLevel.values()) {
                breakdown.put(level, 0L); // every level present, even at zero
            }
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder generatedAt(Instant generatedAt) {
            this.generatedAt = generatedAt;
            return this;
        }

        /** Fold one earthquake into the running totals. */
        public Builder addEarthquake(Earthquake e) {
            Objects.requireNonNull(e, "earthquake");
            count++;

            double magnitude = e.magnitude().value();
            sumMagnitude += magnitude;
            sumDepthKm += e.depthKm();
            if (magnitude > maxMagnitude) {
                maxMagnitude = magnitude;
            }
            if (magnitude > strongestMagnitude) {
                strongestMagnitude = magnitude;
                strongestId = e.id();
            }

            // only scored earthquakes contribute to the risk breakdown
            if (e.riskScore() != null) {
                breakdown.merge(e.riskScore().level(), 1L, Long::sum);
            }

            Instant when = e.occurredAt();
            if (earliest == null || when.isBefore(earliest)) {
                earliest = when;
            }
            if (latest == null || when.isAfter(latest)) {
                latest = when;
            }
            return this;
        }

        /** Convenience: fold a whole collection. */
        public Builder addAll(Iterable<Earthquake> earthquakes) {
            Objects.requireNonNull(earthquakes, "earthquakes");
            for (Earthquake e : earthquakes) {
                addEarthquake(e);
            }
            return this;
        }

        public SeismicReport build() {
            return new SeismicReport(this);
        }
    }
}
