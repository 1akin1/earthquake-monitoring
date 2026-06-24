package com.afet.monitoring.application.usecase;

import com.afet.monitoring.application.annotation.UseCase;
import com.afet.monitoring.domain.model.DisasterAssessment;
import com.afet.monitoring.domain.service.disaster.DisasterHandlerFactory;

/**
 * Assesses a disaster reading: the Factory picks the right handler at runtime, the
 * handler does the type-specific evaluation.
 *
 * <p>Note: no {@code @Transactional} here — this is a pure computation with no DB
 * write, so there's no transaction boundary to open. (Contrast with
 * RegisterEarthquakeUseCase, which persists and therefore is transactional.)
 */
@UseCase
public class AssessDisasterUseCase {

    private final DisasterHandlerFactory factory;

    public AssessDisasterUseCase(DisasterHandlerFactory factory) {
        this.factory = factory;
    }

    public DisasterAssessment handle(AssessDisasterCommand command) {
        return factory.create(command.type()).assess(command.intensity());
    }
}
