package com.afet.monitoring.domain.service.disaster;

import com.afet.monitoring.domain.model.DisasterAssessment;
import com.afet.monitoring.domain.model.DisasterType;
import com.afet.monitoring.domain.model.RiskLevel;

/**
 * Handles EARTHQUAKE disasters. Here {@code intensity} is the Richter-scale magnitude
 * (0–10). Bands deliberately mirror the risk-scoring strategies so the platform stays
 * consistent across both patterns.
 */
public class EarthquakeDisasterHandler implements DisasterHandler {

    @Override
    public DisasterType type() {
        return DisasterType.EARTHQUAKE;
    }

    @Override
    public DisasterAssessment assess(double magnitude) {
        RiskLevel level;
        String advisory;
        if (magnitude < 4.0) {
            level = RiskLevel.LOW;
            advisory = "Minor tremor. No action required; log for seismic history.";
        } else if (magnitude < 6.0) {
            level = RiskLevel.MEDIUM;
            advisory = "Moderate quake. Inspect older structures and alert local authorities.";
        } else if (magnitude < 8.0) {
            level = RiskLevel.HIGH;
            advisory = "Destructive quake. Trigger regional alerts and dispatch response teams.";
        } else {
            level = RiskLevel.CRITICAL;
            advisory = "Catastrophic quake. Activate national emergency protocol immediately.";
        }
        return new DisasterAssessment(type(), level, advisory);
    }
}
