package com.afet.monitoring.presentation.controller;

import org.springframework.context.annotation.Profile;
import com.afet.monitoring.infrastructure.messaging.ReportStatistics;
import com.afet.monitoring.presentation.controller.dto.StatsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the statistics that the report Kafka consumer builds up — so the Observer
 * fan-out is visible over HTTP: publish events, then watch these numbers change.
 */
@Profile("!platform") // consumer-side reaction; in the platform it lives in its own service
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final ReportStatistics statistics;

    public StatsController(ReportStatistics statistics) {
        this.statistics = statistics;
    }

    @GetMapping
    public StatsResponse stats() {
        return new StatsResponse(
                statistics.total(),
                statistics.maxMagnitude(),
                statistics.byRiskLevel());
    }
}
