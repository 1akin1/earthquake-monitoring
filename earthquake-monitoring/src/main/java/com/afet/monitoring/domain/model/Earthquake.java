package com.afet.monitoring.domain.model;

import java.time.Instant;
import java.io.Serializable;
import java.util.Objects;

/**
 * Domain Entity (has identity). Pure Java — no Spring, no JPA. Immutable: assigning
 * a risk score produces a NEW instance rather than mutating this one.
 *
 * <p>Implements {@link Serializable} (a JDK interface, not a framework type, so the
 * domain stays framework-free) purely so cached instances can be stored in Redis.
 */
public class Earthquake implements Serializable {

    private final Long id;
    private final Magnitude magnitude;
    private final double depthKm;
    private final GeoLocation location;
    private final String source;
    private final Instant occurredAt;
    private final RiskScore riskScore; // null until the earthquake has been assessed

    private Earthquake(Long id, Magnitude magnitude, double depthKm, GeoLocation location,
                       String source, Instant occurredAt, RiskScore riskScore) {
        if (depthKm < 0) {
            throw new IllegalArgumentException("Depth cannot be negative: " + depthKm);
        }
        this.id = id;
        this.magnitude = Objects.requireNonNull(magnitude, "magnitude");
        this.depthKm = depthKm;
        this.location = Objects.requireNonNull(location, "location");
        this.source = Objects.requireNonNull(source, "source");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.riskScore = riskScore; // may be null
    }

    /** A brand-new, unscored earthquake (no id, no risk yet). */
    public static Earthquake register(Magnitude magnitude, double depthKm,
                                      GeoLocation location, String source, Instant occurredAt) {
        return new Earthquake(null, magnitude, depthKm, location, source, occurredAt, null);
    }

    /** Rebuild from persistence (id and possibly a stored risk score). */
    public static Earthquake reconstitute(Long id, Magnitude magnitude, double depthKm,
                                          GeoLocation location, String source,
                                          Instant occurredAt, RiskScore riskScore) {
        return new Earthquake(id, magnitude, depthKm, location, source, occurredAt, riskScore);
    }

    /** Returns a new immutable copy carrying the assigned risk score. */
    public Earthquake assessedWith(RiskScore riskScore) {
        return new Earthquake(id, magnitude, depthKm, location, source, occurredAt,
                Objects.requireNonNull(riskScore, "riskScore"));
    }

    public Long id() { return id; }
    public Magnitude magnitude() { return magnitude; }
    public double depthKm() { return depthKm; }
    public GeoLocation location() { return location; }
    public String source() { return source; }
    public Instant occurredAt() { return occurredAt; }
    public RiskScore riskScore() { return riskScore; } // may be null
}
