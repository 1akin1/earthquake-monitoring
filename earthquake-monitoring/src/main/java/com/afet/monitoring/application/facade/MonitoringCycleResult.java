package com.afet.monitoring.application.facade;

import com.afet.monitoring.application.usecase.ImportResult;
import com.afet.monitoring.domain.model.SeismicReport;

/**
 * Result of one monitoring cycle: what the import step persisted, plus the summary report
 * taken straight afterwards. Bundles two subsystems' outputs so the facade can return the
 * whole cycle in a single value.
 */
public record MonitoringCycleResult(ImportResult imported, SeismicReport report) {}
