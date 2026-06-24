package com.afet.monitoring.application.usecase;

import java.time.Instant;

/** Input data for registering an earthquake. Primitives in, domain converts to VOs. */
public record RegisterEarthquakeCommand(
        double magnitude,
        double depthKm,
        double latitude,
        double longitude,
        String source,
        Instant occurredAt) {}
