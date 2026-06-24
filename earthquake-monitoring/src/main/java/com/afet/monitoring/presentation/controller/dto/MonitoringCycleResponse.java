package com.afet.monitoring.presentation.controller.dto;

import com.afet.monitoring.application.facade.MonitoringCycleResult;

/** API view of one monitoring cycle: the import summary plus the report taken afterwards. */
public record MonitoringCycleResponse(ImportResultResponse imported, ReportResponse report) {

    public static MonitoringCycleResponse from(MonitoringCycleResult r) {
        return new MonitoringCycleResponse(
                ImportResultResponse.from(r.imported()),
                ReportResponse.from(r.report()));
    }
}
