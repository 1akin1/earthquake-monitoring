package com.afet.monitoring.domain.event;

import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.model.RiskLevel;
import java.time.Instant;

/**
 * Domain event — "an earthquake was detected and scored". A pure record of simple
 * fields (no Spring, no JPA), so it doubles as the Kafka wire payload without dragging
 * the domain model onto the wire. This is the message every Observer reacts to.
 */
public record EarthquakeDetectedEvent(
        Long id,
        double magnitude,
        double depthKm,
        double latitude,
        double longitude,
        String source,
        Instant occurredAt,
        Double riskScore,
        RiskLevel riskLevel) {

    /** Build the event from a persisted, scored earthquake. */
    public static EarthquakeDetectedEvent from(Earthquake e) {
        return new EarthquakeDetectedEvent(
                e.id(), e.magnitude().value(), e.depthKm(),
                e.location().latitude(), e.location().longitude(),
                e.source(), e.occurredAt(),
                e.riskScore() == null ? null : e.riskScore().value(),
                e.riskScore() == null ? null : e.riskScore().level());
    }
}
