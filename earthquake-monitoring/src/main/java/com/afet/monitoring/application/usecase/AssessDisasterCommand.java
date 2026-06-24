package com.afet.monitoring.application.usecase;

import com.afet.monitoring.domain.model.DisasterType;

/** Input for assessing a disaster reading. Type is already parsed to the enum. */
public record AssessDisasterCommand(DisasterType type, double intensity) {}
