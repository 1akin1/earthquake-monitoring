package com.afet.monitoring.presentation.controller.dto;

import com.afet.monitoring.application.usecase.ImportResult;
import java.util.Map;

/** Outbound DTO for an import run. */
public record ImportResultResponse(int imported, Map<String, Integer> bySource) {

    public static ImportResultResponse from(ImportResult r) {
        return new ImportResultResponse(r.imported(), r.bySource());
    }
}
