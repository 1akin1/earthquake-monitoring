package com.afet.monitoring.domain.service.disaster;

import com.afet.monitoring.domain.model.DisasterAssessment;
import com.afet.monitoring.domain.model.DisasterType;

/**
 * The product the Factory builds. Each implementation owns ONE disaster type and
 * knows how to interpret that type's primary intensity metric (which means something
 * different per type — see each handler). Pure domain: no Spring, no JPA.
 *
 * <p>{@link #supports(DisasterType)} lets each handler self-identify, so the factory
 * never needs a switch statement — adding a type is a new class, not an edit (OCP).
 */
public interface DisasterHandler {

    /** Which disaster type this handler is responsible for. */
    DisasterType type();

    /** True if this handler can process the given type. */
    default boolean supports(DisasterType type) {
        return type() == type;
    }

    /**
     * Evaluate a reading and return a severity assessment. The meaning of
     * {@code intensity} is type-specific (magnitude, water level, burned area, …)
     * and documented on each implementation.
     */
    DisasterAssessment assess(double intensity);
}
