package com.afet.platform.events;

import java.time.Instant;

/**
 * The canonical "earthquake detected and scored" event — the contract every service shares.
 *
 * <p>Field names and types are identical to the JSON the detection-service already
 * publishes, so consumers deserialize that JSON straight into this record (the consumer
 * Kafka config ignores the producer's type header and uses this as the default type). A
 * plain record with no Spring/JPA dependency, so it travels on the wire cleanly and the
 * services depend only on this tiny module — not on each other.
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
}
