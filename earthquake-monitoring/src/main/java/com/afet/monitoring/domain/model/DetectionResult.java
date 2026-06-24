package com.afet.monitoring.domain.model;

import java.time.Instant;

/**
 * Value Object: the OUTPUT of running a detector over a {@link SeismicSignal}.
 * When {@code detected} is false, the magnitude/time fields are null and
 * {@code staLtaRatio} is the peak ratio observed (just below the trigger threshold).
 */
public record DetectionResult(
        boolean detected,
        Double estimatedMagnitude,   // null when not detected
        double peakAmplitude,
        double staLtaRatio,          // ratio at the trigger (or peak ratio seen)
        Instant triggeredAt) {       // null when not detected

    public static DetectionResult notDetected(double peakAmplitude, double peakRatio) {
        return new DetectionResult(false, null, peakAmplitude, peakRatio, null);
    }

    public static DetectionResult detected(double estimatedMagnitude, double peakAmplitude,
                                           double ratio, Instant triggeredAt) {
        return new DetectionResult(true, estimatedMagnitude, peakAmplitude, ratio, triggeredAt);
    }
}
