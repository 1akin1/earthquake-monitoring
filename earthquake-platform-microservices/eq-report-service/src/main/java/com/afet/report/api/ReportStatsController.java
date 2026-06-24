package com.afet.report.api;

import com.afet.report.stats.ReportStatistics;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Exposes the running statistics this service has accumulated from the event stream. */
@RestController
@RequestMapping("/api/report-stats")
public class ReportStatsController {
    private final ReportStatistics statistics;
    public ReportStatsController(ReportStatistics statistics) { this.statistics = statistics; }

    @GetMapping
    public Map<String, Object> stats() {
        return Map.of(
                "total", statistics.total(),
                "maxMagnitude", statistics.maxMagnitude(),
                "byRiskLevel", statistics.byRiskLevel());
    }
}
