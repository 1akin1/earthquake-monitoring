package com.afet.monitoring.application.usecase;

import com.afet.monitoring.application.annotation.UseCase;
import com.afet.monitoring.domain.event.EarthquakeDetectedEvent;
import com.afet.monitoring.domain.model.DetectionResult;
import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.model.GeoLocation;
import com.afet.monitoring.domain.model.Magnitude;
import com.afet.monitoring.domain.model.SeismicSignal;
import com.afet.monitoring.domain.port.EarthquakeEventPublisherPort;
import com.afet.monitoring.domain.port.EarthquakeRepositoryPort;
import com.afet.monitoring.domain.service.RiskScoringService;
import com.afet.monitoring.domain.service.detection.EarthquakeDetector;
import com.afet.monitoring.domain.service.validation.SignalValidator;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

/**
 * Detection is the front door of the whole pipeline:
 * raw signal -&gt; <b>detect</b> (STA/LTA) -&gt; <b>score</b> (Strategy) -&gt;
 * <b>persist</b> -&gt; <b>publish</b> (Observer). If nothing is detected, nothing is
 * saved and no event is fired.
 */
@UseCase
public class DetectEarthquakeUseCase {

    /** Single-station signals can't resolve depth; assume a shallow default. */
    private static final double DEFAULT_DEPTH_KM = 10.0;

    private final EarthquakeDetector detector;
    private final SignalValidator signalValidationChain;
    private final RiskScoringService riskScoringService;
    private final EarthquakeRepositoryPort repository;
    private final EarthquakeEventPublisherPort eventPublisher;

    public DetectEarthquakeUseCase(EarthquakeDetector detector,
                                   SignalValidator signalValidationChain,
                                   RiskScoringService riskScoringService,
                                   EarthquakeRepositoryPort repository,
                                   EarthquakeEventPublisherPort eventPublisher) {
        this.detector = detector;
        this.signalValidationChain = signalValidationChain;
        this.riskScoringService = riskScoringService;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @CacheEvict(value = "earthquakeList", allEntries = true)
    @Transactional
    public DetectionOutcome handle(DetectEarthquakeCommand command) {
        SeismicSignal signal = new SeismicSignal(
                command.stationId(),
                new GeoLocation(command.latitude(), command.longitude()),
                command.sampleRateHz(),
                command.startTime(),
                command.amplitudes());

        // Chain of Responsibility: reject unusable signals (non-finite, too short,
        // dead channel, clipped) before spending detection effort on them.
        signalValidationChain.validate(signal);

        DetectionResult result = detector.analyze(signal);
        if (!result.detected()) {
            return new DetectionOutcome(result, null);   // just noise — nothing to record
        }

        Earthquake earthquake = Earthquake.register(
                new Magnitude(result.estimatedMagnitude()),
                DEFAULT_DEPTH_KM,
                signal.location(),
                signal.stationId(),                       // source = the detecting station
                result.triggeredAt());

        Earthquake saved = repository.save(
                earthquake.assessedWith(riskScoringService.assess(earthquake)));
        eventPublisher.publishDetected(EarthquakeDetectedEvent.from(saved));

        return new DetectionOutcome(result, saved);
    }
}
