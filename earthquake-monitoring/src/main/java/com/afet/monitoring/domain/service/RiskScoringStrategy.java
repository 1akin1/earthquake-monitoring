package com.afet.monitoring.domain.service;

import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.model.Magnitude;
import com.afet.monitoring.domain.model.RiskScore;

/**
 * Strategy Pattern. Each implementation owns ONE magnitude band and its own
 * scoring formula. Adding a band means adding a class (OCP) — no existing code changes.
 */
public interface RiskScoringStrategy {
    boolean supports(Magnitude magnitude);
    RiskScore score(Earthquake earthquake);

    /** Shared helper so every formula stays inside 0–100. */
    default double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }
}
