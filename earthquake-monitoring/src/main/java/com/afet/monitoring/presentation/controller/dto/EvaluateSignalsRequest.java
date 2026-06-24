package com.afet.monitoring.presentation.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Inbound DTO: a batch of signal windows to evaluate read-only (no persistence).
 * Each element reuses the same {@link AnalyzeSignalRequest} contract as the single-shot
 * {@code /api/detection/analyze} route, so the client builds them identically.
 */
public record EvaluateSignalsRequest(
        @NotEmpty @Valid List<AnalyzeSignalRequest> signals) {}
