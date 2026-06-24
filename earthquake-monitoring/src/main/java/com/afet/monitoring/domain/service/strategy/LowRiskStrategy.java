package com.afet.monitoring.domain.service.strategy;

import com.afet.monitoring.domain.model.*;
import com.afet.monitoring.domain.service.RiskScoringStrategy;

/** Magnitude < 4.0 — minor tremors. */
public class LowRiskStrategy implements RiskScoringStrategy {

    @Override
    public boolean supports(Magnitude magnitude) {
        return magnitude.value() < 4.0;
    }

    @Override
    public RiskScore score(Earthquake e) {
        double s = e.magnitude().value() * 5.0;          // ~0–20
        return new RiskScore(clamp(s), RiskLevel.LOW);
    }
}
