package com.afet.monitoring.presentation.controller;

import com.afet.monitoring.application.facade.DisasterFacade;
import com.afet.monitoring.presentation.controller.dto.MonitoringCycleResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Thin adapter over the {@link DisasterFacade}. */
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    private final DisasterFacade facade;

    public MonitoringController(DisasterFacade facade) {
        this.facade = facade;
    }

    /** Run a full monitoring cycle (import every feed, then summarise) in one call. */
    @PostMapping("/cycle")
    public MonitoringCycleResponse runCycle() {
        return MonitoringCycleResponse.from(facade.runMonitoringCycle());
    }
}
