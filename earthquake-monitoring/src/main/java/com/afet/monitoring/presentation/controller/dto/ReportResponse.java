package com.afet.monitoring.presentation.controller.dto;

import com.afet.monitoring.domain.model.RiskLevel;
import com.afet.monitoring.domain.model.SeismicReport;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** API view of a {@link SeismicReport}. */
public record ReportResponse(
        String title,
        Instant generatedAt,
        int totalEarthquakes,
        double maxMagnitude,
        double averageMagnitude,
        double averageDepthKm,
        Map<String, Long> riskBreakdown,
        Instant earliest,
        Instant latest,
        Long strongestEarthquakeId) {

    public static ReportResponse from(SeismicReport r) {
        Map<String, Long> breakdown = new LinkedHashMap<>();
        for (RiskLevel level : RiskLevel.values()) {
            breakdown.put(level.name(), r.riskBreakdown().getOrDefault(level, 0L));
        }
        return new ReportResponse(
                r.title(), r.generatedAt(), r.totalEarthquakes(),
                round(r.maxMagnitude()), round(r.averageMagnitude()), round(r.averageDepthKm()),
                breakdown, r.earliest(), r.latest(), r.strongestEarthquakeId());
    }

    /** Round to 2 decimals for a tidy API payload. */
    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
