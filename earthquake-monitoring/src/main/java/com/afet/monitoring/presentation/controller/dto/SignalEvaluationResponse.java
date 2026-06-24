package com.afet.monitoring.presentation.controller.dto;

import com.afet.monitoring.application.usecase.SignalEvaluation;
import com.afet.monitoring.domain.model.DetectionResult;

/**
 * One row of a batch evaluation — mirrors {@link DetectionResponse} minus the
 * earthquake id (the sweep never creates one). {@code riskLevel} is null when the
 * signal was not detected or was rejected by the validation chain.
 */
public record SignalEvaluationResponse(
        boolean detected,
        Double estimatedMagnitude,
        double peakAmplitude,
        double staLtaRatio,
        String riskLevel) {

    public static SignalEvaluationResponse from(SignalEvaluation evaluation) {
        DetectionResult r = evaluation.result();
        return new SignalEvaluationResponse(
                r.detected(),
                r.estimatedMagnitude(),
                r.peakAmplitude(),
                r.staLtaRatio(),
                evaluation.riskLevel() == null ? null : evaluation.riskLevel().name());
    }
}
