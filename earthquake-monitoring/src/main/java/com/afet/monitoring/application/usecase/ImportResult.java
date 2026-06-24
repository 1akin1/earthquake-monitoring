package com.afet.monitoring.application.usecase;

import java.util.Map;

/** Summary of an import run: total persisted plus a per-source breakdown. */
public record ImportResult(int imported, Map<String, Integer> bySource) {}
