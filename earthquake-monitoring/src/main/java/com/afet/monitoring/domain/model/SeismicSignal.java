package com.afet.monitoring.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Value Object: a window of raw ground-motion samples from one station — the INPUT to
 * detection (before we know whether it contains an earthquake at all).
 *
 * @param stationId    the reporting station, e.g. "STA-YLV"
 * @param location     where the station is
 * @param sampleRateHz samples per second (e.g. 100 Hz)
 * @param startTime    timestamp of the first sample (UTC)
 * @param amplitudes   ground-motion samples (counts / velocity); sign ignored by the detector
 */
public record SeismicSignal(
        String stationId,
        GeoLocation location,
        double sampleRateHz,
        Instant startTime,
        double[] amplitudes) {

    public SeismicSignal {
        if (stationId == null || stationId.isBlank()) {
            throw new IllegalArgumentException("stationId is required");
        }
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(startTime, "startTime");
        if (sampleRateHz <= 0) {
            throw new IllegalArgumentException("sampleRateHz must be positive: " + sampleRateHz);
        }
        if (amplitudes == null || amplitudes.length == 0) {
            throw new IllegalArgumentException("amplitudes must not be empty");
        }
    }

    public int length() {
        return amplitudes.length;
    }
}
