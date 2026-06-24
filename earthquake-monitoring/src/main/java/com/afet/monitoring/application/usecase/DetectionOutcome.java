package com.afet.monitoring.application.usecase;

import com.afet.monitoring.domain.model.DetectionResult;
import com.afet.monitoring.domain.model.Earthquake;

/**
 * Result of a detection run: the raw detection figures plus, IF something was detected
 * and persisted, the resulting earthquake (else null).
 */
public record DetectionOutcome(DetectionResult result, Earthquake earthquake) {}
