package com.afet.monitoring.presentation.controller.dto;

import com.afet.monitoring.application.usecase.DetectionOutcome;
import com.afet.monitoring.domain.model.Earthquake;
import java.time.Instant;

/** Outbound DTO for a detection run. Earthquake fields are null when nothing detected. */
public record DetectionResponse(
        boolean detected,
        Double estimatedMagnitude,
        double peakAmplitude,
        double staLtaRatio,
        Instant triggeredAt,
        Long earthquakeId,
        String riskLevel) {

    public static DetectionResponse from(DetectionOutcome outcome) {
        var r = outcome.result();
        Earthquake e = outcome.earthquake();
        return new DetectionResponse(
                r.detected(),
                r.estimatedMagnitude(),
                r.peakAmplitude(),
                r.staLtaRatio(),
                r.triggeredAt(),
                e == null ? null : e.id(),
                e == null || e.riskScore() == null ? null : e.riskScore().level().name());
    }
}
