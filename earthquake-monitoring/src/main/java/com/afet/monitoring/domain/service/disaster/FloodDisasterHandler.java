package com.afet.monitoring.domain.service.disaster;

import com.afet.monitoring.domain.model.DisasterAssessment;
import com.afet.monitoring.domain.model.DisasterType;
import com.afet.monitoring.domain.model.RiskLevel;

/**
 * Handles FLOOD disasters. Here {@code intensity} is the water level above flood
 * stage, in metres. Completely different thresholds from earthquakes — which is the
 * whole point: same interface, type-specific logic, picked by the Factory.
 */
public class FloodDisasterHandler implements DisasterHandler {

    @Override
    public DisasterType type() {
        return DisasterType.FLOOD;
    }

    @Override
    public DisasterAssessment assess(double metresAboveFloodStage) {
        RiskLevel level;
        String advisory;
        if (metresAboveFloodStage < 1.0) {
            level = RiskLevel.LOW;
            advisory = "Water near flood stage. Monitor river gauges.";
        } else if (metresAboveFloodStage < 3.0) {
            level = RiskLevel.MEDIUM;
            advisory = "Localised flooding. Warn low-lying neighbourhoods.";
        } else if (metresAboveFloodStage < 5.0) {
            level = RiskLevel.HIGH;
            advisory = "Major flooding. Begin evacuation of riverside zones.";
        } else {
            level = RiskLevel.CRITICAL;
            advisory = "Extreme flooding. Full evacuation and dam-safety review required.";
        }
        return new DisasterAssessment(type(), level, advisory);
    }
}
