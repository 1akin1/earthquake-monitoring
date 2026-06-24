package com.afet.monitoring.domain.service.disaster;

import com.afet.monitoring.domain.model.DisasterAssessment;
import com.afet.monitoring.domain.model.DisasterType;
import com.afet.monitoring.domain.model.RiskLevel;

/**
 * Handles WILDFIRE disasters. Here {@code intensity} is the burned area in km².
 * Yet another scale and another set of thresholds, behind the same handler contract.
 */
public class WildfireDisasterHandler implements DisasterHandler {

    @Override
    public DisasterType type() {
        return DisasterType.WILDFIRE;
    }

    @Override
    public DisasterAssessment assess(double burnedAreaKm2) {
        RiskLevel level;
        String advisory;
        if (burnedAreaKm2 < 10.0) {
            level = RiskLevel.LOW;
            advisory = "Small fire. Ground crews can contain it.";
        } else if (burnedAreaKm2 < 100.0) {
            level = RiskLevel.MEDIUM;
            advisory = "Spreading fire. Deploy aerial support and pre-position resources.";
        } else if (burnedAreaKm2 < 500.0) {
            level = RiskLevel.HIGH;
            advisory = "Large wildfire. Evacuate threatened settlements; request mutual aid.";
        } else {
            level = RiskLevel.CRITICAL;
            advisory = "Megafire. Declare disaster zone and coordinate national resources.";
        }
        return new DisasterAssessment(type(), level, advisory);
    }
}
