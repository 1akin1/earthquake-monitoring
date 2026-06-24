package com.afet.monitoring.domain.service;

import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.model.RiskScore;
import java.util.List;

/**
 * Selects, at runtime, the strategy whose band matches the earthquake's magnitude.
 * Pure domain — the strategy list is supplied from outside (DIP), so this class
 * has no idea how many strategies exist or how they are wired.
 */
public class RiskScoringService {

    private final List<RiskScoringStrategy> strategies;

    public RiskScoringService(List<RiskScoringStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }

    public RiskScore assess(Earthquake earthquake) {
        return strategies.stream()
                .filter(s -> s.supports(earthquake.magnitude()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No risk strategy for magnitude " + earthquake.magnitude().value()))
                .score(earthquake);
    }
}
