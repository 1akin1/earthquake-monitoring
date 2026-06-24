package com.afet.monitoring.domain.service.strategy;

import com.afet.monitoring.domain.model.*;
import com.afet.monitoring.domain.service.RiskScoringStrategy;

/** Magnitude 6.0–7.9 — destructive; shallow depth sharply raises risk. */
public class HighRiskStrategy implements RiskScoringStrategy {

    @Override
    public boolean supports(Magnitude magnitude) {
        double v = magnitude.value();
        return v >= 6.0 && v < 8.0;
    }

    @Override
    public RiskScore score(Earthquake e) {
        double depthFactor = Math.max(0.0, 1.0 - e.depthKm() / 100.0);
        double s = 50 + (e.magnitude().value() - 6.0) * 10.0 + depthFactor * 15.0;
        return new RiskScore(clamp(s), RiskLevel.HIGH);
    }
}
