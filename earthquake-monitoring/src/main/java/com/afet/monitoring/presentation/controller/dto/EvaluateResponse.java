package com.afet.monitoring.presentation.controller.dto;

import com.afet.monitoring.application.usecase.SignalEvaluation;
import java.util.List;

/**
 * Outbound DTO for a batch sweep: per-signal verdicts plus a small aggregate so the
 * client can show "{detected} of {total} would trigger" without recounting.
 */
public record EvaluateResponse(
        int total,
        int detected,
        List<SignalEvaluationResponse> results) {

    public static EvaluateResponse from(List<SignalEvaluation> evaluations) {
        List<SignalEvaluationResponse> rows = evaluations.stream()
                .map(SignalEvaluationResponse::from)
                .toList();
        int detected = (int) rows.stream().filter(SignalEvaluationResponse::detected).count();
        return new EvaluateResponse(rows.size(), detected, rows);
    }
}
