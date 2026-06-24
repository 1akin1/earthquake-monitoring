package com.afet.monitoring.presentation.controller.dto;

import com.afet.monitoring.domain.model.Earthquake;
import java.time.Instant;

/** Outbound DTO. The domain model never leaks directly to HTTP. */
public record EarthquakeResponse(
        Long id,
        double magnitude,
        double depthKm,
        double latitude,
        double longitude,
        String source,
        Instant occurredAt,
        Double riskScore,
        String riskLevel) {

    public static EarthquakeResponse from(Earthquake e) {
        return new EarthquakeResponse(
                e.id(), e.magnitude().value(), e.depthKm(),
                e.location().latitude(), e.location().longitude(),
                e.source(), e.occurredAt(),
                e.riskScore() == null ? null : e.riskScore().value(),
                e.riskScore() == null ? null : e.riskScore().level().name());
    }
}
