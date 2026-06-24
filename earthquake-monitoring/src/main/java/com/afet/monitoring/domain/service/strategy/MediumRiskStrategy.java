package com.afet.monitoring.domain.service.strategy;

import com.afet.monitoring.domain.model.*;
import com.afet.monitoring.domain.service.RiskScoringStrategy;

/** Magnitude 4.0–5.9 — felt widely; depth starts to matter. */
public class MediumRiskStrategy implements RiskScoringStrategy {

    @Override
    public boolean supports(Magnitude magnitude) {
        double v = magnitude.value();
        return v >= 4.0 && v < 6.0;
    }

    @Override
    public RiskScore score(Earthquake e) {
        double depthFactor = Math.max(0.0, 1.0 - e.depthKm() / 100.0); // shallow -> 1
        double s = 20 + e.magnitude().value() * 5.0 + depthFactor * 10.0;
        return new RiskScore(clamp(s), RiskLevel.MEDIUM);
    }
}
