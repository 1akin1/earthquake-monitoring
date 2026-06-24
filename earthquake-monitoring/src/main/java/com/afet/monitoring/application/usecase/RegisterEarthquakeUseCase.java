package com.afet.monitoring.application.usecase;

import com.afet.monitoring.application.annotation.UseCase;
import com.afet.monitoring.domain.event.EarthquakeDetectedEvent;
import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.model.GeoLocation;
import com.afet.monitoring.domain.model.Magnitude;
import com.afet.monitoring.domain.model.RiskScore;
import com.afet.monitoring.domain.port.EarthquakeEventPublisherPort;
import com.afet.monitoring.domain.port.EarthquakeRepositoryPort;
import com.afet.monitoring.domain.service.RiskScoringService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the registration flow: build domain object -> score risk -> persist ->
 * publish event. Depends only on abstractions (ports + the domain service), injected
 * by constructor.
 */
@UseCase
public class RegisterEarthquakeUseCase {

    private final EarthquakeRepositoryPort repository;
    private final RiskScoringService riskScoringService;
    private final EarthquakeEventPublisherPort eventPublisher;

    public RegisterEarthquakeUseCase(EarthquakeRepositoryPort repository,
                                     RiskScoringService riskScoringService,
                                     EarthquakeEventPublisherPort eventPublisher) {
        this.repository = repository;
        this.riskScoringService = riskScoringService;
        this.eventPublisher = eventPublisher;
    }

    @CacheEvict(value = "earthquakeList", allEntries = true)  // a new row invalidates the list
    @Transactional
    public Earthquake handle(RegisterEarthquakeCommand command) {
        Earthquake earthquake = Earthquake.register(
                new Magnitude(command.magnitude()),
                command.depthKm(),
                new GeoLocation(command.latitude(), command.longitude()),
                command.source(),
                command.occurredAt());

        RiskScore risk = riskScoringService.assess(earthquake);   // Strategy selected at runtime
        Earthquake saved = repository.save(earthquake.assessedWith(risk));

        // Observer fan-out: alert/report/user consumers react independently.
        // NOTE: published inside the tx for simplicity; production would publish AFTER
        // commit (transactional outbox / @TransactionalEventListener) to avoid phantom
        // events if the tx rolls back.
        eventPublisher.publishDetected(EarthquakeDetectedEvent.from(saved));
        return saved;
    }
}
