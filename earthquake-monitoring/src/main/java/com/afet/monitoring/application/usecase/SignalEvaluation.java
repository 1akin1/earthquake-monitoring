package com.afet.monitoring.application.usecase;

import com.afet.monitoring.domain.model.DetectionResult;
import com.afet.monitoring.domain.model.RiskLevel;

/**
 * Read-only counterpart to {@link DetectionOutcome}: the detection figures for one
 * signal plus the risk level it WOULD score — with nothing persisted or published.
 * Used by the batch "evaluate all" sweep so an operator can audit how the detector
 * reacts across every stored event without mutating any data. {@code riskLevel} is
 * null when nothing was detected (or the signal was rejected by validation).
 */
public record SignalEvaluation(DetectionResult result, RiskLevel riskLevel) {}
