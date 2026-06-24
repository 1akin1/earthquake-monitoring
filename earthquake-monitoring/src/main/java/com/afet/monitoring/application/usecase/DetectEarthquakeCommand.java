package com.afet.monitoring.application.usecase;

import java.time.Instant;

/** Input for analysing a raw signal window from one station. */
public record DetectEarthquakeCommand(
        String stationId,
        double latitude,
        double longitude,
        double sampleRateHz,
        Instant startTime,
        double[] amplitudes) {}
