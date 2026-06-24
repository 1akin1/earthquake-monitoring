package com.afet.monitoring.presentation.controller.dto;

import java.util.Map;

/** Snapshot of the report consumer's statistics. */
public record StatsResponse(long totalEvents, double maxMagnitude, Map<String, Long> byRiskLevel) {}
