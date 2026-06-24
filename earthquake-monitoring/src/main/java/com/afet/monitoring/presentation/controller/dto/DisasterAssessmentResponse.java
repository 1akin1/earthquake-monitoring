package com.afet.monitoring.presentation.controller.dto;

import com.afet.monitoring.domain.model.DisasterAssessment;

/** Outbound DTO. The domain assessment never leaks directly to HTTP. */
public record DisasterAssessmentResponse(String type, String riskLevel, String advisory) {

    public static DisasterAssessmentResponse from(DisasterAssessment a) {
        return new DisasterAssessmentResponse(a.type().name(), a.level().name(), a.advisory());
    }
}
