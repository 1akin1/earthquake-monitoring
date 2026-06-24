package com.afet.monitoring.domain.service.strategy;

import com.afet.monitoring.domain.model.*;
import com.afet.monitoring.domain.service.RiskScoringStrategy;

/** Magnitude 8.0+ — catastrophic. */
public class CriticalRiskStrategy implements RiskScoringStrategy {

    @Override
    public boolean supports(Magnitude magnitude) {
        return magnitude.value() >= 8.0;
    }

    @Override
    public RiskScore score(Earthquake e) {
        double depthFactor = Math.max(0.0, 1.0 - e.depthKm() / 100.0);
        double s = 85 + (e.magnitude().value() - 8.0) * 5.0 + depthFactor * 10.0;
        return new RiskScore(clamp(s), RiskLevel.CRITICAL);
    }
}
